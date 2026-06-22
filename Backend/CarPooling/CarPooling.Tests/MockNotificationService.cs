using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using CarPooling.Services;

namespace CarPooling.Tests;

public class MockNotificationService : INotificationService
{
    public Task SendNotificationAsync(Guid userId, string title, string body, Dictionary<string, string>? data = null)
    {
        return Task.CompletedTask;
    }

    public Task SendNotificationToMultipleAsync(IEnumerable<Guid> userIds, string title, string body, Dictionary<string, string>? data = null)
    {
        return Task.CompletedTask;
    }
}
