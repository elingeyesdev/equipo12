using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class AuditController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet("logs")]
    [RequirePermission(AppPermissions.ReadAudit)]
    public async Task<ActionResult<IReadOnlyList<AuditLogResponseDto>>> GetLogsAsync(
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to,
        [FromQuery] Guid? userId,
        [FromQuery] string? action,
        [FromQuery] string? module,
        [FromQuery] string? entity,
        [FromQuery] string? entityId,
        [FromQuery] string? role,
        [FromQuery] string? result,
        [FromQuery] int limit = 200)
    {
        var query = _context.AuditLogs
            .AsNoTracking()
            .Include(l => l.ActorUser)
            .AsQueryable();

        if (from.HasValue)
        {
            query = query.Where(l => l.CreatedAt >= from.Value);
        }

        if (to.HasValue)
        {
            query = query.Where(l => l.CreatedAt <= to.Value);
        }

        if (userId.HasValue)
        {
            query = query.Where(l => l.ActorUserId == userId.Value);
        }

        if (!string.IsNullOrWhiteSpace(action))
        {
            var normalizedAction = action.Trim();
            query = query.Where(l => l.ActionType.Contains(normalizedAction));
        }

        if (!string.IsNullOrWhiteSpace(module))
        {
            var normalizedModule = module.Trim();
            query = query.Where(l => l.Module.Contains(normalizedModule));
        }

        if (!string.IsNullOrWhiteSpace(entity))
        {
            var normalizedEntity = entity.Trim();
            query = query.Where(l => l.EntityName.Contains(normalizedEntity));
        }

        if (!string.IsNullOrWhiteSpace(entityId))
        {
            var normalizedEntityId = entityId.Trim();
            query = query.Where(l => l.EntityId != null && l.EntityId.Contains(normalizedEntityId));
        }

        if (!string.IsNullOrWhiteSpace(role))
        {
            var normalizedRole = role.Trim();
            query = query.Where(l => l.ActorUser != null &&
                l.ActorUser.UserRoles.Any(ur => ur.Role.Name.Contains(normalizedRole)));
        }

        if (!string.IsNullOrWhiteSpace(result))
        {
            var normalizedResult = result.Trim();
            query = query.Where(l => l.Result == normalizedResult);
        }

        var safeLimit = Math.Clamp(limit, 1, 1000);
        var logs = await query
            .OrderByDescending(l => l.CreatedAt)
            .Take(safeLimit)
            .ToListAsync();

        var response = logs.Select(AuditLogResponseDto.FromEntity).ToList();
        await EnrichSupportTicketLogsAsync(response);
        return Ok(response);
    }

    [HttpGet("users/{userId:guid}")]
    [RequirePermission(AppPermissions.ReadAudit)]
    public async Task<ActionResult<object>> GetUserAuditAsync(Guid userId)
    {
        var user = await _context.Users
            .AsNoTracking()
            .Include(u => u.UserRoles)
                .ThenInclude(ur => ur.Role)
            .FirstOrDefaultAsync(u => u.Id == userId);

        if (user is null)
        {
            return NotFound("Usuario no encontrado.");
        }

        var logs = await _context.AuditLogs
            .AsNoTracking()
            .Include(l => l.ActorUser)
            .Where(l => l.ActorUserId == userId || (l.EntityName == "User" && l.EntityId == userId.ToString()))
            .OrderByDescending(l => l.CreatedAt)
            .Take(300)
            .ToListAsync();

        var tripsCount = await _context.Trips.CountAsync(t => t.DriverUserId == userId);
        var reservationsCount = await _context.Reservations.CountAsync(r => r.PassengerUserId == userId);
        var reportsSent = await _context.SupportTickets.CountAsync(t => t.UserId == userId);
        var reportsReceived = await CountReportsReceivedByUserAsync(userId);

        return Ok(new
        {
            user = UserResponseDto.FromEntity(user),
            summary = new
            {
                tripsCount,
                reservationsCount,
                reportsSent,
                reportsReceived,
                accountChanges = logs.Count(l => l.EntityName == "User"),
                passwordChanges = logs.Count(l => l.ActionType.Contains("Password", StringComparison.OrdinalIgnoreCase))
            },
            logs = logs.Select(AuditLogResponseDto.FromEntity).ToList()
        });
    }

    [HttpGet("trips/{tripId:guid}")]
    [RequirePermission(AppPermissions.ReadAudit)]
    public async Task<ActionResult<object>> GetTripAuditAsync(Guid tripId)
    {
        var trip = await _context.Trips
            .AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);

        if (trip is null)
        {
            return NotFound("Viaje no encontrado.");
        }

        var logs = await _context.AuditLogs
            .AsNoTracking()
            .Include(l => l.ActorUser)
            .Where(l => l.EntityName == "Trip" && l.EntityId == tripId.ToString())
            .OrderBy(l => l.CreatedAt)
            .ToListAsync();

        var reservations = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .Where(r => r.TripId == tripId)
            .OrderBy(r => r.CreatedAt)
            .ToListAsync();

        return Ok(new
        {
            trip = Services.TripService.MapToDto(trip),
            reservations = reservations.Select(Services.ReservationService.MapToDto).ToList(),
            timeline = logs.Select(AuditLogResponseDto.FromEntity).ToList()
        });
    }

    [HttpGet("reservations/{reservationId:guid}")]
    [RequirePermission(AppPermissions.ReadAudit)]
    public async Task<ActionResult<object>> GetReservationAuditAsync(Guid reservationId)
    {
        var reservation = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.PassengerUser)
            .Include(r => r.StatusEntity)
            .FirstOrDefaultAsync(r => r.Id == reservationId);

        if (reservation is null)
        {
            return NotFound("Reserva no encontrada.");
        }

        var logs = await _context.AuditLogs
            .AsNoTracking()
            .Include(l => l.ActorUser)
            .Where(l => l.EntityName == "Reservation" && l.EntityId == reservationId.ToString())
            .OrderBy(l => l.CreatedAt)
            .ToListAsync();

        return Ok(new
        {
            reservation = Services.ReservationService.MapToDto(reservation),
            timeline = logs.Select(AuditLogResponseDto.FromEntity).ToList()
        });
    }

    [HttpGet("dashboard")]
    [RequirePermission(AppPermissions.ReadAudit)]
    public async Task<ActionResult<AuditDashboardDto>> GetDashboardAsync(
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to)
    {
        var start = from ?? DateTime.UtcNow.AddDays(-30);
        var end = to ?? DateTime.UtcNow;

        var logsQuery = _context.AuditLogs.AsNoTracking().Where(l => l.CreatedAt >= start && l.CreatedAt <= end);
        var usersQuery = _context.Users.AsNoTracking().Where(u => u.CreatedAt >= start && u.CreatedAt <= end);
        var tripsQuery = _context.Trips.AsNoTracking().Where(t => t.CreatedAt >= start && t.CreatedAt <= end);
        var reservationsQuery = _context.Reservations.AsNoTracking().Where(r => r.CreatedAt >= start && r.CreatedAt <= end);
        var ticketsQuery = _context.SupportTickets.AsNoTracking().Where(t => t.CreatedAt >= start && t.CreatedAt <= end);

        var activeDrivers = await _context.Users
            .AsNoTracking()
            .CountAsync(u => u.IsActive && u.UserRoles.Any(ur => ur.Role.Name == "Driver"));

        var activePassengers = await _context.Users
            .AsNoTracking()
            .CountAsync(u => u.IsActive && u.UserRoles.Any(ur => ur.Role.Name == "Student"));

        var recentAdminChanges = await _context.AuditLogs
            .AsNoTracking()
            .Include(l => l.ActorUser)
            .Where(l => l.CreatedAt >= start && l.CreatedAt <= end && l.ActorUser != null &&
                l.ActorUser.UserRoles.Any(ur => ur.Role.Name.Contains("Admin") || ur.Role.Name == "SuperAdmin"))
            .OrderByDescending(l => l.CreatedAt)
            .Take(8)
            .Select(l => new AuditMetricItemDto
            {
                Label = l.ActionType,
                Count = 1,
                Detail = (l.ActorEmailSnapshot ?? l.ActorUser!.Email) + " - " + l.EntityName + " " + l.EntityId
            })
            .ToListAsync();

        var busiestHours = await tripsQuery
            .GroupBy(t => t.CreatedAt.Hour)
            .Select(g => new AuditMetricItemDto
            {
                Label = g.Key.ToString("00") + ":00",
                Count = g.Count()
            })
            .OrderByDescending(i => i.Count)
            .Take(5)
            .ToListAsync();

        var usersWithManyCancellations = await reservationsQuery
            .Where(r => r.StatusId == 4)
            .GroupBy(r => r.PassengerUserId)
            .Select(g => new { UserId = g.Key, Count = g.Count() })
            .OrderByDescending(x => x.Count)
            .Take(5)
            .Join(_context.Users.AsNoTracking(), x => x.UserId, u => u.Id, (x, u) => new AuditMetricItemDto
            {
                Label = u.FullName,
                Count = x.Count,
                Detail = u.Email
            })
            .ToListAsync();

        var driversWithManyReports = await ticketsQuery
            .Where(t => t.TripId != null)
            .Join(_context.Trips.AsNoTracking(), t => t.TripId, trip => trip.Id, (t, trip) => new { trip.DriverUserId })
            .Where(x => x.DriverUserId != null)
            .GroupBy(x => x.DriverUserId!.Value)
            .Select(g => new { UserId = g.Key, Count = g.Count() })
            .OrderByDescending(x => x.Count)
            .Take(5)
            .Join(_context.Users.AsNoTracking(), x => x.UserId, u => u.Id, (x, u) => new AuditMetricItemDto
            {
                Label = u.FullName,
                Count = x.Count,
                Detail = u.Email
            })
            .ToListAsync();

        var passengersNoShowRisk = await _context.Reservations
            .AsNoTracking()
            .Where(r => r.CreatedAt >= start && r.CreatedAt <= end && r.StatusId == 2 && r.Trip.StatusId == 4)
            .GroupBy(r => r.PassengerUserId)
            .Select(g => new { UserId = g.Key, Count = g.Count() })
            .OrderByDescending(x => x.Count)
            .Take(5)
            .Join(_context.Users.AsNoTracking(), x => x.UserId, u => u.Id, (x, u) => new AuditMetricItemDto
            {
                Label = u.FullName,
                Count = x.Count,
                Detail = "Reservas confirmadas sin abordaje en viajes finalizados"
            })
            .ToListAsync();

        var repeatedCancelledTrips = await tripsQuery
            .Where(t => t.StatusId == 5 && t.DriverUserId != null)
            .GroupBy(t => t.DriverUserId!.Value)
            .Select(g => new { UserId = g.Key, Count = g.Count() })
            .OrderByDescending(x => x.Count)
            .Take(5)
            .Join(_context.Users.AsNoTracking(), x => x.UserId, u => u.Id, (x, u) => new AuditMetricItemDto
            {
                Label = u.FullName,
                Count = x.Count,
                Detail = u.Email
            })
            .ToListAsync();

        var incidentHours = await ticketsQuery
            .GroupBy(t => t.CreatedAt.Hour)
            .Select(g => new AuditMetricItemDto
            {
                Label = g.Key.ToString("00") + ":00",
                Count = g.Count()
            })
            .OrderByDescending(i => i.Count)
            .Take(5)
            .ToListAsync();

        return Ok(new AuditDashboardDto
        {
            TotalLogs = await logsQuery.CountAsync(),
            AdministrativeChanges = await logsQuery.CountAsync(l => l.Module == "Admin" || l.ActionType.StartsWith("Admin")),
            UserChanges = await logsQuery.CountAsync(l => l.EntityName == "User"),
            TripChanges = await logsQuery.CountAsync(l => l.EntityName == "Trip"),
            ReservationChanges = await logsQuery.CountAsync(l => l.EntityName == "Reservation"),
            ReportChanges = await logsQuery.CountAsync(l => l.EntityName == "SupportTicket"),
            UsersRegistered = await usersQuery.CountAsync(),
            ActiveDrivers = activeDrivers,
            ActivePassengers = activePassengers,
            TripsCreated = await tripsQuery.CountAsync(),
            TripsCompleted = await tripsQuery.CountAsync(t => t.StatusId == 4),
            TripsCancelled = await tripsQuery.CountAsync(t => t.StatusId == 5),
            BlockedOrInactiveUsers = await _context.Users.AsNoTracking().CountAsync(u => !u.IsActive),
            ReportsResolved = await ticketsQuery.CountAsync(t => t.Status == SupportTicketStatus.Resolved || t.Status == SupportTicketStatus.Closed),
            ReportsUnresolved = await ticketsQuery.CountAsync(t => t.Status == SupportTicketStatus.Open || t.Status == SupportTicketStatus.InReview),
            BusiestHours = busiestHours,
            RecentAdministrativeChanges = recentAdminChanges,
            UsersWithManyCancellations = usersWithManyCancellations,
            DriversWithManyReports = driversWithManyReports,
            PassengersNoShowRisk = passengersNoShowRisk,
            RepeatedCancelledTrips = repeatedCancelledTrips,
            IncidentHours = incidentHours
        });
    }

    private async Task<int> CountReportsReceivedByUserAsync(Guid userId)
    {
        var tripReports = await _context.SupportTickets
            .AsNoTracking()
            .Where(t => t.TripId != null)
            .Join(_context.Trips.AsNoTracking(), t => t.TripId, trip => trip.Id, (t, trip) => trip)
            .CountAsync(trip => trip.DriverUserId == userId);

        return tripReports;
    }

    private async Task EnrichSupportTicketLogsAsync(List<AuditLogResponseDto> logs)
    {
        var ticketIds = logs
            .Where(l => l.EntityName == "SupportTicket" && Guid.TryParse(l.EntityId, out _))
            .Select(l => Guid.Parse(l.EntityId!))
            .Distinct()
            .ToList();

        if (ticketIds.Count == 0)
        {
            return;
        }

        var tickets = await _context.SupportTickets
            .AsNoTracking()
            .Include(t => t.User)
            .Include(t => t.Trip)
                .ThenInclude(t => t!.DriverUser)
            .Where(t => ticketIds.Contains(t.Id))
            .ToDictionaryAsync(t => t.Id);

        foreach (var log in logs.Where(l => l.EntityName == "SupportTicket" && Guid.TryParse(l.EntityId, out _)))
        {
            var ticketId = Guid.Parse(log.EntityId!);
            if (!tickets.TryGetValue(ticketId, out var ticket))
            {
                continue;
            }

            var driverName = ticket.Trip?.DriverName;
            if (string.IsNullOrWhiteSpace(driverName))
            {
                driverName = ticket.Trip?.DriverUser?.FullName;
            }

            var statusLabel = FormatSupportTicketStatus(ticket.Status);
            log.RelatedSubject = ticket.Subject;
            log.RelatedDescription = ticket.Description;
            log.RelatedReporterName = ticket.User?.FullName;
            log.RelatedDriverName = driverName;
            log.RelatedStatus = statusLabel;
            log.RelatedTripId = ticket.TripId?.ToString();
            log.RelatedReservationId = ticket.ReservationId?.ToString();
            log.RelatedSummary = $"Reporte de {ticket.User?.FullName ?? "usuario"}" +
                (string.IsNullOrWhiteSpace(driverName) ? "" : $" sobre {driverName}") +
                $": {ticket.Subject}";
            log.RelatedSearchText = string.Join(" ", new[]
            {
                ticket.Subject,
                ticket.Description,
                ticket.User?.FullName,
                ticket.User?.Email,
                driverName,
                ticket.Trip?.DriverUser?.Email,
                statusLabel,
                ticket.TripId?.ToString(),
                ticket.ReservationId?.ToString()
            }.Where(v => !string.IsNullOrWhiteSpace(v)));
        }
    }

    private static string FormatSupportTicketStatus(SupportTicketStatus status) => status switch
    {
        SupportTicketStatus.Open => "Abierto",
        SupportTicketStatus.InReview => "En revision",
        SupportTicketStatus.Resolved => "Resuelto",
        SupportTicketStatus.Closed => "Cerrado",
        _ => "Desconocido"
    };
}
