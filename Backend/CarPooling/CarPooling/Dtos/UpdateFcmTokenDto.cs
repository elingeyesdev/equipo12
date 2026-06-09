using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class UpdateFcmTokenDto
{
    [Required]
    public string FcmToken { get; set; } = string.Empty;

    public string? DeviceName { get; set; }
}
