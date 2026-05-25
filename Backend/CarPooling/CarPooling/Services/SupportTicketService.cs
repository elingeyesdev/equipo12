using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class SupportTicketService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

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

        if (dto.TripId.HasValue)
        {
            await ValidateTripLinkAsync(userId, dto.TripId.Value);
        }

        var ticket = new SupportTicket
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            TripId = dto.TripId,
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
            throw new InvalidOperationException("Solo puedes vincular viajes en los que participaste como conductor o pasajero.");
        }
    }
}
