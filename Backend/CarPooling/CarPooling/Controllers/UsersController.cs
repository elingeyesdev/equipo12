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

    [HttpPost("register")]
    public async Task<ActionResult<UserResponseDto>> RegisterAsync([FromBody] RegisterUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();

        var exists = await _context.Users.AnyAsync(u => u.Email == normalizedEmail);
        if (exists)
        {
            return Conflict("Ya existe un usuario con ese email.");
        }

        var user = new User
        {
            FullName = dto.FullName.Trim(),
            Email = normalizedEmail,
            PasswordHash = HashPassword(dto.Password),
            PhoneNumber = string.IsNullOrWhiteSpace(dto.PhoneNumber) ? null : dto.PhoneNumber.Trim()
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        return CreatedAtRoute("GetUserById", new { id = user.Id }, UserResponseDto.FromEntity(user));
    }

    [HttpPost("login")]
    public async Task<ActionResult<UserResponseDto>> LoginAsync([FromBody] LoginUserDto dto)
    {
        var normalizedEmail = dto.Email.Trim().ToLowerInvariant();
        var incomingHash = HashPassword(dto.Password);

        var user = await _context.Users
            .AsNoTracking()
            .FirstOrDefaultAsync(u => u.Email == normalizedEmail && u.PasswordHash == incomingHash);

        if (user is null)
        {
            return Unauthorized("Credenciales inválidas.");
        }

        return Ok(UserResponseDto.FromEntity(user));
    }

    [HttpGet("{id:guid}", Name = "GetUserById")]
    public async Task<ActionResult<UserResponseDto>> GetByIdAsync(Guid id)
    {
        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == id);
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

        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Email == normalizedEmail);
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
}
