using CarPooling.Dtos;
using CarPooling.Services;
using CarPooling.Security;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace CarPooling.Controllers;

[ApiController]
[Route("api")]
[Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
public class RatingsController(RatingService ratingService) : ControllerBase
{
    private readonly RatingService _ratingService = ratingService;

    /// <summary>
    /// Envía una calificación para un usuario participante de un viaje finalizado.
    /// </summary>
    [HttpPost("Trips/{tripId:guid}/ratings")]
    public async Task<ActionResult<TripRatingResponseDto>> CreateRating(Guid tripId, [FromBody] CreateTripRatingDto dto)
    {
        var evaluatorUserId = GetCurrentUserId();
        if (evaluatorUserId == null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            var response = await _ratingService.CreateRatingAsync(tripId, evaluatorUserId.Value, dto);
            return Ok(response);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    /// <summary>
    /// Obtiene la lista de todas las calificaciones recibidas por el usuario.
    /// </summary>
    [HttpGet("users/{userId:guid}/ratings")]
    public async Task<ActionResult<IEnumerable<TripRatingResponseDto>>> GetUserRatings(Guid userId)
    {
        var ratings = await _ratingService.GetRatingsForUserAsync(userId);
        return Ok(ratings);
    }

    /// <summary>
    /// Obtiene el resumen de calificaciones (promedio de estrellas y cantidad) del usuario.
    /// </summary>
    [HttpGet("users/{userId:guid}/ratings/summary")]
    public async Task<ActionResult<UserRatingSummaryDto>> GetUserRatingSummary(Guid userId)
    {
        try
        {
            var summary = await _ratingService.GetAverageRatingForUserAsync(userId);
            return Ok(summary);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    private Guid? GetCurrentUserId()
    {
        var nameIdentifier = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (Guid.TryParse(nameIdentifier, out var userId))
        {
            return userId;
        }
        return null;
    }
}
