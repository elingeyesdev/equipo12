using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class ChatMessageResponseDto
{
    public Guid Id { get; set; }
    
    public Guid SenderUserId { get; set; }
    
    public string SenderFullName { get; set; } = string.Empty;
    
    public string? SenderProfilePicture { get; set; }
    
    public string MessageText { get; set; } = string.Empty;
    
    public DateTime CreatedAt { get; set; }
    
    public List<Guid> ReadByUserIds { get; set; } = [];
    public List<ChatMessageReaderDto> Readers { get; set; } = [];
}

public class ChatMessageReaderDto
{
    public Guid UserId { get; set; }
    public string FullName { get; set; } = string.Empty;
}

public class SendMessageRequestDto
{
    [Required]
    [MaxLength(2000)]
    public string MessageText { get; set; } = string.Empty;
}
