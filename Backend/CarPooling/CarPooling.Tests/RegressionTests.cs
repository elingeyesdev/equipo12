using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;
using CarPooling.Data;
using CarPooling.Dtos;
using Microsoft.Extensions.DependencyInjection;
using Xunit;

namespace CarPooling.Tests;

public class RegressionTests : IClassFixture<TestWebApplicationFactory<Program>>
{
    private readonly TestWebApplicationFactory<Program> _factory;

    public RegressionTests(TestWebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Reservation_OverbookingPrevention_ReturnsBadRequest()
    {
        // Arrange
        var client = _factory.CreateClient();
        var tripId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        var passengerId = Guid.Parse("44444444-4444-4444-4444-444444444444");

        // Setup: Set trip available seats to 0 in database
        using (var scope = _factory.Services.CreateScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();
            var trip = await db.Trips.FindAsync(tripId);
            if (trip != null)
            {
                trip.AvailableSeats = 0;
                await db.SaveChangesAsync();
            }
        }

        var reservationRequest = new CreateReservationDto
        {
            PassengerUserId = passengerId,
            SeatsReserved = 1
        };

        // Act: Try to create a reservation on a trip with 0 available seats
        var response = await client.PostAsJsonAsync($"/api/Trips/{tripId}/Reservations", reservationRequest);

        // Assert: Verify it returns 400 Bad Request to prevent overbooking
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task AdminEndpoint_AccessSecurityPolicies_RestrictsRegularUser()
    {
        // Arrange
        var client = _factory.CreateClient();
        var regularUserId = "44444444-4444-4444-4444-444444444444"; // Passenger (seeded as Student)

        // Act: Call an admin endpoint (GET /api/admin/users) with a regular user header
        var request = new HttpRequestMessage(HttpMethod.Get, "/api/admin/users");
        request.Headers.Add("X-User-Id", regularUserId);

        var response = await client.SendAsync(request);

        // Assert: Access is Forbidden for regular users
        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }
}
