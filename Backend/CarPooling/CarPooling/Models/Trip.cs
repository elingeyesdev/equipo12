using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class Trip
{
    public Guid Id { get; set; }

    [Range(-90, 90)]
    public double OriginLatitude { get; set; }

    [Range(-180, 180)]
    public double OriginLongitude { get; set; }

    [Range(-90, 90)]
    public double? DestinationLatitude { get; set; }

    [Range(-180, 180)]
    public double? DestinationLongitude { get; set; }

    public TripStatus Status { get; set; } = TripStatus.AwaitingDestination;

    public int AvailableSeats { get; set; } = 4;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public DateTime? CancelledAt { get; set; }
}
