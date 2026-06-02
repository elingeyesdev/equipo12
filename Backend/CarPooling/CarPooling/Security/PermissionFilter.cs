using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using System.Security.Claims;
using CarPooling.Data;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Security;

public class PermissionFilter(CarPoolingContext context) : IAsyncAuthorizationFilter
{
    private readonly CarPoolingContext _context = context;

    public async Task OnAuthorizationAsync(AuthorizationFilterContext filterContext)
    {
        // 1. Obtener el ID de usuario desde los Claims
        var userIdClaim = filterContext.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier);
        if (userIdClaim == null || !Guid.TryParse(userIdClaim.Value, out var userId))
        {
            filterContext.Result = new UnauthorizedResult();
            return;
        }

        // 2. Obtener el permiso requerido
        var attribute = filterContext.ActionDescriptor.EndpointMetadata
            .OfType<RequirePermissionAttribute>()
            .FirstOrDefault();

        var requiredPermission = attribute?.Arguments?[0] as string;

        if (string.IsNullOrEmpty(requiredPermission))
        {
            return;
        }

        // 3. Verificar en la base de datos si tiene el permiso
        var hasPermission = await _context.Users
            .Where(u => u.Id == userId)
            .SelectMany(u => u.UserRoles)
            .Select(ur => ur.Role)
            .SelectMany(r => r.RolePermissions)
            .AnyAsync(rp => rp.PermissionId == requiredPermission);

        if (!hasPermission)
        {
            filterContext.Result = new ForbidResult();
        }
    }
}
