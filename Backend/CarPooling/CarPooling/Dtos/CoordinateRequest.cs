using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CoordinateRequest
{
    [Required]
    [Range(-90, 90)]
    public double Latitude { get; set; }

    [Required]
    [Range(-180, 180)]
    public double Longitude { get; set; }

    [MaxLength(100)]
    public string? DriverName { get; set; }

    public Guid? DriverUserId { get; set; }

    public Guid? VehicleId { get; set; }

    [Range(1, 50)]
    public int? OfferedSeats { get; set; }

    [Range(0.5, 1000)]
    public decimal? FareAmount { get; set; }
}
