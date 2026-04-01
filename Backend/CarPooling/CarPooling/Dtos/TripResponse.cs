using CarPooling.Models;

namespace CarPooling.Dtos;

public class TripResponse
{
    public Guid Id { get; init; }
    public double OriginLatitude { get; init; }
    public double OriginLongitude { get; init; }
    public double? DestinationLatitude { get; init; }
    public double? DestinationLongitude { get; init; }
    public TripStatus Status { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime? UpdatedAt { get; init; }
    public DateTime? CancelledAt { get; init; }

    public static TripResponse FromEntity(Trip trip) => new()
    {
        Id = trip.Id,
        OriginLatitude = trip.OriginLatitude,
        OriginLongitude = trip.OriginLongitude,
        DestinationLatitude = trip.DestinationLatitude,
        DestinationLongitude = trip.DestinationLongitude,
        Status = trip.Status,
        CreatedAt = trip.CreatedAt,
        UpdatedAt = trip.UpdatedAt,
        CancelledAt = trip.CancelledAt
    };
}
