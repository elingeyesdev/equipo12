using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Admin")]
public class AdminController(
    CarPoolingContext context,
    SupportTicketService supportTicketService,
    SupportTicketMessagingService supportTicketMessagingService) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private readonly SupportTicketService _supportTicketService = supportTicketService;
    private readonly SupportTicketMessagingService _supportTicketMessagingService = supportTicketMessagingService;

    [HttpGet("users")]
    public async Task<ActionResult<IReadOnlyList<UserResponseDto>>> GetAllUsersAsync(
        [FromQuery] string? name,
        [FromQuery] string? email)
    {
        var usersQuery = _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
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
            return Conflict("El email ingresado ya esta en uso por otro usuario.");
        }

        if (!TryParseAdminRoleId(dto.RoleId, out var role))
        {
            return BadRequest("RoleId invalido. Usa 1 (student), 2 (driver) o 3 (admin).");
        }

        user.FullName = dto.FullName.Trim();
        user.Email = normalizedEmail;
        user.PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim();
        user.Role = role;

        if (!string.IsNullOrWhiteSpace(dto.Password))
        {
            var bytes = System.Security.Cryptography.SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(dto.Password));
            user.PasswordHash = System.Convert.ToHexString(bytes);
        }

        if (role == UserRole.Driver)
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
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        return Ok(trips.Select(TripService.MapToDto).ToList());
    }

    [HttpPut("trips/{id:guid}")]
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
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .AsNoTracking()
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return Ok(reservations.Select(ReservationService.MapToDto).ToList());
    }

    [HttpPut("reservations/{id:guid}")]
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
    public async Task<ActionResult<SupportTicketListResponseDto>> GetSupportTicketsAsync(
        [FromQuery] int? status,
        [FromQuery] int? category)
    {
        var list = await _supportTicketService.ListAllForAdminAsync(status, category);
        return Ok(list);
    }

    [HttpGet("support-tickets/{id:guid}")]
    public async Task<ActionResult<SupportTicketResponseDto>> GetSupportTicketAsync(Guid id)
    {
        var ticket = await _supportTicketService.GetByIdForAdminAsync(id);
        if (ticket is null)
        {
            return NotFound(new { message = "Reporte de soporte no encontrado." });
        }

        return Ok(ticket);
    }

    [HttpGet("support-tickets/{id:guid}/messages")]
    public async Task<ActionResult<IEnumerable<SupportTicketMessageResponseDto>>> GetSupportTicketMessagesAsync(Guid id)
    {
        try
        {
            var messages = await _supportTicketMessagingService.GetMessagesForAdminAsync(id);
            return Ok(messages);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [HttpPost("support-tickets/{id:guid}/messages")]
    public async Task<ActionResult<SupportTicketMessageResponseDto>> SendSupportTicketMessageAsync(
        Guid id,
        [FromBody] SendSupportTicketMessageDto dto)
    {
        var adminUserId = GetCurrentUserId();
        if (adminUserId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            var message = await _supportTicketMessagingService.SendMessageAsAdminAsync(adminUserId.Value, id, dto);
            return StatusCode(StatusCodes.Status201Created, message);
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

    [HttpPatch("support-tickets/{id:guid}/status")]
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
    public async Task<ActionResult<object>> GetAllDataAsync()
    {
        var users = await _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
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

    private Guid? GetCurrentUserId()
    {
        var nameIdentifier = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (Guid.TryParse(nameIdentifier, out var userId))
        {
            return userId;
        }

        return null;
    }

    public sealed class AdminUpdateUserDto
    {
        public string FullName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? PhoneNumber { get; set; }
        public int RoleId { get; set; }
        public string? Password { get; set; }
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
