using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TripsController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet("match-candidates")]
    public async Task<ActionResult<IReadOnlyList<DriverTripMatchResponse>>> GetMatchCandidatesAsync(
        [FromQuery] double referenceLatitude,
        [FromQuery] double referenceLongitude)
    {
        if (referenceLatitude is < -90 or > 90 || referenceLongitude is < -180 or > 180)
        {
            return BadRequest("Coordenadas de referencia invalidas.");
        }

        var trips = await _context.Trips.AsNoTracking()
            .Where(t =>
                (t.Status == TripStatus.Ready || t.Status == TripStatus.InProgress)
                && t.DestinationLatitude != null
                && t.DestinationLongitude != null
                && t.AvailableSeats > 0)
            .ToListAsync();

        var results = trips.Select(t =>
        {
            var destLat = t.DestinationLatitude!.Value;
            var destLon = t.DestinationLongitude!.Value;
            var dOrigin = HaversineKm(referenceLatitude, referenceLongitude, t.OriginLatitude, t.OriginLongitude);
            var dDest = HaversineKm(referenceLatitude, referenceLongitude, destLat, destLon);
            var distanceKm = Math.Min(dOrigin, dDest);
            const double assumedKmh = 28.0;
            var eta = (int)Math.Clamp(Math.Round(distanceKm / assumedKmh * 60.0), 1, 999);

            var name = string.IsNullOrWhiteSpace(t.DriverName) ? "Conductor" : t.DriverName.Trim();
            if (name.Length > 100)
            {
                name = name[..100];
            }

            return new DriverTripMatchResponse
            {
                TripId = t.Id,
                DriverName = name,
                OriginLatitude = t.OriginLatitude,
                OriginLongitude = t.OriginLongitude,
                DestinationLatitude = destLat,
                DestinationLongitude = destLon,
                Status = t.Status,
                AvailableSeats = t.AvailableSeats,
                DistanceKm = Math.Round(distanceKm, 2, MidpointRounding.AwayFromZero),
                EtaMinutes = eta,
            };
        })
            .OrderBy(r => r.DistanceKm)
            .ToList();

        return Ok(results);
    }

    [HttpPost("origin")]
    public async Task<ActionResult<TripResponse>> CreateOriginAsync([FromBody] CoordinateRequest request)
    {
        var driverName = request.DriverName?.Trim() ?? "";
        if (driverName.Length > 100)
        {
            driverName = driverName[..100];
        }

        var trip = new Trip
        {
            OriginLatitude = request.Latitude,
            OriginLongitude = request.Longitude,
            Status = TripStatus.AwaitingDestination,
            UpdatedAt = null,
            CancelledAt = null,
            DriverName = driverName,
            DriverUserId = request.DriverUserId
        };

        _context.Trips.Add(trip);
        await _context.SaveChangesAsync();

        return CreatedAtRoute("GetTripById", new { id = trip.Id }, TripResponse.FromEntity(trip));
    }

    /// <summary>Viaje activo del conductor (no cancelado ni finalizado), más reciente primero.</summary>
    [HttpGet("for-driver/{driverUserId:guid}")]
    public async Task<ActionResult<TripResponse>> GetActiveTripForDriverAsync(
        Guid driverUserId,
        [FromQuery] string? displayName = null)
    {
        var trip = await _context.Trips.AsNoTracking()
            .Where(t =>
                t.DriverUserId == driverUserId
                && t.Status != TripStatus.Cancelled
                && t.Status != TripStatus.Finished)
            .OrderByDescending(t => t.CreatedAt)
            .FirstOrDefaultAsync();

        if (trip is null && !string.IsNullOrWhiteSpace(displayName))
        {
            var normalized = displayName.Trim();
            trip = await _context.Trips.AsNoTracking()
                .Where(t =>
                    t.DriverUserId == null
                    && t.DriverName != null
                    && t.DriverName.Trim() == normalized
                    && t.Status != TripStatus.Cancelled
                    && t.Status != TripStatus.Finished)
                .OrderByDescending(t => t.CreatedAt)
                .FirstOrDefaultAsync();
        }

        if (trip is null)
        {
            return NotFound();
        }

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpPost("{id:guid}/destination")]
    public async Task<ActionResult<TripResponse>> SetDestinationAsync(Guid id, [FromBody] CoordinateRequest request)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound();
        }

        if (trip.Status == TripStatus.Cancelled)
        {
            return BadRequest("El viaje ya fue cancelado.");
        }

        if (trip.Status == TripStatus.Ready)
        {
            return BadRequest("El viaje ya cuenta con destino.");
        }

        trip.DestinationLatitude = request.Latitude;
        trip.DestinationLongitude = request.Longitude;
        trip.Status = TripStatus.Ready;
        trip.CancelledAt = null;
        trip.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpPost("{id:guid}/cancel")]
    public async Task<ActionResult<TripResponse>> CancelTripAsync(Guid id)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound();
        }

        if (trip.Status == TripStatus.Cancelled)
        {
            return Ok(TripResponse.FromEntity(trip));
        }

        trip.Status = TripStatus.Cancelled;
        trip.CancelledAt = DateTime.UtcNow;
        trip.UpdatedAt = trip.CancelledAt;

        await _context.SaveChangesAsync();

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpPost("{id:guid}/start")]
    public async Task<ActionResult<TripResponse>> StartTripAsync(Guid id, [FromBody] StartTripRequestDto? request)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound();
        }

        if (trip.Status == TripStatus.Cancelled)
        {
            return BadRequest("El viaje fue cancelado y no puede iniciarse.");
        }

        if (trip.Status != TripStatus.Ready)
        {
            return BadRequest("El viaje aún no está listo para iniciar (debe tener destino confirmado).");
        }

        // Para iniciar, debe haber al menos un pasajero ya abordado.
        var boardedPassengersCount = await _context.Reservations.CountAsync(r =>
            r.TripId == id && r.Status == ReservationStatus.Boarded);

        if (boardedPassengersCount <= 0)
        {
            return BadRequest("No se puede iniciar el viaje: debe haber al menos un pasajero abordado.");
        }

        trip.Status = TripStatus.InProgress;
        trip.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpPost("{id:guid}/finish")]
    public async Task<ActionResult<TripResponse>> FinishTripAsync(Guid id)
    {
        var trip = await _context.Trips.FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound();
        }

        if (trip.Status == TripStatus.Cancelled)
        {
            return BadRequest("El viaje fue cancelado y no puede finalizarse.");
        }

        if (trip.Status == TripStatus.Finished)
        {
            return Ok(TripResponse.FromEntity(trip));
        }

        if (trip.Status != TripStatus.InProgress)
        {
            return BadRequest("Solo se puede finalizar un viaje que esté en curso.");
        }

        trip.Status = TripStatus.Finished;
        trip.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return Ok(TripResponse.FromEntity(trip));
    }

    [HttpGet("{id:guid}", Name = "GetTripById")]
    public async Task<ActionResult<TripResponse>> GetTripByIdAsync(Guid id)
    {
        var trip = await _context.Trips.AsNoTracking().FirstOrDefaultAsync(t => t.Id == id);
        if (trip is null)
        {
            return NotFound();
        }

        return Ok(TripResponse.FromEntity(trip));
    }

    private static double HaversineKm(double lat1, double lon1, double lat2, double lon2)
    {
        const double earthRadiusKm = 6371.0;
        static double ToRad(double degrees) => degrees * (Math.PI / 180.0);

        var dLat = ToRad(lat2 - lat1);
        var dLon = ToRad(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2)
                + Math.Cos(ToRad(lat1)) * Math.Cos(ToRad(lat2)) * Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
