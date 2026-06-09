using CarPooling.Dtos;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/safe-zones")]
[AllowAnonymous]
public class SafeZonesController(SafeZoneService safeZoneService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<SafeZoneResponseDto>>> GetActiveAsync()
    {
        var zones = await safeZoneService.GetActiveAsync();
        return Ok(zones);
    }
}
