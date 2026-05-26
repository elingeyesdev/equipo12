using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class SupportTicket
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }
    public User User { get; set; } = null!;

    public Guid? TripId { get; set; }
    public Trip? Trip { get; set; }

    public Guid? ReservationId { get; set; }
    public Reservation? Reservation { get; set; }

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

    public DateTime? FirstAdminReplyAt { get; set; }

    public DateTime? LastMessageAt { get; set; }

    public Guid? AssignedAdminUserId { get; set; }

    [ForeignKey(nameof(AssignedAdminUserId))]
    public User? AssignedAdminUser { get; set; }

    public DateTime? ClosedAt { get; set; }

    public ICollection<SupportTicketMessage> Messages { get; set; } = [];
}
