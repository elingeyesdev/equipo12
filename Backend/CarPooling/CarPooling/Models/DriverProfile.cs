using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class DriverProfile
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }

    public bool IsVerified { get; set; } = false;

    [MaxLength(30)]
    public string? LicenseNumber { get; set; }

    [MaxLength(300)]
    public string? LicenseDocumentUrl { get; set; }

    public DateTime? VerifiedAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }

    [ForeignKey(nameof(UserId))]
    public User User { get; set; } = null!;
}
