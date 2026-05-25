namespace CarPooling.Dtos;

/// <summary>
/// Viaje ofrecido a un pasajero, ordenado por proximidad al punto de referencia.
/// </summary>
public sealed class DriverTripMatchResponse
{
    public Guid TripId { get; init; }
    public string DriverName { get; init; } = "";
    public LocationDto Origin { get; init; } = null!;
    public LocationDto Destination { get; init; } = null!;
    public string StatusLabel { get; init; } = "";
    public int AvailableSeats { get; init; }
    public double DistanceKm { get; init; }
    public int EtaMinutes { get; init; }
    public string VehicleBrand { get; init; } = "";
    public string VehicleColor { get; init; } = "";
    public string VehiclePlate { get; init; } = "";
}
