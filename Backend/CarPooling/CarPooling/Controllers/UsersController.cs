        using System.Security.Cryptography;
using System.Text;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class UsersController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private const string AllowedDomain = "@univalle.edu";

    [HttpPost("register")]
    public async Task<ActionResult<UserResponseDto>> RegisterAsync([FromBody] RegisterUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();

        if (!TryParseUserRole(dto.Role, out var parsedRole))
        {
            return BadRequest("Rol invalido. Usa: student/estudiante (1), driver/chofer (2) o admin/administrador (3).");
        }

        if (!IsUniversityEmail(normalizedEmail))
        {
            return BadRequest("Solo se permiten correos institucionales @univalle.edu");
        }

        var exists = await _context.Users.AnyAsync(u => u.Email == normalizedEmail);
        if (exists)
        {
            return Conflict("Ya existe un usuario con ese email.");
        }

        var user = new User
        {
            Id = Guid.NewGuid(),
            FullName = dto.FullName.Trim(),
            Email = normalizedEmail,
            PasswordHash = HashPassword(dto.Password),
            PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim(),
            Role = parsedRole
        };

        DriverProfile? driverProfile = null;
        if (parsedRole == UserRole.Driver)
        {
            if (dto.DriverProfile is null)
            {
                return BadRequest("Para registrar un chofer debes enviar los datos del vehiculo.");
            }

            driverProfile = BuildDriverProfile(dto.DriverProfile, user.Id);
        }

        _context.Users.Add(user);
        if (driverProfile is not null)
        {
            _context.DriverProfiles.Add(driverProfile);
        }
        await _context.SaveChangesAsync();

        user.DriverProfile = driverProfile;

        return CreatedAtRoute("GetUserById", new { id = user.Id }, UserResponseDto.FromEntity(user));
    }

    [HttpPost("login")]
    public async Task<ActionResult<UserResponseDto>> LoginAsync([FromBody] LoginUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();

        if (!IsUniversityEmail(normalizedEmail))
        {
            return Unauthorized("Debes iniciar sesión con tu correo institucional @univalle.edu");
        }

        var incomingHash = HashPassword(dto.Password);

        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == normalizedEmail && u.PasswordHash == incomingHash);

        if (user is null)
        {
            return Unauthorized("Credenciales inválidas.");
        }

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<UserResponseDto>> UpdateAsync(Guid id, [FromBody] UpdateUserDto dto)
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

        user.FullName = dto.FullName.Trim();
        user.Email = normalizedEmail;
        user.PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim();

        var requestedRole = user.Role;
        var roleWasProvided = !string.IsNullOrWhiteSpace(dto.Role);
        if (!string.IsNullOrWhiteSpace(dto.Role))
        {
            if (!TryParseUserRole(dto.Role, out var parsedRole))
            {
                return BadRequest("Rol invalido. Usa: student/estudiante (1), driver/chofer (2) o admin/administrador (3).");
            }

            requestedRole = parsedRole;
        }

        var roleIsChanging = roleWasProvided && requestedRole != user.Role;
        var adminOverride = IsAdminOverrideRequested();

        if (roleIsChanging && !dto.RoleChangeRequested && !adminOverride)
        {
            return BadRequest("El rol solo puede cambiarse cuando el usuario lo solicita explícitamente o por override de admin.");
        }

        if (roleIsChanging)
        {
            user.Role = requestedRole;
        }

        if (user.Role == UserRole.Driver)
        {
            if (dto.DriverProfile is not null)
            {
                if (user.DriverProfile is null)
                {
                    user.DriverProfile = BuildDriverProfile(dto.DriverProfile, user.Id);
                }
                else
                {
                    user.DriverProfile.AvailableSeats = dto.DriverProfile.AvailableSeats;
                    user.DriverProfile.LicensePlate = dto.DriverProfile.LicensePlate.Trim().ToUpperInvariant();
                    user.DriverProfile.VehicleBrand = dto.DriverProfile.VehicleBrand.Trim();
                    user.DriverProfile.VehicleColor = dto.DriverProfile.VehicleColor.Trim();
                    user.DriverProfile.UpdatedAt = DateTime.UtcNow;
                }
            }
            else if (user.DriverProfile is null)
            {
                return BadRequest("Para rol chofer debes enviar los datos del vehiculo.");
            }
        }
        else if (user.DriverProfile is not null)
        {
            _context.DriverProfiles.Remove(user.DriverProfile);
            user.DriverProfile = null;
        }

        if (!string.IsNullOrWhiteSpace(dto.NewPassword))
        {
            user.PasswordHash = HashPassword(dto.NewPassword);
        }

        await _context.SaveChangesAsync();

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPost("logout")]
    public IActionResult Logout()
    {
        return Ok(new { message = "Sesión cerrada." });
    }

    [HttpGet("{id:guid}", Name = "GetUserById")]
    public async Task<ActionResult<UserResponseDto>> GetByIdAsync(Guid id)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Id == id);
        if (user is null)
        {
            return NotFound();
        }

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpGet("email/{email}")]
    public async Task<ActionResult<UserResponseDto>> GetByEmailAsync(string email)
    {
        var normalizedEmail = email.Trim().ToLowerInvariant();

        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == normalizedEmail);
        if (user is null)
        {
            return NotFound();
        }

        return Ok(UserResponseDto.FromEntity(user));
    }

    private static string HashPassword(string password)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(bytes);
    }

    private static bool IsUniversityEmail(string email)
    {
        return email.EndsWith(AllowedDomain, StringComparison.OrdinalIgnoreCase);
    }

    private static DriverProfile BuildDriverProfile(DriverProfileDto dto, Guid userId)
    {
        return new DriverProfile
        {
            UserId = userId,
            AvailableSeats = dto.AvailableSeats,
            LicensePlate = dto.LicensePlate.Trim().ToUpperInvariant(),
            VehicleBrand = dto.VehicleBrand.Trim(),
            VehicleColor = dto.VehicleColor.Trim()
        };
    }

    private bool IsAdminOverrideRequested()
    {
        if (!Request.Headers.TryGetValue("X-Admin-Override", out var headerValue))
        {
            return false;
        }

        var normalized = headerValue.ToString().Trim().ToLowerInvariant();
        return normalized is "true" or "1" or "yes";
    }

    private static bool TryParseUserRole(string? roleValue, out UserRole role)
    {
        var normalizedRole = roleValue?.Trim().ToLowerInvariant();

        if (string.IsNullOrWhiteSpace(normalizedRole))
        {
            role = UserRole.Student;
            return true;
        }

        if (normalizedRole is "driver" or "chofer" or "2")
        {
            role = UserRole.Driver;
            return true;
        }

        if (normalizedRole is "admin" or "administrador" or "3")
        {
            role = UserRole.Admin;
            return true;
        }

        if (normalizedRole is "student" or "estudiante" or "1")
        {
            role = UserRole.Student;
            return true;
        }

        role = UserRole.Student;
        return false;
    }
}
