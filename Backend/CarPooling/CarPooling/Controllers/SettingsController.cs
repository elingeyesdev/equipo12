using CarPooling.Data;
using CarPooling.Models;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Text.Json;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class SettingsController(CarPoolingContext context) : ControllerBase
{
    private readonly CarPoolingContext _context = context;

    [HttpGet("theme")]
    [AllowAnonymous]
    public async Task<ActionResult<object>> GetThemeAsync()
    {
        var setting = await _context.AppSettings
            .AsNoTracking()
            .FirstOrDefaultAsync(s => s.Key == "theme");

        if (setting is null)
        {
            // Fallback to default Sunset Orange theme with custom text colors
            return Ok(new
            {
                primaryLight = "#e08c75",
                secondaryLight = "#6b8f8d",
                textLight = "#1f1d1a",
                primaryDark = "#e27b53",
                secondaryDark = "#85aba9",
                textDark = "#e0e0e0"
            });
        }

        try
        {
            var parsed = JsonSerializer.Deserialize<Dictionary<string, string>>(setting.Value);
            return Ok(parsed);
        }
        catch
        {
            return Ok(new { raw = setting.Value });
        }
    }

    [HttpPut("theme")]
    [RequirePermission(AppPermissions.ManageRoles)]
    public async Task<IActionResult> UpdateThemeAsync([FromBody] Dictionary<string, string> themeColors)
    {
        if (themeColors is null || (!themeColors.ContainsKey("primary") && !themeColors.ContainsKey("primaryLight")))
        {
            return BadRequest("Colores de tema inválidos. Debe proporcionar al menos el color 'primary' o 'primaryLight'.");
        }

        var setting = await _context.AppSettings
            .FirstOrDefaultAsync(s => s.Key == "theme");

        var valueJson = JsonSerializer.Serialize(themeColors);

        if (setting is null)
        {
            setting = new AppSetting { Key = "theme", Value = valueJson };
            _context.AppSettings.Add(setting);
        }
        else
        {
            setting.Value = valueJson;
        }

        await _context.SaveChangesAsync();

        return Ok(themeColors);
    }
}
