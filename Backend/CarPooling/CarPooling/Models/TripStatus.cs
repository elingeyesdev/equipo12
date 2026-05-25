using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

/// <summary>Legacy enum - será reemplazado por <see cref="TripStatusEntity"/>.</summary>
public enum TripStatus
{
    AwaitingDestination = 0,
    Ready = 1,
    Cancelled = 2,
    InProgress = 3,
    Finished = 4
}

/// <summary>
/// Tabla de estados de viaje. Fuente de verdad para los estados del sistema.
/// Mapeada a la tabla <c>TripStatuses</c>.
/// </summary>
public class TripStatusEntity
{
    public int Id { get; set; }

    [Required]
    [MaxLength(30)]
    public string Code { get; set; } = string.Empty;

    [Required]
    [MaxLength(40)]
    public string LabelEs { get; set; } = string.Empty;

    public bool IsActiveState { get; set; }

    public ICollection<Trip> Trips { get; set; } = [];

    public const string Scheduled = "scheduled";
    public const string Started = "started";
    public const string Finished = "finished";
    public const string Cancelled = "cancelled";
}
