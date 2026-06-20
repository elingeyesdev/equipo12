using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class ReservationService(
    CarPoolingContext context,
    INotificationService notificationService,
    AuditService auditService)
{
    private readonly CarPoolingContext _context = context;
    private readonly INotificationService _notificationService = notificationService;
    private readonly AuditService _auditService = auditService;

    /// <summary>Crea reserva en estado "pending". Genera código de abordaje.</summary>
    public async Task<Reservation> CreateAsync(Guid tripId, CreateReservationDto dto)
    {
        var trip = await _context.Trips.FindAsync(tripId)
            ?? throw new InvalidOperationException("Viaje no encontrado.");

        if (trip.StatusId == 5) // cancelled
            throw new InvalidOperationException("El viaje no esta disponible.");

        if (trip.AvailableSeats < dto.SeatsReserved)
            throw new InvalidOperationException("No hay suficientes cupos disponibles.");

        var userExists = await _context.Users.AnyAsync(u => u.Id == dto.PassengerUserId);
        if (!userExists)
            throw new InvalidOperationException("Usuario pasajero no encontrado.");

        var existing = await _context.Reservations.AnyAsync(r =>
            r.TripId == tripId && r.PassengerUserId == dto.PassengerUserId
            && (r.StatusId == 1 || r.StatusId == 2)); // pending o confirmed
        if (existing)
            throw new InvalidOperationException("Ya tienes una solicitud para este viaje.");

        var code = new Random().Next(1000, 9999).ToString();
        var reservation = new Reservation
        {
            TripId = tripId,
            PassengerUserId = dto.PassengerUserId,
            SeatsReserved = dto.SeatsReserved,
            StatusId = 1, // pending
            BoardingCode = code
        };

        _context.Reservations.Add(reservation);
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserReservedSeat",
            "Reservations",
            "Reservation",
            reservation.Id.ToString(),
            null,
            AuditService.SnapshotReservation(reservation),
            actorUserId: reservation.PassengerUserId,
            description: $"Pasajero reservo {reservation.SeatsReserved} asiento(s) en el viaje {tripId}.");

        var passenger = await _context.Users.FindAsync(dto.PassengerUserId);
        if (passenger != null && trip.DriverUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                trip.DriverUserId.Value,
                "Nueva solicitud de reserva",
                $"{passenger.FullName} ha solicitado unirse a tu viaje.",
                new Dictionary<string, string> { { "type", "reservation_request" }, { "tripId", tripId.ToString() } }
            );
        }

        return reservation;
    }

    /// <summary>Conductor acepta solicitud → confirmed, descuenta asientos.</summary>
    public async Task<Reservation> AcceptAsync(Guid reservationId)
    {
        var reservation = await _context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new InvalidOperationException("Reserva no encontrada.");

        if (reservation.StatusId != 1) // no está pending
            throw new InvalidOperationException("Solo se pueden aceptar reservas pendientes.");

        if (reservation.Trip.AvailableSeats < reservation.SeatsReserved)
            throw new InvalidOperationException("No hay suficientes cupos disponibles.");

        var oldValues = AuditService.SnapshotReservation(reservation);

        reservation.StatusId = 2; // confirmed
        reservation.Trip.AvailableSeats -= reservation.SeatsReserved;

        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "DriverAcceptedReservation",
            "Reservations",
            "Reservation",
            reservation.Id.ToString(),
            oldValues,
            AuditService.SnapshotReservation(reservation),
            actorUserId: reservation.Trip.DriverUserId,
            description: $"Conductor acepto la reserva {reservation.Id}.");

        await _notificationService.SendNotificationAsync(
            reservation.PassengerUserId,
            "Reserva Confirmada",
            $"Tu solicitud de viaje con {reservation.Trip.DriverName} ha sido aceptada.",
            new Dictionary<string, string> { { "type", "reservation_accepted" }, { "tripId", reservation.TripId.ToString() } }
        );

        return reservation;
    }

    /// <summary>Conductor rechaza solicitud pendiente → cancelled.</summary>
    public async Task<Reservation> RejectAsync(Guid reservationId)
    {
        var reservation = await _context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new InvalidOperationException("Reserva no encontrada.");

        if (reservation.StatusId != 1) // no está pending
            throw new InvalidOperationException("Solo se pueden rechazar reservas pendientes.");

        var oldValues = AuditService.SnapshotReservation(reservation);

        reservation.StatusId = 4; // cancelled
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "DriverRejectedReservation",
            "Reservations",
            "Reservation",
            reservation.Id.ToString(),
            oldValues,
            AuditService.SnapshotReservation(reservation),
            actorUserId: reservation.Trip.DriverUserId,
            description: $"Conductor rechazo la reserva {reservation.Id}.");

        await _notificationService.SendNotificationAsync(
            reservation.PassengerUserId,
            "Reserva Rechazada",
            $"Lo sentimos, tu solicitud de viaje con {reservation.Trip.DriverName} fue rechazada.",
            new Dictionary<string, string> { { "type", "reservation_rejected" }, { "tripId", reservation.TripId.ToString() } }
        );

        return reservation;
    }

    /// <summary>Marcar como abordado (solo desde confirmed).</summary>
    public async Task<Reservation> BoardAsync(Guid reservationId)
    {
        var reservation = await _context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new InvalidOperationException("Reserva no encontrada.");

        if (reservation.StatusId != 2) // no está confirmed
            throw new InvalidOperationException("Solo se pueden abordar reservas confirmadas.");

        var oldValues = AuditService.SnapshotReservation(reservation);

        reservation.StatusId = 3; // boarded
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserBoardedTrip",
            "Reservations",
            "Reservation",
            reservation.Id.ToString(),
            oldValues,
            AuditService.SnapshotReservation(reservation),
            actorUserId: reservation.PassengerUserId,
            description: $"Pasajero abordo el viaje {reservation.TripId}.");

        await _notificationService.SendNotificationAsync(
            reservation.PassengerUserId,
            "Abordaje confirmado",
            $"Tu abordaje al viaje con {reservation.Trip.DriverName} fue confirmado.",
            new Dictionary<string, string>
            {
                { "type", "reservation_boarded" },
                { "tripId", reservation.TripId.ToString() },
                { "reservationId", reservation.Id.ToString() }
            });

        return reservation;
    }

    /// <summary>Cancelar reserva. Si estaba confirmed, restaura asientos.</summary>
    public async Task<Reservation> CancelAsync(Guid reservationId)
    {
        var reservation = await _context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new InvalidOperationException("Reserva no encontrada.");

        if (reservation.StatusId == 4) // already cancelled
            throw new InvalidOperationException("La reserva ya esta cancelada.");

        if (reservation.StatusId == 3) // boarded
            throw new InvalidOperationException("No se puede cancelar una reserva abordada.");

        if (reservation.StatusId == 2) // confirmed → restore seats
        {
            reservation.Trip.AvailableSeats += reservation.SeatsReserved;
        }

        var oldValues = AuditService.SnapshotReservation(reservation);

        reservation.StatusId = 4; // cancelled
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserCancelledReservation",
            "Reservations",
            "Reservation",
            reservation.Id.ToString(),
            oldValues,
            AuditService.SnapshotReservation(reservation),
            actorUserId: reservation.PassengerUserId,
            description: $"Pasajero cancelo la reserva {reservation.Id}.");

        var passenger = await _context.Users.FindAsync(reservation.PassengerUserId);
        if (passenger != null && reservation.Trip.DriverUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                reservation.Trip.DriverUserId.Value,
                "Reserva Cancelada",
                $"{passenger.FullName} ha cancelado su reserva.",
                new Dictionary<string, string> { { "type", "reservation_cancelled" }, { "tripId", reservation.TripId.ToString() } }
            );
        }

        return reservation;
    }

    public async Task<List<Reservation>> GetPendingForTripAsync(Guid tripId)
    {
        return await _context.Reservations
            .Include(r => r.PassengerUser)
            .Where(r => r.TripId == tripId && r.StatusId == 1) // pending
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();
    }

    public async Task<List<Reservation>> GetConfirmedForTripAsync(Guid tripId)
    {
        return await _context.Reservations
            .Include(r => r.PassengerUser)
            .Where(r => r.TripId == tripId && r.StatusId == 2) // confirmed
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();
    }

    public async Task<List<Reservation>> GetBoardedForTripAsync(Guid tripId)
    {
        return await _context.Reservations
            .Include(r => r.PassengerUser)
            .Where(r => r.TripId == tripId && r.StatusId == 3) // boarded
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();
    }

    public async Task<Reservation?> GetActiveForPassengerAsync(Guid passengerUserId)
    {
        return await _context.Reservations
            .Include(r => r.Trip).ThenInclude(t => t.OriginLocation)
            .Include(r => r.Trip).ThenInclude(t => t.DestinationLocation)
            .Include(r => r.Trip).ThenInclude(t => t.Vehicle)
            .Include(r => r.StatusEntity)
            .Where(r => r.PassengerUserId == passengerUserId
                && r.StatusId != 4
                && r.Trip.StatusId != 4
                && r.Trip.StatusId != 5)
            .OrderByDescending(r => r.CreatedAt)
            .FirstOrDefaultAsync();
    }

    public async Task<bool> VerifyBoardingCodeAsync(Guid reservationId, string code)
    {
        var r = await _context.Reservations.FirstOrDefaultAsync(r => r.Id == reservationId);
        return r != null && r.BoardingCode == code;
    }

    public async Task<int> GetPendingCountForTripAsync(Guid tripId)
    {
        return await _context.Reservations.CountAsync(r => r.TripId == tripId && r.StatusId == 1);
    }

    public async Task<int> GetConfirmedCountForTripAsync(Guid tripId)
    {
        return await _context.Reservations.CountAsync(r => r.TripId == tripId && r.StatusId == 2);
    }

    public static ReservationDto MapToDto(Reservation r)
    {
        return new ReservationDto
        {
            Id = r.Id,
            TripId = r.TripId,
            PassengerUserId = r.PassengerUserId,
            PassengerName = r.PassengerUser?.FullName ?? "",
            SeatsReserved = r.SeatsReserved,
            Status = r.StatusEntity?.LabelEs ?? "",
            StatusId = r.StatusId,
            BoardingCode = r.BoardingCode,
            CreatedAt = r.CreatedAt
        };
    }

    public async Task<ReservationDto> MapToDtoAsync(Reservation r)
    {
        var dto = MapToDto(r);
        dto.PassengerProfilePicture = r.PassengerUser?.ProfilePicture;
        
        var avgRating = await _context.TripRatings
            .Where(tr => tr.EvaluatedUserId == r.PassengerUserId && tr.RatingRole == RatingRole.DriverToPassenger)
            .Select(tr => (double?)tr.Score)
            .AverageAsync();
            
        dto.PassengerRating = avgRating.HasValue ? Math.Round(avgRating.Value, 1) : 5.0;
        return dto;
    }

    public async Task<List<ReservationDto>> MapToDtoListAsync(List<Reservation> reservations)
    {
        if (reservations.Count == 0)
        {
            return new List<ReservationDto>();
        }

        var passengerIds = reservations.Select(r => r.PassengerUserId).Distinct().ToList();
        var ratings = await _context.TripRatings
            .Where(tr => passengerIds.Contains(tr.EvaluatedUserId) && tr.RatingRole == RatingRole.DriverToPassenger)
            .GroupBy(tr => tr.EvaluatedUserId)
            .Select(g => new { PassengerUserId = g.Key, AvgScore = g.Average(tr => tr.Score) })
            .ToDictionaryAsync(x => x.PassengerUserId, x => x.AvgScore);

        return reservations.Select(r => 
        {
            var dto = MapToDto(r);
            dto.PassengerProfilePicture = r.PassengerUser?.ProfilePicture;
            dto.PassengerRating = ratings.TryGetValue(r.PassengerUserId, out var rating) ? Math.Round(rating, 1) : 5.0;
            return dto;
        }).ToList();
    }
}
