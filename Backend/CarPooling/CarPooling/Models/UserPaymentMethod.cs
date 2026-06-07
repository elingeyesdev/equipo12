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

    public PaymentMethod PaymentMethod { get; set; } = null!;

    [MaxLength(80)]
    public string? Alias { get; set; }

    [MaxLength(80)]
    public string? MaskedValue { get; set; }

    [MaxLength(120)]
    public string? ProviderToken { get; set; }

    [MaxLength(300)]
    public string? QrImageUrl { get; set; }

    [MaxLength(120)]
    public string? BankName { get; set; }

    [MaxLength(120)]
    public string? AccountHolderName { get; set; }

    public bool IsDefault { get; set; }

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}
