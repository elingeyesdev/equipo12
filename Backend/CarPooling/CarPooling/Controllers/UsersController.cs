using System.Security.Cryptography;
using System.Text;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class UsersController(CarPoolingContext context,
    VehicleService vehicleService) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private readonly VehicleService _vehicleService = vehicleService;
    private const string AllowedDomain = "@univalle.edu";

    [HttpPost("register")]
    public async Task<ActionResult<UserResponseDto>> RegisterAsync([FromBody] RegisterUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        
        string dynamicRoleName = dto.Role?.Trim().ToLowerInvariant() == "driver" ? "Driver" : "Student";
        var role = await _context.Roles.FirstOrDefaultAsync(r => r.Name == dynamicRoleName);
        if (role == null)
        {
            return BadRequest("El rol especificado no existe en el sistema.");
        }

        if (!normalizedEmail.EndsWith(AllowedDomain, StringComparison.OrdinalIgnoreCase))
            return BadRequest("Solo correos @univalle.edu");
        if (await _context.Users.AnyAsync(u => u.Email == normalizedEmail))
            return Conflict("Email ya existe.");

        var user = new User
        {
            Id = Guid.NewGuid(),
            FullName = dto.FullName.Trim(),
            Email = normalizedEmail,
            PasswordHash = HashPassword(dto.Password),
            PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim(),
            ProfilePicture = dto.ProfilePicture
        };

        _context.Users.Add(user);
        
        _context.UserRoles.Add(new UserRole { UserId = user.Id, RoleId = role.Id });

        if (dynamicRoleName == "Driver")
        {
            if (dto.DriverProfile is null)
                return BadRequest("Chofer debe enviar datos de vehiculo.");
            await _vehicleService.CreateForDriverAsync(user.Id, dto.DriverProfile);
        }

        await _context.SaveChangesAsync();
        user.Vehicles = await _vehicleService.GetAllForDriverAsync(user.Id);
        
        // Load UserRoles and Roles for mapper
        await _context.Entry(user).Collection(u => u.UserRoles).Query().Include(ur => ur.Role).LoadAsync();

        return CreatedAtRoute("GetUserById", new { id = user.Id }, UserResponseDto.FromEntity(user));
    }

    [HttpPost("login")]
    public async Task<ActionResult<UserResponseDto>> LoginAsync([FromBody] LoginUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        if (!normalizedEmail.EndsWith(AllowedDomain, StringComparison.OrdinalIgnoreCase))
            return Unauthorized("Debes usar correo @univalle.edu");

        var incomingHash = HashPassword(dto.Password);
        var user = await _context.Users
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
                    .ThenInclude(r => r.RolePermissions)
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == normalizedEmail && u.PasswordHash == incomingHash);

        if (user is null) return Unauthorized("Credenciales invalidas.");
        if (!user.IsActive) return Unauthorized("Tu cuenta ha sido deshabilitada. Contacta al administrador.");
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<UserResponseDto>> UpdateAsync(Guid id, [FromBody] UpdateUserDto dto)
    {
        var user = await _context.Users
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
                    .ThenInclude(r => r.RolePermissions)
            .FirstOrDefaultAsync(u => u.Id == id);
        if (user is null) return NotFound();

        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        if (!normalizedEmail.EndsWith(AllowedDomain, StringComparison.OrdinalIgnoreCase))
            return BadRequest("Solo correos @univalle.edu");
        if (await _context.Users.AnyAsync(u => u.Email == normalizedEmail && u.Id != id))
            return Conflict("Email en uso.");

        user.FullName = dto.FullName.Trim();
        user.Email = normalizedEmail;
        user.PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim();
        user.ProfilePicture = dto.ProfilePicture;

        bool isCurrentlyDriver = user.UserRoles.Any(ur => ur.Role != null && ur.Role.Name == "Driver");
        bool wantsToBeDriver = !string.IsNullOrWhiteSpace(dto.Role) 
            ? (dto.Role.Trim().ToLowerInvariant() == "driver") 
            : isCurrentlyDriver;

        if (!string.IsNullOrWhiteSpace(dto.Role))
        {
            bool changing = wantsToBeDriver != isCurrentlyDriver;
            if (changing && !dto.RoleChangeRequested && !IsAdminOverride())
                return BadRequest("Cambio de rol requiere confirmacion.");

            if (changing)
            {
                var newRoleName = wantsToBeDriver ? "Driver" : "Student";
                var newRole = await _context.Roles.FirstOrDefaultAsync(r => r.Name == newRoleName);
                if (newRole != null)
                {
                    var rolesToRemove = user.UserRoles
                        .Where(ur => ur.Role != null && (ur.Role.Name == "Student" || ur.Role.Name == "Driver"))
                        .ToList();
                    foreach (var r in rolesToRemove)
                    {
                        _context.UserRoles.Remove(r);
                    }
                    _context.UserRoles.Add(new UserRole { UserId = user.Id, RoleId = newRole.Id });
                }
            }
        }

        if (wantsToBeDriver)
        {
            if (dto.DriverProfile is not null)
                await _vehicleService.UpsertForDriverAsync(user.Id, dto.DriverProfile);
            else if (!isCurrentlyDriver)
                return BadRequest("Chofer debe enviar datos de vehiculo.");
        }

        if (!string.IsNullOrWhiteSpace(dto.NewPassword))
            user.PasswordHash = HashPassword(dto.NewPassword);

        await _context.SaveChangesAsync();
        await _context.Entry(user).Collection(u => u.Vehicles).LoadAsync();
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPost("logout")]
    public IActionResult Logout() => Ok(new { message = "Sesion cerrada." });

    [HttpGet("{id:guid}", Name = "GetUserById")]
    public async Task<ActionResult<UserResponseDto>> GetByIdAsync(Guid id)
    {
        var user = await _context.Users
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
                    .ThenInclude(r => r.RolePermissions)
            .AsNoTracking().FirstOrDefaultAsync(u => u.Id == id);
        if (user is null) return NotFound();
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpGet("email/{email}")]
    public async Task<ActionResult<UserResponseDto>> GetByEmailAsync(string email)
    {
        var user = await _context.Users
            .Include(u => u.Vehicles)
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
                    .ThenInclude(r => r.RolePermissions)
            .AsNoTracking().FirstOrDefaultAsync(u => u.Email == email.Trim().ToLowerInvariant());
        if (user is null) return NotFound();
        return Ok(UserResponseDto.FromEntity(user));
    }

    public static string HashPassword(string p)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(p));
        return Convert.ToHexString(bytes);
    }

    [HttpPost("{id:guid}/fcm-token")]
    public async Task<IActionResult> UpdateFcmTokenAsync(Guid id, [FromBody] UpdateFcmTokenDto dto)
    {
        var user = await _context.Users.FindAsync(id);
        if (user is null) return NotFound("Usuario no encontrado.");

        var device = await _context.UserDevices
            .FirstOrDefaultAsync(d => d.UserId == id && d.FcmToken == dto.FcmToken);

        if (device == null)
        {
            device = new UserDevice
            {
                Id = Guid.NewGuid(),
                UserId = id,
                FcmToken = dto.FcmToken,
                DeviceName = dto.DeviceName,
                LastUsedAt = DateTime.UtcNow
            };
            _context.UserDevices.Add(device);
        }
        else
        {
            device.LastUsedAt = DateTime.UtcNow;
            device.DeviceName = dto.DeviceName;
        }

        await _context.SaveChangesAsync();
        return Ok(new { message = "Token FCM registrado con éxito." });
    }

    [HttpPost("logout-device")]
    public async Task<IActionResult> LogoutDeviceAsync([FromBody] UpdateFcmTokenDto dto)
    {
        var device = await _context.UserDevices.FirstOrDefaultAsync(d => d.FcmToken == dto.FcmToken);
        if (device != null)
        {
            _context.UserDevices.Remove(device);
            await _context.SaveChangesAsync();
        }
        return Ok(new { message = "Dispositivo desvinculado con éxito." });
    }

    private bool IsAdminOverride() =>
        Request.Headers.TryGetValue("X-Admin-Override", out var v) && v.ToString().Trim().ToLowerInvariant() is "true" or "1" or "yes";


}
