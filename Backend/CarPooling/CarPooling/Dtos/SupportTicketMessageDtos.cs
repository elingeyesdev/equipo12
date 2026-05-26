using System.ComponentModel.DataAnnotations;
using CarPooling.Models;

namespace CarPooling.Dtos;

public class SendSupportTicketMessageDto
{
    [Required]
    [MaxLength(2000)]
    public string MessageText { get; set; } = string.Empty;
}

public class SupportTicketMessageResponseDto
{
    public Guid Id { get; set; }
    public Guid TicketId { get; set; }
    public Guid SenderUserId { get; set; }
    public string SenderFullName { get; set; } = string.Empty;
    public SupportMessageSenderKind SenderKind { get; set; }
    public string SenderKindLabel { get; set; } = string.Empty;
    public string MessageText { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public List<Guid> ReadByUserIds { get; set; } = [];
}
