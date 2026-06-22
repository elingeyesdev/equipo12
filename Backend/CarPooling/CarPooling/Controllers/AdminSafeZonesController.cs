using CarPooling.Dtos;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/admin/safe-zones")]
[Authorize(Roles = "Admin")]
public class AdminSafeZonesController(SafeZoneService safeZoneService) : ControllerBase
{
    private const string GetSafeZoneByIdRouteName = "GetAdminSafeZoneById";

    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<SafeZoneResponseDto>>> GetAllAsync()
    {
        var zones = await safeZoneService.GetAllForAdminAsync();
        return Ok(zones);
    }

    [HttpGet("{id:guid}", Name = GetSafeZoneByIdRouteName)]
    public async Task<ActionResult<SafeZoneResponseDto>> GetByIdAsync(Guid id)
    {
        var zone = await safeZoneService.GetByIdAsync(id);
        if (zone is null)
        {
            return NotFound("Zona segura no encontrada.");
        }

        return Ok(zone);
    }

    [HttpPost]
    public async Task<ActionResult<SafeZoneResponseDto>> CreateAsync([FromBody] CreateSafeZoneDto dto)
    {
        try
        {
            var created = await safeZoneService.CreateAsync(dto);
            return CreatedAtRoute(GetSafeZoneByIdRouteName, new { id = created.Id }, created);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<SafeZoneResponseDto>> UpdateAsync(Guid id, [FromBody] UpdateSafeZoneDto dto)
    {
        try
        {
            var updated = await safeZoneService.UpdateAsync(id, dto);
            return Ok(updated);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteAsync(Guid id)
    {
        try
        {
            await safeZoneService.DeleteAsync(id);
            return NoContent();
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }
}
