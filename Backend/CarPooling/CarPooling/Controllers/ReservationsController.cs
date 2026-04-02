using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController(CarPoolingContext context) : ControllerBase
{
    [HttpPost("~/api/Trips/{tripId}/Reservations")]
    public async Task<ActionResult<ReservationDto>> CreateReservation(Guid tripId, CreateReservationDto dto)
    {
        var trip = await context.Trips.FindAsync(tripId);

        if (trip == null)
        {
            return NotFound("Viaje no encontrado.");
        }

        if (trip.Status == TripStatus.Cancelled)
        {
            return BadRequest("El viaje no está disponible para reserva.");
        }

        if (trip.AvailableSeats <= 0)
        {
            return BadRequest("No hay cupos disponibles en este viaje.");
        }

        var existingReservation = await context.Reservations
            .FirstOrDefaultAsync(r => r.TripId == tripId && r.PassengerName == dto.PassengerName && r.Status == ReservationStatus.Active);

        if (existingReservation != null)
        {
            return BadRequest("Este pasajero ya tiene una reserva activa para este viaje.");
        }

        var reservation = new Reservation
        {
            TripId = tripId,
            PassengerName = dto.PassengerName,
            Status = ReservationStatus.Active,
            CreatedAt = DateTime.UtcNow
        };

        trip.AvailableSeats--;

        context.Reservations.Add(reservation);
        await context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetReservations), new { tripId = trip.Id }, MapToDto(reservation));
    }

    [HttpGet("~/api/Trips/{tripId}/Reservations")]
    public async Task<ActionResult<IEnumerable<ReservationDto>>> GetReservations(Guid tripId)
    {
        var tripExists = await context.Trips.AnyAsync(t => t.Id == tripId);
        if (!tripExists)
        {
            return NotFound("Viaje no encontrado.");
        }

        var reservations = await context.Reservations
            .Where(r => r.TripId == tripId && r.Status == ReservationStatus.Active)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return reservations.Select(MapToDto).ToList();
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> CancelReservation(Guid id)
    {
        var reservation = await context.Reservations.Include(r => r.Trip).FirstOrDefaultAsync(r => r.Id == id);

        if (reservation == null)
        {
            return NotFound("Reserva no encontrada.");
        }

        if (reservation.Status == ReservationStatus.Cancelled)
        {
            return BadRequest("La reserva ya está cancelada.");
        }

        reservation.Status = ReservationStatus.Cancelled;
        reservation.Trip.AvailableSeats++;

        await context.SaveChangesAsync();

        return Ok(MapToDto(reservation));
    }

    private static ReservationDto MapToDto(Reservation reservation)
    {
        return new ReservationDto
        {
            Id = reservation.Id,
            TripId = reservation.TripId,
            PassengerName = reservation.PassengerName,
            Status = reservation.Status.ToString(),
            CreatedAt = reservation.CreatedAt
        };
    }
}
