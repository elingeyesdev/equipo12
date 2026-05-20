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
    public LocationDto Origin { get; set; } = null!;
    public LocationDto? Destination { get; set; }
    public DateTime CreatedAt { get; set; }
    public int UseCount { get; set; }
    public DateTime? LastUsedAt { get; set; }

    public static TripBookmarkResponseDto FromTrip(Trip trip)
    {
        var route = trip.DestinationLocationId != trip.OriginLocationId;
        var dto = new TripBookmarkResponseDto
        {
            Id = trip.Id,
            Kind = route ? "route" : "place",
            Title = trip.DriverName ?? string.Empty,
            Origin = new LocationDto
            {
                Id = trip.OriginLocation.Id,
                Latitude = trip.OriginLocation.Latitude,
                Longitude = trip.OriginLocation.Longitude,
                AddressLabel = trip.OriginLocation.AddressLabel
            },
            CreatedAt = trip.CreatedAt,
            UseCount = trip.BookmarkUseCount,
            LastUsedAt = trip.BookmarkLastUsedAt
        };

        if (route)
        {
            dto.Destination = new LocationDto
            {
                Id = trip.DestinationLocation.Id,
                Latitude = trip.DestinationLocation.Latitude,
                Longitude = trip.DestinationLocation.Longitude,
                AddressLabel = trip.DestinationLocation.AddressLabel
            };
        }

        return dto;
    }
}
