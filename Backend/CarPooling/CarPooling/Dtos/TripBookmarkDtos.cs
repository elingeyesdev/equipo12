using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class CreateTripBookmarkDto
{
    [Required]
    [MaxLength(20)]
    public string Kind { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public double? OriginLatitude { get; set; }

    [Required]
    public double? OriginLongitude { get; set; }

    public double? DestinationLatitude { get; set; }

    public double? DestinationLongitude { get; set; }
}

/// <summary>
/// Misma forma que antes en la app (lugares/viajes favoritos), pero el id es un <see cref="Trip"/>.
/// </summary>
public class TripBookmarkResponseDto
{
    public Guid Id { get; set; }
    public string Kind { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public double OriginLatitude { get; set; }
    public double OriginLongitude { get; set; }
    public double? DestinationLatitude { get; set; }
    public double? DestinationLongitude { get; set; }
    public DateTime CreatedAt { get; set; }
    public int UseCount { get; set; }
    public DateTime? LastUsedAt { get; set; }

    public static TripBookmarkResponseDto FromTrip(Trip trip)
    {
        var route = trip.DestinationLatitude is not null && trip.DestinationLongitude is not null;
        return new TripBookmarkResponseDto
        {
            Id = trip.Id,
            Kind = route ? "route" : "place",
            Title = trip.DriverName ?? string.Empty,
            OriginLatitude = trip.OriginLatitude,
            OriginLongitude = trip.OriginLongitude,
            DestinationLatitude = trip.DestinationLatitude,
            DestinationLongitude = trip.DestinationLongitude,
            CreatedAt = trip.CreatedAt,
            UseCount = trip.BookmarkUseCount,
            LastUsedAt = trip.BookmarkLastUsedAt
        };
    }
}
