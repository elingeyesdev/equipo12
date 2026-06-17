using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class RecurringReservationController(CarPoolingContext context) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<RecurringReservationResponse>> SubscribeAsync([FromBody] CreateRecurringReservationDto dto)
    {
        var schedule = await context.TripSchedules.FindAsync(dto.TripScheduleId);
        if (schedule is null) return NotFound("Schedule not found");

        var passenger = await context.Users.FindAsync(dto.PassengerUserId);
        if (passenger is null) return BadRequest("Passenger not found");

        // Check if already subscribed
        var existing = await context.RecurringReservations
            .FirstOrDefaultAsync(r => r.TripScheduleId == dto.TripScheduleId && r.PassengerUserId == dto.PassengerUserId && r.IsActive);
        if (existing is not null) return BadRequest("Already subscribed to this schedule");

        var subscription = new RecurringReservation
        {
            Id = Guid.NewGuid(),
            TripScheduleId = dto.TripScheduleId,
            PassengerUserId = dto.PassengerUserId,
            SeatsReserved = dto.SeatsReserved,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        context.RecurringReservations.Add(subscription);
        await context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetSubscriptionByIdAsync), new { id = subscription.Id }, MapToResponse(subscription, passenger.FullName));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<RecurringReservationResponse>> GetSubscriptionByIdAsync(Guid id)
    {
        var subscription = await context.RecurringReservations
            .Include(r => r.PassengerUser)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.OriginLocation)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.DestinationLocation)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.DriverUser)
            .FirstOrDefaultAsync(r => r.Id == id);

        if (subscription is null) return NotFound();

        return Ok(MapToResponse(subscription, subscription.PassengerUser.FullName));
    }

    [HttpGet("passenger/{passengerUserId:guid}")]
    public async Task<ActionResult<IEnumerable<RecurringReservationResponse>>> GetPassengerSubscriptionsAsync(Guid passengerUserId)
    {
        var subscriptions = await context.RecurringReservations
            .Include(r => r.PassengerUser)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.OriginLocation)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.DestinationLocation)
            .Include(r => r.TripSchedule)
                .ThenInclude(s => s.DriverUser)
            .Where(r => r.PassengerUserId == passengerUserId)
            .ToListAsync();

        return Ok(subscriptions.Select(s => MapToResponse(s, s.PassengerUser.FullName)));
    }

    [HttpPost("{id:guid}/cancel")]
    public async Task<IActionResult> CancelSubscriptionAsync(Guid id)
    {
        var subscription = await context.RecurringReservations.FindAsync(id);
        if (subscription is null) return NotFound();

        subscription.IsActive = false;
        await context.SaveChangesAsync();
        return NoContent();
    }

    private static RecurringReservationResponse MapToResponse(RecurringReservation r, string passengerName)
    {
        return new RecurringReservationResponse
        {
            Id = r.Id,
            TripScheduleId = r.TripScheduleId,
            PassengerUserId = r.PassengerUserId,
            PassengerName = passengerName,
            SeatsReserved = r.SeatsReserved,
            IsActive = r.IsActive,
            CreatedAt = r.CreatedAt,
            OriginAddress = r.TripSchedule?.OriginLocation?.AddressLabel ?? "",
            DestinationAddress = r.TripSchedule?.DestinationLocation?.AddressLabel ?? "",
            DepartureTime = r.TripSchedule?.DepartureTime ?? TimeSpan.Zero,
            DaysOfWeek = r.TripSchedule?.DaysOfWeek ?? "",
            DriverName = r.TripSchedule?.DriverUser?.FullName ?? ""
        };
    }
}