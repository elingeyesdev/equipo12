using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Vehicle
{
    public Guid Id { get; set; }

    [Required]
    public Guid OwnerUserId { get; set; }

    [Required]
    [MaxLength(20)]
    public string LicensePlate { get; set; } = string.Empty;

    [Required]
    [MaxLength(60)]
    public string Brand { get; set; } = string.Empty;

    [MaxLength(60)]
    public string Model { get; set; } = string.Empty;

    [Required]
    [MaxLength(30)]
    public string Color { get; set; } = string.Empty;

    [Range(1900, 2100)]
    public int? VehicleYear { get; set; }

    [Range(1, 50)]
    public int TotalSeats { get; set; } = 4;

    public bool IsActive { get; set; } = true;

    public bool IsVerified { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [ForeignKey(nameof(OwnerUserId))]
    public User OwnerUser { get; set; } = null!;

    public ICollection<Trip> Trips { get; set; } = [];
}
