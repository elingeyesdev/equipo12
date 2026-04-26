namespace CarPooling.Models;

/// <summary>
/// Registro por usuario para ocultar un viaje del historial sin borrar datos operativos.
/// </summary>
public class UserHistoryHiddenTrip
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid TripId { get; set; }
    public DateTime HiddenAt { get; set; } = DateTime.UtcNow;
}
