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

    [HttpPost("origin")]
    public async Task<ActionResult<TripResponse>> CreateOriginAsync([FromBody] CoordinateRequest request)
    {
        var trip = new Trip
        {
            OriginLatitude = request.Latitude,
            OriginLongitude = request.Longitude,
            Status = TripStatus.AwaitingDestination,
            UpdatedAt = null,
            CancelledAt = null
        };

        _context.Trips.Add(trip);
        await _context.SaveChangesAsync();

        return CreatedAtRoute("GetTripById", new { id = trip.Id }, TripResponse.FromEntity(trip));
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
}
