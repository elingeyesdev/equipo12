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

        if (trip.Kind != TripKind.Regular)
        {
            return BadRequest("Este viaje no admite reservas.");
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

    [HttpGet("~/api/Trips/{tripId}/Reservations/boarded")]
    public async Task<ActionResult<IEnumerable<ReservationDto>>> GetBoardedPassengers(Guid tripId)
    {
        var tripExists = await context.Trips.AnyAsync(t => t.Id == tripId);
        if (!tripExists)
        {
            return NotFound("Viaje no encontrado.");
        }

        var boardedReservations = await context.Reservations
            .Where(r => r.TripId == tripId && r.Status == ReservationStatus.Boarded)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return boardedReservations.Select(MapToDto).ToList();
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

        if (reservation.Status == ReservationStatus.Boarded)
        {
            return BadRequest("No se puede cancelar una reserva de un pasajero ya abordado.");
        }

        reservation.Status = ReservationStatus.Cancelled;
        reservation.Trip.AvailableSeats++;

        await context.SaveChangesAsync();

        return Ok(MapToDto(reservation));
    }

    [HttpPost("~/api/Trips/{tripId}/Reservations/board")]
    public async Task<ActionResult<ReservationDto>> ConfirmBoarding(Guid tripId, CreateReservationDto dto)
    {
        var tripExists = await context.Trips.AnyAsync(t => t.Id == tripId);
        if (!tripExists)
        {
            return NotFound("Viaje no encontrado.");
        }

        var reservation = await context.Reservations
            .Where(r => r.TripId == tripId && r.PassengerName == dto.PassengerName)
            .OrderByDescending(r => r.CreatedAt)
            .FirstOrDefaultAsync();

        if (reservation == null)
        {
            return NotFound("No existe una reserva para este pasajero en el viaje.");
        }

        if (reservation.Status == ReservationStatus.Cancelled)
        {
            return BadRequest("La reserva está cancelada y no puede abordarse.");
        }

        if (reservation.Status == ReservationStatus.Boarded)
        {
            return BadRequest("El pasajero ya fue marcado como abordado.");
        }

        reservation.Status = ReservationStatus.Boarded;
        await context.SaveChangesAsync();

        return Ok(MapToDto(reservation));
    }

    [HttpPatch("~/api/Trips/{tripId}/Reservations/{reservationId}/manual-status")]
    public async Task<ActionResult<ReservationDto>> UpdatePassengerStatusManual(
        Guid tripId,
        Guid reservationId,
        ManualReservationStatusUpdateDto dto)
    {
        var reservation = await context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId && r.TripId == tripId);

        if (reservation == null)
        {
            return NotFound("Reserva no encontrada para el viaje indicado.");
        }

        var targetStatus = ParseReservationStatus(dto.Status);
        if (targetStatus == null)
        {
            return BadRequest("Estado inválido. Usa: active, boarded o cancelled.");
        }

        if (reservation.Status == targetStatus.Value)
        {
            return Ok(MapToDto(reservation));
        }

        if (targetStatus.Value == ReservationStatus.Boarded && reservation.Status == ReservationStatus.Cancelled)
        {
            return BadRequest("No se puede marcar como abordado una reserva cancelada.");
        }

        var currentCancelled = reservation.Status == ReservationStatus.Cancelled;
        var targetCancelled = targetStatus.Value == ReservationStatus.Cancelled;

        if (!currentCancelled && targetCancelled)
        {
            reservation.Trip.AvailableSeats++;
        }
        else if (currentCancelled && !targetCancelled)
        {
            if (reservation.Trip.AvailableSeats <= 0)
            {
                return BadRequest("No hay cupos disponibles para reactivar esta reserva.");
            }

            reservation.Trip.AvailableSeats--;
        }

        reservation.Status = targetStatus.Value;
        await context.SaveChangesAsync();

        return Ok(MapToDto(reservation));
    }

    private static ReservationStatus? ParseReservationStatus(string value)
    {
        var normalized = value.Trim().ToLowerInvariant();

        if (normalized == "active" || normalized == "activo")
        {
            return ReservationStatus.Active;
        }

        if (normalized == "boarded" || normalized == "abordado")
        {
            return ReservationStatus.Boarded;
        }

        if (normalized == "cancelled" || normalized == "cancelado")
        {
            return ReservationStatus.Cancelled;
        }

        return null;
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
