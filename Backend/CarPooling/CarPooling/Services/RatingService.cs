using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class RatingService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

    /// <summary>
    /// Crea una nueva calificación en el contexto de un viaje finalizado.
    /// Valida que el viaje esté terminado y que los participantes formen parte del viaje.
    /// </summary>
    public async Task<TripRatingResponseDto> CreateRatingAsync(Guid tripId, Guid evaluatorUserId, CreateTripRatingDto dto)
    {
        // 1. Obtener viaje y validar existencia
        var trip = await _context.Trips
            .Include(t => t.Reservations)
            .FirstOrDefaultAsync(t => t.Id == tripId);

        if (trip == null)
        {
            throw new KeyNotFoundException("El viaje especificado no existe.");
        }

        // 2. Validar que el viaje esté Finalizado (StatusId == 4)
        if (trip.StatusId != 4)
        {
            throw new InvalidOperationException("Solo se pueden calificar viajes que hayan sido completados/finalizados.");
        }

        // 3. Validar existencia del usuario evaluado
        var evaluatedUserExists = await _context.Users.AnyAsync(u => u.Id == dto.EvaluatedUserId);
        if (!evaluatedUserExists)
        {
            throw new KeyNotFoundException("El usuario a calificar no existe.");
        }

        // 4. Validar que el evaluador no se califique a sí mismo
        if (evaluatorUserId == dto.EvaluatedUserId)
        {
            throw new InvalidOperationException("No puedes calificarte a ti mismo.");
        }

        // 5. Determinar el rol de la calificación y validar pertenencia al viaje
        RatingRole role;
        if (trip.DriverUserId == evaluatorUserId)
        {
            // El evaluador es el conductor. El evaluado debe ser un pasajero con reserva confirmada (2) o abordada (3).
            var isPassenger = trip.Reservations.Any(r => 
                r.PassengerUserId == dto.EvaluatedUserId && 
                (r.StatusId == 2 || r.StatusId == 3));

            if (!isPassenger)
            {
                throw new InvalidOperationException("El usuario calificado no fue un pasajero confirmado/abordado en este viaje.");
            }

            role = RatingRole.DriverToPassenger;
        }
        else if (trip.DriverUserId == dto.EvaluatedUserId)
        {
            // El evaluado es el conductor. El evaluador debe ser un pasajero con reserva confirmada (2) o abordada (3).
            var isPassenger = trip.Reservations.Any(r => 
                r.PassengerUserId == evaluatorUserId && 
                (r.StatusId == 2 || r.StatusId == 3));

            if (!isPassenger)
            {
                throw new InvalidOperationException("No estás autorizado para calificar en este viaje ya que no fuiste un pasajero confirmado/abordado.");
            }

            role = RatingRole.PassengerToDriver;
        }
        else
        {
            // Calificación no válida (p. ej., pasajero calificando a otro pasajero, o un tercero externo)
            throw new InvalidOperationException("Operación de calificación no permitida. Solo se permiten calificaciones de Conductor a Pasajero o de Pasajero a Conductor.");
        }

        // 6. Validar que no exista calificación duplicada en este viaje para esta misma pareja
        var alreadyRated = await _context.TripRatings.AnyAsync(r => 
            r.TripId == tripId && 
            r.EvaluatorUserId == evaluatorUserId && 
            r.EvaluatedUserId == dto.EvaluatedUserId);

        if (alreadyRated)
        {
            throw new InvalidOperationException("Ya has calificado a este participante para este viaje. No se permiten calificaciones duplicadas.");
        }

        // 7. Crear y persistir la calificación
        var rating = new TripRating
        {
            Id = Guid.NewGuid(),
            TripId = tripId,
            EvaluatorUserId = evaluatorUserId,
            EvaluatedUserId = dto.EvaluatedUserId,
            RatingRole = role,
            Score = dto.Score,
            Comment = dto.Comment,
            CreatedAt = DateTime.UtcNow
        };

        _context.TripRatings.Add(rating);
        await _context.SaveChangesAsync();

        // 8. Cargar relaciones del evaluador y evaluado para construir el DTO de respuesta
        var fullRating = await _context.TripRatings
            .Include(r => r.EvaluatorUser)
            .Include(r => r.EvaluatedUser)
            .FirstAsync(r => r.Id == rating.Id);

        return TripRatingResponseDto.FromEntity(fullRating);
    }

    /// <summary>
    /// Obtiene la lista de calificaciones recibidas por un usuario específico.
    /// </summary>
    public async Task<IEnumerable<TripRatingResponseDto>> GetRatingsForUserAsync(Guid userId)
    {
        var ratings = await _context.TripRatings
            .Include(r => r.EvaluatorUser)
            .Include(r => r.EvaluatedUser)
            .Where(r => r.EvaluatedUserId == userId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return ratings.Select(TripRatingResponseDto.FromEntity);
    }

    /// <summary>
    /// Obtiene el resumen consolidado de calificaciones de un usuario (Promedio de estrellas y total).
    /// </summary>
    public async Task<UserRatingSummaryDto> GetAverageRatingForUserAsync(Guid userId)
    {
        var user = await _context.Users.FindAsync(userId);
        if (user == null)
        {
            throw new KeyNotFoundException("El usuario especificado no existe.");
        }

        var scores = await _context.TripRatings
            .Where(r => r.EvaluatedUserId == userId)
            .Select(r => r.Score)
            .ToListAsync();

        double average = scores.Any() ? scores.Average() : 0.0;
        int count = scores.Count;

        return new UserRatingSummaryDto
        {
            UserId = userId,
            UserFullName = user.FullName,
            AverageScore = Math.Round(average, 2),
            TotalRatingsCount = count
        };
    }
}
