using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class AdminController(CarPoolingContext context, SupportTicketService supportTicketService) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private readonly SupportTicketService _supportTicketService = supportTicketService;

    [HttpGet("users")]
    [RequirePermission(AppPermissions.ReadUsers)]
    public async Task<ActionResult<IReadOnlyList<UserResponseDto>>> GetAllUsersAsync(
        [FromQuery] string? name,
        [FromQuery] string? email)
    {
        var usersQuery = _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
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
    [RequirePermission(AppPermissions.WriteUsers)]
    public async Task<ActionResult<UserResponseDto>> UpdateUserAsync(Guid id, [FromBody] AdminUpdateUserDto dto)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
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
            return Conflict("El email ingresado ya esta en uso por otro usuario.");
        }

        Role? targetRole = null;
        if (dto.CustomRoleId.HasValue && dto.CustomRoleId.Value != Guid.Empty)
        {
            targetRole = await _context.Roles.FirstOrDefaultAsync(r => r.Id == dto.CustomRoleId.Value);
        }
        else
        {
            string roleName = dto.RoleId switch
            {
                1 => "Student",
                2 => "Driver",
                3 => "SuperAdmin",
                _ => "Student"
            };
            targetRole = await _context.Roles.FirstOrDefaultAsync(r => r.Name == roleName);
        }

        if (targetRole == null)
        {
            return BadRequest("El Rol especificado no existe.");
        }

        user.FullName = dto.FullName.Trim();
        user.Email = normalizedEmail;
        user.PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim();

        // Clear existing and assign new role
        var oldUserRoles = user.UserRoles.ToList();
        foreach (var ur in oldUserRoles)
        {
            _context.UserRoles.Remove(ur);
        }
        _context.UserRoles.Add(new UserRole { UserId = user.Id, RoleId = targetRole.Id });

        if (targetRole.Name == "Driver")
        {
            if (user.DriverProfile is null)
            {
                user.DriverProfile = new DriverProfile
                {
                    UserId = user.Id,
                    IsVerified = false
                };

                _context.Vehicles.Add(new Vehicle
                {
                    OwnerUserId = user.Id,
                    LicensePlate = "TEMP-000",
                    Brand = "Por definir",
                    Color = "Por definir",
                    TotalSeats = 3,
                    IsActive = true
                });
            }
        }
        else if (user.DriverProfile is not null)
        {
            _context.DriverProfiles.Remove(user.DriverProfile);
            user.DriverProfile = null;
        }

        await _context.SaveChangesAsync();

        // Force load vehicles for UserResponseDto
        await _context.Entry(user).Collection(u => u.Vehicles).LoadAsync();

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpDelete("users/{id:guid}")]
    [RequirePermission(AppPermissions.DeleteUsers)]
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
    [RequirePermission(AppPermissions.ReadTrips)]
    public async Task<ActionResult<IReadOnlyList<TripResponse>>> GetAllTripsAsync()
    {
        var trips = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        return Ok(trips.Select(TripService.MapToDto).ToList());
    }

    [HttpPut("trips/{id:guid}")]
    [RequirePermission(AppPermissions.WriteTrips)]
    public async Task<ActionResult<TripResponse>> UpdateTripAsync(Guid id, [FromBody] AdminUpdateTripDto dto)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        if (dto.DriverUserId is not null)
        {
            var driverExists = await _context.Users.AnyAsync(u => u.Id == dto.DriverUserId.Value);
            if (!driverExists)
            {
                return BadRequest("DriverUserId no corresponde a un usuario existente.");
            }
        }

        if (!TryParseTripStatusId(dto.Status, out var statusId))
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
        trip.AvailableSeats = dto.AvailableSeats;
        trip.StatusId = statusId;
        trip.UpdatedAt = DateTime.UtcNow;
        trip.CancelledAt = statusId == 5 ? DateTime.UtcNow : null;

        trip.OriginLocation.Latitude = dto.OriginLatitude;
        trip.OriginLocation.Longitude = dto.OriginLongitude;

        if (dto.DestinationLatitude is not null)
        {
            trip.DestinationLocation.Latitude = dto.DestinationLatitude.Value;
        }

        if (dto.DestinationLongitude is not null)
        {
            trip.DestinationLocation.Longitude = dto.DestinationLongitude.Value;
        }

        await _context.SaveChangesAsync();

        return Ok(TripService.MapToDto(trip));
    }

    [HttpDelete("trips/{id:guid}")]
    [RequirePermission(AppPermissions.DeleteTrips)]
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
    [RequirePermission(AppPermissions.ReadReservations)]
    public async Task<ActionResult<IReadOnlyList<ReservationDto>>> GetAllReservationsAsync()
    {
        var reservations = await _context.Reservations
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return Ok(reservations.Select(ReservationService.MapToDto).ToList());
    }

    [HttpPut("reservations/{id:guid}")]
    [RequirePermission(AppPermissions.WriteReservations)]
    public async Task<ActionResult<ReservationDto>> UpdateReservationAsync(Guid id, [FromBody] AdminUpdateReservationDto dto)
    {
        var reservation = await _context.Reservations
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .FirstOrDefaultAsync(r => r.Id == id);
        if (reservation is null)
        {
            return NotFound("Reserva no encontrada.");
        }

        if (!TryParseReservationStatusId(dto.Status, out var statusId))
        {
            return BadRequest("Estado de reserva invalido.");
        }

        var targetTripExists = await _context.Trips.AnyAsync(t => t.Id == dto.TripId);
        if (!targetTripExists)
        {
            return BadRequest("TripId no corresponde a un viaje existente.");
        }

        var passengerExists = await _context.Users.AnyAsync(u => u.Id == dto.PassengerUserId);
        if (!passengerExists)
        {
            return BadRequest("PassengerUserId no corresponde a un usuario existente.");
        }

        reservation.PassengerUserId = dto.PassengerUserId;
        reservation.TripId = dto.TripId;
        reservation.StatusId = statusId;

        await _context.SaveChangesAsync();

        return Ok(ReservationService.MapToDto(reservation));
    }

    [HttpDelete("reservations/{id:guid}")]
    [RequirePermission(AppPermissions.DeleteReservations)]
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

    [HttpGet("support-tickets")]
    [RequirePermission(AppPermissions.ReadSupport)]
    public async Task<ActionResult<SupportTicketListResponseDto>> GetSupportTicketsAsync(
        [FromQuery] int? status,
        [FromQuery] int? category)
    {
        var list = await _supportTicketService.ListAllForAdminAsync(status, category);
        return Ok(list);
    }

    [HttpGet("support-tickets/{id:guid}")]
    [RequirePermission(AppPermissions.ReadSupport)]
    public async Task<ActionResult<SupportTicketResponseDto>> GetSupportTicketAsync(Guid id)
    {
        var ticket = await _supportTicketService.GetByIdForAdminAsync(id);
        if (ticket is null)
        {
            return NotFound(new { message = "Reporte de soporte no encontrado." });
        }

        return Ok(ticket);
    }

    [HttpPatch("support-tickets/{id:guid}/status")]
    [RequirePermission(AppPermissions.WriteSupport)]
    public async Task<ActionResult<SupportTicketResponseDto>> UpdateSupportTicketStatusAsync(
        Guid id,
        [FromBody] AdminUpdateSupportTicketStatusDto dto)
    {
        try
        {
            var updated = await _supportTicketService.UpdateStatusForAdminAsync(id, dto.Status);
            return Ok(updated);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpGet("all-data")]
    [RequirePermission(AppPermissions.ViewMetrics)]
    public async Task<ActionResult<object>> GetAllDataAsync()
    {
        var users = await _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
            .AsNoTracking()
            .OrderByDescending(u => u.CreatedAt)
            .ToListAsync();

        var trips = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        var reservations = await _context.Reservations
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return Ok(new
        {
            users = users.Select(UserResponseDto.FromEntity).ToList(),
            trips = trips.Select(TripService.MapToDto).ToList(),
            reservations = reservations.Select(ReservationService.MapToDto).ToList()
        });
    }

    private static bool IsUniversityEmail(string email)
    {
        return email.EndsWith("@univalle.edu", StringComparison.OrdinalIgnoreCase);
    }



    private static bool TryParseTripStatusId(string value, out int statusId)
    {
        var normalized = value.Trim().ToLowerInvariant();

        switch (normalized)
        {
            case "1" or "scheduled" or "programado":
                statusId = 1; return true;
            case "2" or "ready" or "listo":
                statusId = 2; return true;
            case "3" or "in_progress" or "inprogress" or "en_curso" or "en curso":
                statusId = 3; return true;
            case "4" or "finished" or "finalizado":
                statusId = 4; return true;
            case "5" or "cancelled" or "cancelado":
                statusId = 5; return true;
        }

        statusId = 1;
        return false;
    }

    private static bool TryParseReservationStatusId(string value, out int statusId)
    {
        var normalized = value.Trim().ToLowerInvariant();

        switch (normalized)
        {
            case "1" or "pending" or "pendiente":
                statusId = 1; return true;
            case "2" or "confirmed" or "confirmado":
                statusId = 2; return true;
            case "3" or "boarded" or "abordado":
                statusId = 3; return true;
            case "4" or "cancelled" or "cancelado":
                statusId = 4; return true;
        }

        statusId = 1;
        return false;
    }

    public sealed class AdminUpdateUserDto
    {
        public string FullName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? PhoneNumber { get; set; }
        public int RoleId { get; set; }
        public Guid? CustomRoleId { get; set; }
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
        public Guid PassengerUserId { get; set; }
        public string Status { get; set; } = string.Empty;
    }
}
