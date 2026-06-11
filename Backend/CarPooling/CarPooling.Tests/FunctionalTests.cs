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

public class FunctionalTests : IClassFixture<TestWebApplicationFactory<Program>>
{
    private readonly TestWebApplicationFactory<Program> _factory;

    public FunctionalTests(TestWebApplicationFactory<Program> factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Trip_ReservationLifecycle_Workflow()
    {
        // Arrange
        var client = _factory.CreateClient();
        var driverId = Guid.Parse("22222222-2222-2222-2222-222222222222");
        var passengerId = Guid.Parse("44444444-4444-4444-4444-444444444444");
        var vehicleId = Guid.Parse("99999999-9999-9999-9999-999999999991");

        // 1. Conductor crea el viaje (origen) -> POST /api/trips/origin
        var originRequest = new
        {
            driverUserId = driverId,
            driverName = "Driver Test",
            latitude = 3.374,
            longitude = -76.532,
            offeredSeats = 4,
            fareAmount = 5.0,
            vehicleId = vehicleId
        };
        var createResponse = await client.PostAsJsonAsync("/api/trips/origin", originRequest);
        Assert.Equal(HttpStatusCode.Created, createResponse.StatusCode);
        var tripDto = await createResponse.Content.ReadFromJsonAsync<TripResponse>();
        Assert.NotNull(tripDto);
        var tripId = tripDto.Id;
        Assert.Equal(1, tripDto.StatusId); // Scheduled

        // 2. Conductor establece el destino -> POST /api/trips/{tripId}/destination
        var destRequest = new
        {
            latitude = 3.424,
            longitude = -76.545
        };
        var destResponse = await client.PostAsJsonAsync($"/api/trips/{tripId}/destination", destRequest);
        Assert.Equal(HttpStatusCode.OK, destResponse.StatusCode);
        var tripReadyDto = await destResponse.Content.ReadFromJsonAsync<TripResponse>();
        Assert.NotNull(tripReadyDto);
        Assert.Equal(2, tripReadyDto.StatusId); // Ready

        // 3. Pasajero solicita una reserva -> POST /api/Trips/{tripId}/Reservations
        var reservationRequest = new CreateReservationDto
        {
            PassengerUserId = passengerId,
            SeatsReserved = 1
        };
        var resResponse = await client.PostAsJsonAsync($"/api/Trips/{tripId}/Reservations", reservationRequest);
        Assert.Equal(HttpStatusCode.Created, resResponse.StatusCode);
        var resDto = await resResponse.Content.ReadFromJsonAsync<ReservationDto>();
        Assert.NotNull(resDto);
        var reservationId = resDto.Id;
        Assert.Equal(1, resDto.StatusId); // Pending

        // 4. Conductor acepta la reserva -> POST /api/Trips/{tripId}/Reservations/{reservationId}/accept
        // We need X-User-Id header for driver
        var acceptRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/Trips/{tripId}/Reservations/{reservationId}/accept");
        acceptRequest.Headers.Add("X-User-Id", driverId.ToString());
        var acceptResponse = await client.SendAsync(acceptRequest);
        Assert.Equal(HttpStatusCode.OK, acceptResponse.StatusCode);
        var acceptedResDto = await acceptResponse.Content.ReadFromJsonAsync<ReservationDto>();
        Assert.NotNull(acceptedResDto);
        Assert.Equal(2, acceptedResDto.StatusId); // Confirmed

        // 5. Verificar que los asientos disponibles del viaje se redujeron a 3
        var getTripResponse = await client.GetAsync($"/api/trips/{tripId}");
        var finalTripDto = await getTripResponse.Content.ReadFromJsonAsync<TripResponse>();
        Assert.NotNull(finalTripDto);
        Assert.Equal(3, finalTripDto.AvailableSeats); // 4 offered - 1 reserved
    }

    [Fact]
    public async Task SupportTicket_ChatWorkflow()
    {
        // Arrange
        var client = _factory.CreateClient();
        var passengerId = Guid.Parse("44444444-4444-4444-4444-444444444444");
        var adminId = Guid.Parse("11111111-1111-1111-1111-111111111111");

        // 1. Usuario crea ticket de soporte -> POST /api/users/{passengerId}/support-tickets
        var createRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/users/{passengerId}/support-tickets");
        createRequest.Headers.Add("X-User-Id", passengerId.ToString());
        createRequest.Content = JsonContent.Create(new
        {
            category = 5, // Other
            subject = "Problema con mi cobro",
            description = "Se me cobró doble el viaje"
        });

        var createResponse = await client.SendAsync(createRequest);
        Assert.Equal(HttpStatusCode.Created, createResponse.StatusCode);
        var ticket = await createResponse.Content.ReadFromJsonAsync<SupportTicketResponseDto>();
        Assert.NotNull(ticket);
        var ticketId = ticket.Id;

        // 2. Administrador responde al ticket -> POST /api/admin/support-tickets/{ticketId}/messages
        var adminMsgRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/admin/support-tickets/{ticketId}/messages");
        adminMsgRequest.Headers.Add("X-User-Id", adminId.ToString());
        adminMsgRequest.Content = JsonContent.Create(new
        {
            messageText = "Hola, estamos revisando el cobro doble. Danos unos minutos."
        });

        var adminMsgResponse = await client.SendAsync(adminMsgRequest);
        Assert.Equal(HttpStatusCode.Created, adminMsgResponse.StatusCode);

        // 3. Usuario consulta los mensajes del ticket -> GET /api/users/{passengerId}/support-tickets/{ticketId}/messages
        var getMsgsRequest = new HttpRequestMessage(HttpMethod.Get, $"/api/users/{passengerId}/support-tickets/{ticketId}/messages");
        getMsgsRequest.Headers.Add("X-User-Id", passengerId.ToString());
        var getMsgsResponse = await client.SendAsync(getMsgsRequest);
        Assert.Equal(HttpStatusCode.OK, getMsgsResponse.StatusCode);
        var messagesList = await getMsgsResponse.Content.ReadFromJsonAsync<SupportTicketMessageResponseDto[]>();
        Assert.NotNull(messagesList);
        Assert.Single(messagesList);
        Assert.Equal("Hola, estamos revisando el cobro doble. Danos unos minutos.", messagesList[0].MessageText);

        // 4. Usuario responde al chat de soporte -> POST /api/users/{passengerId}/support-tickets/{ticketId}/messages
        var userReplyRequest = new HttpRequestMessage(HttpMethod.Post, $"/api/users/{passengerId}/support-tickets/{ticketId}/messages");
        userReplyRequest.Headers.Add("X-User-Id", passengerId.ToString());
        userReplyRequest.Content = JsonContent.Create(new
        {
            messageText = "Entendido, quedo atento."
        });
        var userReplyResponse = await client.SendAsync(userReplyRequest);
        Assert.Equal(HttpStatusCode.Created, userReplyResponse.StatusCode);
    }
}
