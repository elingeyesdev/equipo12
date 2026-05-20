using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

/// <summary>Legacy enum - será reemplazado por <see cref="ReservationStatusEntity"/>.</summary>
public enum ReservationStatus
{
    Active = 0,
    Cancelled = 1,
    Boarded = 2
}

/// <summary>
/// Tabla de estados de reserva. Fuente de verdad para los estados del sistema.
/// Mapeada a la tabla <c>ReservationStatuses</c>.
/// </summary>
public class ReservationStatusEntity
{
    public int Id { get; set; }

    [Required]
    [MaxLength(30)]
    public string Code { get; set; } = string.Empty;

    [Required]
    [MaxLength(40)]
    public string LabelEs { get; set; } = string.Empty;

    public ICollection<Reservation> Reservations { get; set; } = [];

    public const string Pending = "pending";
    public const string Confirmed = "confirmed";
    public const string Boarded = "boarded";
    public const string Cancelled = "cancelled";
}
