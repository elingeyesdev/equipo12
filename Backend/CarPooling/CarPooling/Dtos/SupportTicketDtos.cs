using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class CreateSupportTicketDto
{
    [Required]
    public SupportTicketCategory Category { get; set; }

    [Required]
    [MaxLength(120)]
    public string Subject { get; set; } = string.Empty;

    [Required]
    [MaxLength(2000)]
    public string Description { get; set; } = string.Empty;

    public Guid? TripId { get; set; }
}

public class SupportTicketResponseDto
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string UserFullName { get; set; } = string.Empty;
    public Guid? TripId { get; set; }
    public SupportTicketCategory Category { get; set; }
    public string CategoryLabel { get; set; } = string.Empty;
    public string Subject { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public SupportTicketStatus Status { get; set; }
    public string StatusLabel { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }

    public static SupportTicketResponseDto FromEntity(SupportTicket ticket)
    {
        return new SupportTicketResponseDto
        {
            Id = ticket.Id,
            UserId = ticket.UserId,
            UserFullName = ticket.User?.FullName ?? string.Empty,
            TripId = ticket.TripId,
            Category = ticket.Category,
            CategoryLabel = ToCategoryLabel(ticket.Category),
            Subject = ticket.Subject,
            Description = ticket.Description,
            Status = ticket.Status,
            StatusLabel = ToStatusLabel(ticket.Status),
            CreatedAt = ticket.CreatedAt,
            UpdatedAt = ticket.UpdatedAt
        };
    }

    private static string ToCategoryLabel(SupportTicketCategory category) => category switch
    {
        SupportTicketCategory.Trip => "Viaje",
        SupportTicketCategory.Reservation => "Reserva",
        SupportTicketCategory.Account => "Cuenta",
        SupportTicketCategory.Payment => "Pago",
        SupportTicketCategory.Other => "Otro",
        _ => "Otro"
    };

    private static string ToStatusLabel(SupportTicketStatus status) => status switch
    {
        SupportTicketStatus.Open => "Abierto",
        SupportTicketStatus.InReview => "En revisión",
        SupportTicketStatus.Resolved => "Resuelto",
        SupportTicketStatus.Closed => "Cerrado",
        _ => "Desconocido"
    };
}

public class SupportTicketListResponseDto
{
    public IReadOnlyList<SupportTicketResponseDto> Items { get; set; } = [];
    public int TotalCount { get; set; }
}
