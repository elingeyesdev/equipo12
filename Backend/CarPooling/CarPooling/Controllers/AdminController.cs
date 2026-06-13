using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class AdminController(
    CarPoolingContext context,
    SupportTicketService supportTicketService,
    SupportTicketMessagingService supportTicketMessagingService) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private readonly SupportTicketService _supportTicketService = supportTicketService;
    private readonly SupportTicketMessagingService _supportTicketMessagingService = supportTicketMessagingService;

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
                    .ThenInclude(r => r.RolePermissions)
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
                    .ThenInclude(r => r.RolePermissions)
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

        if (!string.IsNullOrWhiteSpace(dto.Password))
        {
            var bytes = System.Security.Cryptography.SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(dto.Password));
            user.PasswordHash = System.Convert.ToHexString(bytes);
        }

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

        // Comprobar si el usuario tiene viajes asociados como conductor
        var hasTrips = await _context.Trips.AnyAsync(t => t.DriverUserId == id);
        if (hasTrips)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene viajes asociados.");
        }

        // Comprobar si el usuario tiene reservas asociadas como pasajero
        var hasReservations = await _context.Reservations.AnyAsync(r => r.PassengerUserId == id);
        if (hasReservations)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene reservas asociadas.");
        }

        // Comprobar si el usuario tiene reportes de soporte creados o asignados
        var hasSupportTickets = await _context.SupportTickets.AnyAsync(t => t.UserId == id || t.AssignedAdminUserId == id);
        if (hasSupportTickets)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene reportes de soporte asociados.");
        }

        // Comprobar si tiene calificaciones asociadas
        var hasRatings = await _context.TripRatings.AnyAsync(r => r.EvaluatorUserId == id || r.EvaluatedUserId == id);
        if (hasRatings)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene calificaciones asociadas.");
        }

        // Comprobar si tiene pagos o reembolsos asociados
        var hasPayments = await _context.Payments.AnyAsync(p => p.PassengerUserId == id || p.ConfirmedByUserId == id) ||
                          await _context.Refunds.AnyAsync(r => r.RequestedByUserId == id || r.ProcessedByUserId == id);
        if (hasPayments)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene pagos o reembolsos asociados.");
        }

        // Comprobar si tiene mensajes de chat o soporte asociados
        var hasMessages = await _context.SupportTicketMessages.AnyAsync(m => m.SenderUserId == id) ||
                          await _context.TripChatMessages.AnyAsync(m => m.SenderUserId == id);
        if (hasMessages)
        {
            return BadRequest("No se puede eliminar el usuario porque tiene mensajes de chat o soporte asociados.");
        }

        // Limpiar confirmaciones de lectura de chats y soporte
        var chatReads = await _context.TripChatMessageReads.Where(r => r.UserId == id).ToListAsync();
        _context.TripChatMessageReads.RemoveRange(chatReads);

        var supportReads = await _context.SupportTicketMessageReads.Where(r => r.UserId == id).ToListAsync();
        _context.SupportTicketMessageReads.RemoveRange(supportReads);

        // Limpiar roles explícitamente
        var userRoles = await _context.UserRoles.Where(ur => ur.UserId == id).ToListAsync();
        _context.UserRoles.RemoveRange(userRoles);

        // Eliminar dispositivos del usuario
        var devices = await _context.UserDevices.Where(d => d.UserId == id).ToListAsync();
        _context.UserDevices.RemoveRange(devices);

        // Eliminar métodos de pago del usuario
        var userPaymentMethods = await _context.UserPaymentMethods.Where(m => m.UserId == id).ToListAsync();
        _context.UserPaymentMethods.RemoveRange(userPaymentMethods);

        // Eliminar vehículos propiedad del usuario (ya es seguro porque no tienen viajes asociados)
        var vehicles = await _context.Vehicles.Where(v => v.OwnerUserId == id).ToListAsync();
        _context.Vehicles.RemoveRange(vehicles);

        // Eliminar perfil de conductor si existe
        if (user.DriverProfile != null)
        {
            _context.DriverProfiles.Remove(user.DriverProfile);
        }

        _context.Users.Remove(user);
        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpPatch("users/{id:guid}/status")]
    [RequirePermission(AppPermissions.WriteUsers)]
    public async Task<ActionResult<UserResponseDto>> ToggleUserStatusAsync(Guid id, [FromBody] ToggleUserStatusDto dto)
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

        user.IsActive = dto.IsActive;
        await _context.SaveChangesAsync();

        return Ok(UserResponseDto.FromEntity(user));
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
                    .ThenInclude(r => r.RolePermissions)
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

    [HttpPost("users/create-web-admin")]
    [RequirePermission(AppPermissions.ManageRoles)]
    public async Task<ActionResult<UserResponseDto>> CreateWebAdminAsync([FromBody] CreateWebAdminDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        if (!IsUniversityEmail(normalizedEmail))
        {
            return BadRequest("Solo se permiten correos institucionales @univalle.edu");
        }

        var emailExists = await _context.Users.AnyAsync(u => u.Email == normalizedEmail);
        if (emailExists)
        {
            return Conflict("El email ingresado ya esta en uso.");
        }

        if (string.IsNullOrWhiteSpace(dto.FullName))
        {
            return BadRequest("El nombre completo es obligatorio.");
        }

        if (string.IsNullOrWhiteSpace(dto.Password) || dto.Password.Length < 6)
        {
            return BadRequest("La contraseña es obligatoria y debe tener al menos 6 caracteres.");
        }

        // 1. Create a custom Role for this admin user
        var cleanRoleName = string.IsNullOrWhiteSpace(dto.RoleName) ? dto.FullName.Trim() : dto.RoleName.Trim();
        var roleName = $"Admin - {cleanRoleName} ({normalizedEmail})";
        if (roleName.Length > 100)
        {
            roleName = roleName.Substring(0, 100);
        }

        var customRole = new Role
        {
            Id = Guid.NewGuid(),
            Name = roleName,
            Description = $"Rol administrativo personalizado para {dto.FullName}",
            IsSystemRole = false
        };

        _context.Roles.Add(customRole);

        // 2. Add selected permissions
        if (dto.Permissions != null && dto.Permissions.Count > 0)
        {
            foreach (var permissionId in dto.Permissions)
            {
                var permExists = await _context.Permissions.AnyAsync(p => p.Id == permissionId);
                if (permExists)
                {
                    _context.RolePermissions.Add(new RolePermission
                    {
                        RoleId = customRole.Id,
                        PermissionId = permissionId
                    });
                }
            }
        }

        // 3. Create the User with hashed password
        var user = new User
        {
            Id = Guid.NewGuid(),
            FullName = dto.FullName.Trim(),
            Email = normalizedEmail,
            PasswordHash = UsersController.HashPassword(dto.Password),
            PhoneNumber = null
        };

        _context.Users.Add(user);

        // 4. Link User to Custom Role
        _context.UserRoles.Add(new UserRole
        {
            UserId = user.Id,
            RoleId = customRole.Id
        });

        await _context.SaveChangesAsync();

        // Load relations for mapper
        await _context.Entry(user).Collection(u => u.UserRoles).Query().Include(ur => ur.Role).LoadAsync();

        return Ok(UserResponseDto.FromEntity(user));
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

    public sealed class CreateWebAdminDto
    {
        public string FullName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
        public string? RoleName { get; set; }
        public List<string> Permissions { get; set; } = [];
    }

    public sealed class ToggleUserStatusDto
    {
        public bool IsActive { get; set; }
    }
}
