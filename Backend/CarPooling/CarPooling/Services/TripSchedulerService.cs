using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using CarPooling.Data;
using CarPooling.Models;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class TripSchedulerService : BackgroundService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<TripSchedulerService> _logger;

    public TripSchedulerService(IServiceProvider serviceProvider, ILogger<TripSchedulerService> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("TripSchedulerService is starting.");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await GenerateTripsFromSchedulesAsync();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error occurred while generating trips from schedules.");
            }

            // Sleep until next midnight
            var now = DateTime.UtcNow;
            var nextRun = now.Date.AddDays(1); // Midnight tomorrow
            var delay = nextRun - now;
            
            _logger.LogInformation("TripSchedulerService will run next in {Delay}.", delay);
            await Task.Delay(delay, stoppingToken);
        }
    }

    private async Task GenerateTripsFromSchedulesAsync()
    {
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();

        var now = DateTime.UtcNow;
        var startDate = now.Date;
        var endDate = startDate.AddDays(7);

        _logger.LogInformation("Generating trips from schedules between {Start} and {End}.", startDate, endDate);

        var activeSchedules = await context.TripSchedules
            .Include(ts => ts.DriverUser)
            .Include(ts => ts.OriginLocation)
            .Include(ts => ts.DestinationLocation)
            .Include(ts => ts.RecurringReservations)
            .Where(ts => ts.IsActive && ts.StartDate <= endDate && (ts.EndDate == null || ts.EndDate >= startDate))
            .ToListAsync();

        foreach (var schedule in activeSchedules)
        {
            var activeDays = schedule.DaysOfWeek
                .Split(',', StringSplitOptions.RemoveEmptyEntries)
                .Select(d => int.TryParse(d, out var val) ? (DayOfWeek?)(val % 7) : null)
                .Where(d => d.HasValue)
                .Select(d => d!.Value)
                .ToHashSet();

            for (var date = startDate; date <= endDate; date = date.AddDays(1))
            {
                if (!activeDays.Contains(date.DayOfWeek)) continue;

                var scheduledDateTime = date.Add(schedule.DepartureTime);

                var exists = await context.Trips.AnyAsync(t => t.TripScheduleId == schedule.Id && t.ScheduledDate == scheduledDateTime);
                if (exists) continue;

                var tripId = Guid.NewGuid();
                var trip = new Trip
                {
                    Id = tripId,
                    TripScheduleId = schedule.Id,
                    OriginLocationId = schedule.OriginLocationId,
                    DestinationLocationId = schedule.DestinationLocationId,
                    StatusId = 1, // scheduled
                    OfferedSeats = schedule.OfferedSeats,
                    AvailableSeats = schedule.OfferedSeats,
                    FareAmount = schedule.FareAmount,
                    VehicleId = schedule.VehicleId,
                    DriverName = schedule.DriverUser?.FullName ?? "Chofer",
                    DriverUserId = schedule.DriverUserId,
                    ScheduledDate = scheduledDateTime,
                    CreatedAt = now
                };

                foreach (var subscription in schedule.RecurringReservations.Where(r => r.IsActive))
                {
                    if (trip.AvailableSeats >= subscription.SeatsReserved)
                    {
                        var reservation = new Reservation
                        {
                            Id = Guid.NewGuid(),
                            TripId = tripId,
                            PassengerUserId = subscription.PassengerUserId,
                            SeatsReserved = subscription.SeatsReserved,
                            StatusId = 2, // confirmed
                            CreatedAt = now,
                            RecurringReservationId = subscription.Id,
                            BoardingCode = new Random().Next(1000, 9999).ToString()
                        };
                        context.Reservations.Add(reservation);
                        trip.AvailableSeats -= subscription.SeatsReserved;
                    }
                }

                context.Trips.Add(trip);
                _logger.LogInformation("Instantiated trip {TripId} from schedule {ScheduleId} for date {Date}.", trip.Id, schedule.Id, scheduledDateTime);
            }
        }

        await context.SaveChangesAsync();
    }
}