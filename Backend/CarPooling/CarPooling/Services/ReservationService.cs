using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class ReservationService(CarPoolingContext context, INotificationService notificationService)
{
    private readonly CarPoolingContext _context = context;
    private readonly INotificationService _notificationService = notificationService;

    /// <summary>Crea reserva en estado "pending". Genera código de abordaje.</summary>
    public async Task<Reservation> CreateAsync(Guid tripId, CreateReservationDto dto)
    {
        var trip = await _context.Trips.FindAsync(tripId)
            ?? throw new InvalidOperationException("Viaje no encontrado.");

        if (trip.Kind != TripKind.Regular)
            throw new InvalidOperationException("Este viaje no admite reservas.");

        if (trip.StatusId == 5) // cancelled
            throw new InvalidOperationException("El viaje no esta disponible.");

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

        reservation.StatusId = 2; // confirmed
        reservation.Trip.AvailableSeats -= reservation.SeatsReserved;

        await _context.SaveChangesAsync();

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

        reservation.StatusId = 4; // cancelled
        await _context.SaveChangesAsync();

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

        reservation.StatusId = 3; // boarded
        await _context.SaveChangesAsync();
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

        reservation.StatusId = 4; // cancelled
        await _context.SaveChangesAsync();

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
}
