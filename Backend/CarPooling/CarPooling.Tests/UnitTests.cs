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
    public async Task VehicleService_DeleteAsync_RemovesVehicleWhenNotUsedInTrips()
    {
        // Arrange
        using var context = GetInMemoryContext("DeleteUnusedVehicleDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var vehicleId = Guid.NewGuid();

        context.Vehicles.Add(new Vehicle
        {
            Id = vehicleId,
            OwnerUserId = ownerId,
            LicensePlate = "DEL001",
            Brand = "Suzuki",
            Model = "Swift",
            Color = "Blanco",
            TotalSeats = 4,
            IsActive = true
        });
        await context.SaveChangesAsync();

        // Act
        await service.DeleteAsync(vehicleId, ownerId);

        // Assert
        Assert.False(await context.Vehicles.AnyAsync(v => v.Id == vehicleId));
    }

    [Fact]
    public async Task VehicleService_UpdateAsync_ThrowsWhenVehicleBelongsToAnotherDriver()
    {
        // Arrange
        using var context = GetInMemoryContext("UpdateOtherDriverVehicleDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var otherDriverId = Guid.NewGuid();
        var vehicleId = Guid.NewGuid();

        context.Vehicles.Add(new Vehicle
        {
            Id = vehicleId,
            OwnerUserId = ownerId,
            LicensePlate = "OWN123",
            Brand = "Kia",
            Model = "Rio",
            Color = "Rojo",
            TotalSeats = 4,
            IsActive = true
        });
        await context.SaveChangesAsync();

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.UpdateAsync(vehicleId, otherDriverId, "NEW123", "Kia", "Soluto", "Azul", 2022, 4)
        );
        Assert.Contains("no encontrado", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task VehicleService_GetAllForDriverAsync_ReturnsOnlyRequestedDriverVehiclesActiveFirst()
    {
        // Arrange
        using var context = GetInMemoryContext("ListDriverVehiclesDb");
        var service = new VehicleService(context);
        var ownerId = Guid.NewGuid();
        var anotherOwnerId = Guid.NewGuid();
        var inactiveVehicleId = Guid.NewGuid();
        var activeVehicleId = Guid.NewGuid();

        context.Vehicles.AddRange(
            new Vehicle
            {
                Id = inactiveVehicleId,
                OwnerUserId = ownerId,
                LicensePlate = "OLD111",
                Brand = "Toyota",
                Color = "Gris",
                TotalSeats = 4,
                IsActive = false,
                CreatedAt = DateTime.UtcNow.AddDays(-1)
            },
            new Vehicle
            {
                Id = activeVehicleId,
                OwnerUserId = ownerId,
                LicensePlate = "ACT222",
                Brand = "Honda",
                Color = "Negro",
                TotalSeats = 5,
                IsActive = true,
                CreatedAt = DateTime.UtcNow.AddDays(-2)
            },
            new Vehicle
            {
                Id = Guid.NewGuid(),
                OwnerUserId = anotherOwnerId,
                LicensePlate = "EXT333",
                Brand = "Nissan",
                Color = "Plata",
                TotalSeats = 4,
                IsActive = true
            });
        await context.SaveChangesAsync();

        // Act
        var vehicles = await service.GetAllForDriverAsync(ownerId);

        // Assert
        Assert.Equal(2, vehicles.Count);
        Assert.Equal(activeVehicleId, vehicles[0].Id);
        Assert.Equal(inactiveVehicleId, vehicles[1].Id);
        Assert.All(vehicles, vehicle => Assert.Equal(ownerId, vehicle.OwnerUserId));
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

    [Fact]
    public async Task RatingService_GetAverageRatingForUserAsync_ReturnsRoundedAveragesAndDistributions()
    {
        // Arrange
        using var context = GetInMemoryContext("RatingSummaryDb");
        var service = new RatingService(context);
        var userId = Guid.NewGuid();
        var evaluatorOneId = Guid.NewGuid();
        var evaluatorTwoId = Guid.NewGuid();
        var tripId = Guid.NewGuid();

        context.Users.AddRange(
            new User { Id = userId, FullName = "Usuario Evaluado", Email = "evaluado@test.com" },
            new User { Id = evaluatorOneId, FullName = "Evaluador Uno", Email = "uno@test.com" },
            new User { Id = evaluatorTwoId, FullName = "Evaluador Dos", Email = "dos@test.com" });
        context.TripRatings.AddRange(
            new TripRating
            {
                Id = Guid.NewGuid(),
                TripId = tripId,
                EvaluatorUserId = evaluatorOneId,
                EvaluatedUserId = userId,
                RatingRole = RatingRole.PassengerToDriver,
                Score = 5,
                CreatedAt = DateTime.UtcNow
            },
            new TripRating
            {
                Id = Guid.NewGuid(),
                TripId = tripId,
                EvaluatorUserId = evaluatorTwoId,
                EvaluatedUserId = userId,
                RatingRole = RatingRole.PassengerToDriver,
                Score = 4,
                CreatedAt = DateTime.UtcNow
            },
            new TripRating
            {
                Id = Guid.NewGuid(),
                TripId = tripId,
                EvaluatorUserId = evaluatorTwoId,
                EvaluatedUserId = userId,
                RatingRole = RatingRole.DriverToPassenger,
                Score = 3,
                CreatedAt = DateTime.UtcNow
            });
        await context.SaveChangesAsync();

        // Act
        var summary = await service.GetAverageRatingForUserAsync(userId);

        // Assert
        Assert.Equal("Usuario Evaluado", summary.UserFullName);
        Assert.Equal(4.0, summary.AverageScore);
        Assert.Equal(3, summary.TotalRatingsCount);
        Assert.Equal(4.5, summary.AverageDriverScore);
        Assert.Equal(2, summary.TotalDriverRatingsCount);
        Assert.Equal(1, summary.DriverStarsDistribution[3]);
        Assert.Equal(1, summary.DriverStarsDistribution[4]);
        Assert.Equal(3.0, summary.AveragePassengerScore);
        Assert.Equal(1, summary.TotalPassengerRatingsCount);
        Assert.Equal(1, summary.PassengerStarsDistribution[2]);
    }

    [Fact]
    public async Task PaymentService_CreateUserPaymentMethodAsync_RejectsBankQrWithoutBase64Image()
    {
        // Arrange
        using var context = GetInMemoryContext("BankQrValidationDb");
        var service = new PaymentService(context, new MockNotificationService());
        var userId = Guid.NewGuid();

        context.Users.Add(new User
        {
            Id = userId,
            FullName = "Conductor QR",
            Email = "qr@test.com",
            PasswordHash = "hash"
        });
        context.PaymentMethods.Add(new PaymentMethod
        {
            Id = 10,
            Code = "BANK_QR",
            Name = "QR Bancario",
            Type = PaymentMethodType.BankQr,
            RequiresManualConfirmation = true,
            IsActive = true
        });
        await context.SaveChangesAsync();

        var dto = new CreateUserPaymentMethodDto
        {
            PaymentMethodId = 10,
            Alias = "QR personal",
            QrImageUrl = "https://example.com/qr.png",
            IsDefault = true
        };

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CreateUserPaymentMethodAsync(userId, dto)
        );
        Assert.Contains("base64", exception.Message, StringComparison.OrdinalIgnoreCase);
    }
}
