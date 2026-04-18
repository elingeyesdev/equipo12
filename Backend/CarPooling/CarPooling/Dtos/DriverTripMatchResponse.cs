using CarPooling.Models;

namespace CarPooling.Dtos;

/// <summary>
/// Viaje ofrecido a un pasajero, ordenado por proximidad al punto de referencia (p. ej. su destino).
/// </summary>
public sealed class DriverTripMatchResponse
{
    public Guid TripId { get; init; }
    public string DriverName { get; init; } = "";
    public double OriginLatitude { get; init; }
    public double OriginLongitude { get; init; }
    public double DestinationLatitude { get; init; }
    public double DestinationLongitude { get; init; }
    public TripStatus Status { get; init; }
    public int AvailableSeats { get; init; }
    /// <summary>Distancia aproximada en km: mínimo entre el punto de referencia y origen o destino del viaje.</summary>
    public double DistanceKm { get; init; }
    public int EtaMinutes { get; init; }
}
