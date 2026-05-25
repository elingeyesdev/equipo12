using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class TripChat
{
    public Guid Id { get; set; }

    [Required]
    public Guid TripId { get; set; }

    [ForeignKey(nameof(TripId))]
    public Trip Trip { get; set; } = null!;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<TripChatMessage> Messages { get; set; } = [];
}
