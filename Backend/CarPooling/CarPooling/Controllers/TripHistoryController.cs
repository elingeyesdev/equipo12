using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/users/{userId:guid}/history")]
public class TripHistoryController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet]
    public async Task<ActionResult<TripHistoryListResponseDto>> ListAsync(Guid userId, [FromQuery] string? passengerName = null)
    {
        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var hiddenTripIds = await _context.UserHistoryHiddenTrips
            .AsNoTracking()
            .Where(h => h.UserId == userId)
            .Select(h => h.TripId)
            .ToListAsync();

        var driverHistory = await _context.Trips
            .AsNoTracking()
            .Where(t => t.Kind == TripKind.Regular && t.DriverUserId == userId && !hiddenTripIds.Contains(t.Id))
            .OrderByDescending(t => t.CreatedAt)
            .Select(t => new TripHistorySummaryDto
            {
                TripId = t.Id,
                Category = "driver",
                OriginLabel = $"{t.OriginLatitude:F6}, {t.OriginLongitude:F6}",
                DestinationLabel = t.DestinationLatitude == null || t.DestinationLongitude == null
                    ? "Sin destino"
                    : $"{t.DestinationLatitude.Value:F6}, {t.DestinationLongitude.Value:F6}",
                StatusLabel = TripHistoryLabelMapper.ToTripStatusLabel(t.Status),
                CreatedAt = t.CreatedAt
            })
            .ToListAsync();

        var studentName = (passengerName ?? user.FullName).Trim();
        var reservations = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.Trip)
            .Where(r => r.PassengerName == studentName && r.Trip.Kind == TripKind.Regular && !hiddenTripIds.Contains(r.TripId))
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        var studentHistory = reservations
            .GroupBy(r => r.TripId)
            .Select(g => g.First())
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new TripHistorySummaryDto
            {
                TripId = r.TripId,
                Category = "student",
                OriginLabel = $"{r.Trip.OriginLatitude:F6}, {r.Trip.OriginLongitude:F6}",
                DestinationLabel = r.Trip.DestinationLatitude == null || r.Trip.DestinationLongitude == null
                    ? "Sin destino"
                    : $"{r.Trip.DestinationLatitude.Value:F6}, {r.Trip.DestinationLongitude.Value:F6}",
                StatusLabel = TripHistoryLabelMapper.ToTripStatusLabel(r.Trip.Status),
                CreatedAt = r.CreatedAt
            })
            .ToList();

        return Ok(new TripHistoryListResponseDto
        {
            Summary = new TripHistoryStatsDto
            {
                PassengerTripsCount = studentHistory.Count,
                DriverTripsCount = driverHistory.Count,
                TotalTripsCount = studentHistory.Count + driverHistory.Count
            },
            DriverHistory = driverHistory,
            StudentHistory = studentHistory
        });
    }

    [HttpGet("{tripId:guid}")]
    public async Task<ActionResult<TripHistoryDetailDto>> GetDetailAsync(Guid userId, Guid tripId, [FromQuery] string? passengerName = null)
    {
        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var trip = await _context.Trips.AsNoTracking().FirstOrDefaultAsync(t => t.Id == tripId && t.Kind == TripKind.Regular);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        var isHidden = await _context.UserHistoryHiddenTrips
            .AsNoTracking()
            .AnyAsync(h => h.UserId == userId && h.TripId == tripId);
        if (isHidden)
        {
            return NotFound("Este viaje fue ocultado de tu historial.");
        }

        var reservationStats = await _context.Reservations
            .AsNoTracking()
            .Where(r => r.TripId == tripId)
            .GroupBy(_ => 1)
            .Select(g => new
            {
                Total = g.Count(),
                Boarded = g.Count(x => x.Status == ReservationStatus.Boarded),
                Cancelled = g.Count(x => x.Status == ReservationStatus.Cancelled)
            })
            .FirstOrDefaultAsync();

        var profile = trip.DriverUserId == null
            ? null
            : await _context.DriverProfiles.AsNoTracking().FirstOrDefaultAsync(p => p.UserId == trip.DriverUserId);

        var resolvedPassengerName = (passengerName ?? user.FullName).Trim();
        var viewerReservation = await _context.Reservations
            .AsNoTracking()
            .Where(r => r.TripId == tripId && r.PassengerName == resolvedPassengerName)
            .OrderByDescending(r => r.CreatedAt)
            .FirstOrDefaultAsync();

        var category = viewerReservation != null ? "student" : (trip.DriverUserId == userId ? "driver" : "unknown");

        var detail = new TripHistoryDetailDto
        {
            TripId = trip.Id,
            Category = category,
            StatusLabel = TripHistoryLabelMapper.ToTripStatusLabel(trip.Status),
            CreatedAt = trip.CreatedAt,
            UpdatedAt = trip.UpdatedAt,
            OriginLabel = $"{trip.OriginLatitude:F6}, {trip.OriginLongitude:F6}",
            DestinationLabel = trip.DestinationLatitude == null || trip.DestinationLongitude == null
                ? "Sin destino"
                : $"{trip.DestinationLatitude.Value:F6}, {trip.DestinationLongitude.Value:F6}",
            OriginLatitude = trip.OriginLatitude,
            OriginLongitude = trip.OriginLongitude,
            DestinationLatitude = trip.DestinationLatitude,
            DestinationLongitude = trip.DestinationLongitude,
            DriverName = string.IsNullOrWhiteSpace(trip.DriverName) ? "Conductor" : trip.DriverName,
            DriverVehicleBrand = profile?.VehicleBrand,
            DriverVehicleColor = profile?.VehicleColor,
            DriverLicensePlate = profile?.LicensePlate,
            ReservationCount = reservationStats?.Total ?? 0,
            BoardedCount = reservationStats?.Boarded ?? 0,
            CancelledCount = reservationStats?.Cancelled ?? 0,
            PassengerReservationStatus = viewerReservation == null ? null : TripHistoryLabelMapper.ToReservationStatusLabel(viewerReservation.Status),
            PassengerName = viewerReservation?.PassengerName
        };

        return Ok(detail);
    }

    [HttpDelete("{tripId:guid}")]
    public async Task<IActionResult> HideFromHistoryAsync(Guid userId, Guid tripId)
    {
        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var trip = await _context.Trips.AsNoTracking().FirstOrDefaultAsync(t => t.Id == tripId && t.Kind == TripKind.Regular);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        var resolvedPassengerName = user.FullName.Trim();
        var isRelated = trip.DriverUserId == userId
            || await _context.Reservations.AsNoTracking().AnyAsync(r => r.TripId == tripId && r.PassengerName == resolvedPassengerName);
        if (!isRelated)
        {
            return Forbid();
        }

        var alreadyHidden = await _context.UserHistoryHiddenTrips
            .AsNoTracking()
            .AnyAsync(h => h.UserId == userId && h.TripId == tripId);
        if (alreadyHidden)
        {
            return NoContent();
        }

        _context.UserHistoryHiddenTrips.Add(new UserHistoryHiddenTrip
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TripId = tripId,
            HiddenAt = DateTime.UtcNow
        });
        await _context.SaveChangesAsync();

        return NoContent();
    }

    [HttpPost("{tripId:guid}/restore")]
    public async Task<IActionResult> RestoreToHistoryAsync(Guid userId, Guid tripId)
    {
        var user = await _context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var trip = await _context.Trips.AsNoTracking().FirstOrDefaultAsync(t => t.Id == tripId && t.Kind == TripKind.Regular);
        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        var resolvedPassengerName = user.FullName.Trim();
        var isRelated = trip.DriverUserId == userId
            || await _context.Reservations.AsNoTracking().AnyAsync(r => r.TripId == tripId && r.PassengerName == resolvedPassengerName);
        if (!isRelated)
        {
            return Forbid();
        }

        var hidden = await _context.UserHistoryHiddenTrips
            .FirstOrDefaultAsync(h => h.UserId == userId && h.TripId == tripId);
        if (hidden is null)
        {
            return NoContent();
        }

        _context.UserHistoryHiddenTrips.Remove(hidden);
        await _context.SaveChangesAsync();
        return NoContent();
    }
}
