using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CarPooling.Models;

public class AuditLog
{
    public Guid Id { get; set; }

    public Guid? ActorUserId { get; set; }

    [ForeignKey(nameof(ActorUserId))]
    public User? ActorUser { get; set; }

    [MaxLength(120)]
    public string? ActorEmailSnapshot { get; set; }

    [Required]
    [MaxLength(80)]
    public string ActionType { get; set; } = string.Empty;

    [Required]
    [MaxLength(60)]
    public string Module { get; set; } = string.Empty;

    [Required]
    [MaxLength(80)]
    public string EntityName { get; set; } = string.Empty;

    [MaxLength(80)]
    public string? EntityId { get; set; }

    public string? OldValuesJson { get; set; }

    public string? NewValuesJson { get; set; }

    [MaxLength(1000)]
    public string? ChangedFieldsJson { get; set; }

    [Required]
    [MaxLength(30)]
    public string Result { get; set; } = "Success";

    [MaxLength(500)]
    public string? Description { get; set; }

    [MaxLength(45)]
    public string? IpAddress { get; set; }

    [MaxLength(500)]
    public string? UserAgent { get; set; }

    [MaxLength(200)]
    public string? RequestPath { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
