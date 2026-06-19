using CarPooling.Dtos;
using CarPooling.Services;
using Microsoft.AspNetCore.Mvc;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TripsController(TripService tripService) : ControllerBase
{
    [HttpGet("match-candidates")]
    public async Task<ActionResult<IReadOnlyList<DriverTripMatchResponse>>> GetMatchCandidatesAsync(
        [FromQuery] double? referenceLatitude,
        [FromQuery] double? referenceLongitude)
    {
        try
        {
            var results = await tripService.GetMatchCandidatesAsync(referenceLatitude, referenceLongitude);
            return Ok(results);
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("origin")]
    public async Task<ActionResult<TripResponse>> CreateOriginAsync([FromBody] CoordinateRequest request)
    {
        try
        {
            var trip = await tripService.CreateTripAsync(request);
            return CreatedAtRoute("GetTripById", new { id = trip.Id }, TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpGet("for-driver/{driverUserId:guid}")]
    public async Task<ActionResult<TripResponse>> GetActiveTripForDriverAsync(
        Guid driverUserId, [FromQuery] string? displayName = null)
    {
        var trip = await tripService.GetActiveTripForDriverAsync(driverUserId, displayName);
        if (trip is null) return NotFound();
        return Ok(TripService.MapToDto(trip));
    }

    [HttpPost("{id:guid}/destination")]
    public async Task<ActionResult<TripResponse>> SetDestinationAsync(Guid id, [FromBody] CoordinateRequest request)
    {
        try
        {
            var trip = await tripService.SetDestinationAsync(id, request);
            return Ok(TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("{id:guid}/cancel")]
    public async Task<ActionResult<TripResponse>> CancelTripAsync(Guid id)
    {
        try
        {
            var trip = await tripService.CancelTripAsync(id);
            return Ok(TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("{id:guid}/start")]
    public async Task<ActionResult<TripResponse>> StartTripAsync(Guid id, [FromBody] StartTripRequestDto? request)
    {
        try
        {
            var trip = await tripService.StartTripAsync(id, request);
            return Ok(TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("{id:guid}/finish")]
    public async Task<ActionResult<TripResponse>> FinishTripAsync(Guid id)
    {
        try
        {
            var trip = await tripService.FinishTripAsync(id);
            return Ok(TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("{id:guid}/location")]
    public async Task<ActionResult<TripResponse>> UpdateTripLocationAsync(Guid id, [FromQuery] double latitude, [FromQuery] double longitude)
    {
        try
        {
            var trip = await tripService.UpdateTripLocationAsync(id, latitude, longitude);
            return Ok(TripService.MapToDto(trip));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpGet("{id:guid}", Name = "GetTripById")]
    public async Task<ActionResult<TripResponse>> GetTripByIdAsync(Guid id)
    {
        var trip = await tripService.GetByIdAsync(id);
        if (trip is null) return NotFound();
        return Ok(TripService.MapToDto(trip));
    }
}
