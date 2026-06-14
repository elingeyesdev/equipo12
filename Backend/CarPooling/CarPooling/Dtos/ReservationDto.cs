namespace CarPooling.Dtos;

public class ReservationDto
{
    public Guid Id { get; set; }
    public Guid TripId { get; set; }
    public Guid PassengerUserId { get; set; }
    public string PassengerName { get; set; } = string.Empty;
    public string? PassengerProfilePicture { get; set; }
    public double PassengerRating { get; set; } = 5.0;
    public int SeatsReserved { get; set; }
    public string Status { get; set; } = string.Empty;
    public int StatusId { get; set; }
    public string? BoardingCode { get; set; }
    public DateTime CreatedAt { get; set; }
}
