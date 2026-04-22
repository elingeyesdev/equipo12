using CarPooling.Models;

namespace CarPooling.Dtos;

public class UserResponseDto
{
    public Guid Id { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? PhoneNumber { get; set; }
    public string Role { get; set; } = string.Empty;
    public int RoleId { get; set; }
    public DriverProfileDto? DriverProfile { get; set; }
    public DateTime CreatedAt { get; set; }

    public static UserResponseDto FromEntity(User user)
    {
        return new UserResponseDto
        {
            Id = user.Id,
            FullName = user.FullName,
            Email = user.Email,
            PhoneNumber = user.PhoneNumber,
            Role = user.Role == UserRole.Driver
                ? "driver"
                : user.Role == UserRole.Admin
                    ? "admin"
                    : "student",
            RoleId = (int)user.Role,
            DriverProfile = user.DriverProfile is null ? null : DriverProfileDto.FromEntity(user.DriverProfile),
            CreatedAt = user.CreatedAt
        };
    }
}
