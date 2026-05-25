using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class RegisterUserDto
{
    [Required]
    [MaxLength(120)]
    public string FullName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [MaxLength(120)]
    public string Email { get; set; } = string.Empty;

    [Required]
    [MinLength(6)]
    [MaxLength(120)]
    public string Password { get; set; } = string.Empty;

    [MaxLength(25)]
    public string? PhoneNumber { get; set; }

    [Required]
    [MaxLength(20)]
    public string Role { get; set; } = "student";

    public DriverProfileDto? DriverProfile { get; set; }
}
