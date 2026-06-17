using CarPooling.Dtos;
using CarPooling.Models;
using CarPooling.Services;
using Microsoft.AspNetCore.Mvc;

namespace CarPooling.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController(ReservationService reservationService) : ControllerBase
{
    [HttpPost("~/api/Trips/{tripId}/Reservations")]
    public async Task<ActionResult<ReservationDto>> CreateReservation(Guid tripId, CreateReservationDto dto)
    {
        try
        {
            var r = await reservationService.CreateAsync(tripId, dto);
            return CreatedAtRoute("GetPendingReservations", new { tripId }, await reservationService.MapToDtoAsync(r));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("~/api/Trips/{tripId}/Reservations/{reservationId}/accept")]
    public async Task<ActionResult<ReservationDto>> AcceptReservation(Guid tripId, Guid reservationId)
    {
        try
        {
            var r = await reservationService.AcceptAsync(reservationId);
            return Ok(await reservationService.MapToDtoAsync(r));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("~/api/Trips/{tripId}/Reservations/{reservationId}/reject")]
    public async Task<ActionResult<ReservationDto>> RejectReservation(Guid tripId, Guid reservationId)
    {
        try
        {
            var r = await reservationService.RejectAsync(reservationId);
            return Ok(await reservationService.MapToDtoAsync(r));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpPost("~/api/Trips/{tripId}/Reservations/{reservationId}/board")]
    public async Task<ActionResult<ReservationDto>> BoardPassenger(Guid tripId, Guid reservationId)
    {
        try
        {
            var r = await reservationService.BoardAsync(reservationId);
            return Ok(await reservationService.MapToDtoAsync(r));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> CancelReservation(Guid id)
    {
        try
        {
            var r = await reservationService.CancelAsync(id);
            return Ok(await reservationService.MapToDtoAsync(r));
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpGet("~/api/Trips/{tripId}/Reservations/pending", Name = "GetPendingReservations")]
    public async Task<ActionResult<IEnumerable<ReservationDto>>> GetPending(Guid tripId)
    {
        var list = await reservationService.GetPendingForTripAsync(tripId);
        return Ok(await reservationService.MapToDtoListAsync(list));
    }

    [HttpGet("~/api/Trips/{tripId}/Reservations/confirmed")]
    public async Task<ActionResult<IEnumerable<ReservationDto>>> GetConfirmed(Guid tripId)
    {
        var list = await reservationService.GetConfirmedForTripAsync(tripId);
        return Ok(await reservationService.MapToDtoListAsync(list));
    }

    [HttpGet("~/api/Trips/{tripId}/Reservations/boarded")]
    public async Task<ActionResult<IEnumerable<ReservationDto>>> GetBoarded(Guid tripId)
    {
        var list = await reservationService.GetBoardedForTripAsync(tripId);
        return Ok(await reservationService.MapToDtoListAsync(list));
    }

    [HttpPost("~/api/Trips/{tripId}/Reservations/{reservationId}/verify-code")]
    public async Task<ActionResult> VerifyBoardingCode(Guid tripId, Guid reservationId, [FromBody] VerifyCodeDto dto)
    {
        try
        {
            var ok = await reservationService.VerifyBoardingCodeAsync(reservationId, dto.Code);
            if (!ok) return BadRequest("Codigo invalido.");
            await reservationService.BoardAsync(reservationId);
            return Ok(new { message = "Abordaje confirmado" });
        }
        catch (InvalidOperationException ex) { return BadRequest(ex.Message); }
    }

    [HttpGet("~/api/users/{userId:guid}/active-reservation")]
    public async Task<ActionResult<ActiveReservationDto>> GetActiveReservation(Guid userId)
    {
        var r = await reservationService.GetActiveForPassengerAsync(userId);
        if (r is null) return NotFound();
        return Ok(ActiveReservationDto.FromReservation(r));
    }
}

public class VerifyCodeDto
{
    public string Code { get; set; } = "";
}

public class ActiveReservationDto
{
    public Guid ReservationId { get; set; }
    public Guid TripId { get; set; }
    public string DriverName { get; set; } = "";
    public string StatusLabel { get; set; } = "";
    public int StatusId { get; set; }
    public string BoardingCode { get; set; } = "";
    public string OriginAddress { get; set; } = "";
    public string DestinationAddress { get; set; } = "";
    public double OriginLatitude { get; set; }
    public double OriginLongitude { get; set; }
    public double DestinationLatitude { get; set; }
    public double DestinationLongitude { get; set; }
    public string VehicleBrand { get; set; } = "";
    public string VehicleColor { get; set; } = "";
    public string VehiclePlate { get; set; } = "";
    public double? CurrentLatitude { get; set; }
    public double? CurrentLongitude { get; set; }

    public static ActiveReservationDto FromReservation(Reservation r)
    {
        return new ActiveReservationDto
        {
            ReservationId = r.Id,
            TripId = r.TripId,
            DriverName = r.Trip.DriverName,
            StatusLabel = r.StatusEntity?.LabelEs ?? "",
            StatusId = r.StatusId,
            BoardingCode = r.BoardingCode ?? "",
            OriginAddress = r.Trip.OriginLocation?.AddressLabel ?? "",
            DestinationAddress = r.Trip.DestinationLocation?.AddressLabel ?? "",
            OriginLatitude = r.Trip.OriginLocation?.Latitude ?? 0,
            OriginLongitude = r.Trip.OriginLocation?.Longitude ?? 0,
            DestinationLatitude = r.Trip.DestinationLocation?.Latitude ?? 0,
            DestinationLongitude = r.Trip.DestinationLocation?.Longitude ?? 0,
            VehicleBrand = r.Trip.Vehicle?.Brand ?? "",
            VehicleColor = r.Trip.Vehicle?.Color ?? "",
            VehiclePlate = r.Trip.Vehicle?.LicensePlate ?? "",
            CurrentLatitude = r.Trip.CurrentLatitude,
            CurrentLongitude = r.Trip.CurrentLongitude
        };
    }
}
