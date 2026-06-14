using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/users/{userId:guid}/trip-bookmarks")]
public class TripBookmarksController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<TripBookmarkResponseDto>>> ListAsync(Guid userId)
    {
        if (!await UserExistsAsync(userId))
            return NotFound("Usuario no encontrado.");

        var list = await _context.UserBookmarks
            .AsNoTracking()
            .Include(b => b.OriginLocation)
            .Include(b => b.DestinationLocation)
            .Where(b => b.UserId == userId)
            .OrderByDescending(b => b.LastUsedAt ?? b.CreatedAt)
            .ThenByDescending(b => b.UseCount)
            .ToListAsync();

        return Ok(list.ConvertAll(TripBookmarkResponseDto.FromEntity));
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
        _context.Locations.Add(originLocation);

        Location? destLocation = null;
        if (asRoute)
        {
            destLocation = new Location
            {
                Id = Guid.NewGuid(),
                Latitude = dto.DestinationLatitude!.Value,
                Longitude = dto.DestinationLongitude!.Value
            };
            _context.Locations.Add(destLocation);
        }

        var bookmark = new UserBookmark
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            Kind = asRoute ? "route" : "place",
            Title = title,
            OriginLocationId = originLocation.Id,
            DestinationLocationId = destLocation?.Id,
            UseCount = 0,
            LastUsedAt = null,
            CreatedAt = DateTime.UtcNow
        };

        _context.UserBookmarks.Add(bookmark);
        await _context.SaveChangesAsync();

        // Load navigations for Dto mapper
        bookmark.OriginLocation = originLocation;
        bookmark.DestinationLocation = destLocation;

        var response = TripBookmarkResponseDto.FromEntity(bookmark);
        return CreatedAtRoute(nameof(GetByIdAsync), new { userId, tripId = bookmark.Id }, response);
    }

    [HttpGet("{tripId:guid}", Name = nameof(GetByIdAsync))]
    public async Task<ActionResult<TripBookmarkResponseDto>> GetByIdAsync(Guid userId, Guid tripId)
    {
        var bookmark = await _context.UserBookmarks
            .AsNoTracking()
            .Include(b => b.OriginLocation)
            .Include(b => b.DestinationLocation)
            .FirstOrDefaultAsync(b => b.Id == tripId && b.UserId == userId);

        if (bookmark is null)
            return NotFound();

        return Ok(TripBookmarkResponseDto.FromEntity(bookmark));
    }

    [HttpDelete("{tripId:guid}")]
    public async Task<IActionResult> DeleteAsync(Guid userId, Guid tripId)
    {
        var bookmark = await _context.UserBookmarks.FirstOrDefaultAsync(b =>
            b.Id == tripId && b.UserId == userId);

        if (bookmark is null)
            return NotFound();

        _context.UserBookmarks.Remove(bookmark);
        await _context.SaveChangesAsync();
        return NoContent();
    }

    [HttpPost("{tripId:guid}/use")]
    public async Task<ActionResult<TripBookmarkResponseDto>> RecordUseAsync(Guid userId, Guid tripId)
    {
        var bookmark = await _context.UserBookmarks
            .Include(b => b.OriginLocation)
            .Include(b => b.DestinationLocation)
            .FirstOrDefaultAsync(b => b.Id == tripId && b.UserId == userId);

        if (bookmark is null)
            return NotFound();

        bookmark.UseCount += 1;
        bookmark.LastUsedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(TripBookmarkResponseDto.FromEntity(bookmark));
    }

    private Task<bool> UserExistsAsync(Guid userId) =>
        _context.Users.AnyAsync(u => u.Id == userId);

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
