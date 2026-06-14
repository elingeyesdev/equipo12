using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class UserBookmark
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }

    [ForeignKey(nameof(UserId))]
    public User User { get; set; } = null!;

    [Required]
    [MaxLength(20)]
    public string Kind { get; set; } = "place"; // "place" o "route"

    [Required]
    [MaxLength(100)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public Guid OriginLocationId { get; set; }

    [ForeignKey(nameof(OriginLocationId))]
    public Location OriginLocation { get; set; } = null!;

    public Guid? DestinationLocationId { get; set; }

    [ForeignKey(nameof(DestinationLocationId))]
    public Location? DestinationLocation { get; set; }

    public int UseCount { get; set; } = 0;

    public DateTime? LastUsedAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
