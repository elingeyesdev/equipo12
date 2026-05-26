using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class SupportTicketMessage
{
    public Guid Id { get; set; }

    [Required]
    public Guid TicketId { get; set; }

    [ForeignKey(nameof(TicketId))]
    public SupportTicket Ticket { get; set; } = null!;

    [Required]
    public Guid SenderUserId { get; set; }

    [ForeignKey(nameof(SenderUserId))]
    public User SenderUser { get; set; } = null!;

    [Required]
    public SupportMessageSenderKind SenderKind { get; set; }

    [Required]
    [MaxLength(2000)]
    public string MessageText { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<SupportTicketMessageRead> Reads { get; set; } = [];
}
