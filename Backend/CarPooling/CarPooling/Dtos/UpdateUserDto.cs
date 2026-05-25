using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class UpdateUserDto
{
    [Required]
    [MaxLength(120)]
    public string FullName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [MaxLength(120)]
    public string Email { get; set; } = string.Empty;

    [MaxLength(25)]
    public string? PhoneNumber { get; set; }

    [MinLength(6)]
    [MaxLength(120)]
    public string? NewPassword { get; set; }

    [MaxLength(20)]
    public string? Role { get; set; }

    public bool RoleChangeRequested { get; set; } = false;

    public DriverProfileDto? DriverProfile { get; set; }
}
