using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class SupportTicketService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

    private static readonly SupportTicketStatus[] ActiveStatuses =
    [
        SupportTicketStatus.Open,
        SupportTicketStatus.InReview
    ];

    public async Task<SupportTicketResponseDto> CreateAsync(Guid userId, CreateSupportTicketDto dto)
    {
        var userExists = await _context.Users.AnyAsync(u => u.Id == userId);
        if (!userExists)
        {
            throw new KeyNotFoundException("Usuario no encontrado.");
        }

        if (!Enum.IsDefined(typeof(SupportTicketCategory), dto.Category))
        {
            throw new InvalidOperationException("Categoría de soporte no válida.");
        }

        var subject = dto.Subject.Trim();
        var description = dto.Description.Trim();

        if (subject.Length < 3)
        {
            throw new InvalidOperationException("El asunto debe tener al menos 3 caracteres.");
        }

        if (description.Length < 10)
        {
            throw new InvalidOperationException("La descripción debe tener al menos 10 caracteres.");
        }

        Guid? tripId = dto.TripId;
        Guid? reservationId = dto.ReservationId;

        switch (dto.Category)
        {
            case SupportTicketCategory.Trip:
                if (!tripId.HasValue)
                {
                    throw new InvalidOperationException("Debes vincular un viaje para reportes de tipo Viaje.");
                }
                await ValidateTripLinkAsync(userId, tripId.Value);
                reservationId = null;
                break;

            case SupportTicketCategory.Reservation:
                if (!reservationId.HasValue)
                {
                    throw new InvalidOperationException("Debes vincular una reserva para reportes de tipo Reserva.");
                }
                tripId = await ValidateReservationLinkAsync(userId, reservationId.Value);
                break;

            case SupportTicketCategory.Account:
            case SupportTicketCategory.Payment:
            case SupportTicketCategory.Other:
                if (tripId.HasValue || reservationId.HasValue)
                {
                    throw new InvalidOperationException(
                        "Los reportes de cuenta, pago u otro no deben vincularse a un viaje o reserva.");
                }
                tripId = null;
                reservationId = null;
                break;
        }

        await ValidateNoDuplicateActiveAsync(userId, dto.Category, tripId, reservationId);

        var ticket = new SupportTicket
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TripId = tripId,
            ReservationId = reservationId,
            Category = dto.Category,
            Subject = subject,
            Description = description,
            Status = SupportTicketStatus.Open,
            CreatedAt = DateTime.UtcNow
        };

        _context.SupportTickets.Add(ticket);
        await _context.SaveChangesAsync();

        return await GetByIdAsync(userId, ticket.Id)
            ?? throw new InvalidOperationException("No se pudo recuperar el reporte creado.");
    }

    public async Task<SupportTicketListResponseDto> ListForUserAsync(Guid userId)
    {
        var userExists = await _context.Users.AnyAsync(u => u.Id == userId);
        if (!userExists)
        {
            throw new KeyNotFoundException("Usuario no encontrado.");
        }

        var tickets = await _context.SupportTickets
            .AsNoTracking()
            .Include(t => t.User)
            .Where(t => t.UserId == userId)
            .OrderByDescending(t => t.CreatedAt)
            .ToListAsync();

        var items = tickets.ConvertAll(SupportTicketResponseDto.FromEntity);

        return new SupportTicketListResponseDto
        {
            Items = items,
            TotalCount = items.Count
        };
    }

    public async Task<SupportTicketResponseDto?> GetByIdAsync(Guid userId, Guid ticketId)
    {
        var ticket = await _context.SupportTickets
            .AsNoTracking()
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.Id == ticketId && t.UserId == userId);

        return ticket is null ? null : SupportTicketResponseDto.FromEntity(ticket);
    }

    private async Task ValidateTripLinkAsync(Guid userId, Guid tripId)
    {
        var trip = await _context.Trips
            .AsNoTracking()
            .Include(t => t.Reservations)
            .FirstOrDefaultAsync(t => t.Id == tripId && t.Kind == TripKind.Regular);

        if (trip is null)
        {
            throw new KeyNotFoundException("El viaje indicado no existe.");
        }

        var isDriver = trip.DriverUserId == userId;
        var isPassenger = trip.Reservations.Any(r => r.PassengerUserId == userId);

        if (!isDriver && !isPassenger)
        {
            throw new InvalidOperationException(
                "Solo puedes vincular viajes en los que participaste como conductor o pasajero.");
        }
    }

    private async Task<Guid> ValidateReservationLinkAsync(Guid userId, Guid reservationId)
    {
        var reservation = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId);

        if (reservation is null)
        {
            throw new KeyNotFoundException("La reserva indicada no existe.");
        }

        if (reservation.PassengerUserId != userId)
        {
            throw new InvalidOperationException("Solo puedes vincular reservas que te pertenecen como pasajero.");
        }

        if (reservation.Trip is null || reservation.Trip.Kind != TripKind.Regular)
        {
            throw new InvalidOperationException("La reserva no está asociada a un viaje válido.");
        }

        return reservation.TripId;
    }

    private async Task ValidateNoDuplicateActiveAsync(
        Guid userId,
        SupportTicketCategory category,
        Guid? tripId,
        Guid? reservationId)
    {
        var query = _context.SupportTickets
            .Where(t => t.UserId == userId
                        && t.Category == category
                        && ActiveStatuses.Contains(t.Status));

        if (category == SupportTicketCategory.Trip)
        {
            if (!tripId.HasValue)
            {
                return;
            }

            var exists = await query.AnyAsync(t => t.TripId == tripId);
            if (exists)
            {
                throw new InvalidOperationException(
                    "Ya tienes un reporte activo de tipo Viaje para este viaje.");
            }
            return;
        }

        if (category == SupportTicketCategory.Reservation)
        {
            if (!reservationId.HasValue)
            {
                return;
            }

            var exists = await query.AnyAsync(t => t.ReservationId == reservationId);
            if (exists)
            {
                throw new InvalidOperationException(
                    "Ya tienes un reporte activo de tipo Reserva para esta reserva.");
            }
        }
    }
}
