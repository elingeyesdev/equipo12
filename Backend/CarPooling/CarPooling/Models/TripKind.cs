namespace CarPooling.Models;

/// <summary>
/// Distingue viajes operativos de marcadores guardados en la misma tabla <see cref="Trip"/>.
/// </summary>
public enum TripKind
{
    Regular = 0,
    /// <summary>Marcador de lugar o ruta favorita del usuario (no admite reservas ni match).</summary>
    UserBookmark = 1
}
