using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class TripChatMessage
{
    public Guid Id { get; set; }

    [Required]
    public Guid ChatId { get; set; }

    [ForeignKey(nameof(ChatId))]
    public TripChat Chat { get; set; } = null!;

    [Required]
    public Guid SenderUserId { get; set; }

    [ForeignKey(nameof(SenderUserId))]
    public User SenderUser { get; set; } = null!;

    [Required]
    public string MessageText { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<TripChatMessageRead> Reads { get; set; } = [];
}
