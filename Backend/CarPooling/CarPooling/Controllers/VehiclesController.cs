using CarPooling.Dtos;
using CarPooling.Services;
using Microsoft.AspNetCore.Mvc;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/users/{userId:guid}/vehicles")]
public class VehiclesController(VehicleService vehicleService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<List<VehicleDto>>> GetAll(Guid userId)
    {
        var vehicles = await vehicleService.GetAllForDriverAsync(userId);
        return Ok(vehicles.Select(VehicleDto.FromEntity).ToList());
    }

    [HttpPost]
    public async Task<ActionResult<VehicleDto>> Create(Guid userId, [FromBody] CreateVehicleDto dto)
    {
        var vehicle = await vehicleService.CreateAsync(userId, dto.LicensePlate, dto.Brand, dto.Model,
            dto.Color, dto.VehicleYear, dto.TotalSeats);
        return CreatedAtAction(nameof(GetAll), new { userId }, VehicleDto.FromEntity(vehicle));
    }

    [HttpPut("{vehicleId:guid}")]
    public async Task<ActionResult<VehicleDto>> Update(Guid userId, Guid vehicleId, [FromBody] CreateVehicleDto dto)
    {
        var vehicle = await vehicleService.UpdateAsync(vehicleId, userId, dto.LicensePlate, dto.Brand, dto.Model,
            dto.Color, dto.VehicleYear, dto.TotalSeats);
        return Ok(VehicleDto.FromEntity(vehicle));
    }

    [HttpDelete("{vehicleId:guid}")]
    public async Task<IActionResult> Delete(Guid userId, Guid vehicleId)
    {
        await vehicleService.DeleteAsync(vehicleId, userId);
        return NoContent();
    }
}

public class CreateVehicleDto
{
    public string LicensePlate { get; set; } = "";
    public string Brand { get; set; } = "";
    public string Model { get; set; } = "";
    public string Color { get; set; } = "";
    public int? VehicleYear { get; set; }
    public int TotalSeats { get; set; } = 4;
}
