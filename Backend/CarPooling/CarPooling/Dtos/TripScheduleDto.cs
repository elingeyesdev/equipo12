using System;
using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CreateTripScheduleDto
{
    [Required]
    public Guid DriverUserId { get; set; }

    [Required]
    public double OriginLatitude { get; set; }

    [Required]
    public double OriginLongitude { get; set; }

    public string? OriginAddress { get; set; }

    [Required]
    public double DestinationLatitude { get; set; }

    [Required]
    public double DestinationLongitude { get; set; }

    public string? DestinationAddress { get; set; }

    [Required]
    public TimeSpan DepartureTime { get; set; }

    [Required]
    [MaxLength(50)]
    public string DaysOfWeek { get; set; } = ""; // e.g., "1,2,3,4,5"

    [Required]
    public DateTime StartDate { get; set; }

    public DateTime? EndDate { get; set; }

    public Guid? VehicleId { get; set; }

    public int OfferedSeats { get; set; } = 4;

    public decimal FareAmount { get; set; } = 10m;
}

public class TripScheduleResponse
{
    public Guid Id { get; set; }
    public Guid DriverUserId { get; set; }
    public string DriverName { get; set; } = "";
    public LocationDto Origin { get; set; } = null!;
    public LocationDto Destination { get; set; } = null!;
    public TimeSpan DepartureTime { get; set; }
    public string DaysOfWeek { get; set; } = "";
    public DateTime StartDate { get; set; }
    public DateTime? EndDate { get; set; }
    public Guid? VehicleId { get; set; }
    public int OfferedSeats { get; set; }
    public decimal FareAmount { get; set; }
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
}