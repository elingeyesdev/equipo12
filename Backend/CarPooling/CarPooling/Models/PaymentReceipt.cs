using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class PaymentReceipt
{
    public Guid Id { get; set; }

    [Required]
    public Guid PaymentId { get; set; }

    [ForeignKey(nameof(PaymentId))]
    public Payment Payment { get; set; } = null!;

    [Required]
    [MaxLength(40)]
    public string ReceiptNumber { get; set; } = string.Empty;

    [MaxLength(300)]
    public string? ReceiptUrl { get; set; }

    [MaxLength(300)]
    public string? QrCodeValue { get; set; }

    public DateTime IssuedAt { get; set; } = DateTime.UtcNow;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
