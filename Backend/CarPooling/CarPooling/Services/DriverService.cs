using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class DriverService(CarPoolingContext context, VehicleService vehicleService)
{
    private readonly CarPoolingContext _context = context;
    private readonly VehicleService _vehicleService = vehicleService;

    /// <summary>
    /// Crea un DriverProfile y un Vehicle inicial para un conductor nuevo.
    /// </summary>
    public async Task<(DriverProfile profile, Vehicle vehicle)> CreateProfileAsync(Guid userId, DriverProfileDto dto)
    {
        var profile = new DriverProfile
        {
            UserId = userId,
            LicenseNumber = dto.LicenseNumber,
            IsVerified = false
        };

        _context.DriverProfiles.Add(profile);

        var vehicle = await _vehicleService.CreateForDriverAsync(userId, dto);

        await _context.SaveChangesAsync();

        return (profile, vehicle);
    }

    /// <summary>
    /// Actualiza el perfil de conductor y su vehículo.
    /// Crea el perfil si no existe.
    /// </summary>
    public async Task<DriverProfile> UpsertProfileAsync(Guid userId, DriverProfileDto dto)
    {
        var profile = await _context.DriverProfiles
            .FirstOrDefaultAsync(p => p.UserId == userId);

        if (profile == null)
        {
            var (newProfile, _) = await CreateProfileAsync(userId, dto);
            return newProfile;
        }

        profile.LicenseNumber = dto.LicenseNumber;
        profile.UpdatedAt = DateTime.UtcNow;

        await _vehicleService.UpsertForDriverAsync(userId, dto);

        await _context.SaveChangesAsync();
        return profile;
    }

    /// <summary>
    /// Obtiene el perfil de conductor con su vehículo activo.
    /// </summary>
    public async Task<(DriverProfile? profile, Vehicle? vehicle)> GetWithVehicleAsync(Guid userId)
    {
        var profile = await _context.DriverProfiles
            .FirstOrDefaultAsync(p => p.UserId == userId);

        var vehicle = profile != null
            ? await _vehicleService.GetActiveForDriverAsync(userId)
            : null;

        return (profile, vehicle);
    }
}
