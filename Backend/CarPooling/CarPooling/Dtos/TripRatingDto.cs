using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class CreateTripRatingDto
{
    [Required]
    public Guid EvaluatedUserId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "La puntuación debe estar entre 1 y 5 estrellas.")]
    public int Score { get; set; }

    [MaxLength(1000, ErrorMessage = "El comentario no puede exceder los 1000 caracteres.")]
    public string? Comment { get; set; }

    [MaxLength(500, ErrorMessage = "Las etiquetas no pueden exceder los 500 caracteres.")]
    public string? Tags { get; set; }
}

public class TripRatingResponseDto
{
    public Guid Id { get; set; }
    public Guid TripId { get; set; }
    public Guid EvaluatorUserId { get; set; }
    public string EvaluatorName { get; set; } = string.Empty;
    public Guid EvaluatedUserId { get; set; }
    public string EvaluatedName { get; set; } = string.Empty;
    public RatingRole RatingRole { get; set; }
    public string RatingRoleLabel { get; set; } = string.Empty;
    public int Score { get; set; }
    public string? Comment { get; set; }
    public string? Tags { get; set; }
    public DateTime CreatedAt { get; set; }

    public static TripRatingResponseDto FromEntity(TripRating r)
    {
        return new TripRatingResponseDto
        {
            Id = r.Id,
            TripId = r.TripId,
            EvaluatorUserId = r.EvaluatorUserId,
            EvaluatorName = r.EvaluatorUser?.FullName ?? "Usuario",
            EvaluatedUserId = r.EvaluatedUserId,
            EvaluatedName = r.EvaluatedUser?.FullName ?? "Usuario",
            RatingRole = r.RatingRole,
            RatingRoleLabel = r.RatingRole == RatingRole.DriverToPassenger ? "driver_to_passenger" : "passenger_to_driver",
            Score = r.Score,
            Comment = r.Comment,
            Tags = r.Tags,
            CreatedAt = r.CreatedAt
        };
    }
}

public class UserRatingSummaryDto
{
    public Guid UserId { get; set; }
    public string UserFullName { get; set; } = string.Empty;

    // Resumen Global
    public double AverageScore { get; set; }
    public int TotalRatingsCount { get; set; }

    // Resumen Conductor
    public double AverageDriverScore { get; set; }
    public int TotalDriverRatingsCount { get; set; }
    public int[] DriverStarsDistribution { get; set; } = new int[5];

    // Resumen Pasajero
    public double AveragePassengerScore { get; set; }
    public int TotalPassengerRatingsCount { get; set; }
    public int[] PassengerStarsDistribution { get; set; } = new int[5];
}
