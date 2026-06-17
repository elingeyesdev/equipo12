using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class TripSchedule
{
    public Guid Id { get; set; }

    [Required]
    public Guid DriverUserId { get; set; }

    [ForeignKey(nameof(DriverUserId))]
    public User DriverUser { get; set; } = null!;

    [Required]
    public Guid OriginLocationId { get; set; }

    public Location OriginLocation { get; set; } = null!;

    [Required]
    public Guid DestinationLocationId { get; set; }

    public Location DestinationLocation { get; set; } = null!;

    [Required]
    public TimeSpan DepartureTime { get; set; }

    [Required]
    [MaxLength(50)]
    public string DaysOfWeek { get; set; } = ""; // e.g., "1,2,3,4,5" (Monday to Friday)

    [Required]
    public DateTime StartDate { get; set; }

    public DateTime? EndDate { get; set; }

    public Guid? VehicleId { get; set; }

    [ForeignKey(nameof(VehicleId))]
    public Vehicle? Vehicle { get; set; }

    public int OfferedSeats { get; set; } = 4;

    [Column(TypeName = "decimal(10,2)")]
    public decimal FareAmount { get; set; } = 10m;

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<Trip> Trips { get; set; } = [];
    public ICollection<RecurringReservation> RecurringReservations { get; set; } = [];
}