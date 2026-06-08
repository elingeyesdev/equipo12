using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class UserDevice
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }

    public User User { get; set; } = null!;

    [Required]
    [MaxLength(500)]
    public string FcmToken { get; set; } = string.Empty;

    [MaxLength(100)]
    public string? DeviceName { get; set; }

    public DateTime LastUsedAt { get; set; } = DateTime.UtcNow;
}
