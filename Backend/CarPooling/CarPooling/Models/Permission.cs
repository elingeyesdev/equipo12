using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class Permission
{
    [Key]
    [MaxLength(100)]
    public string Id { get; set; } = string.Empty; // e.g. "users:read"

    [Required]
    [MaxLength(120)]
    public string Name { get; set; } = string.Empty;

    [Required]
    [MaxLength(120)]
    public string GroupName { get; set; } = string.Empty;

    public ICollection<RolePermission> RolePermissions { get; set; } = [];
}
