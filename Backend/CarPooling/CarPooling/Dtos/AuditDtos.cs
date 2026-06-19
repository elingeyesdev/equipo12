using CarPooling.Models;

namespace CarPooling.Dtos;

public class AuditLogResponseDto
{
    public Guid Id { get; set; }
    public Guid? ActorUserId { get; set; }
    public string? ActorEmail { get; set; }
    public string? ActorName { get; set; }
    public string ActionType { get; set; } = string.Empty;
    public string Module { get; set; } = string.Empty;
    public string EntityName { get; set; } = string.Empty;
    public string? EntityId { get; set; }
    public string? OldValuesJson { get; set; }
    public string? NewValuesJson { get; set; }
    public string? ChangedFieldsJson { get; set; }
    public string Result { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? IpAddress { get; set; }
    public DateTime CreatedAt { get; set; }
    public string? RelatedSearchText { get; set; }
    public string? RelatedSummary { get; set; }
    public string? RelatedSubject { get; set; }
    public string? RelatedDescription { get; set; }
    public string? RelatedReporterName { get; set; }
    public string? RelatedDriverName { get; set; }
    public string? RelatedStatus { get; set; }
    public string? RelatedTripId { get; set; }
    public string? RelatedReservationId { get; set; }

    public static AuditLogResponseDto FromEntity(AuditLog log)
    {
        return new AuditLogResponseDto
        {
            Id = log.Id,
            ActorUserId = log.ActorUserId,
            ActorEmail = log.ActorUser?.Email ?? log.ActorEmailSnapshot,
            ActorName = log.ActorUser?.FullName,
            ActionType = log.ActionType,
            Module = log.Module,
            EntityName = log.EntityName,
            EntityId = log.EntityId,
            OldValuesJson = log.OldValuesJson,
            NewValuesJson = log.NewValuesJson,
            ChangedFieldsJson = log.ChangedFieldsJson,
            Result = log.Result,
            Description = log.Description,
            IpAddress = log.IpAddress,
            CreatedAt = log.CreatedAt
        };
    }
}

public class AuditDashboardDto
{
    public int TotalLogs { get; set; }
    public int AdministrativeChanges { get; set; }
    public int UserChanges { get; set; }
    public int TripChanges { get; set; }
    public int ReservationChanges { get; set; }
    public int ReportChanges { get; set; }
    public int UsersRegistered { get; set; }
    public int ActiveDrivers { get; set; }
    public int ActivePassengers { get; set; }
    public int TripsCreated { get; set; }
    public int TripsCompleted { get; set; }
    public int TripsCancelled { get; set; }
    public int BlockedOrInactiveUsers { get; set; }
    public int ReportsResolved { get; set; }
    public int ReportsUnresolved { get; set; }
    public IReadOnlyList<AuditMetricItemDto> BusiestHours { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> RecentAdministrativeChanges { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> UsersWithManyCancellations { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> DriversWithManyReports { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> PassengersNoShowRisk { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> RepeatedCancelledTrips { get; set; } = [];
    public IReadOnlyList<AuditMetricItemDto> IncidentHours { get; set; } = [];
}

public class AuditMetricItemDto
{
    public string Label { get; set; } = string.Empty;
    public int Count { get; set; }
    public string? Detail { get; set; }
}
