using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class Location
{
    public Guid Id { get; set; }

    [Range(-90, 90)]
    public double Latitude { get; set; }

    [Range(-180, 180)]
    public double Longitude { get; set; }

    [MaxLength(200)]
    public string? AddressLabel { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
