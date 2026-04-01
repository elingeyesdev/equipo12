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
            Status = TripStatus.AwaitingDestination
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
