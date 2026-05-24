using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public enum RatingRole
{
    DriverToPassenger = 1,
    PassengerToDriver = 2
}

public class TripRating
{
    public Guid Id { get; set; }

    [Required]
    public Guid TripId { get; set; }
    public Trip Trip { get; set; } = null!;

    [Required]
    public Guid EvaluatorUserId { get; set; }
    public User EvaluatorUser { get; set; } = null!;

    [Required]
    public Guid EvaluatedUserId { get; set; }
    public User EvaluatedUser { get; set; } = null!;

    [Required]
    public RatingRole RatingRole { get; set; }

    [Required]
    [Range(1, 5)]
    public int Score { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }

    [MaxLength(500)]
    public string? Tags { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
