using CarPooling.Models;

namespace CarPooling.Dtos;

public class UserResponseDto
{
    public Guid Id { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? PhoneNumber { get; set; }
    public string? ProfilePicture { get; set; }
    public string Role { get; set; } = string.Empty;
    public int RoleId { get; set; }
    public List<string> RawRoles { get; set; } = [];
    public List<string> Permissions { get; set; } = [];
    public DriverProfileDto? DriverProfile { get; set; }
    public List<VehicleDto> Vehicles { get; set; } = [];
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }

    public static UserResponseDto FromEntity(User user)
    {
        Vehicle? activeVehicle = user.Vehicles?.FirstOrDefault(v => v.IsActive);

        string mappedRole = "student";
        int mappedRoleId = 1;

        bool isDriver = user.UserRoles?.Any(ur => ur.Role != null && ur.Role.Name == "Driver") ?? false;

        if (isDriver)
        {
            mappedRole = "driver";
            mappedRoleId = 2;
        }
        else if (user.UserRoles != null && user.UserRoles.Any(ur => ur.Role != null && (
            ur.Role.Name == "SuperAdmin" || 
            ur.Role.Name == "Admin" || 
            ur.Role.Name == "Analyst" || 
            ur.Role.Name.Contains("Admin") || 
            ur.Role.Name.Contains("Analyst") || 
            (ur.Role.RolePermissions != null && ur.Role.RolePermissions.Any())
        )))
        {
            mappedRole = "admin";
            mappedRoleId = 3;
        }

        var rawRoles = user.UserRoles?.Select(ur => ur.Role.Name).ToList() ?? [];
        var permissions = user.UserRoles?
            .Select(ur => ur.Role)
            .Where(r => r != null)
            .SelectMany(r => r.RolePermissions)
            .Select(rp => rp.PermissionId)
            .Distinct()
            .ToList() ?? [];

        return new UserResponseDto
        {
            Id = user.Id,
            FullName = user.FullName,
            Email = user.Email,
            PhoneNumber = user.PhoneNumber,
            ProfilePicture = user.ProfilePicture,
            Role = mappedRole,
            RoleId = mappedRoleId,
            RawRoles = rawRoles,
            Permissions = permissions,
            DriverProfile = activeVehicle is null
                ? null
                : DriverProfileDto.FromEntity(activeVehicle),
            Vehicles = user.Vehicles?.Select(VehicleDto.FromEntity).ToList() ?? [],
            IsActive = user.IsActive,
            CreatedAt = user.CreatedAt
        };
    }
}

public class VehicleDto
{
    public Guid Id { get; set; }
    public string LicensePlate { get; set; } = string.Empty;
    public string Brand { get; set; } = string.Empty;
    public string Model { get; set; } = string.Empty;
    public string Color { get; set; } = string.Empty;
    public int? VehicleYear { get; set; }
    public int TotalSeats { get; set; }
    public bool IsActive { get; set; }
    public bool IsVerified { get; set; }

    public static VehicleDto FromEntity(Vehicle vehicle)
    {
        return new VehicleDto
        {
            Id = vehicle.Id,
            LicensePlate = vehicle.LicensePlate,
            Brand = vehicle.Brand,
            Model = vehicle.Model,
            Color = vehicle.Color,
            VehicleYear = vehicle.VehicleYear,
            TotalSeats = vehicle.TotalSeats,
            IsActive = vehicle.IsActive,
            IsVerified = vehicle.IsVerified
        };
    }
}
