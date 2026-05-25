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
    DriverService driverService,
    VehicleService vehicleService) : ControllerBase
{
    private readonly CarPoolingContext _context = context;
    private readonly DriverService _driverService = driverService;
    private readonly VehicleService _vehicleService = vehicleService;
    private const string AllowedDomain = "@univalle.edu";

    [HttpPost("register")]
    public async Task<ActionResult<UserResponseDto>> RegisterAsync([FromBody] RegisterUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        if (!TryParseUserRole(dto.Role, out var parsedRole))
            return BadRequest("Rol invalido.");
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
            Role = parsedRole
        };

        _context.Users.Add(user);

        if (parsedRole == UserRole.Driver)
        {
            if (dto.DriverProfile is null)
                return BadRequest("Chofer debe enviar datos de vehiculo.");
            await _driverService.CreateProfileAsync(user.Id, dto.DriverProfile);
        }

        await _context.SaveChangesAsync();
        user.DriverProfile = await _context.DriverProfiles.FirstOrDefaultAsync(p => p.UserId == user.Id);
        user.Vehicles = await _vehicleService.GetAllForDriverAsync(user.Id);

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
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == normalizedEmail && u.PasswordHash == incomingHash);

        if (user is null) return Unauthorized("Credenciales invalidas.");
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<UserResponseDto>> UpdateAsync(Guid id, [FromBody] UpdateUserDto dto)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile)
            .Include(u => u.Vehicles)
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

        if (!string.IsNullOrWhiteSpace(dto.Role) && TryParseUserRole(dto.Role, out var pr))
        {
            var changing = pr != user.Role;
            if (changing && !dto.RoleChangeRequested && !IsAdminOverride())
                return BadRequest("Cambio de rol requiere confirmacion.");
            if (changing) user.Role = pr;
        }

        if (user.Role == UserRole.Driver)
        {
            if (dto.DriverProfile is not null)
                await _driverService.UpsertProfileAsync(user.Id, dto.DriverProfile);
            else if (user.DriverProfile is null)
                return BadRequest("Chofer debe enviar datos de vehiculo.");
        }
        else if (user.DriverProfile is not null)
        {
            _context.DriverProfiles.Remove(user.DriverProfile);
            user.DriverProfile = null;
        }

        if (!string.IsNullOrWhiteSpace(dto.NewPassword))
            user.PasswordHash = HashPassword(dto.NewPassword);

        await _context.SaveChangesAsync();
        await _context.Entry(user).Reference(u => u.DriverProfile).LoadAsync();
        await _context.Entry(user).Collection(u => u.Vehicles).LoadAsync();
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpPost("logout")]
    public IActionResult Logout() => Ok(new { message = "Sesion cerrada." });

    [HttpGet("{id:guid}", Name = "GetUserById")]
    public async Task<ActionResult<UserResponseDto>> GetByIdAsync(Guid id)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile).Include(u => u.Vehicles)
            .AsNoTracking().FirstOrDefaultAsync(u => u.Id == id);
        if (user is null) return NotFound();
        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpGet("email/{email}")]
    public async Task<ActionResult<UserResponseDto>> GetByEmailAsync(string email)
    {
        var user = await _context.Users
            .Include(u => u.DriverProfile).Include(u => u.Vehicles)
            .AsNoTracking().FirstOrDefaultAsync(u => u.Email == email.Trim().ToLowerInvariant());
        if (user is null) return NotFound();
        return Ok(UserResponseDto.FromEntity(user));
    }

    private static string HashPassword(string p)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(p));
        return Convert.ToHexString(bytes);
    }

    private bool IsAdminOverride() =>
        Request.Headers.TryGetValue("X-Admin-Override", out var v) && v.ToString().Trim().ToLowerInvariant() is "true" or "1" or "yes";

    private static bool TryParseUserRole(string? r, out UserRole role)
    {
        role = (r?.Trim().ToLowerInvariant()) switch
        {
            "driver" or "chofer" or "2" => UserRole.Driver,
            "admin" or "administrador" or "3" => UserRole.Admin,
            _ => UserRole.Student
        };
        return true;
    }
}
