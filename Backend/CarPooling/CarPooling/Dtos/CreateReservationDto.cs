using System.ComponentModel.DataAnnotations;

namespace CarPooling.Dtos;

public class CreateReservationDto
{
    [Required]
    public Guid PassengerUserId { get; set; }

    [Range(1, 50)]
    public int SeatsReserved { get; set; } = 1;
}
