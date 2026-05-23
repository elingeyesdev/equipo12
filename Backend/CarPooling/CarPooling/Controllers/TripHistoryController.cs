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
    [HttpGet]
    public async Task<ActionResult<TripHistoryListResponseDto>> ListAsync(Guid userId)
    {
        var user = await context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null) return NotFound("Usuario no encontrado.");

        var driverHistory = await context.Trips
            .AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .Where(t => t.Kind == TripKind.Regular && t.DriverUserId == userId)
            .OrderByDescending(t => t.CreatedAt)
            .Select(t => new TripHistorySummaryDto
            {
                TripId = t.Id,
                Category = "driver",
                OriginLabel = t.OriginLocation.AddressLabel ?? $"{t.OriginLocation.Latitude:F5}, {t.OriginLocation.Longitude:F5}",
                DestinationLabel = t.DestinationLocation.AddressLabel ?? $"{t.DestinationLocation.Latitude:F5}, {t.DestinationLocation.Longitude:F5}",
                StatusLabel = t.StatusEntity.LabelEs,
                CreatedAt = t.CreatedAt
            })
            .ToListAsync();

        var studentHistory = await context.Reservations
            .AsNoTracking()
            .Include(r => r.Trip).ThenInclude(t => t.OriginLocation)
            .Include(r => r.Trip).ThenInclude(t => t.DestinationLocation)
            .Include(r => r.Trip).ThenInclude(t => t.StatusEntity)
            .Where(r => r.PassengerUserId == userId && r.Trip.Kind == TripKind.Regular)
            .OrderByDescending(r => r.CreatedAt)
            .Select(r => new TripHistorySummaryDto
            {
                TripId = r.TripId,
                Category = "student",
                OriginLabel = r.Trip.OriginLocation.AddressLabel ?? $"{r.Trip.OriginLocation.Latitude:F5}, {r.Trip.OriginLocation.Longitude:F5}",
                DestinationLabel = r.Trip.DestinationLocation.AddressLabel ?? $"{r.Trip.DestinationLocation.Latitude:F5}, {r.Trip.DestinationLocation.Longitude:F5}",
                StatusLabel = r.Trip.StatusEntity.LabelEs,
                CreatedAt = r.CreatedAt
            })
            .ToListAsync();

        var distinctStudent = studentHistory.GroupBy(r => r.TripId).Select(g => g.First()).ToList();

        return Ok(new TripHistoryListResponseDto
        {
            Summary = new TripHistoryStatsDto
            {
                PassengerTripsCount = distinctStudent.Count,
                DriverTripsCount = driverHistory.Count,
                TotalTripsCount = distinctStudent.Count + driverHistory.Count
            },
            DriverHistory = driverHistory,
            StudentHistory = distinctStudent
        });
    }

    [HttpGet("{tripId:guid}")]
    public async Task<ActionResult<TripHistoryDetailDto>> GetDetailAsync(Guid userId, Guid tripId)
    {
        var user = await context.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == userId);
        if (user is null) return NotFound("Usuario no encontrado.");

        var trip = await context.Trips.AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId && t.Kind == TripKind.Regular);
        if (trip is null) return NotFound("Viaje no encontrado.");

        var reservationStats = await context.Reservations
            .Where(r => r.TripId == tripId)
            .GroupBy(_ => 1)
            .Select(g => new
            {
                Total = g.Count(),
                Boarded = g.Count(x => x.StatusId == 3),
                Cancelled = g.Count(x => x.StatusId == 4)
            })
            .FirstOrDefaultAsync();

        var participants = await context.Reservations
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .Where(r => r.TripId == tripId)
            .OrderBy(r => r.CreatedAt)
            .Select(r => new TripHistoryParticipantDto
            {
                Name = r.PassengerUser.FullName,
                StatusLabel = r.StatusEntity.LabelEs,
                ReservedAt = r.CreatedAt
            })
            .ToListAsync();

        var viewerReservation = await context.Reservations
            .Include(r => r.StatusEntity)
            .Where(r => r.TripId == tripId && r.PassengerUserId == userId)
            .OrderByDescending(r => r.CreatedAt)
            .FirstOrDefaultAsync();

        var category = viewerReservation != null ? "student" : (trip.DriverUserId == userId ? "driver" : "unknown");

        Vehicle? vehicle = null;
        if (trip.VehicleId is not null)
            vehicle = await context.Vehicles.AsNoTracking().FirstOrDefaultAsync(v => v.Id == trip.VehicleId.Value);

        return Ok(new TripHistoryDetailDto
        {
            TripId = trip.Id,
            Category = category,
            StatusLabel = trip.StatusEntity?.LabelEs ?? string.Empty,
            CreatedAt = trip.CreatedAt,
            StartedAt = trip.StartedAt,
            FinishedAt = trip.FinishedAt,
            UpdatedAt = trip.UpdatedAt,
            OriginLabel = trip.OriginLocation?.AddressLabel ?? $"{trip.OriginLocation?.Latitude:F5}, {trip.OriginLocation?.Longitude:F5}",
            DestinationLabel = trip.DestinationLocation?.AddressLabel ?? $"{trip.DestinationLocation?.Latitude:F5}, {trip.DestinationLocation?.Longitude:F5}",
            OriginLatitude = trip.OriginLocation?.Latitude ?? 0,
            OriginLongitude = trip.OriginLocation?.Longitude ?? 0,
            DestinationLatitude = trip.DestinationLocation?.Latitude,
            DestinationLongitude = trip.DestinationLocation?.Longitude,
            DriverName = string.IsNullOrWhiteSpace(trip.DriverName) ? "Conductor" : trip.DriverName,
            DriverVehicleBrand = vehicle?.Brand,
            DriverVehicleColor = vehicle?.Color,
            DriverLicensePlate = vehicle?.LicensePlate,
            ReservationCount = reservationStats?.Total ?? 0,
            BoardedCount = reservationStats?.Boarded ?? 0,
            CancelledCount = reservationStats?.Cancelled ?? 0,
            PassengerReservationStatus = viewerReservation?.StatusEntity?.LabelEs,
            PassengerName = viewerReservation?.PassengerUser?.FullName,
            Participants = participants
        });
    }
}
