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

    public static TripBookmarkResponseDto FromEntity(UserBookmark bookmark)
    {
        return new TripBookmarkResponseDto
        {
            Id = bookmark.Id,
            Kind = bookmark.Kind,
            Title = bookmark.Title,
            Origin = new LocationDto
            {
                Id = bookmark.OriginLocation.Id,
                Latitude = bookmark.OriginLocation.Latitude,
                Longitude = bookmark.OriginLocation.Longitude,
                AddressLabel = bookmark.OriginLocation.AddressLabel
            },
            Destination = bookmark.DestinationLocation is not null
                ? new LocationDto
                {
                    Id = bookmark.DestinationLocation.Id,
                    Latitude = bookmark.DestinationLocation.Latitude,
                    Longitude = bookmark.DestinationLocation.Longitude,
                    AddressLabel = bookmark.DestinationLocation.AddressLabel
                }
                : null,
            CreatedAt = bookmark.CreatedAt,
            UseCount = bookmark.UseCount,
            LastUsedAt = bookmark.LastUsedAt
        };
    }
}
