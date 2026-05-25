using CarPooling.Models;

namespace CarPooling.Dtos;

public class TripResponse
{
    public Guid Id { get; init; }
    public TripKind Kind { get; init; } = TripKind.Regular;
    public LocationDto Origin { get; init; } = null!;
    public LocationDto Destination { get; init; } = null!;
    public string StatusLabel { get; init; } = "";
    public int StatusId { get; init; }
    public int OfferedSeats { get; init; }
    public int AvailableSeats { get; init; }
    public Guid? VehicleId { get; init; }
    public string DriverName { get; init; } = "";
    public Guid? DriverUserId { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime? UpdatedAt { get; init; }
    public DateTime? CancelledAt { get; init; }
}
