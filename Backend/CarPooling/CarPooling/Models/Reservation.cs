using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Reservation
{
    public Guid Id { get; set; }

    [Required]
    public Guid TripId { get; set; }

    [Required]
    public Guid PassengerUserId { get; set; }

    [ForeignKey(nameof(PassengerUserId))]
    public User PassengerUser { get; set; } = null!;

    public int SeatsReserved { get; set; } = 1;

    public int StatusId { get; set; }

    public ReservationStatusEntity StatusEntity { get; set; } = null!;

    [MaxLength(10)]
    public string? BoardingCode { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public Guid? RecurringReservationId { get; set; }

    [ForeignKey(nameof(RecurringReservationId))]
    public RecurringReservation? RecurringReservation { get; set; }

    [ForeignKey(nameof(TripId))]
    public Trip Trip { get; set; } = null!;
}
