using System.ComponentModel.DataAnnotations;

namespace CarPooling.Models;

public class RolePermission
{
    public Guid RoleId { get; set; }
    public Role Role { get; set; } = null!;

    [Required]
    [MaxLength(100)]
    public string PermissionId { get; set; } = string.Empty;
    public Permission Permission { get; set; } = null!;
}
