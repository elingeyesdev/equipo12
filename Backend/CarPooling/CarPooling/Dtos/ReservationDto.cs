using CarPooling.Models;

namespace CarPooling.Dtos;

public class ReservationDto
{
    public Guid Id { get; set; }
    public Guid TripId { get; set; }
    public string PassengerName { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}
