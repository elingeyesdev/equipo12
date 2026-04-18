using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CoordinateRequest
{
    [Required]
    [Range(-90, 90)]
    public double Latitude { get; set; }

    [Required]
    [Range(-180, 180)]
    public double Longitude { get; set; }

    /// <summary>Opcional: se usa al crear el origen del viaje para listados de match.</summary>
    [MaxLength(100)]
    public string? DriverName { get; set; }

    /// <summary>Opcional: id del usuario conductor (sesión móvil) para recuperar el viaje activo.</summary>
    public Guid? DriverUserId { get; set; }
}
