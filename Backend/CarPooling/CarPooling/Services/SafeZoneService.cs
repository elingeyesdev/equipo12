using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class SafeZoneService(CarPoolingContext context, GeocodingService geocodingService)
{
    private readonly CarPoolingContext _context = context;
    private readonly GeocodingService _geocoding = geocodingService;

    public async Task<IReadOnlyList<SafeZoneResponseDto>> GetActiveAsync()
    {
        var zones = await _context.SafeZones
            .AsNoTracking()
            .Where(z => z.IsActive)
            .OrderBy(z => z.DisplayOrder)
            .ThenBy(z => z.Name)
            .ToListAsync();

        return zones.ConvertAll(SafeZoneResponseDto.FromEntity);
    }

    public async Task<IReadOnlyList<SafeZoneResponseDto>> GetAllForAdminAsync()
    {
        var zones = await _context.SafeZones
            .AsNoTracking()
            .OrderBy(z => z.DisplayOrder)
            .ThenBy(z => z.Name)
            .ToListAsync();

        return zones.ConvertAll(SafeZoneResponseDto.FromEntity);
    }

    public async Task<SafeZoneResponseDto?> GetByIdAsync(Guid id)
    {
        var zone = await _context.SafeZones.AsNoTracking().FirstOrDefaultAsync(z => z.Id == id);
        return zone is null ? null : SafeZoneResponseDto.FromEntity(zone);
    }

    public async Task<SafeZoneResponseDto> CreateAsync(CreateSafeZoneDto dto)
    {
        ValidateCoordinates(dto.Latitude, dto.Longitude);
        var name = NormalizeName(dto.Name);
        if (!TryParsePurpose(dto.Purpose, out var purpose))
        {
            throw new InvalidOperationException("Purpose invalido. Usa 0 (ambos), 1 (recogida) o 2 (destino).");
        }

        var address = await _geocoding.ReverseGeocodeAsync(dto.Latitude, dto.Longitude);
        var zone = new SafeZone
        {
            Name = name,
            Description = NormalizeOptionalText(dto.Description, 400),
            Latitude = dto.Latitude,
            Longitude = dto.Longitude,
            AddressLabel = address,
            Purpose = purpose,
            IsActive = dto.IsActive,
            DisplayOrder = dto.DisplayOrder,
            CampusArea = NormalizeOptionalText(dto.CampusArea, 80)
        };

        _context.SafeZones.Add(zone);
        await _context.SaveChangesAsync();
        return SafeZoneResponseDto.FromEntity(zone);
    }

    public async Task<SafeZoneResponseDto> UpdateAsync(Guid id, UpdateSafeZoneDto dto)
    {
        var zone = await _context.SafeZones.FirstOrDefaultAsync(z => z.Id == id);
        if (zone is null)
        {
            throw new InvalidOperationException("Zona segura no encontrada.");
        }

        ValidateCoordinates(dto.Latitude, dto.Longitude);
        if (!TryParsePurpose(dto.Purpose, out var purpose))
        {
            throw new InvalidOperationException("Purpose invalido. Usa 0 (ambos), 1 (recogida) o 2 (destino).");
        }

        var coordsChanged = Math.Abs(zone.Latitude - dto.Latitude) > 0.000001
            || Math.Abs(zone.Longitude - dto.Longitude) > 0.000001;

        zone.Name = NormalizeName(dto.Name);
        zone.Description = NormalizeOptionalText(dto.Description, 400);
        zone.Latitude = dto.Latitude;
        zone.Longitude = dto.Longitude;
        zone.Purpose = purpose;
        zone.IsActive = dto.IsActive;
        zone.DisplayOrder = dto.DisplayOrder;
        zone.CampusArea = NormalizeOptionalText(dto.CampusArea, 80);
        zone.UpdatedAt = DateTime.UtcNow;

        if (coordsChanged)
        {
            zone.AddressLabel = await _geocoding.ReverseGeocodeAsync(dto.Latitude, dto.Longitude);
        }

        await _context.SaveChangesAsync();
        return SafeZoneResponseDto.FromEntity(zone);
    }

    public async Task DeleteAsync(Guid id)
    {
        var zone = await _context.SafeZones.FirstOrDefaultAsync(z => z.Id == id);
        if (zone is null)
        {
            throw new InvalidOperationException("Zona segura no encontrada.");
        }

        _context.SafeZones.Remove(zone);
        await _context.SaveChangesAsync();
    }

    private static void ValidateCoordinates(double latitude, double longitude)
    {
        if (latitude is < -90 or > 90 || longitude is < -180 or > 180)
        {
            throw new InvalidOperationException("Coordenadas invalidas.");
        }
    }

    private static string NormalizeName(string name)
    {
        var trimmed = name.Trim();
        if (trimmed.Length == 0)
        {
            throw new InvalidOperationException("El nombre es obligatorio.");
        }

        return trimmed.Length > 120 ? trimmed[..120] : trimmed;
    }

    private static string? NormalizeOptionalText(string? value, int maxLength)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return null;
        }

        var trimmed = value.Trim();
        return trimmed.Length > maxLength ? trimmed[..maxLength] : trimmed;
    }

    private static bool TryParsePurpose(int purposeValue, out SafeZonePurpose purpose)
    {
        if (!Enum.IsDefined(typeof(SafeZonePurpose), purposeValue))
        {
            purpose = SafeZonePurpose.Both;
            return false;
        }

        purpose = (SafeZonePurpose)purposeValue;
        return true;
    }
}
