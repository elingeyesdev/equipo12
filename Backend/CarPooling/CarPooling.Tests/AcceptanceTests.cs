using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Controllers;
using Microsoft.Extensions.DependencyInjection;
using Xunit;


namespace CarPooling.Tests;

public class AcceptanceTests : IClassFixture<TestWebApplicationFactory<Program>>
{
    private readonly TestWebApplicationFactory<Program> _factory;

    public AcceptanceTests(TestWebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Acceptance_PublishTrip_US1()
    {
        // GIVEN: Un conductor registrado y verificado con un vehículo activo
        var client = _factory.CreateClient();
        var driverId = Guid.Parse("22222222-2222-2222-2222-222222222222"); // Driver Test
        var vehicleId = Guid.Parse("99999999-9999-9999-9999-999999999991");

        // WHEN: El conductor publica un viaje programado con origen, asientos ofrecidos y tarifa
        var request = new
        {
            driverUserId = driverId,
            driverName = "Driver Test",
            latitude = 3.374,
            longitude = -76.532,
            offeredSeats = 3,
            fareAmount = 8.5,
            vehicleId = vehicleId
        };
        var response = await client.PostAsJsonAsync("/api/trips/origin", request);

        // THEN: El viaje se crea exitosamente en estado "Programado"
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var trip = await response.Content.ReadFromJsonAsync<TripResponse>();
        Assert.NotNull(trip);
        Assert.Equal(1, trip.StatusId); // 1 = Scheduled
        Assert.Equal(3, trip.OfferedSeats);
        Assert.Equal(3, trip.AvailableSeats);
        Assert.Equal(8.5m, trip.FareAmount);
    }

    [Fact]
    public async Task Acceptance_VerifyBoardingCode_US2()
    {
        // GIVEN: Un pasajero con una reserva confirmada y su código de abordaje "1234"
        var client = _factory.CreateClient();
        var tripId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        var reservationId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc1");
        var driverId = Guid.Parse("22222222-2222-2222-2222-222222222222");

        // Preparar escenario: Asegurar que el viaje esté en estado "Listo" (StatusId = 2) para permitir abordar
        using (var scope = _factory.Services.CreateScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();
            var trip = await db.Trips.FindAsync(tripId);
            var res = await db.Reservations.FindAsync(reservationId);
            if (trip != null && res != null)
            {
                trip.StatusId = 2; // Ready
                res.StatusId = 2; // Confirmed
                res.BoardingCode = "1234";
                await db.SaveChangesAsync();
            }
        }

        // WHEN: El conductor valida el código de abordaje correcto
        var verifyRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/Trips/{tripId}/Reservations/{reservationId}/verify-code");
        verifyRequest.Headers.Add("X-User-Id", driverId.ToString());
        verifyRequest.Content = JsonContent.Create(new { Code = "1234" });

        var verifyResponse = await client.SendAsync(verifyRequest);

        // THEN: Se autoriza el abordaje (200 OK) y el estado de la reserva pasa a "Abordado" (StatusId = 3)
        Assert.Equal(HttpStatusCode.OK, verifyResponse.StatusCode);

        // Consultar el estado actual de la reserva
        var getResRequest = new HttpRequestMessage(HttpMethod.Get, $"/api/users/44444444-4444-4444-4444-444444444444/active-reservation");
        getResRequest.Headers.Add("X-User-Id", "44444444-4444-4444-4444-444444444444");
        var getResResponse = await client.SendAsync(getResRequest);
        var activeRes = await getResResponse.Content.ReadFromJsonAsync<ActiveReservationDto>();
        
        Assert.NotNull(activeRes);
        Assert.Equal(3, activeRes.StatusId); // 3 = Boarded
    }
}
