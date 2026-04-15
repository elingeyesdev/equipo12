using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class DriverProfileDto
{
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

    public static DriverProfileDto FromEntity(DriverProfile profile)
    {
        return new DriverProfileDto
        {
            AvailableSeats = profile.AvailableSeats,
            LicensePlate = profile.LicensePlate,
            VehicleBrand = profile.VehicleBrand,
            VehicleColor = profile.VehicleColor
        };
    }
}
