using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class SupportTicket
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }
    public User User { get; set; } = null!;

    public Guid? TripId { get; set; }
    public Trip? Trip { get; set; }

    [Required]
    public SupportTicketCategory Category { get; set; }

    [Required]
    [MaxLength(120)]
    public string Subject { get; set; } = string.Empty;

    [Required]
    [MaxLength(2000)]
    public string Description { get; set; } = string.Empty;

    [Required]
    public SupportTicketStatus Status { get; set; } = SupportTicketStatus.Open;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}
