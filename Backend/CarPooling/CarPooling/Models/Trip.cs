using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Trip
{
    public Guid Id { get; set; }


    // --- Location references (reemplazan coordenadas directas) ---
    [Required]
    public Guid OriginLocationId { get; set; }

    public Location OriginLocation { get; set; } = null!;

    [Required]
    public Guid DestinationLocationId { get; set; }

    public Location DestinationLocation { get; set; } = null!;

    // --- Status (FK a tabla de estados) ---
    public int StatusId { get; set; }

    public TripStatusEntity StatusEntity { get; set; } = null!;

    // --- Asientos ---
    public int OfferedSeats { get; set; } = 4;

    public int AvailableSeats { get; set; } = 4;

    [Column(TypeName = "decimal(10,2)")]
    public decimal FareAmount { get; set; } = 10m;

    // --- Vehículo ---
    public Guid? VehicleId { get; set; }

    public Vehicle? Vehicle { get; set; }

    // --- Conductor ---
    [MaxLength(100)]
    public string DriverName { get; set; } = "";

    public Guid? DriverUserId { get; set; }

    [ForeignKey(nameof(DriverUserId))]
    public User? DriverUser { get; set; }

    public ICollection<Reservation> Reservations { get; set; } = [];

    public Guid? TripScheduleId { get; set; }

    [ForeignKey(nameof(TripScheduleId))]
    public TripSchedule? TripSchedule { get; set; }

    public DateTime? ScheduledDate { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
    public DateTime? CancelledAt { get; set; }
    public DateTime? StartedAt { get; set; }
    public DateTime? FinishedAt { get; set; }

    public double? CurrentLatitude { get; set; }
    public double? CurrentLongitude { get; set; }

    public int BookmarkUseCount { get; set; }
    public DateTime? BookmarkLastUsedAt { get; set; }
}
