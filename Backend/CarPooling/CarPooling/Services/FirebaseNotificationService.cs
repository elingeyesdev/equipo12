using CarPooling.Data;
using FirebaseAdmin.Messaging;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class FirebaseNotificationService(CarPoolingContext context) : INotificationService
{
    private readonly CarPoolingContext _context = context;

    public async Task SendNotificationAsync(Guid userId, string title, string body, Dictionary<string, string>? data = null)
    {
        var tokens = await _context.UserDevices
            .Where(d => d.UserId == userId)
            .Select(d => d.FcmToken)
            .ToListAsync();

        if (tokens.Count == 0) return;

        var tokensToRemove = new List<string>();

        foreach (var token in tokens)
        {
            var message = new Message
            {
                Token = token,
                Notification = new Notification
                {
                    Title = title,
                    Body = body
                },
                Data = data
            };

            try
            {
                await FirebaseMessaging.DefaultInstance.SendAsync(message);
            }
            catch (FirebaseMessagingException ex) when (ex.MessagingErrorCode == MessagingErrorCode.Unregistered)
            {
                tokensToRemove.Add(token);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error al enviar notificación a token {token} del usuario {userId}: {ex.Message}");
            }
        }

        if (tokensToRemove.Count > 0)
        {
            var expiredDevices = await _context.UserDevices
                .Where(d => tokensToRemove.Contains(d.FcmToken))
                .ToListAsync();

            _context.UserDevices.RemoveRange(expiredDevices);
            await _context.SaveChangesAsync();
        }
    }

    public async Task SendNotificationToMultipleAsync(IEnumerable<Guid> userIds, string title, string body, Dictionary<string, string>? data = null)
    {
        var tokens = await _context.UserDevices
            .Where(d => userIds.Contains(d.UserId))
            .Select(d => d.FcmToken)
            .ToListAsync();

        if (tokens.Count == 0) return;

        var tokensToRemove = new List<string>();

        foreach (var token in tokens)
        {
            var message = new Message
            {
                Token = token,
                Notification = new Notification
                {
                    Title = title,
                    Body = body
                },
                Data = data
            };

            try
            {
                await FirebaseMessaging.DefaultInstance.SendAsync(message);
            }
            catch (FirebaseMessagingException ex) when (ex.MessagingErrorCode == MessagingErrorCode.Unregistered)
            {
                tokensToRemove.Add(token);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error al enviar notificación masiva a token {token}: {ex.Message}");
            }
        }

        if (tokensToRemove.Count > 0)
        {
            var expiredDevices = await _context.UserDevices
                .Where(d => tokensToRemove.Contains(d.FcmToken))
                .ToListAsync();

            _context.UserDevices.RemoveRange(expiredDevices);
            await _context.SaveChangesAsync();
        }
    }
}
