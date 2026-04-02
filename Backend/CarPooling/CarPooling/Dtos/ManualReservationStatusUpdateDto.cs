using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class ManualReservationStatusUpdateDto
{
    [Required]
    [MaxLength(32)]
    public string Status { get; set; } = string.Empty;
}
