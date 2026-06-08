using CarPooling.Dtos;
using CarPooling.Security;
using CarPooling.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;

namespace CarPooling.Controllers;

[ApiController]
public class PaymentsController(PaymentService paymentService) : ControllerBase
{
    [HttpGet("api/payment-methods")]
    public async Task<ActionResult<IEnumerable<PaymentMethodResponseDto>>> ListPaymentMethods()
    {
        return Ok(await paymentService.ListMethodsAsync());
    }

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/users/{userId:guid}/payment-methods")]
    public async Task<ActionResult<IEnumerable<UserPaymentMethodResponseDto>>> ListUserPaymentMethods(Guid userId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            return Ok(await paymentService.ListUserPaymentMethodsAsync(userId));
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/users/{userId:guid}/payment-methods")]
    public async Task<ActionResult<UserPaymentMethodResponseDto>> CreateUserPaymentMethod(
        Guid userId,
        [FromBody] CreateUserPaymentMethodDto dto)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var created = await paymentService.CreateUserPaymentMethodAsync(userId, dto);
            return StatusCode(StatusCodes.Status201Created, created);
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/reservations/{reservationId:guid}/driver-payment-methods")]
    public async Task<ActionResult<IEnumerable<UserPaymentMethodResponseDto>>> ListDriverPaymentMethodsForReservation(
        Guid reservationId)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.ListDriverPaymentMethodsForReservationAsync(userId.Value, reservationId));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpDelete("api/users/{userId:guid}/payment-methods/{methodId:guid}")]
    public async Task<ActionResult<UserPaymentMethodResponseDto>> DisableUserPaymentMethod(Guid userId, Guid methodId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            return Ok(await paymentService.DisableUserPaymentMethodAsync(userId, methodId));
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/users/{userId:guid}/payments")]
    public async Task<ActionResult<PaymentResponseDto>> CreatePayment(Guid userId, [FromBody] CreatePaymentDto dto)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            var created = await paymentService.CreatePaymentAsync(userId, dto);
            return StatusCode(StatusCodes.Status201Created, created);
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/users/{userId:guid}/payments")]
    public async Task<ActionResult<IEnumerable<PaymentResponseDto>>> ListUserPayments(Guid userId)
    {
        if (!TryAuthorizeUser(userId, out var forbidden))
        {
            return forbidden!;
        }

        try
        {
            return Ok(await paymentService.ListPaymentsForUserAsync(userId));
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/reservations/{reservationId:guid}/payments")]
    public async Task<ActionResult<IEnumerable<PaymentResponseDto>>> ListReservationPayments(Guid reservationId)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.ListPaymentsForReservationAsync(userId.Value, reservationId));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/payments/{paymentId:guid}")]
    public async Task<ActionResult<PaymentResponseDto>> GetPayment(Guid paymentId)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.GetPaymentForUserAsync(userId.Value, paymentId));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/payments/{paymentId:guid}/simulate")]
    public async Task<ActionResult<PaymentResponseDto>> SimulatePayment(
        Guid paymentId,
        [FromBody] SimulatePaymentDto dto)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.SimulatePaymentAsync(userId.Value, paymentId, dto));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/payments/{paymentId:guid}/confirm")]
    public async Task<ActionResult<PaymentResponseDto>> ConfirmManualPayment(
        Guid paymentId,
        [FromBody] ConfirmPaymentDto dto)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.ConfirmManualPaymentAsync(userId.Value, paymentId, dto));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/payments/{paymentId:guid}/cancel")]
    public async Task<ActionResult<PaymentResponseDto>> CancelPayment(Guid paymentId)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.CancelPaymentAsync(userId.Value, paymentId));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/payments/{paymentId:guid}/refunds")]
    public async Task<ActionResult<PaymentResponseDto>> RequestRefund(
        Guid paymentId,
        [FromBody] CreateRefundDto dto)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.RequestRefundAsync(userId.Value, paymentId, dto));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/refunds/{refundId:guid}/approve")]
    public async Task<ActionResult<RefundResponseDto>> ApproveRefund(Guid refundId)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.ApproveRefundAsync(userId.Value, refundId));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpPost("api/refunds/{refundId:guid}/reject")]
    public async Task<ActionResult<RefundResponseDto>> RejectRefund(
        Guid refundId,
        [FromBody] ProcessRefundDto dto)
    {
        var userId = GetCurrentUserId();
        if (userId is null)
        {
            return Unauthorized(new { message = "Usuario no autenticado." });
        }

        try
        {
            return Ok(await paymentService.RejectRefundAsync(userId.Value, refundId, dto));
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

    [Authorize(AuthenticationSchemes = HeaderUserAuthenticationHandler.SchemeName)]
    [HttpGet("api/payments")]
    public async Task<ActionResult<IEnumerable<PaymentResponseDto>>> ListAllPayments()
    {
        try
        {
            return Ok(await paymentService.ListAllPaymentsAsync());
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    private bool TryAuthorizeUser(Guid routeUserId, out ActionResult? forbidden)
    {
        forbidden = null;
        var currentUserId = GetCurrentUserId();
        if (currentUserId is null)
        {
            forbidden = Unauthorized(new { message = "Usuario no autenticado." });
            return false;
        }

        if (currentUserId.Value != routeUserId)
        {
            forbidden = Forbid();
            return false;
        }

        return true;
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
