using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class PhysicalPayment
{
    public Guid Id { get; set; }

    [Required]
    public Guid PaymentId { get; set; }

    [ForeignKey(nameof(PaymentId))]
    public Payment Payment { get; set; } = null!;

    [Required]
    public Guid ReceivedByUserId { get; set; }

    [ForeignKey(nameof(ReceivedByUserId))]
    public User ReceivedByUser { get; set; } = null!;

    public Guid? DeliveredByUserId { get; set; }

    [ForeignKey(nameof(DeliveredByUserId))]
    public User? DeliveredByUser { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal ExpectedAmount { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal ReceivedAmount { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal ChangeAmount { get; set; }

    public PhysicalPaymentStatus Status { get; set; } = PhysicalPaymentStatus.Pending;

    [MaxLength(300)]
    public string? EvidenceImageUrl { get; set; }

    [MaxLength(300)]
    public string? Notes { get; set; }

    public DateTime? ReceivedAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}
