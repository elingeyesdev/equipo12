using System;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using Xunit;

namespace CarPooling.Tests;

public class PaymentUnitTests
{
    private static CarPoolingContext GetInMemoryContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<CarPoolingContext>()
            .UseInMemoryDatabase(databaseName: dbName)
            .Options;
        return new CarPoolingContext(options);
    }

    [Fact]
    public async Task PaymentService_ConfirmManualPaymentAsync_ApprovesCashPayment()
    {
        using var context = GetInMemoryContext("ConfirmCashPaymentDb");
        var driverId = Guid.NewGuid();
        var passengerId = Guid.NewGuid();
        var tripId = Guid.NewGuid();
        var reservationId = Guid.NewGuid();
        var paymentId = Guid.NewGuid();
        var userPaymentMethodId = Guid.NewGuid();
        var originId = Guid.NewGuid();
        var destinationId = Guid.NewGuid();

        context.Users.AddRange(
            new User { Id = driverId, FullName = "Conductor", Email = "driver@test.com", PasswordHash = "x" },
            new User { Id = passengerId, FullName = "Pasajero", Email = "passenger@test.com", PasswordHash = "x" });
        context.Locations.AddRange(
            new Location { Id = originId, Latitude = -17.78, Longitude = -63.18 },
            new Location { Id = destinationId, Latitude = -17.79, Longitude = -63.19 });
        context.Trips.Add(new Trip
        {
            Id = tripId,
            DriverUserId = driverId,
            DriverName = "Conductor",
            OriginLocationId = originId,
            DestinationLocationId = destinationId,
            StatusId = 1,
            FareAmount = 10m
        });
        context.Reservations.Add(new Reservation
        {
            Id = reservationId,
            TripId = tripId,
            PassengerUserId = passengerId,
            StatusId = 3,
            SeatsReserved = 1
        });
        context.UserPaymentMethods.Add(new UserPaymentMethod
        {
            Id = userPaymentMethodId,
            UserId = driverId,
            PaymentMethodId = 1,
            PaymentMethodCode = "CASH",
            PaymentMethodName = "Efectivo",
            Type = PaymentMethodType.Cash,
            RequiresManualConfirmation = true,
            IsActive = true
        });
        context.Payments.Add(new Payment
        {
            Id = paymentId,
            ReservationId = reservationId,
            UserPaymentMethodId = userPaymentMethodId,
            Amount = 10m,
            Currency = "BOB",
            Status = PaymentStatus.Pending,
            ExternalReference = "PAY-TEST-001",
            CreatedAt = DateTime.UtcNow
        });
        context.PaymentTransactions.Add(new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = paymentId,
            TransactionType = PaymentTransactionType.Payment,
            Status = PaymentTransactionStatus.Pending,
            Amount = 10m,
            Provider = "CASH",
            CreatedAt = DateTime.UtcNow
        });
        await context.SaveChangesAsync();

        var service = new PaymentService(context, new MockNotificationService());
        var result = await service.ConfirmManualPaymentAsync(driverId, paymentId, new ConfirmPaymentDto
        {
            Notes = "Recibido en efectivo"
        });

        Assert.Equal(PaymentStatus.Approved, result.Status);
        Assert.Equal("Recibido en efectivo", result.ConfirmationNotes);
    }
}
