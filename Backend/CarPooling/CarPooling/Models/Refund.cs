using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Refund
{
    public Guid Id { get; set; }

    [Required]
    public Guid PaymentId { get; set; }

    [ForeignKey(nameof(PaymentId))]
    public Payment Payment { get; set; } = null!;

    [Column(TypeName = "decimal(10,2)")]
    public decimal Amount { get; set; }

    public RefundStatus Status { get; set; } = RefundStatus.Requested;

    [Required]
    public Guid RequestedByUserId { get; set; }

    [ForeignKey(nameof(RequestedByUserId))]
    public User RequestedByUser { get; set; } = null!;

    public Guid? ProcessedByUserId { get; set; }

    [ForeignKey(nameof(ProcessedByUserId))]
    public User? ProcessedByUser { get; set; }

    [MaxLength(500)]
    public string? Reason { get; set; }

    [MaxLength(500)]
    public string? RejectionReason { get; set; }

    public bool IsWithinCancellationWindow { get; set; } = true;

    public DateTime? CancellationDeadline { get; set; }

    public int? MinutesBeforeTripStart { get; set; }

    public DateTime RequestedAt { get; set; } = DateTime.UtcNow;

    public DateTime? ProcessedAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
