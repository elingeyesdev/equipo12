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
public class TripScheduleController(CarPoolingContext context) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<TripScheduleResponse>> CreateScheduleAsync([FromBody] CreateTripScheduleDto dto)
    {
        var driver = await context.Users.FindAsync(dto.DriverUserId);
        if (driver is null) return BadRequest("Driver user not found");

        var originLocation = new Location
        {
            Id = Guid.NewGuid(),
            Latitude = dto.OriginLatitude,
            Longitude = dto.OriginLongitude,
            AddressLabel = dto.OriginAddress ?? ""
        };

        var destinationLocation = new Location
        {
            Id = Guid.NewGuid(),
            Latitude = dto.DestinationLatitude,
            Longitude = dto.DestinationLongitude,
            AddressLabel = dto.DestinationAddress ?? ""
        };

        context.Locations.Add(originLocation);
        context.Locations.Add(destinationLocation);

        var schedule = new TripSchedule
        {
            Id = Guid.NewGuid(),
            DriverUserId = dto.DriverUserId,
            OriginLocationId = originLocation.Id,
            DestinationLocationId = destinationLocation.Id,
            DepartureTime = dto.DepartureTime,
            DaysOfWeek = dto.DaysOfWeek,
            StartDate = dto.StartDate,
            EndDate = dto.EndDate,
            VehicleId = dto.VehicleId,
            OfferedSeats = dto.OfferedSeats,
            FareAmount = dto.FareAmount,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        context.TripSchedules.Add(schedule);
        await context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetScheduleByIdAsync), new { id = schedule.Id }, MapToResponse(schedule, driver.FullName));
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<TripScheduleResponse>> GetScheduleByIdAsync(Guid id)
    {
        var schedule = await context.TripSchedules
            .Include(s => s.DriverUser)
            .Include(s => s.OriginLocation)
            .Include(s => s.DestinationLocation)
            .FirstOrDefaultAsync(s => s.Id == id);

        if (schedule is null) return NotFound();

        return Ok(MapToResponse(schedule, schedule.DriverUser.FullName));
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<TripScheduleResponse>>> GetActiveSchedulesAsync()
    {
        var schedules = await context.TripSchedules
            .Include(s => s.DriverUser)
            .Include(s => s.OriginLocation)
            .Include(s => s.DestinationLocation)
            .Where(s => s.IsActive)
            .ToListAsync();

        return Ok(schedules.Select(s => MapToResponse(s, s.DriverUser.FullName)));
    }

    [HttpGet("driver/{driverUserId:guid}")]
    public async Task<ActionResult<IEnumerable<TripScheduleResponse>>> GetDriverSchedulesAsync(Guid driverUserId)
    {
        var schedules = await context.TripSchedules
            .Include(s => s.DriverUser)
            .Include(s => s.OriginLocation)
            .Include(s => s.DestinationLocation)
            .Where(s => s.DriverUserId == driverUserId)
            .ToListAsync();

        return Ok(schedules.Select(s => MapToResponse(s, s.DriverUser.FullName)));
    }

    [HttpPost("{id:guid}/toggle")]
    public async Task<IActionResult> ToggleScheduleAsync(Guid id, [FromQuery] bool active)
    {
        var schedule = await context.TripSchedules.FindAsync(id);
        if (schedule is null) return NotFound();

        schedule.IsActive = active;
        await context.SaveChangesAsync();
        return NoContent();
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteScheduleAsync(Guid id)
    {
        var schedule = await context.TripSchedules.FindAsync(id);
        if (schedule is null) return NotFound();

        context.TripSchedules.Remove(schedule);
        await context.SaveChangesAsync();
        return NoContent();
    }

    private static TripScheduleResponse MapToResponse(TripSchedule s, string driverName)
    {
        return new TripScheduleResponse
        {
            Id = s.Id,
            DriverUserId = s.DriverUserId,
            DriverName = driverName,
            Origin = new LocationDto
            {
                Id = s.OriginLocationId,
                Latitude = s.OriginLocation.Latitude,
                Longitude = s.OriginLocation.Longitude,
                AddressLabel = s.OriginLocation.AddressLabel
            },
            Destination = new LocationDto
            {
                Id = s.DestinationLocationId,
                Latitude = s.DestinationLocation.Latitude,
                Longitude = s.DestinationLocation.Longitude,
                AddressLabel = s.DestinationLocation.AddressLabel
            },
            DepartureTime = s.DepartureTime,
            DaysOfWeek = s.DaysOfWeek,
            StartDate = s.StartDate,
            EndDate = s.EndDate,
            VehicleId = s.VehicleId,
            OfferedSeats = s.OfferedSeats,
            FareAmount = s.FareAmount,
            IsActive = s.IsActive,
            CreatedAt = s.CreatedAt
        };
    }
}