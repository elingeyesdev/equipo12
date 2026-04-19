using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class Trip
{
    public Guid Id { get; set; }

    public TripKind Kind { get; set; } = TripKind.Regular;

    [Range(-90, 90)]
    public double OriginLatitude { get; set; }

    [Range(-180, 180)]
    public double OriginLongitude { get; set; }

    [Range(-90, 90)]
    public double? DestinationLatitude { get; set; }

    [Range(-180, 180)]
    public double? DestinationLongitude { get; set; }

    public TripStatus Status { get; set; } = TripStatus.AwaitingDestination;

    public int AvailableSeats { get; set; } = 4;

    /// <summary>Nombre del conductor que publicó el viaje (persistido al crear el origen).</summary>
    public string DriverName { get; set; } = "";

    /// <summary>Usuario conductor dueño del viaje (para recuperar viaje activo al volver a iniciar sesión).</summary>
    public Guid? DriverUserId { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public DateTime? CancelledAt { get; set; }

    /// <summary>Solo para <see cref="TripKind.UserBookmark"/>: veces aplicado al mapa.</summary>
    public int BookmarkUseCount { get; set; }

    /// <summary>Solo para <see cref="TripKind.UserBookmark"/>.</summary>
    public DateTime? BookmarkLastUsedAt { get; set; }
}
