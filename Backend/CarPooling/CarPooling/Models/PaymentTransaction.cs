using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class PaymentTransaction
{
    public Guid Id { get; set; }

    [Required]
    public Guid PaymentId { get; set; }

    [ForeignKey(nameof(PaymentId))]
    public Payment Payment { get; set; } = null!;

    public PaymentTransactionType TransactionType { get; set; } = PaymentTransactionType.Payment;

    public PaymentTransactionStatus Status { get; set; } = PaymentTransactionStatus.Pending;

    [Column(TypeName = "decimal(10,2)")]
    public decimal Amount { get; set; }

    [MaxLength(80)]
    public string? Provider { get; set; }

    [MaxLength(120)]
    public string? ProviderTransactionId { get; set; }

    [MaxLength(40)]
    public string? AuthorizationCode { get; set; }

    [MaxLength(40)]
    public string? ResponseCode { get; set; }

    [MaxLength(300)]
    public string? ResponseMessage { get; set; }

    public DateTime? ProcessedAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
