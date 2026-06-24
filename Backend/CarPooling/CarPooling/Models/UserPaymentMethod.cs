using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class UserPaymentMethod
{
    public Guid Id { get; set; }

    [Required]
    public Guid UserId { get; set; }

    [ForeignKey(nameof(UserId))]
    public User User { get; set; } = null!;

    [Required]
    public int PaymentMethodId { get; set; }

    [Required]
    [MaxLength(30)]
    public string PaymentMethodCode { get; set; } = string.Empty;

    [Required]
    [MaxLength(80)]
    public string PaymentMethodName { get; set; } = string.Empty;

    [MaxLength(300)]
    public string? PaymentMethodDescription { get; set; }

    public PaymentMethodType Type { get; set; } = PaymentMethodType.Simulated;

    public bool RequiresManualConfirmation { get; set; }

    [MaxLength(80)]
    public string? Alias { get; set; }

    [MaxLength(80)]
    public string? MaskedValue { get; set; }

    [MaxLength(120)]
    public string? ProviderToken { get; set; }

    [Column(TypeName = "nvarchar(max)")]
    public string? QrImageUrl { get; set; }

    [MaxLength(120)]
    public string? BankName { get; set; }

    [MaxLength(120)]
    public string? AccountHolderName { get; set; }

    public bool IsDefault { get; set; }

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public ICollection<Payment> Payments { get; set; } = [];
}
