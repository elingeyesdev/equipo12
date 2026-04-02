using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Reservation
{
    public Guid Id { get; set; }

    [Required]
    public Guid TripId { get; set; }

    [Required]
    [MaxLength(100)]
    public string PassengerName { get; set; } = string.Empty;

    public ReservationStatus Status { get; set; } = ReservationStatus.Active;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [ForeignKey("TripId")]
    public Trip Trip { get; set; } = null!;
}
