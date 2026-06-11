using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;
using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.Extensions.DependencyInjection;
using Xunit;

namespace CarPooling.Tests;

public class IntegrationTests : IClassFixture<TestWebApplicationFactory<Program>>
{
    private readonly TestWebApplicationFactory<Program> _factory;

    public IntegrationTests(TestWebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Chat_AccessControl_AllowsAuthorized_RestrictsUnrelated()
    {
        // Arrange
        var client = _factory.CreateClient();
        var tripId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        var passengerId = "44444444-4444-4444-4444-444444444444"; // Seeded passenger (confirmed)
        var unrelatedId = "33333333-3333-3333-3333-333333333333"; // Seeded unrelated student

        // 1. Authorized passenger gets chat messages -> 200 OK
        var requestPassenger = new HttpRequestMessage(HttpMethod.Get, $"/api/Trips/{tripId}/chat/messages");
        requestPassenger.Headers.Add("X-User-Id", passengerId);
        
        var responsePassenger = await client.SendAsync(requestPassenger);
        Assert.Equal(HttpStatusCode.OK, responsePassenger.StatusCode);

        // 2. Unrelated user gets chat messages -> 403 Forbidden
        var requestUnrelated = new HttpRequestMessage(HttpMethod.Get, $"/api/Trips/{tripId}/chat/messages");
        requestUnrelated.Headers.Add("X-User-Id", unrelatedId);

        var responseUnrelated = await client.SendAsync(requestUnrelated);
        Assert.Equal(HttpStatusCode.Forbidden, responseUnrelated.StatusCode);

        // 3. Unrelated user tries to POST a message -> 403 Forbidden
        var postRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/Trips/{tripId}/chat/messages");
        postRequest.Headers.Add("X-User-Id", unrelatedId);
        postRequest.Content = JsonContent.Create(new { messageText = "Intento de intrusión" });

        var postResponse = await client.SendAsync(postRequest);
        Assert.Equal(HttpStatusCode.Forbidden, postResponse.StatusCode);
    }

    [Fact]
    public async Task Rating_DuplicatePrevention_ReturnsBadRequest()
    {
        // Arrange
        var client = _factory.CreateClient();
        var tripId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
        var driverId = "22222222-2222-2222-2222-222222222222";
        var passengerId = "44444444-4444-4444-4444-444444444444";

        // Setup: Make the trip "Finished" (StatusId = 4) in database to allow rating
        using (var scope = _factory.Services.CreateScope())
        {
            var db = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();
            var trip = await db.Trips.FindAsync(tripId);
            if (trip != null)
            {
                trip.StatusId = 4; // Finished
                await db.SaveChangesAsync();
            }
        }

        var ratingDto = new
        {
            evaluatedUserId = driverId,
            score = 5,
            comment = "Excelente conductor",
            tags = "Amable,Puntual"
        };

        // 1. First Rating -> 200 OK
        var firstRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/Trips/{tripId}/ratings");
        firstRequest.Headers.Add("X-User-Id", passengerId);
        firstRequest.Content = JsonContent.Create(ratingDto);

        var firstResponse = await client.SendAsync(firstRequest);
        Assert.Equal(HttpStatusCode.OK, firstResponse.StatusCode);

        // 2. Second Rating (Duplicate) -> 400 Bad Request
        var secondRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/Trips/{tripId}/ratings");
        secondRequest.Headers.Add("X-User-Id", passengerId);
        secondRequest.Content = JsonContent.Create(ratingDto);

        var secondResponse = await client.SendAsync(secondRequest);
        Assert.Equal(HttpStatusCode.BadRequest, secondResponse.StatusCode);
    }
}
