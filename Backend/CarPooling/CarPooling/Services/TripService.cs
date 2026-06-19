using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class TripService(
    CarPoolingContext context,
    GeocodingService geocodingService,
    INotificationService notificationService,
    AuditService auditService)
{
    private readonly CarPoolingContext _context = context;
    private readonly GeocodingService _geocoding = geocodingService;
    private readonly INotificationService _notificationService = notificationService;
    private readonly AuditService _auditService = auditService;

    public async Task<Trip> CreateTripAsync(CoordinateRequest request)
    {
        var driverName = request.DriverName?.Trim() ?? "";
        if (driverName.Length > 100) driverName = driverName[..100];

        if (request.DriverUserId is not null)
        {
            var exists = await _context.Users.AnyAsync(u => u.Id == request.DriverUserId.Value);
            if (!exists) throw new InvalidOperationException("DriverUserId no existe.");
        }

        var originAddress = await _geocoding.ReverseGeocodeAsync(request.Latitude, request.Longitude);
        var origin = new Location
        {
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            AddressLabel = originAddress
        };

        _context.Locations.Add(origin);
        await _context.SaveChangesAsync();

        var trip = new Trip
        {
            OriginLocationId = origin.Id,
            DestinationLocationId = origin.Id, // placeholder, se actualiza con SetDestination
            StatusId = 1, // scheduled
            DriverName = driverName,
            DriverUserId = request.DriverUserId,
            OfferedSeats = request.OfferedSeats ?? 4,
            AvailableSeats = request.OfferedSeats ?? 4,
            FareAmount = request.FareAmount ?? 10m,
            VehicleId = request.VehicleId
        };

        _context.Trips.Add(trip);
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserCreatedTrip",
            "Trips",
            "Trip",
            trip.Id.ToString(),
            null,
            AuditService.SnapshotTrip(trip),
            actorUserId: trip.DriverUserId,
            description: $"Conductor creo el viaje {trip.Id}.");

        return trip;
    }

    public async Task<Trip> SetDestinationAsync(Guid tripId, CoordinateRequest request)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);
        if (trip is null) throw new InvalidOperationException("Viaje no encontrado.");
        if (trip.StatusId == 5) throw new InvalidOperationException("El viaje fue cancelado.");
        if (trip.StatusId != 1) throw new InvalidOperationException("El viaje ya tiene destino.");

        var oldValues = AuditService.SnapshotTrip(trip);

        var address = await _geocoding.ReverseGeocodeAsync(request.Latitude, request.Longitude);
        var destination = new Location
        {
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            AddressLabel = address
        };
        _context.Locations.Add(destination);

        trip.DestinationLocationId = destination.Id;
        trip.StatusId = 2; // ready (listo)
        trip.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserUpdatedTripDestination",
            "Trips",
            "Trip",
            trip.Id.ToString(),
            oldValues,
            AuditService.SnapshotTrip(trip),
            actorUserId: trip.DriverUserId ?? request.DriverUserId,
            description: $"Conductor definio destino del viaje {trip.Id}.");

        return trip;
    }

    public async Task<Trip> CancelTripAsync(Guid tripId)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);
        if (trip is null) throw new InvalidOperationException("Viaje no encontrado.");
        if (trip.StatusId == 5) return trip;

        var oldValues = AuditService.SnapshotTrip(trip);

        trip.StatusId = 5; // cancelled
        trip.CancelledAt = DateTime.UtcNow;
        trip.UpdatedAt = trip.CancelledAt;
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserCancelledTrip",
            "Trips",
            "Trip",
            trip.Id.ToString(),
            oldValues,
            AuditService.SnapshotTrip(trip),
            actorUserId: trip.DriverUserId,
            description: $"Conductor cancelo el viaje {trip.Id}.");

        var passengerIds = await _context.Reservations
            .Where(r => r.TripId == tripId && (r.StatusId == 1 || r.StatusId == 2 || r.StatusId == 3))
            .Select(r => r.PassengerUserId)
            .ToListAsync();

        if (passengerIds.Count > 0)
        {
            await _notificationService.SendNotificationToMultipleAsync(
                passengerIds,
                "Viaje Cancelado",
                $"Lamentamos informarte que {trip.DriverName} ha cancelado el viaje.",
                new Dictionary<string, string> { { "type", "trip_cancelled" }, { "tripId", trip.Id.ToString() } }
            );
        }

        return trip;
    }

    public async Task<Trip> StartTripAsync(Guid tripId)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);
        if (trip is null) throw new InvalidOperationException("Viaje no encontrado.");
        if (trip.StatusId == 5) throw new InvalidOperationException("Viaje cancelado.");
        if (trip.StatusId != 2) throw new InvalidOperationException("El viaje no esta listo para iniciar.");

        // Permitir iniciar viaje sin pasajeros abordados (se pueden abordar en ruta)
        var oldValues = AuditService.SnapshotTrip(trip);

        trip.StatusId = 3; // in_progress
        trip.StartedAt ??= DateTime.UtcNow;
        trip.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserStartedTrip",
            "Trips",
            "Trip",
            trip.Id.ToString(),
            oldValues,
            AuditService.SnapshotTrip(trip),
            actorUserId: trip.DriverUserId,
            description: $"Conductor inicio el viaje {trip.Id}.");

        var passengerIds = await _context.Reservations
            .Where(r => r.TripId == tripId && r.StatusId == 2) // confirmed
            .Select(r => r.PassengerUserId)
            .ToListAsync();

        if (passengerIds.Count > 0)
        {
            await _notificationService.SendNotificationToMultipleAsync(
                passengerIds,
                "¡Viaje Iniciado!",
                $"{trip.DriverName} ha iniciado el viaje. ¡Que tengan un buen recorrido!",
                new Dictionary<string, string> { { "type", "trip_started" }, { "tripId", trip.Id.ToString() } }
            );
        }

        return trip;
    }

    public async Task<Trip> FinishTripAsync(Guid tripId)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .FirstOrDefaultAsync(t => t.Id == tripId);
        if (trip is null) throw new InvalidOperationException("Viaje no encontrado.");
        if (trip.StatusId == 5) throw new InvalidOperationException("Viaje cancelado.");
        if (trip.StatusId == 4) return trip;
        if (trip.StatusId != 3) throw new InvalidOperationException("Solo se puede finalizar un viaje en curso.");

        var oldValues = AuditService.SnapshotTrip(trip);

        trip.StatusId = 4; // finished
        trip.FinishedAt ??= DateTime.UtcNow;
        trip.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        await _auditService.RecordAsync(
            "UserFinishedTrip",
            "Trips",
            "Trip",
            trip.Id.ToString(),
            oldValues,
            AuditService.SnapshotTrip(trip),
            actorUserId: trip.DriverUserId,
            description: $"Conductor finalizo el viaje {trip.Id}.");

        var passengerIds = await _context.Reservations
            .Where(r => r.TripId == tripId && r.StatusId == 3) // boarded
            .Select(r => r.PassengerUserId)
            .ToListAsync();

        if (passengerIds.Count > 0)
        {
            await _notificationService.SendNotificationToMultipleAsync(
                passengerIds,
                "Viaje Finalizado",
                $"El viaje con {trip.DriverName} ha finalizado. ¡Gracias por usar Carpooling!",
                new Dictionary<string, string> { { "type", "trip_finished" }, { "tripId", trip.Id.ToString() } }
            );
        }

        return trip;
    }

    public async Task<Trip> UpdateTripLocationAsync(Guid tripId, double latitude, double longitude)
    {
        var trip = await _context.Trips
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .Include(t => t.DriverUser)
            .FirstOrDefaultAsync(t => t.Id == tripId);
        if (trip is null) throw new InvalidOperationException("Viaje no encontrado.");
        
        trip.CurrentLatitude = latitude;
        trip.CurrentLongitude = longitude;
        trip.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return trip;
    }

    public async Task<Trip?> GetByIdAsync(Guid tripId)
    {
        return await _context.Trips.AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .Include(t => t.Vehicle)
            .Include(t => t.DriverUser)
            .FirstOrDefaultAsync(t => t.Id == tripId);
    }

    public async Task<List<DriverTripMatchResponse>> GetMatchCandidatesAsync(
        double? referenceLatitude, double? referenceLongitude)
    {
        if (referenceLatitude.HasValue && (referenceLatitude.Value is < -90 or > 90))
            throw new InvalidOperationException("Coordenadas invalidas.");
        if (referenceLongitude.HasValue && (referenceLongitude.Value is < -180 or > 180))
            throw new InvalidOperationException("Coordenadas invalidas.");

        var trips = await _context.Trips.AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .Include(t => t.Vehicle)
            .Include(t => t.DriverUser)
            .Where(t =>
                (t.StatusId == 1 || t.StatusId == 2 || t.StatusId == 3) // scheduled, ready, or in progress
                && t.AvailableSeats > 0)
            .ToListAsync();

        return trips.Select(t =>
        {
            var destLat = t.DestinationLocation.Latitude;
            var destLon = t.DestinationLocation.Longitude;
            var origLat = t.OriginLocation.Latitude;
            var origLon = t.OriginLocation.Longitude;

            double distanceKm = 0.0;
            int eta = 0;

            if (referenceLatitude.HasValue && referenceLongitude.HasValue)
            {
                var dOrigin = HaversineKm(referenceLatitude.Value, referenceLongitude.Value, origLat, origLon);
                var dDest = HaversineKm(referenceLatitude.Value, referenceLongitude.Value, destLat, destLon);
                distanceKm = Math.Min(dOrigin, dDest);
                const double assumedKmh = 28.0;
                eta = (int)Math.Clamp(Math.Round(distanceKm / assumedKmh * 60.0), 1, 999);
            }

            var name = string.IsNullOrWhiteSpace(t.DriverName) ? "Conductor" : t.DriverName.Trim();
            if (name.Length > 100) name = name[..100];

            return new DriverTripMatchResponse
            {
                TripId = t.Id,
                DriverName = name,
                DriverProfilePicture = t.DriverUser?.ProfilePicture,
                Origin = new LocationDto
                {
                    Id = t.OriginLocation.Id,
                    Latitude = origLat,
                    Longitude = origLon,
                    AddressLabel = t.OriginLocation.AddressLabel
                },
                Destination = new LocationDto
                {
                    Id = t.DestinationLocation.Id,
                    Latitude = destLat,
                    Longitude = destLon,
                    AddressLabel = t.DestinationLocation.AddressLabel
                },
                StatusLabel = t.StatusEntity?.LabelEs ?? "",
                AvailableSeats = t.AvailableSeats,
                FareAmount = t.FareAmount,
                DistanceKm = Math.Round(distanceKm, 2, MidpointRounding.AwayFromZero),
                EtaMinutes = eta,
                VehicleBrand = t.Vehicle?.Brand ?? "",
                VehicleColor = t.Vehicle?.Color ?? "",
                VehiclePlate = t.Vehicle?.LicensePlate ?? "",
            };
        })
        .OrderBy(r => r.DistanceKm)
        .ToList();
    }

    public async Task<Trip?> GetActiveTripForDriverAsync(Guid driverUserId, string? displayName = null)
    {
        var trip = await _context.Trips.AsNoTracking()
            .Include(t => t.OriginLocation)
            .Include(t => t.DestinationLocation)
            .Include(t => t.StatusEntity)
            .Include(t => t.DriverUser)
            .Where(t =>
                t.DriverUserId == driverUserId
                && t.StatusId != 4
                && t.StatusId != 5)
            .OrderByDescending(t => t.CreatedAt)
            .FirstOrDefaultAsync();

        if (trip is null && !string.IsNullOrWhiteSpace(displayName))
        {
            var n = displayName.Trim();
            trip = await _context.Trips.AsNoTracking()
                .Include(t => t.OriginLocation)
                .Include(t => t.DestinationLocation)
                .Include(t => t.StatusEntity)
                .Where(t =>
                    t.DriverUserId == null
                    && t.DriverName != null
                    && t.DriverName.Trim() == n
                    && t.StatusId != 4
                    && t.StatusId != 5)
                .OrderByDescending(t => t.CreatedAt)
                .FirstOrDefaultAsync();
        }

        return trip;
    }

    public static TripResponse MapToDto(Trip trip)
    {
        return new TripResponse
        {
            Id = trip.Id,
            Origin = new LocationDto
            {
                Id = trip.OriginLocation.Id,
                Latitude = trip.OriginLocation.Latitude,
                Longitude = trip.OriginLocation.Longitude,
                AddressLabel = trip.OriginLocation.AddressLabel
            },
            Destination = new LocationDto
            {
                Id = trip.DestinationLocation.Id,
                Latitude = trip.DestinationLocation.Latitude,
                Longitude = trip.DestinationLocation.Longitude,
                AddressLabel = trip.DestinationLocation.AddressLabel
            },
            StatusLabel = trip.StatusEntity?.LabelEs ?? "",
            StatusId = trip.StatusId,
            OfferedSeats = trip.OfferedSeats,
            AvailableSeats = trip.AvailableSeats,
            FareAmount = trip.FareAmount,
            VehicleId = trip.VehicleId,
            DriverName = trip.DriverName,
            DriverUserId = trip.DriverUserId,
            DriverProfilePicture = trip.DriverUser?.ProfilePicture,
            CurrentLatitude = trip.CurrentLatitude,
            CurrentLongitude = trip.CurrentLongitude,
            CreatedAt = trip.CreatedAt,
            UpdatedAt = trip.UpdatedAt,
            CancelledAt = trip.CancelledAt
        };
    }

    private static double HaversineKm(double lat1, double lon1, double lat2, double lon2)
    {
        const double earthRadiusKm = 6371.0;
        static double ToRad(double degrees) => degrees * (Math.PI / 180.0);
        var dLat = ToRad(lat2 - lat1);
        var dLon = ToRad(lon2 - lon1);
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2)
                + Math.Cos(ToRad(lat1)) * Math.Cos(ToRad(lat2)) * Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
