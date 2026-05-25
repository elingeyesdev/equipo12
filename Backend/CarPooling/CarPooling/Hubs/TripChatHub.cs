using CarPooling.Security;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using System.Security.Claims;

namespace CarPooling.Hubs;

[Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
public class TripChatHub(ChatService chatService) : Hub
{
    private readonly ChatService _chatService = chatService;

    /// <summary>
    /// Suscribe al cliente al canal de chat en tiempo real para un viaje específico.
    /// Valida que el usuario autenticado esté autorizado (sea conductor o pasajero confirmado/abordado).
    /// </summary>
    public async Task JoinTripChat(string tripIdString)
    {
        if (!Guid.TryParse(tripIdString, out var tripId))
        {
            await Clients.Caller.SendAsync("Error", "ID de viaje invalido.");
            return;
        }

        var nameIdentifier = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (!Guid.TryParse(nameIdentifier, out var userId))
        {
            await Clients.Caller.SendAsync("Error", "Usuario no autorizado.");
            return;
        }

        var isAuthorized = await _chatService.IsUserAuthorizedForChatAsync(tripId, userId);
        if (!isAuthorized)
        {
            await Clients.Caller.SendAsync("Error", "No estás autorizado para acceder al chat de este viaje.");
            return;
        }

        var chat = await _chatService.GetOrCreateChatByTripIdAsync(tripId);
        if (chat == null)
        {
            await Clients.Caller.SendAsync("Error", "Viaje no encontrado.");
            return;
        }

        // Unir la conexión actual al grupo aislado del chat del viaje
        var groupName = $"trip_chat_{tripId}";
        await Groups.AddToGroupAsync(Context.ConnectionId, groupName);

        // Notificar de regreso que la conexión se suscribió con éxito
        await Clients.Caller.SendAsync("JoinedChat", tripIdString);
    }
}
