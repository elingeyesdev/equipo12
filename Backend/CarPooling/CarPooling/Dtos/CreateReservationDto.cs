using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CreateReservationDto
{
    [Required]
    [MaxLength(100)]
    public string PassengerName { get; set; } = string.Empty;
}
