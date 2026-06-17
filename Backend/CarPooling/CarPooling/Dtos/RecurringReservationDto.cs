using System;
using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CreateRecurringReservationDto
{
    [Required]
    public Guid TripScheduleId { get; set; }

    [Required]
    public Guid PassengerUserId { get; set; }

    [Range(1, 10)]
    public int SeatsReserved { get; set; } = 1;
}

public class RecurringReservationResponse
{
    public Guid Id { get; set; }
    public Guid TripScheduleId { get; set; }
    public Guid PassengerUserId { get; set; }
    public string PassengerName { get; set; } = "";
    public int SeatsReserved { get; set; }
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }

    public string OriginAddress { get; set; } = "";
    public string DestinationAddress { get; set; } = "";
    public TimeSpan DepartureTime { get; set; }
    public string DaysOfWeek { get; set; } = "";
    public string DriverName { get; set; } = "";
}