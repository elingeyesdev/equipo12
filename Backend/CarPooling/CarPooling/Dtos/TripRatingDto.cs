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
            CreatedAt = r.CreatedAt
        };
    }
}

public class UserRatingSummaryDto
{
    public Guid UserId { get; set; }
    public string UserFullName { get; set; } = string.Empty;
    public double AverageScore { get; set; }
    public int TotalRatingsCount { get; set; }
}
