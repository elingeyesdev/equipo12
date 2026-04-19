using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

/// <summary>
/// Favoritos guardados como filas en <see cref="Trip"/> con <see cref="TripKind.UserBookmark"/>.
/// Reutiliza lat/long y metadatos del viaje; no son viajes operativos.
/// </summary>
[ApiController]
[Route("api/users/{userId:guid}/trip-bookmarks")]
public class TripBookmarksController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<TripBookmarkResponseDto>>> ListAsync(Guid userId)
    {
        if (!await UserExistsAsync(userId))
        {
            return NotFound("Usuario no encontrado.");
        }

        var list = await _context.Trips
            .AsNoTracking()
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
        {
            return NotFound("Usuario no encontrado.");
        }

        if (!TryParseBookmarkKind(dto.Kind, out var asRoute))
        {
            return BadRequest("Kind inválido. Usa: place o route.");
        }

        if (dto.OriginLatitude is null || dto.OriginLongitude is null)
        {
            return BadRequest("Origen: latitud y longitud son obligatorias.");
        }

        if (asRoute)
        {
            if (dto.DestinationLatitude is null || dto.DestinationLongitude is null)
            {
                return BadRequest("Para kind route debes enviar destino (latitud y longitud).");
            }
        }
        else
        {
            if (dto.DestinationLatitude is not null || dto.DestinationLongitude is not null)
            {
                return BadRequest("Para kind place no debes enviar destino.");
            }
        }

        var title = dto.Title.Trim();
        if (title.Length > 100)
        {
            title = title[..100];
        }

        var trip = new Trip
        {
            Id = Guid.NewGuid(),
            Kind = TripKind.UserBookmark,
            OriginLatitude = dto.OriginLatitude.Value,
            OriginLongitude = dto.OriginLongitude.Value,
            DestinationLatitude = asRoute ? dto.DestinationLatitude : null,
            DestinationLongitude = asRoute ? dto.DestinationLongitude : null,
            Status = TripStatus.Finished,
            AvailableSeats = 0,
            DriverName = title,
            DriverUserId = userId,
            BookmarkUseCount = 0,
            BookmarkLastUsedAt = null,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = null,
            CancelledAt = null
        };

        _context.Trips.Add(trip);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetByIdAsync), new { userId, tripId = trip.Id }, TripBookmarkResponseDto.FromTrip(trip));
    }

    [HttpGet("{tripId:guid}", Name = nameof(GetByIdAsync))]
    public async Task<ActionResult<TripBookmarkResponseDto>> GetByIdAsync(Guid userId, Guid tripId)
    {
        var trip = await FindOwnedBookmarkAsync(userId, tripId);
        if (trip is null)
        {
            return NotFound();
        }

        return Ok(TripBookmarkResponseDto.FromTrip(trip));
    }

    [HttpDelete("{tripId:guid}")]
    public async Task<IActionResult> DeleteAsync(Guid userId, Guid tripId)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t =>
            t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

        if (trip is null)
        {
            return NotFound();
        }

        _context.Trips.Remove(trip);
        await _context.SaveChangesAsync();
        return NoContent();
    }

    [HttpPost("{tripId:guid}/use")]
    public async Task<ActionResult<TripBookmarkResponseDto>> RecordUseAsync(Guid userId, Guid tripId)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t =>
            t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

        if (trip is null)
        {
            return NotFound();
        }

        trip.BookmarkUseCount += 1;
        trip.BookmarkLastUsedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(TripBookmarkResponseDto.FromTrip(trip));
    }

    private Task<bool> UserExistsAsync(Guid userId) =>
        _context.Users.AnyAsync(u => u.Id == userId);

    private Task<Trip?> FindOwnedBookmarkAsync(Guid userId, Guid tripId) =>
        _context.Trips.AsNoTracking()
            .FirstOrDefaultAsync(t => t.Id == tripId && t.DriverUserId == userId && t.Kind == TripKind.UserBookmark);

    private static bool TryParseBookmarkKind(string raw, out bool asRoute)
    {
        asRoute = false;
        if (string.IsNullOrWhiteSpace(raw))
        {
            return false;
        }

        var n = raw.Trim().ToLowerInvariant();
        if (n is "route" or "ruta" or "viaje")
        {
            asRoute = true;
            return true;
        }

        if (n is "place" or "lugar" or "punto")
        {
            return true;
        }

        return false;
    }
}
