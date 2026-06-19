using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class StartTripRequestDto
{
    [Range(-90, 90)]
    public double? Latitude { get; set; }

    [Range(-180, 180)]
    public double? Longitude { get; set; }

    [Range(0, 1000)]
    public decimal? FareAmount { get; set; }
}

