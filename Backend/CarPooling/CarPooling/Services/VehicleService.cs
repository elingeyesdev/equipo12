using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class VehicleService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

    /// <summary>
    /// Crea un vehículo para el conductor. Si es el primer vehículo, se marca como activo.
    /// </summary>
    public async Task<Vehicle> CreateForDriverAsync(Guid ownerUserId, DriverProfileDto dto)
    {
        var hasOtherVehicles = await _context.Vehicles.AnyAsync(v => v.OwnerUserId == ownerUserId);

        var vehicle = new Vehicle
        {
            OwnerUserId = ownerUserId,
            LicensePlate = dto.LicensePlate.Trim().ToUpperInvariant(),
            Brand = dto.VehicleBrand.Trim(),
            Model = dto.VehicleModel?.Trim() ?? "",
            Color = dto.VehicleColor.Trim(),
            VehicleYear = dto.VehicleYear,
            TotalSeats = dto.AvailableSeats,
            IsActive = true,
            IsVerified = false
        };

        _context.Vehicles.Add(vehicle);
        await _context.SaveChangesAsync();

        return vehicle;
    }

    /// <summary>
    /// Actualiza el vehículo existente o crea uno nuevo si no hay.
    /// </summary>
    public async Task<Vehicle> UpsertForDriverAsync(Guid ownerUserId, DriverProfileDto dto)
    {
        var existing = await _context.Vehicles
            .Where(v => v.OwnerUserId == ownerUserId && v.IsActive)
            .FirstOrDefaultAsync();

        if (existing != null)
        {
            existing.LicensePlate = dto.LicensePlate.Trim().ToUpperInvariant();
            existing.Brand = dto.VehicleBrand.Trim();
            existing.Model = dto.VehicleModel?.Trim() ?? "";
            existing.Color = dto.VehicleColor.Trim();
            existing.VehicleYear = dto.VehicleYear;
            existing.TotalSeats = dto.AvailableSeats;
            await _context.SaveChangesAsync();
            return existing;
        }

        return await CreateForDriverAsync(ownerUserId, dto);
    }

    /// <summary>
    /// Obtiene el primer vehículo activo del conductor, o null si no tiene.
    /// </summary>
    public async Task<Vehicle?> GetActiveForDriverAsync(Guid ownerUserId)
    {
        return await _context.Vehicles
            .Where(v => v.OwnerUserId == ownerUserId && v.IsActive)
            .FirstOrDefaultAsync();
    }

    /// <summary>
    /// Obtiene todos los vehículos del conductor.
    /// </summary>
    public async Task<List<Vehicle>> GetAllForDriverAsync(Guid ownerUserId)
    {
        return await _context.Vehicles
            .Where(v => v.OwnerUserId == ownerUserId)
            .OrderByDescending(v => v.IsActive)
            .ThenByDescending(v => v.CreatedAt)
            .ToListAsync();
    }

    public async Task<Vehicle> CreateAsync(Guid ownerUserId, string licensePlate, string brand,
        string model, string color, int? vehicleYear, int totalSeats)
    {
        var vehicle = new Vehicle
        {
            OwnerUserId = ownerUserId,
            LicensePlate = licensePlate.Trim().ToUpperInvariant(),
            Brand = brand.Trim(),
            Model = (model ?? "").Trim(),
            Color = color.Trim(),
            VehicleYear = vehicleYear,
            TotalSeats = totalSeats,
            IsActive = true,
            IsVerified = false
        };
        _context.Vehicles.Add(vehicle);
        await _context.SaveChangesAsync();
        return vehicle;
    }

    public async Task<Vehicle> UpdateAsync(Guid vehicleId, Guid ownerUserId, string licensePlate,
        string brand, string model, string color, int? vehicleYear, int totalSeats)
    {
        var vehicle = await _context.Vehicles.FirstOrDefaultAsync(v => v.Id == vehicleId && v.OwnerUserId == ownerUserId)
            ?? throw new InvalidOperationException("Vehículo no encontrado.");

        vehicle.LicensePlate = licensePlate.Trim().ToUpperInvariant();
        vehicle.Brand = brand.Trim();
        vehicle.Model = (model ?? "").Trim();
        vehicle.Color = color.Trim();
        vehicle.VehicleYear = vehicleYear;
        vehicle.TotalSeats = totalSeats;
        await _context.SaveChangesAsync();
        return vehicle;
    }

    public async Task DeleteAsync(Guid vehicleId, Guid ownerUserId)
    {
        var vehicle = await _context.Vehicles.FirstOrDefaultAsync(v => v.Id == vehicleId && v.OwnerUserId == ownerUserId)
            ?? throw new InvalidOperationException("Vehículo no encontrado.");

        var usedInTrips = await _context.Trips.AnyAsync(t => t.VehicleId == vehicleId);
        if (usedInTrips)
            throw new InvalidOperationException("No se puede eliminar: el vehículo está asociado a viajes.");

        _context.Vehicles.Remove(vehicle);
        await _context.SaveChangesAsync();
    }
}
