using CarPooling.Data;
using CarPooling.Models;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
[RequirePermission(AppPermissions.ManageRoles)]
public class RolesController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet("permissions")]
    public async Task<ActionResult<object>> GetPermissionsAsync()
    {
        var permissions = await _context.Permissions
            .OrderBy(p => p.GroupName)
            .ThenBy(p => p.Name)
            .Select(p => new
            {
                p.Id,
                p.Name,
                p.GroupName
            })
            .ToListAsync();

        return Ok(permissions);
    }

    [HttpGet]
    public async Task<ActionResult<object>> GetRolesAsync()
    {
        var roles = await _context.Roles
            .Include(r => r.RolePermissions)
                .ThenInclude(rp => rp.Permission)
            .Select(r => new
            {
                r.Id,
                r.Name,
                r.Description,
                r.IsSystemRole,
                Permissions = r.RolePermissions.Select(rp => rp.PermissionId).ToList()
            })
            .ToListAsync();

        return Ok(roles);
    }

    [HttpPost]
    public async Task<ActionResult<object>> CreateRoleAsync([FromBody] CreateUpdateRoleDto dto)
    {
        if (string.IsNullOrWhiteSpace(dto.Name))
            return BadRequest("El nombre del rol es obligatorio.");

        var exists = await _context.Roles.AnyAsync(r => r.Name.ToLower() == dto.Name.Trim().ToLower());
        if (exists)
            return Conflict("Ya existe un rol con ese nombre.");

        var role = new Role
        {
            Id = Guid.NewGuid(),
            Name = dto.Name.Trim(),
            Description = dto.Description?.Trim() ?? string.Empty,
            IsSystemRole = false
        };

        _context.Roles.Add(role);

        if (dto.Permissions != null && dto.Permissions.Count > 0)
        {
            foreach (var permissionId in dto.Permissions)
            {
                var permExists = await _context.Permissions.AnyAsync(p => p.Id == permissionId);
                if (permExists)
                {
                    _context.RolePermissions.Add(new RolePermission
                    {
                        RoleId = role.Id,
                        PermissionId = permissionId
                    });
                }
            }
        }

        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetRolesAsync), new { id = role.Id }, new
        {
            role.Id,
            role.Name,
            role.Description,
            role.IsSystemRole,
            Permissions = dto.Permissions ?? []
        });
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<object>> UpdateRoleAsync(Guid id, [FromBody] CreateUpdateRoleDto dto)
    {
        var role = await _context.Roles
            .Include(r => r.RolePermissions)
            .FirstOrDefaultAsync(r => r.Id == id);

        if (role is null)
            return NotFound("Rol no encontrado.");

        if (role.IsSystemRole)
            return BadRequest("No se pueden modificar roles del sistema.");

        if (string.IsNullOrWhiteSpace(dto.Name))
            return BadRequest("El nombre del rol es obligatorio.");

        var exists = await _context.Roles.AnyAsync(r => r.Name.ToLower() == dto.Name.Trim().ToLower() && r.Id != id);
        if (exists)
            return Conflict("Ya existe otro rol con ese nombre.");

        role.Name = dto.Name.Trim();
        role.Description = dto.Description?.Trim() ?? string.Empty;

        foreach (var rp in role.RolePermissions.ToList())
        {
            _context.RolePermissions.Remove(rp);
        }

        if (dto.Permissions != null && dto.Permissions.Count > 0)
        {
            foreach (var permissionId in dto.Permissions)
            {
                var permExists = await _context.Permissions.AnyAsync(p => p.Id == permissionId);
                if (permExists)
                {
                    _context.RolePermissions.Add(new RolePermission
                    {
                        RoleId = role.Id,
                        PermissionId = permissionId
                    });
                }
            }
        }

        await _context.SaveChangesAsync();

        return Ok(new
        {
            role.Id,
            role.Name,
            role.Description,
            role.IsSystemRole,
            Permissions = dto.Permissions ?? []
        });
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteRoleAsync(Guid id)
    {
        var role = await _context.Roles.FirstOrDefaultAsync(r => r.Id == id);
        if (role is null)
            return NotFound("Rol no encontrado.");

        if (role.IsSystemRole)
            return BadRequest("No se pueden eliminar roles del sistema.");

        var usersWithRole = await _context.UserRoles.AnyAsync(ur => ur.RoleId == id);
        if (usersWithRole)
            return BadRequest("No se puede eliminar el rol porque tiene usuarios asociados.");

        _context.Roles.Remove(role);
        await _context.SaveChangesAsync();

        return NoContent();
    }

    public class CreateUpdateRoleDto
    {
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public List<string> Permissions { get; set; } = [];
    }
}
