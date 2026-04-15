using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class DriverProfile
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }

    [Range(1, 12)]
    public int AvailableSeats { get; set; }

    [Required]
    [MaxLength(20)]
    public string LicensePlate { get; set; } = string.Empty;

    [Required]
    [MaxLength(60)]
    public string VehicleBrand { get; set; } = string.Empty;

    [Required]
    [MaxLength(30)]
    public string VehicleColor { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }

    [ForeignKey("UserId")]
    public User User { get; set; } = null!;
}
