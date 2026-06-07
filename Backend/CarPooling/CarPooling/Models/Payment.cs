using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class Payment
{
    public Guid Id { get; set; }

    [Required]
    public Guid ReservationId { get; set; }

    [ForeignKey(nameof(ReservationId))]
    public Reservation Reservation { get; set; } = null!;

    [Required]
    public Guid PassengerUserId { get; set; }

    [ForeignKey(nameof(PassengerUserId))]
    public User PassengerUser { get; set; } = null!;

    [Required]
    public int PaymentMethodId { get; set; }

    public PaymentMethod PaymentMethod { get; set; } = null!;

    public Guid? UserPaymentMethodId { get; set; }

    public UserPaymentMethod? UserPaymentMethod { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal Amount { get; set; }

    [Column(TypeName = "decimal(10,2)")]
    public decimal RefundedAmount { get; set; }

    [Required]
    [MaxLength(3)]
    public string Currency { get; set; } = "BOB";

    public PaymentStatus Status { get; set; } = PaymentStatus.Pending;

    [MaxLength(300)]
    public string? Description { get; set; }

    [MaxLength(80)]
    public string? ExternalReference { get; set; }

    [MaxLength(300)]
    public string? FailureReason { get; set; }

    public Guid? ConfirmedByUserId { get; set; }

    [ForeignKey(nameof(ConfirmedByUserId))]
    public User? ConfirmedByUser { get; set; }

    public DateTime? ConfirmedAt { get; set; }

    [MaxLength(300)]
    public string? ConfirmationNotes { get; set; }

    [MaxLength(300)]
    public string? ConfirmationEvidenceUrl { get; set; }

    public DateTime? PaidAt { get; set; }

    public DateTime? ExpiresAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public ICollection<PaymentTransaction> Transactions { get; set; } = [];

    public PaymentReceipt? Receipt { get; set; }

    public PhysicalPayment? PhysicalPayment { get; set; }

    public ICollection<Refund> Refunds { get; set; } = [];
}
