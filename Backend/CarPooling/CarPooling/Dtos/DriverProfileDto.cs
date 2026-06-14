using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class DriverProfileDto
{
    [Range(1, 50)]
    public int AvailableSeats { get; set; } = 4;

    [Required]
    [MaxLength(20)]
    public string LicensePlate { get; set; } = string.Empty;

    [Required]
    [MaxLength(60)]
    public string VehicleBrand { get; set; } = string.Empty;

    [MaxLength(60)]
    public string VehicleModel { get; set; } = string.Empty;

    [Required]
    [MaxLength(30)]
    public string VehicleColor { get; set; } = string.Empty;

    [Range(1900, 2100)]
    public int? VehicleYear { get; set; }

    public static DriverProfileDto FromEntity(Vehicle vehicle)
    {
        return new DriverProfileDto
        {
            AvailableSeats = vehicle.TotalSeats,
            LicensePlate = vehicle.LicensePlate,
            VehicleBrand = vehicle.Brand,
            VehicleModel = vehicle.Model,
            VehicleColor = vehicle.Color,
            VehicleYear = vehicle.VehicleYear
        };
    }
}
