using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class PaymentMethod
{
    public int Id { get; set; }

    [Required]
    [MaxLength(30)]
    public string Code { get; set; } = string.Empty;

    [Required]
    [MaxLength(80)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(300)]
    public string? Description { get; set; }

    public PaymentMethodType Type { get; set; } = PaymentMethodType.Simulated;

    public bool RequiresManualConfirmation { get; set; }

    public bool SupportsRefunds { get; set; } = true;

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }

    public ICollection<UserPaymentMethod> UserPaymentMethods { get; set; } = [];

    public ICollection<Payment> Payments { get; set; } = [];
}
