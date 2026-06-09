namespace CarPooling.Services;

public interface INotificationService
{
    Task SendNotificationAsync(Guid userId, string title, string body, Dictionary<string, string>? data = null);
    Task SendNotificationToMultipleAsync(IEnumerable<Guid> userIds, string title, string body, Dictionary<string, string>? data = null);
}
