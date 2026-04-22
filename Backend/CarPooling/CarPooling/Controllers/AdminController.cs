using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Admin")]
public class AdminController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet("users")]
    public async Task<ActionResult<IReadOnlyList<UserResponseDto>>> GetAllUsersAsync(
        [FromQuery] string? name,
        [FromQuery] string? email)
    {
        var usersQuery = _context.Users
            .Include(u => u.DriverProfile)
            .AsNoTracking();

        if (!string.IsNullOrWhiteSpace(name))
        {
            var normalizedName = name.Trim();
            usersQuery = usersQuery.Where(u => u.FullName.Contains(normalizedName));
        }

        if (!string.IsNullOrWhiteSpace(email))
        {
            var normalizedEmail = email.Trim().ToLowerInvariant();
            usersQuery = usersQuery.Where(u => u.Email.Contains(normalizedEmail));
        }

        var users = await usersQuery
            .OrderByDescending(u => u.CreatedAt)
            .ToListAsync();

        return Ok(users.Select(UserResponseDto.FromEntity).ToList());
    }

    [HttpPut("users/{id:guid}")]
    public async Task<ActionResult<UserResponseDto>> UpdateUserAsync(Guid id, [FromBody] AdminUpdateUserDto dto)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .FirstOrDefaultAsync(u => u.Id == id);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        if (!IsUniversityEmail(normalizedEmail))
        {
            return BadRequest("Solo se permiten correos institucionales @univalle.edu");
        }

        var emailBelongsToAnotherUser = await _context.Users.AnyAsync(u => u.Email == normalizedEmail && u.Id != id);
        if (emailBelongsToAnotherUser)
        {
            return Conflict("El email ingresado ya está en uso por otro usuario.");
        }

        if (!TryParseAdminRoleId(dto.RoleId, out var role))
        {
            return BadRequest("RoleId invalido. Usa 1 (student), 2 (driver) o 3 (admin).");
        }

        user.FullName = dto.FullName.Trim();
        user.Email = normalizedEmail;
        user.PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim();
        user.Role = role;

        if (role == UserRole.Driver)
        {
            if (user.DriverProfile is null)
            {
                user.DriverProfile = new DriverProfile
                {
                    UserId = user.Id,
                    AvailableSeats = 3,
                    LicensePlate = "TEMP-000",
                    VehicleBrand = "Por definir",
                    VehicleColor = "Por definir"
                };
            }
        }
        else if (user.DriverProfile is not null)
        {
            _context.DriverProfiles.Remove(user.DriverProfile);
            user.DriverProfile = null;
        }

        await _context.SaveChangesAsync();

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpDelete("users/{id:guid}")]
    public async Task<IActionResult> DeleteUserAsync(Guid id)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .FirstOrDefaultAsync(u => u.Id == id);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        _context.Users.Remove(user);
        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpGet("trips")]
    public async Task<ActionResult<IReadOnlyList<TripResponse>>> GetAllTripsAsync()
    {
        var trips = await _context.Trips
            .AsNoTracking()
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        return Ok(trips.Select(TripResponse.FromEntity).ToList());
    }

    [HttpPut("trips/{id:guid}")]
    public async Task<ActionResult<TripResponse>> UpdateTripAsync(Guid id, [FromBody] AdminUpdateTripDto dto)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        if (!TryParseTripStatus(dto.Status, out var status))
        {
            return BadRequest("Estado de viaje invalido.");
        }

        if (dto.AvailableSeats < 0)
        {
            return BadRequest("AvailableSeats no puede ser negativo.");
        }

        if (dto.OriginLatitude is < -90 or > 90 || dto.OriginLongitude is < -180 or > 180)
        {
            return BadRequest("Coordenadas de origen invalidas.");
        }

        if (dto.DestinationLatitude is not null && (dto.DestinationLatitude < -90 || dto.DestinationLatitude > 90))
        {
            return BadRequest("Latitud de destino invalida.");
        }

        if (dto.DestinationLongitude is not null && (dto.DestinationLongitude < -180 || dto.DestinationLongitude > 180))
        {
            return BadRequest("Longitud de destino invalida.");
        }

        trip.DriverName = dto.DriverName.Trim();
        trip.DriverUserId = dto.DriverUserId;
        trip.OriginLatitude = dto.OriginLatitude;
        trip.OriginLongitude = dto.OriginLongitude;
        trip.DestinationLatitude = dto.DestinationLatitude;
        trip.DestinationLongitude = dto.DestinationLongitude;
        trip.AvailableSeats = dto.AvailableSeats;
        trip.Status = status;
        trip.UpdatedAt = DateTime.UtcNow;
        trip.CancelledAt = status == TripStatus.Cancelled ? DateTime.UtcNow : null;

        await _context.SaveChangesAsync();

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpDelete("trips/{id:guid}")]
    public async Task<IActionResult> DeleteTripAsync(Guid id)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        var linkedReservations = await _context.Reservations.AnyAsync(r => r.TripId == id);
        if (linkedReservations)
        {
            return BadRequest("No se puede eliminar el viaje porque tiene reservas asociadas.");
        }

        _context.Trips.Remove(trip);
        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpGet("reservations")]
    public async Task<ActionResult<IReadOnlyList<ReservationDto>>> GetAllReservationsAsync()
    {
        var reservations = await _context.Reservations
            .AsNoTracking()
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new ReservationDto
            {
                Id = r.Id,
                TripId = r.TripId,
                PassengerName = r.PassengerName,
                Status = r.Status.ToString(),
                CreatedAt = r.CreatedAt
            })
            .ToListAsync();

        return Ok(reservations);
    }

    [HttpPut("reservations/{id:guid}")]
    public async Task<ActionResult<ReservationDto>> UpdateReservationAsync(Guid id, [FromBody] AdminUpdateReservationDto dto)
    {
        var reservation = await _context.Reservations.FirstOrDefaultAsync(r => r.Id == id);
        if (reservation is null)
        {
            return NotFound("Reserva no encontrada.");
        }

        if (!TryParseReservationStatus(dto.Status, out var status))
        {
            return BadRequest("Estado de reserva invalido.");
        }

        var targetTripExists = await _context.Trips.AnyAsync(t => t.Id == dto.TripId);
        if (!targetTripExists)
        {
            return BadRequest("TripId no corresponde a un viaje existente.");
        }

        reservation.PassengerName = dto.PassengerName.Trim();
        reservation.TripId = dto.TripId;
        reservation.Status = status;

        await _context.SaveChangesAsync();

        return Ok(new ReservationDto
        {
            Id = reservation.Id,
            TripId = reservation.TripId,
            PassengerName = reservation.PassengerName,
            Status = reservation.Status.ToString(),
            CreatedAt = reservation.CreatedAt
        });
    }

    [HttpDelete("reservations/{id:guid}")]
    public async Task<IActionResult> DeleteReservationAsync(Guid id)
    {
        var reservation = await _context.Reservations.FirstOrDefaultAsync(r => r.Id == id);
        if (reservation is null)
        {
            return NotFound("Reserva no encontrada.");
        }

        _context.Reservations.Remove(reservation);
        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpGet("all-data")]
    public async Task<ActionResult<object>> GetAllDataAsync()
    {
        var users = await _context.Users
            .Include(u => u.DriverProfile)
            .AsNoTracking()
            .OrderByDescending(u => u.CreatedAt)
            .ToListAsync();

        var trips = await _context.Trips
            .AsNoTracking()
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        var reservations = await _context.Reservations
            .AsNoTracking()
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new ReservationDto
            {
                Id = r.Id,
                TripId = r.TripId,
                PassengerName = r.PassengerName,
                Status = r.Status.ToString(),
                CreatedAt = r.CreatedAt
            })
            .ToListAsync();

        return Ok(new
        {
            users = users.Select(UserResponseDto.FromEntity).ToList(),
            trips = trips.Select(TripResponse.FromEntity).ToList(),
            reservations
        });
    }

    private static bool IsUniversityEmail(string email)
    {
        return email.EndsWith("@univalle.edu", StringComparison.OrdinalIgnoreCase);
    }

    private static bool TryParseAdminRoleId(int roleId, out UserRole role)
    {
        role = roleId switch
        {
            1 => UserRole.Student,
            2 => UserRole.Driver,
            3 => UserRole.Admin,
            _ => UserRole.Student
        };

        return roleId is 1 or 2 or 3;
    }

    private static bool TryParseTripStatus(string value, out TripStatus status)
    {
        var normalized = value.Trim().ToLowerInvariant();

        if (normalized is "0" or "awaitingdestination" or "awaiting_destination" or "activo" or "pending")
        {
            status = TripStatus.AwaitingDestination;
            return true;
        }

        if (normalized is "1" or "ready" or "listo")
        {
            status = TripStatus.Ready;
            return true;
        }

        if (normalized is "2" or "cancelled" or "cancelado")
        {
            status = TripStatus.Cancelled;
            return true;
        }

        if (normalized is "3" or "inprogress" or "in_progress" or "en_curso")
        {
            status = TripStatus.InProgress;
            return true;
        }

        if (normalized is "4" or "finished" or "finalizado")
        {
            status = TripStatus.Finished;
            return true;
        }

        status = TripStatus.AwaitingDestination;
        return false;
    }

    private static bool TryParseReservationStatus(string value, out ReservationStatus status)
    {
        var normalized = value.Trim().ToLowerInvariant();

        if (normalized is "0" or "active" or "activo")
        {
            status = ReservationStatus.Active;
            return true;
        }

        if (normalized is "1" or "cancelled" or "cancelado")
        {
            status = ReservationStatus.Cancelled;
            return true;
        }

        if (normalized is "2" or "boarded" or "abordado")
        {
            status = ReservationStatus.Boarded;
            return true;
        }

        status = ReservationStatus.Active;
        return false;
    }

    public sealed class AdminUpdateUserDto
    {
        public string FullName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? PhoneNumber { get; set; }
        public int RoleId { get; set; }
    }

    public sealed class AdminUpdateTripDto
    {
        public string DriverName { get; set; } = string.Empty;
        public Guid? DriverUserId { get; set; }
        public double OriginLatitude { get; set; }
        public double OriginLongitude { get; set; }
        public double? DestinationLatitude { get; set; }
        public double? DestinationLongitude { get; set; }
        public int AvailableSeats { get; set; }
        public string Status { get; set; } = string.Empty;
    }

    public sealed class AdminUpdateReservationDto
    {
        public Guid TripId { get; set; }
        public string PassengerName { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
    }
}