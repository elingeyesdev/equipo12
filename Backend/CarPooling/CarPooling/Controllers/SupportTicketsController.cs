using CarPooling.Dtos;
using CarPooling.Security;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/users/{userId:guid}/support-tickets")]
[Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
public class SupportTicketsController(
    SupportTicketService supportTicketService,
    SupportTicketMessagingService supportTicketMessagingService) : ControllerBase
{
    private readonly SupportTicketService _supportTicketService = supportTicketService;
    private readonly SupportTicketMessagingService _supportTicketMessagingService = supportTicketMessagingService;

    [HttpPost]
    public async Task<ActionResult<SupportTicketResponseDto>> CreateAsync(
        Guid userId,
        [FromBody] CreateSupportTicketDto dto)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var created = await _supportTicketService.CreateAsync(userId, dto);
            return StatusCode(StatusCodes.Status201Created, created);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpGet]
    public async Task<ActionResult<SupportTicketListResponseDto>> ListAsync(Guid userId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var list = await _supportTicketService.ListForUserAsync(userId);
            return Ok(list);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [HttpGet("{ticketId:guid}")]
    public async Task<ActionResult<SupportTicketResponseDto>> GetByIdAsync(Guid userId, Guid ticketId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        var ticket = await _supportTicketService.GetByIdAsync(userId, ticketId);
        if (ticket is null)
        {
            return NotFound(new { message = "Reporte de soporte no encontrado." });
        }

        return Ok(ticket);
    }

    [HttpGet("{ticketId:guid}/messages")]
    public async Task<ActionResult<IEnumerable<SupportTicketMessageResponseDto>>> GetMessagesAsync(
        Guid userId,
        Guid ticketId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var messages = await _supportTicketMessagingService.GetMessagesForUserAsync(userId, ticketId);
            return Ok(messages);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    [HttpPost("{ticketId:guid}/messages")]
    public async Task<ActionResult<SupportTicketMessageResponseDto>> SendMessageAsync(
        Guid userId,
        Guid ticketId,
        [FromBody] SendSupportTicketMessageDto dto)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var message = await _supportTicketMessagingService.SendMessageAsUserAsync(userId, ticketId, dto);
            return StatusCode(StatusCodes.Status201Created, message);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    private bool TryAuthorizeUser(Guid routeUserId, out ActionResult? forbidden)
    {
        forbidden = null;
        var currentUserId = GetCurrentUserId();
        if (currentUserId is null)
        {
            forbidden = Unauthorized(new { message = "Usuario no autenticado." });
            return false;
        }

        if (currentUserId.Value != routeUserId)
        {
            forbidden = Forbid();
            return false;
        }

        return true;
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
