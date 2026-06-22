using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class RecurringReservation
{
    public Guid Id { get; set; }

    [Required]
    public Guid TripScheduleId { get; set; }

    [ForeignKey(nameof(TripScheduleId))]
    public TripSchedule TripSchedule { get; set; } = null!;

    [Required]
    public Guid PassengerUserId { get; set; }

    [ForeignKey(nameof(PassengerUserId))]
    public User PassengerUser { get; set; } = null!;

    public int SeatsReserved { get; set; } = 1;

    public bool IsActive { get; set; } = true;

    public bool IsAccepted { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<Reservation> Reservations { get; set; } = [];
}