using System;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using Xunit;

namespace CarPooling.Tests;

public class UnitTests
{
    private CarPoolingContext GetInMemoryContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<CarPoolingContext>()
            .UseInMemoryDatabase(databaseName: dbName)
            .Options;
        return new CarPoolingContext(options);
    }

    [Fact]
    public async Task VehicleService_CreateForDriverAsync_SavesAndNormalizesVehicle()
    {
        // Arrange
        using var context = GetInMemoryContext("CreateVehicleDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var dto = new DriverProfileDto
        {
            LicensePlate = "  abc-123  ",
            VehicleBrand = "  Ford ",
            VehicleModel = " Fiesta ",
            VehicleColor = " Azul ",
            VehicleYear = 2018,
            AvailableSeats = 4
        };

        // Act
        var result = await service.CreateForDriverAsync(ownerId, dto);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(ownerId, result.OwnerUserId);
        Assert.Equal("ABC-123", result.LicensePlate); // Normalized plate
        Assert.Equal("Ford", result.Brand);
        Assert.Equal("Fiesta", result.Model);
        Assert.Equal("Azul", result.Color);
        Assert.True(result.IsActive);
        Assert.False(result.IsVerified);

        var saved = await context.Vehicles.FirstOrDefaultAsync(v => v.OwnerUserId == ownerId);
        Assert.NotNull(saved);
        Assert.Equal("ABC-123", saved.LicensePlate);
    }

    [Fact]
    public async Task VehicleService_DeleteAsync_ThrowsExceptionWhenVehicleInTrip()
    {
        // Arrange
        using var context = GetInMemoryContext("DeleteVehicleDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var vehicleId = Guid.NewGuid();

        // Seed vehicle
        var vehicle = new Vehicle
        {
            Id = vehicleId,
            OwnerUserId = ownerId,
            LicensePlate = "XYZ987",
            Brand = "Nissan",
            Color = "Gris",
            TotalSeats = 4,
            IsActive = true
        };
        context.Vehicles.Add(vehicle);

        // Seed trip using this vehicle
        var trip = new Trip
        {
            Id = Guid.NewGuid(),
            VehicleId = vehicleId,
            DriverUserId = ownerId,
            DriverName = "Test Driver",
            OriginLocationId = Guid.NewGuid(),
            DestinationLocationId = Guid.NewGuid(),
            StatusId = 1,
            CreatedAt = DateTime.UtcNow
        };
        context.Trips.Add(trip);
        await context.SaveChangesAsync();

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.DeleteAsync(vehicleId, ownerId)
        );
        Assert.Contains("No se puede eliminar", exception.Message);
    }

    [Fact]
    public async Task VehicleService_UpsertForDriverAsync_UpdatesExistingActiveVehicle()
    {
        // Arrange
        using var context = GetInMemoryContext("UpsertVehicleDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var vehicleId = Guid.NewGuid();

        // Seed an existing active vehicle
        var existing = new Vehicle
        {
            Id = vehicleId,
            OwnerUserId = ownerId,
            LicensePlate = "OLD456",
            Brand = "Toyota",
            Model = "Corolla",
            Color = "Rojo",
            VehicleYear = 2015,
            TotalSeats = 5,
            IsActive = true
        };
        context.Vehicles.Add(existing);
        await context.SaveChangesAsync();

        var dto = new DriverProfileDto
        {
            LicensePlate = "new789",
            VehicleBrand = "Toyota",
            VehicleModel = "Yaris",
            VehicleColor = "Negro",
            VehicleYear = 2022,
            AvailableSeats = 4
        };

        // Act
        var result = await service.UpsertForDriverAsync(ownerId, dto);

        // Assert
        Assert.Equal(vehicleId, result.Id); // Same ID (updated in place)
        Assert.Equal("NEW789", result.LicensePlate);
        Assert.Equal("Yaris", result.Model);
        Assert.Equal("Negro", result.Color);
        Assert.Equal(4, result.TotalSeats);

        var count = await context.Vehicles.CountAsync(v => v.OwnerUserId == ownerId);
        Assert.Equal(1, count); // Did not add a new record
    }

    [Fact]
    public async Task RatingService_CreateRatingAsync_ThrowsWhenTripNotFinished()
    {
        // Arrange
        using var context = GetInMemoryContext("RatingTripDb");
        var service = new RatingService(context);
        var tripId = Guid.NewGuid();
        var driverId = Guid.NewGuid();
        var passengerId = Guid.NewGuid();

        // Seed trip in "Scheduled" state (StatusId = 1) instead of "Finished" (StatusId = 4)
        var trip = new Trip
        {
            Id = tripId,
            StatusId = 1, // Scheduled
            DriverUserId = driverId,
            DriverName = "Driver Name",
            OriginLocationId = Guid.NewGuid(),
            DestinationLocationId = Guid.NewGuid(),
            CreatedAt = DateTime.UtcNow
        };
        context.Trips.Add(trip);

        var passengerUser = new User { Id = passengerId, FullName = "Passenger", Email = "p@p.com" };
        context.Users.Add(passengerUser);
        await context.SaveChangesAsync();

        var dto = new CreateTripRatingDto
        {
            EvaluatedUserId = driverId,
            Score = 5,
            Comment = "Great driver"
        };

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CreateRatingAsync(tripId, passengerId, dto)
        );
        Assert.Contains("completados/finalizados", exception.Message);
    }
}
