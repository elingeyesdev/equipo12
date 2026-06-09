using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class SafeZone
{
    public Guid Id { get; set; }

    [MaxLength(120)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(400)]
    public string? Description { get; set; }

    [Range(-90, 90)]
    public double Latitude { get; set; }

    [Range(-180, 180)]
    public double Longitude { get; set; }

    [MaxLength(200)]
    public string? AddressLabel { get; set; }

    public SafeZonePurpose Purpose { get; set; } = SafeZonePurpose.Both;

    public bool IsActive { get; set; } = true;

    public int DisplayOrder { get; set; }

    [MaxLength(80)]
    public string? CampusArea { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}
