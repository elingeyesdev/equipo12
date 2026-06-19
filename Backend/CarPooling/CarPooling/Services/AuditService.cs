using System.Reflection;
using System.Security.Claims;
using System.Text.Json;
using CarPooling.Data;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class AuditService(CarPoolingContext context, IHttpContextAccessor httpContextAccessor)
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = false
    };

    private readonly CarPoolingContext _context = context;
    private readonly IHttpContextAccessor _httpContextAccessor = httpContextAccessor;

    public async Task RecordAsync(
        string actionType,
        string module,
        string entityName,
        string? entityId = null,
        object? oldValues = null,
        object? newValues = null,
        string result = "Success",
        string? description = null,
        Guid? actorUserId = null)
    {
        var httpContext = _httpContextAccessor.HttpContext;
        var resolvedActorId = actorUserId ?? GetCurrentActorUserId(httpContext);
        var actorEmail = resolvedActorId.HasValue
            ? await _context.Users
                .AsNoTracking()
                .Where(u => u.Id == resolvedActorId.Value)
                .Select(u => u.Email)
                .FirstOrDefaultAsync()
            : null;

        var log = new AuditLog
        {
            Id = Guid.NewGuid(),
            ActorUserId = resolvedActorId,
            ActorEmailSnapshot = actorEmail,
            ActionType = Trim(actionType, 80) ?? "Unknown",
            Module = Trim(module, 60) ?? "General",
            EntityName = Trim(entityName, 80) ?? "Unknown",
            EntityId = Trim(entityId, 80),
            OldValuesJson = SerializeOrNull(oldValues),
            NewValuesJson = SerializeOrNull(newValues),
            ChangedFieldsJson = SerializeOrNull(GetChangedFields(oldValues, newValues)),
            Result = Trim(string.IsNullOrWhiteSpace(result) ? "Success" : result, 30) ?? "Success",
            Description = Trim(description, 500),
            IpAddress = Trim(GetIpAddress(httpContext), 45),
            UserAgent = Trim(httpContext?.Request.Headers.UserAgent.ToString(), 500),
            RequestPath = Trim(httpContext?.Request.Path.Value, 200),
            CreatedAt = DateTime.UtcNow
        };

        _context.AuditLogs.Add(log);
        await _context.SaveChangesAsync();
    }

    public static object SnapshotUser(User user)
    {
        return new
        {
            user.Id,
            user.FullName,
            user.Email,
            user.PhoneNumber,
            user.IsActive,
            Roles = user.UserRoles.Select(ur => ur.Role.Name).OrderBy(r => r).ToList()
        };
    }

    public static object SnapshotTrip(Trip trip)
    {
        return new
        {
            trip.Id,
            trip.DriverName,
            trip.DriverUserId,
            trip.StatusId,
            Status = trip.StatusEntity?.LabelEs,
            trip.AvailableSeats,
            trip.OfferedSeats,
            trip.FareAmount,
            trip.OriginLocationId,
            OriginLatitude = trip.OriginLocation?.Latitude,
            OriginLongitude = trip.OriginLocation?.Longitude,
            trip.DestinationLocationId,
            DestinationLatitude = trip.DestinationLocation?.Latitude,
            DestinationLongitude = trip.DestinationLocation?.Longitude,
            trip.CancelledAt,
            trip.StartedAt,
            trip.FinishedAt
        };
    }

    public static object SnapshotReservation(Reservation reservation)
    {
        return new
        {
            reservation.Id,
            reservation.TripId,
            reservation.PassengerUserId,
            PassengerName = reservation.PassengerUser?.FullName,
            reservation.SeatsReserved,
            reservation.StatusId,
            Status = reservation.StatusEntity?.LabelEs,
            reservation.CreatedAt
        };
    }

    public static object SnapshotSupportTicket(SupportTicket ticket)
    {
        return new
        {
            ticket.Id,
            ticket.UserId,
            ReporterName = ticket.User?.FullName,
            ticket.TripId,
            ticket.ReservationId,
            Category = ticket.Category.ToString(),
            Status = ticket.Status.ToString(),
            ticket.Subject,
            ticket.Description,
            ticket.AssignedAdminUserId,
            ticket.UpdatedAt,
            ticket.ClosedAt
        };
    }

    private static Guid? GetCurrentActorUserId(HttpContext? httpContext)
    {
        var claimValue = httpContext?.User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (Guid.TryParse(claimValue, out var userId))
        {
            return userId;
        }

        return null;
    }

    private static string? GetIpAddress(HttpContext? httpContext)
    {
        if (httpContext is null)
        {
            return null;
        }

        var forwardedFor = httpContext.Request.Headers["X-Forwarded-For"].FirstOrDefault();
        if (!string.IsNullOrWhiteSpace(forwardedFor))
        {
            return forwardedFor.Split(',')[0].Trim();
        }

        return httpContext.Connection.RemoteIpAddress?.ToString();
    }

    private static string? SerializeOrNull(object? value)
    {
        return value is null ? null : JsonSerializer.Serialize(value, JsonOptions);
    }

    private static IReadOnlyList<string>? GetChangedFields(object? oldValues, object? newValues)
    {
        if (oldValues is null || newValues is null)
        {
            return null;
        }

        var oldMap = ToPropertyMap(oldValues);
        var newMap = ToPropertyMap(newValues);
        var changed = new List<string>();

        foreach (var key in oldMap.Keys.Union(newMap.Keys).OrderBy(k => k))
        {
            oldMap.TryGetValue(key, out var oldValue);
            newMap.TryGetValue(key, out var newValue);
            if (!string.Equals(oldValue, newValue, StringComparison.Ordinal))
            {
                changed.Add(key);
            }
        }

        return changed.Count == 0 ? null : changed;
    }

    private static Dictionary<string, string?> ToPropertyMap(object value)
    {
        return value.GetType()
            .GetProperties(BindingFlags.Public | BindingFlags.Instance)
            .Where(p => p.GetIndexParameters().Length == 0)
            .ToDictionary(
                p => p.Name,
                p => SerializeOrNull(p.GetValue(value)),
                StringComparer.OrdinalIgnoreCase);
    }

    private static string? Trim(string? value, int maxLength)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return null;
        }

        var trimmed = value.Trim();
        return trimmed.Length <= maxLength ? trimmed : trimmed[..maxLength];
    }
}
