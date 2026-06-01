using CarPooling.Models;

namespace CarPooling.Dtos;

public class SafeZoneResponseDto
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string? AddressLabel { get; set; }
    public int Purpose { get; set; }
    public string PurposeLabel { get; set; } = string.Empty;
    public bool IsActive { get; set; }
    public int DisplayOrder { get; set; }
    public string? CampusArea { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }

    public static SafeZoneResponseDto FromEntity(SafeZone zone) => new()
    {
        Id = zone.Id,
        Name = zone.Name,
        Description = zone.Description,
        Latitude = zone.Latitude,
        Longitude = zone.Longitude,
        AddressLabel = zone.AddressLabel,
        Purpose = (int)zone.Purpose,
        PurposeLabel = GetPurposeLabel(zone.Purpose),
        IsActive = zone.IsActive,
        DisplayOrder = zone.DisplayOrder,
        CampusArea = zone.CampusArea,
        CreatedAt = zone.CreatedAt,
        UpdatedAt = zone.UpdatedAt
    };

    private static string GetPurposeLabel(SafeZonePurpose purpose) => purpose switch
    {
        SafeZonePurpose.PickupOnly => "Solo recogida",
        SafeZonePurpose.DropoffOnly => "Solo destino",
        _ => "Recogida y destino"
    };
}

public class CreateSafeZoneDto
{
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public int Purpose { get; set; }
    public bool IsActive { get; set; } = true;
    public int DisplayOrder { get; set; }
    public string? CampusArea { get; set; }
}

public class UpdateSafeZoneDto
{
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public int Purpose { get; set; }
    public bool IsActive { get; set; } = true;
    public int DisplayOrder { get; set; }
    public string? CampusArea { get; set; }
}
