using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class TripChatMessageRead
{
    [Required]
    public Guid MessageId { get; set; }

    [ForeignKey(nameof(MessageId))]
    public TripChatMessage Message { get; set; } = null!;

    [Required]
    public Guid UserId { get; set; }

    [ForeignKey(nameof(UserId))]
    public User User { get; set; } = null!;

    public DateTime ReadAt { get; set; } = DateTime.UtcNow;
}
