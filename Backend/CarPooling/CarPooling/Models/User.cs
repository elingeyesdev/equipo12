using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class User
{
    public Guid Id { get; set; }

    [Required]
    [MaxLength(120)]
    public string FullName { get; set; } = string.Empty;

    [Required]
    [MaxLength(120)]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    [MaxLength(256)]
    public string PasswordHash { get; set; } = string.Empty;

    [MaxLength(25)]
    public string? PhoneNumber { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
