using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class SupportTicketMessagingService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

    private static readonly SupportTicketStatus[] ClosedStatuses =
    [
        SupportTicketStatus.Resolved,
        SupportTicketStatus.Closed
    ];

    public async Task<List<SupportTicketMessageResponseDto>> GetMessagesForUserAsync(Guid userId, Guid ticketId)
    {
        var ticket = await GetTicketForUserAsync(userId, ticketId);
        if (ticket.FirstAdminReplyAt is null)
        {
            throw new InvalidOperationException(
                "El chat de soporte estará disponible cuando el equipo responda tu solicitud.");
        }

        return await GetMessagesAsync(ticketId);
    }

    public async Task<List<SupportTicketMessageResponseDto>> GetMessagesForAdminAsync(Guid ticketId)
    {
        var exists = await _context.SupportTickets.AnyAsync(t => t.Id == ticketId);
        if (!exists)
        {
            throw new KeyNotFoundException("Reporte de soporte no encontrado.");
        }

        return await GetMessagesAsync(ticketId);
    }

    public async Task<SupportTicketMessageResponseDto> SendMessageAsUserAsync(
        Guid userId,
        Guid ticketId,
        SendSupportTicketMessageDto dto)
    {
        var ticket = await _context.SupportTickets
            .FirstOrDefaultAsync(t => t.Id == ticketId && t.UserId == userId);

        if (ticket is null)
        {
            throw new KeyNotFoundException("Reporte de soporte no encontrado.");
        }

        EnsureTicketAcceptsMessages(ticket);

        if (ticket.FirstAdminReplyAt is null)
        {
            throw new InvalidOperationException(
                "Aún no puedes escribir en el chat. Espera la primera respuesta del equipo de soporte.");
        }

        return await PersistMessageAsync(ticket, userId, SupportMessageSenderKind.User, dto.MessageText, isAdmin: false);
    }

    public async Task<SupportTicketMessageResponseDto> SendMessageAsAdminAsync(
        Guid adminUserId,
        Guid ticketId,
        SendSupportTicketMessageDto dto)
    {
        var admin = await _context.Users
            .AsNoTracking()
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.Id == adminUserId);

        var isAdmin = admin?.UserRoles.Any(ur =>
            ur.Role.Name is "SuperAdmin" or "Admin" ||
            ur.Role.Name.Contains("Admin", StringComparison.OrdinalIgnoreCase)) == true;

        if (admin is null || !isAdmin)
        {
            throw new InvalidOperationException("Solo un administrador puede responder en soporte.");
        }

        var ticket = await _context.SupportTickets
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.Id == ticketId);

        if (ticket is null)
        {
            throw new KeyNotFoundException("Reporte de soporte no encontrado.");
        }

        EnsureTicketAcceptsMessages(ticket);

        var isFirstAdminReply = ticket.FirstAdminReplyAt is null;
        var message = await PersistMessageAsync(
            ticket,
            adminUserId,
            SupportMessageSenderKind.Admin,
            dto.MessageText,
            isAdmin: true);

        if (isFirstAdminReply)
        {
            ticket.FirstAdminReplyAt = message.CreatedAt;
            ticket.AssignedAdminUserId ??= adminUserId;
            if (ticket.Status == SupportTicketStatus.Open)
            {
                ticket.Status = SupportTicketStatus.InReview;
            }
            ticket.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }

        return message;
    }

    private async Task<SupportTicketMessageResponseDto> PersistMessageAsync(
        SupportTicket ticket,
        Guid senderUserId,
        SupportMessageSenderKind senderKind,
        string messageText,
        bool isAdmin)
    {
        var text = messageText.Trim();
        if (text.Length < 1)
        {
            throw new InvalidOperationException("El mensaje no puede estar vacío.");
        }

        if (text.Length > 2000)
        {
            throw new InvalidOperationException("El mensaje no puede exceder 2000 caracteres.");
        }

        var now = DateTime.UtcNow;
        var message = new SupportTicketMessage
        {
            Id = Guid.NewGuid(),
            TicketId = ticket.Id,
            SenderUserId = senderUserId,
            SenderKind = senderKind,
            MessageText = text,
            CreatedAt = now
        };

        _context.SupportTicketMessages.Add(message);
        ticket.LastMessageAt = now;
        ticket.UpdatedAt = now;

        await _context.SaveChangesAsync();

        var sender = await _context.Users.AsNoTracking()
            .FirstOrDefaultAsync(u => u.Id == senderUserId);

        return MapMessage(message, sender?.FullName ?? "Usuario");
    }

    private async Task<List<SupportTicketMessageResponseDto>> GetMessagesAsync(Guid ticketId)
    {
        return await _context.SupportTicketMessages
            .AsNoTracking()
            .Where(m => m.TicketId == ticketId)
            .OrderBy(m => m.CreatedAt)
            .Select(m => new SupportTicketMessageResponseDto
            {
                Id = m.Id,
                TicketId = m.TicketId,
                SenderUserId = m.SenderUserId,
                SenderFullName = m.SenderUser.FullName,
                SenderKind = m.SenderKind,
                SenderKindLabel = m.SenderKind == SupportMessageSenderKind.Admin ? "Administrador"
                    : m.SenderKind == SupportMessageSenderKind.System ? "Sistema" : "Usuario",
                MessageText = m.MessageText,
                CreatedAt = m.CreatedAt,
                ReadByUserIds = new List<Guid>()
            })
            .ToListAsync();
    }

    private async Task<SupportTicket> GetTicketForUserAsync(Guid userId, Guid ticketId)
    {
        var ticket = await _context.SupportTickets
            .AsNoTracking()
            .FirstOrDefaultAsync(t => t.Id == ticketId && t.UserId == userId);

        if (ticket is null)
        {
            throw new KeyNotFoundException("Reporte de soporte no encontrado.");
        }

        return ticket;
    }

    private static void EnsureTicketAcceptsMessages(SupportTicket ticket)
    {
        if (ClosedStatuses.Contains(ticket.Status))
        {
            throw new InvalidOperationException("Este reporte está cerrado y ya no acepta mensajes.");
        }
    }

    private static SupportTicketMessageResponseDto MapMessage(SupportTicketMessage message, string senderName)
    {
        return new SupportTicketMessageResponseDto
        {
            Id = message.Id,
            TicketId = message.TicketId,
            SenderUserId = message.SenderUserId,
            SenderFullName = senderName,
            SenderKind = message.SenderKind,
            SenderKindLabel = message.SenderKind switch
            {
                SupportMessageSenderKind.Admin => "Administrador",
                SupportMessageSenderKind.System => "Sistema",
                _ => "Usuario"
            },
            MessageText = message.MessageText,
            CreatedAt = message.CreatedAt,
            ReadByUserIds = []
        };
    }
}
