using CarPooling.Dtos;
using CarPooling.Services;
using CarPooling.Models;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

using CarPooling.Hubs;
using Microsoft.AspNetCore.SignalR;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/Trips/{tripId}/chat")]
[Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
public class ChatsController(ChatService chatService, IHubContext<TripChatHub> hubContext) : ControllerBase
{
    private readonly ChatService _chatService = chatService;
    private readonly IHubContext<TripChatHub> _hubContext = hubContext;

    /// <summary>
    /// Obtiene el historial de mensajes de chat de un viaje.
    /// Valida que el usuario sea el conductor o un pasajero confirmado/abordado.
    /// Registra automáticamente las lecturas de los mensajes para el usuario actual.
    /// </summary>
    [HttpGet("messages", Name = "GetTripChatMessages")]
    public async Task<ActionResult<IEnumerable<ChatMessageResponseDto>>> GetMessagesAsync(Guid tripId)
    {
        var userId = GetCurrentUserId();
        if (userId == null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        var isAuthorized = await _chatService.IsUserAuthorizedForChatAsync(tripId, userId.Value);
        if (!isAuthorized)
        {
            return StatusCode(StatusCodes.Status403Forbidden, new { message = "No estás autorizado para acceder al chat de este viaje." });
        }

        var chat = await _chatService.GetOrCreateChatByTripIdAsync(tripId);
        if (chat == null)
        {
            return NotFound(new { message = "Viaje no encontrado." });
        }

        // Registrar lectura de los mensajes de forma automática
        await _chatService.MarkMessagesAsReadAsync(chat.Id, userId.Value);

        // Obtener historial de mensajes
        var messages = await _chatService.GetChatMessagesAsync(chat.Id);
        return Ok(messages);
    }

    /// <summary>
    /// Envía un mensaje al chat de un viaje.
    /// </summary>
    [HttpPost("messages")]
    public async Task<ActionResult<ChatMessageResponseDto>> SendMessageAsync(Guid tripId, [FromBody] SendMessageRequestDto dto)
    {
        var userId = GetCurrentUserId();
        if (userId == null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        var isAuthorized = await _chatService.IsUserAuthorizedForChatAsync(tripId, userId.Value);
        if (!isAuthorized)
        {
            return StatusCode(StatusCodes.Status403Forbidden, new { message = "No estás autorizado para enviar mensajes en el chat de este viaje." });
        }

        var chat = await _chatService.GetOrCreateChatByTripIdAsync(tripId);
        if (chat == null)
        {
            return NotFound(new { message = "Viaje no encontrado." });
        }

        var response = await _chatService.SendMessageAsync(chat.Id, userId.Value, dto.MessageText);

        // Retransmitir mensaje por SignalR en tiempo real a los suscritos al viaje
        await _hubContext.Clients.Group($"trip_chat_{tripId}")
            .SendAsync("ReceiveMessage", response);

        return CreatedAtRoute("GetTripChatMessages", new { tripId }, response);
    }

    /// <summary>
    /// Marca explícitamente todos los mensajes no leídos del chat del viaje como leídos.
    /// </summary>
    [HttpPost("read")]
    public async Task<IActionResult> MarkAsReadAsync(Guid tripId)
    {
        var userId = GetCurrentUserId();
        if (userId == null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        var isAuthorized = await _chatService.IsUserAuthorizedForChatAsync(tripId, userId.Value);
        if (!isAuthorized)
        {
            return StatusCode(StatusCodes.Status403Forbidden, new { message = "No estás autorizado para interactuar con este viaje." });
        }

        var chat = await _chatService.GetOrCreateChatByTripIdAsync(tripId);
        if (chat == null)
        {
            return NotFound(new { message = "Viaje no encontrado." });
        }

        await _chatService.MarkMessagesAsReadAsync(chat.Id, userId.Value);
        return NoContent();
    }

    private Guid? GetCurrentUserId()
    {
        var nameIdentifier = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (Guid.TryParse(nameIdentifier, out var userId))
        {
            return userId;
        }
        return null;
    }
}
