using CarPooling.Models;

namespace CarPooling.Dtos;

public class TripHistorySummaryDto
{
    public Guid TripId { get; set; }
    public string Category { get; set; } = string.Empty;
    public string OriginLabel { get; set; } = string.Empty;
    public string DestinationLabel { get; set; } = string.Empty;
    public string StatusLabel { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}

public class TripHistoryDetailDto
{
    public Guid TripId { get; set; }
    public string Category { get; set; } = string.Empty;
    public string StatusLabel { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public string OriginLabel { get; set; } = string.Empty;
    public string DestinationLabel { get; set; } = string.Empty;
    public double OriginLatitude { get; set; }
    public double OriginLongitude { get; set; }
    public double? DestinationLatitude { get; set; }
    public double? DestinationLongitude { get; set; }
    public string DriverName { get; set; } = string.Empty;
    public string? DriverVehicleBrand { get; set; }
    public string? DriverVehicleColor { get; set; }
    public string? DriverLicensePlate { get; set; }
    public int ReservationCount { get; set; }
    public int BoardedCount { get; set; }
    public int CancelledCount { get; set; }
    public string? PassengerReservationStatus { get; set; }
    public string? PassengerName { get; set; }
}

public class TripHistoryListResponseDto
{
    public TripHistoryStatsDto Summary { get; set; } = new();
    public List<TripHistorySummaryDto> DriverHistory { get; set; } = [];
    public List<TripHistorySummaryDto> StudentHistory { get; set; } = [];
}

public class TripHistoryStatsDto
{
    public int PassengerTripsCount { get; set; }
    public int DriverTripsCount { get; set; }
    public int TotalTripsCount { get; set; }
}

internal static class TripHistoryLabelMapper
{
    public static string ToTripStatusLabel(TripStatus status)
    {
        return status == TripStatus.Ready
            ? "listo"
            : status == TripStatus.Cancelled
                ? "cancelado"
                : status == TripStatus.InProgress
                    ? "en_curso"
                    : status == TripStatus.Finished
                        ? "finalizado"
                        : "activo";
    }

    public static string ToReservationStatusLabel(ReservationStatus status)
    {
        return status == ReservationStatus.Boarded
            ? "abordado"
            : status == ReservationStatus.Cancelled
                ? "cancelado"
                : "activo";
    }
}
