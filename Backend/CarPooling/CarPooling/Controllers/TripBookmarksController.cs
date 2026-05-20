using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

/// <summary>
/// Favoritos guardados como filas en <see cref="Trip"/> con <see cref="TripKind.UserBookmark"/>.
/// Las coordenadas se almacenan en entidades <see cref="Location"/> referenciadas por FK.
/// </summary>
[ApiController]
[Route("api/users/{userId:guid}/trip-bookmarks")]
public class TripBookmarksController(CarPoolingContext context) : ControllerBase
{
    private const int FinishedStatusId = 4;

    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<TripBookmarkResponseDto>>> ListAsync(Guid userId)
    {
        if (!await UserExistsAsync(userId))
            return NotFound("Usuario no encontrado.");

        var list = await context.Trips
            .AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Where(t => t.Kind == TripKind.UserBookmark && t.DriverUserId == userId)
            .OrderByDescending(t => t.BookmarkLastUsedAt ?? t.CreatedAt)
            .ThenByDescending(t => t.BookmarkUseCount)
            .ToListAsync();

        return Ok(list.ConvertAll(TripBookmarkResponseDto.FromTrip));
    }

    [HttpPost]
    public async Task<ActionResult<TripBookmarkResponseDto>> CreateAsync(Guid userId, [FromBody] CreateTripBookmarkDto dto)
    {
        if (!await UserExistsAsync(userId))
            return NotFound("Usuario no encontrado.");

        if (!TryParseBookmarkKind(dto.Kind, out var asRoute))
            return BadRequest("Kind inválido. Usa: place o route.");

        if (dto.OriginLatitude is null || dto.OriginLongitude is null)
            return BadRequest("Origen: latitud y longitud son obligatorias.");

        if (asRoute)
        {
            if (dto.DestinationLatitude is null || dto.DestinationLongitude is null)
                return BadRequest("Para kind route debes enviar destino (latitud y longitud).");
        }
        else
        {
            if (dto.DestinationLatitude is not null || dto.DestinationLongitude is not null)
                return BadRequest("Para kind place no debes enviar destino.");
        }

        var title = dto.Title.Trim();
        if (title.Length > 100)
            title = title[..100];

        var originLocation = new Location
        {
            Id = Guid.NewGuid(),
            Latitude = dto.OriginLatitude!.Value,
            Longitude = dto.OriginLongitude!.Value
        };
        context.Locations.Add(originLocation);

        Location? destLocation = null;
        if (asRoute)
        {
            destLocation = new Location
            {
                Id = Guid.NewGuid(),
                Latitude = dto.DestinationLatitude!.Value,
                Longitude = dto.DestinationLongitude!.Value
            };
            context.Locations.Add(destLocation);
        }

        var trip = new Trip
        {
            Id = Guid.NewGuid(),
            Kind = TripKind.UserBookmark,
            OriginLocationId = originLocation.Id,
            DestinationLocationId = destLocation?.Id ?? originLocation.Id,
            StatusId = FinishedStatusId,
            DriverName = title,
            DriverUserId = userId,
            BookmarkUseCount = 0,
            BookmarkLastUsedAt = null,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = null,
            CancelledAt = null
        };

        context.Trips.Add(trip);
        await context.SaveChangesAsync();

        var response = new TripBookmarkResponseDto
        {
            Id = trip.Id,
            Kind = asRoute ? "route" : "place",
            Title = title,
            Origin = new LocationDto
            {
                Id = originLocation.Id,
                Latitude = originLocation.Latitude,
                Longitude = originLocation.Longitude,
                AddressLabel = originLocation.AddressLabel
            },
            Destination = destLocation is not null
                ? new LocationDto
                {
                    Id = destLocation.Id,
                    Latitude = destLocation.Latitude,
                    Longitude = destLocation.Longitude,
                    AddressLabel = destLocation.AddressLabel
                }
                : null,
            CreatedAt = trip.CreatedAt,
            UseCount = 0,
            LastUsedAt = null
        };

        return CreatedAtAction(nameof(GetByIdAsync), new { userId, tripId = trip.Id }, response);
    }

    [HttpGet("{tripId:guid}", Name = nameof(GetByIdAsync))]
    public async Task<ActionResult<TripBookmarkResponseDto>> GetByIdAsync(Guid userId, Guid tripId)
    {
        var trip = await context.Trips
            .AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .FirstOrDefaultAsync(t => t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

        if (trip is null)
            return NotFound();

        return Ok(TripBookmarkResponseDto.FromTrip(trip));
    }

    [HttpDelete("{tripId:guid}")]
    public async Task<IActionResult> DeleteAsync(Guid userId, Guid tripId)
    {
        var trip = await context.Trips.FirstOrDefaultAsync(t =>
            t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

        if (trip is null)
            return NotFound();

        context.Trips.Remove(trip);
        await context.SaveChangesAsync();
        return NoContent();
    }

    [HttpPost("{tripId:guid}/use")]
    public async Task<ActionResult<TripBookmarkResponseDto>> RecordUseAsync(Guid userId, Guid tripId)
    {
        var trip = await context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .FirstOrDefaultAsync(t => t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

        if (trip is null)
            return NotFound();

        trip.BookmarkUseCount += 1;
        trip.BookmarkLastUsedAt = DateTime.UtcNow;
        await context.SaveChangesAsync();

        return Ok(TripBookmarkResponseDto.FromTrip(trip));
    }

    private Task<bool> UserExistsAsync(Guid userId) =>
        context.Users.AnyAsync(u => u.Id == userId);

    private static bool TryParseBookmarkKind(string raw, out bool asRoute)
    {
        asRoute = false;
        if (string.IsNullOrWhiteSpace(raw))
            return false;

        var n = raw.Trim().ToLowerInvariant();
        if (n is "route" or "ruta" or "viaje")
        {
            asRoute = true;
            return true;
        }

        if (n is "place" or "lugar" or "punto")
            return true;

        return false;
    }
}
