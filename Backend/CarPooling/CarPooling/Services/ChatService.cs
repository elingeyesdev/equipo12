using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class ChatService(CarPoolingContext context, INotificationService notificationService)
{
    private readonly CarPoolingContext _context = context;
    private readonly INotificationService _notificationService = notificationService;

    /// <summary>
    /// Valida si un usuario está autorizado para participar en el chat de un viaje.
    /// Un usuario está autorizado si es el conductor o si es un pasajero con reserva Confirmada o Abordada.
    /// </summary>
    public async Task<bool> IsUserAuthorizedForChatAsync(Guid tripId, Guid userId)
    {
        var trip = await _context.Trips
            .AsNoTracking()
            .Include(t => t.Reservations)
            .ThenInclude(r => r.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);

        if (trip == null)
        {
            return false;
        }

        // Restriccion post-viaje de 5 horas: revocar si pasaron mas de 5 horas del cierre
        var now = DateTime.UtcNow;
        if (trip.FinishedAt != null && (now - trip.FinishedAt.Value).TotalHours > 5)
        {
            return false;
        }
        if (trip.CancelledAt != null && (now - trip.CancelledAt.Value).TotalHours > 5)
        {
            return false;
        }

        // El conductor está autorizado
        if (trip.DriverUserId == userId)
        {
            return true;
        }

        // Un pasajero con reserva confirmada o abordada está autorizado
        var isAuthorizedPassenger = trip.Reservations.Any(r => 
            r.PassengerUserId == userId && 
            (r.StatusEntity.Code == ReservationStatusEntity.Confirmed || 
             r.StatusEntity.Code == ReservationStatusEntity.Boarded));

        return isAuthorizedPassenger;
    }

    /// <summary>
    /// Obtiene la conversación (TripChat) asociada a un viaje, o la crea si no existe.
    /// </summary>
    public async Task<TripChat?> GetOrCreateChatByTripIdAsync(Guid tripId)
    {
        // Verificar primero que el viaje realmente exista
        var tripExists = await _context.Trips.AnyAsync(t => t.Id == tripId);
        if (!tripExists)
        {
            return null;
        }

        var chat = await _context.TripChats
            .FirstOrDefaultAsync(c => c.TripId == tripId);

        if (chat == null)
        {
            chat = new TripChat
            {
                Id = Guid.NewGuid(),
                TripId = tripId,
                CreatedAt = DateTime.UtcNow
            };
            _context.TripChats.Add(chat);
            await _context.SaveChangesAsync();
        }

        return chat;
    }

    /// <summary>
    /// Obtiene el historial de mensajes de un chat mapeados a DTOs.
    /// </summary>
    public async Task<List<ChatMessageResponseDto>> GetChatMessagesAsync(Guid chatId)
    {
        var messages = await _context.TripChatMessages
            .AsNoTracking()
            .Where(m => m.ChatId == chatId)
            .OrderBy(m => m.CreatedAt)
            .Select(m => new ChatMessageResponseDto
            {
                Id = m.Id,
                SenderUserId = m.SenderUserId,
                SenderFullName = m.SenderUser.FullName,
                SenderProfilePicture = m.SenderUser.ProfilePicture,
                MessageText = m.MessageText,
                CreatedAt = m.CreatedAt,
                ReadByUserIds = m.Reads.Select(r => r.UserId).ToList(),
                Readers = m.Reads.Select(r => new ChatMessageReaderDto
                {
                    UserId = r.UserId,
                    FullName = r.User.FullName
                }).ToList()
            })
            .ToListAsync();

        return messages;
    }

    /// <summary>
    /// Guarda un nuevo mensaje en la base de datos y registra la lectura automática por parte del emisor.
    /// </summary>
    public async Task<ChatMessageResponseDto> SendMessageAsync(Guid chatId, Guid senderUserId, string messageText)
    {
        var message = new TripChatMessage
        {
            Id = Guid.NewGuid(),
            ChatId = chatId,
            SenderUserId = senderUserId,
            MessageText = messageText,
            CreatedAt = DateTime.UtcNow
        };

        // El emisor del mensaje ya lo leyó implícitamente
        var readReceipt = new TripChatMessageRead
        {
            MessageId = message.Id,
            UserId = senderUserId,
            ReadAt = DateTime.UtcNow
        };

        _context.TripChatMessages.Add(message);
        _context.TripChatMessageReads.Add(readReceipt);
        
        await _context.SaveChangesAsync();

        // Obtener el nombre del emisor y foto para retornar el DTO completo
        var senderUser = await _context.Users
            .Where(u => u.Id == senderUserId)
            .Select(u => new { u.FullName, u.ProfilePicture })
            .FirstOrDefaultAsync();
        var senderFullName = senderUser?.FullName ?? "Usuario Desconocido";
        var senderProfilePicture = senderUser?.ProfilePicture;

        // Disparar la notificación push a los otros participantes del chat
        var tripChat = await _context.TripChats.FindAsync(chatId);
        if (tripChat != null)
        {
            var trip = await _context.Trips
                .Include(t => t.Reservations)
                .FirstOrDefaultAsync(t => t.Id == tripChat.TripId);

            if (trip != null)
            {
                var recipients = new List<Guid>();

                if (trip.DriverUserId.HasValue && trip.DriverUserId.Value != senderUserId)
                {
                    recipients.Add(trip.DriverUserId.Value);
                }

                var passengers = trip.Reservations
                    .Where(r => (r.StatusId == 2 || r.StatusId == 3) && r.PassengerUserId != senderUserId)
                    .Select(r => r.PassengerUserId);

                recipients.AddRange(passengers);

                if (recipients.Count > 0)
                {
                    await _notificationService.SendNotificationToMultipleAsync(
                        recipients,
                        $"Mensaje de {senderFullName}",
                        messageText,
                        new Dictionary<string, string> { { "type", "chat_message" }, { "tripId", trip.Id.ToString() } }
                    );
                }
            }
        }

        return new ChatMessageResponseDto
        {
            Id = message.Id,
            SenderUserId = message.SenderUserId,
            SenderFullName = senderFullName,
            SenderProfilePicture = senderProfilePicture,
            MessageText = message.MessageText,
            CreatedAt = message.CreatedAt,
            ReadByUserIds = [senderUserId],
            Readers = new List<ChatMessageReaderDto>
            {
                new ChatMessageReaderDto
                {
                    UserId = senderUserId,
                    FullName = senderFullName
                }
            }
        };
    }

    /// <summary>
    /// Registra la lectura de todos los mensajes no leídos del chat para el usuario especificado.
    /// </summary>
    public async Task MarkMessagesAsReadAsync(Guid chatId, Guid userId)
    {
        // Encontrar mensajes de este chat que el usuario actual no ha registrado como leídos
        var unreadMessageIds = await _context.TripChatMessages
            .Where(m => m.ChatId == chatId)
            .Where(m => !m.Reads.Any(r => r.UserId == userId))
            .Select(m => m.Id)
            .ToListAsync();

        if (unreadMessageIds.Count == 0)
        {
            return;
        }

        var now = DateTime.UtcNow;
        var newReads = unreadMessageIds.Select(messageId => new TripChatMessageRead
        {
            MessageId = messageId,
            UserId = userId,
            ReadAt = now
        });

        _context.TripChatMessageReads.AddRange(newReads);
        await _context.SaveChangesAsync();
    }
}
