using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class PaymentService(CarPoolingContext context, INotificationService notificationService)
{
    private readonly CarPoolingContext _context = context;
    private readonly INotificationService _notificationService = notificationService;

    public Task<List<PaymentMethodResponseDto>> ListMethodsAsync()
    {
        var methods = PaymentMethodCatalog.All
            .OrderBy(m => m.Id)
            .Select(PaymentMethodResponseDto.FromDefinition)
            .ToList();

        return Task.FromResult(methods);
    }

    public async Task<UserPaymentMethodResponseDto> CreateUserPaymentMethodAsync(
        Guid userId,
        CreateUserPaymentMethodDto dto)
    {
        await EnsureUserExistsAsync(userId);

        var method = ResolvePaymentMethod(dto.PaymentMethodId);
        if (method.Type == PaymentMethodType.BankQr && string.IsNullOrWhiteSpace(dto.QrImageUrl))
        {
            throw new InvalidOperationException("Debes registrar una imagen del QR bancario.");
        }

        if (method.Type == PaymentMethodType.BankQr &&
            !dto.QrImageUrl!.Trim().StartsWith("data:image/", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("El QR bancario debe enviarse como imagen en base64.");
        }

        if (dto.IsDefault)
        {
            await ClearDefaultUserPaymentMethodsAsync(userId, method.Id);
        }

        var userMethod = BuildUserPaymentMethod(userId, method, dto.IsDefault);
        userMethod.Alias = dto.Alias?.Trim();
        userMethod.MaskedValue = dto.MaskedValue?.Trim();
        userMethod.ProviderToken = dto.ProviderToken?.Trim();
        userMethod.QrImageUrl = dto.QrImageUrl?.Trim();
        userMethod.BankName = dto.BankName?.Trim();
        userMethod.AccountHolderName = dto.AccountHolderName?.Trim();

        _context.UserPaymentMethods.Add(userMethod);
        await _context.SaveChangesAsync();

        return UserPaymentMethodResponseDto.FromEntity(userMethod);
    }

    public async Task<List<UserPaymentMethodResponseDto>> ListUserPaymentMethodsAsync(Guid userId)
    {
        await EnsureUserExistsAsync(userId);

        var methods = await _context.UserPaymentMethods
            .AsNoTracking()
            .Where(m => m.UserId == userId && m.IsActive)
            .OrderByDescending(m => m.IsDefault)
            .ThenByDescending(m => m.CreatedAt)
            .ToListAsync();

        return methods.Select(UserPaymentMethodResponseDto.FromEntity).ToList();
    }

    public async Task<List<UserPaymentMethodResponseDto>> ListDriverPaymentMethodsForReservationAsync(
        Guid userId,
        Guid reservationId)
    {
        var reservation = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new KeyNotFoundException("Reserva no encontrada.");

        if (reservation.PassengerUserId != userId && reservation.Trip.DriverUserId != userId)
        {
            throw new InvalidOperationException("No tienes permisos para ver los metodos de pago de esta reserva.");
        }

        if (!reservation.Trip.DriverUserId.HasValue)
        {
            throw new InvalidOperationException("La reserva no tiene conductor asignado.");
        }

        var methods = await _context.UserPaymentMethods
            .AsNoTracking()
            .Where(m => m.UserId == reservation.Trip.DriverUserId.Value &&
                        m.IsActive &&
                        (m.Type == PaymentMethodType.BankQr || m.Type == PaymentMethodType.Cash))
            .OrderByDescending(m => m.IsDefault)
            .ThenByDescending(m => m.CreatedAt)
            .ToListAsync();

        return methods.Select(UserPaymentMethodResponseDto.FromEntity).ToList();
    }

    public async Task<UserPaymentMethodResponseDto> DisableUserPaymentMethodAsync(Guid userId, Guid methodId)
    {
        var method = await _context.UserPaymentMethods
            .FirstOrDefaultAsync(m => m.Id == methodId && m.UserId == userId)
            ?? throw new KeyNotFoundException("Metodo de pago del usuario no encontrado.");

        method.IsActive = false;
        method.IsDefault = false;
        method.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return UserPaymentMethodResponseDto.FromEntity(method);
    }

    public async Task<PaymentResponseDto> CreatePaymentAsync(Guid userId, CreatePaymentDto dto)
    {
        var currency = NormalizeCurrency(dto.Currency);

        var reservation = await _context.Reservations
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == dto.ReservationId)
            ?? throw new KeyNotFoundException("Reserva no encontrada.");

        if (reservation.PassengerUserId != userId)
        {
            throw new InvalidOperationException("Solo el pasajero de la reserva puede crear el pago.");
        }

        if (reservation.StatusId == 4)
        {
            throw new InvalidOperationException("No se puede pagar una reserva cancelada.");
        }

        if (reservation.StatusId != 2 && reservation.StatusId != 3)
        {
            throw new InvalidOperationException("Solo se puede pagar una reserva aceptada por el conductor.");
        }

        if (reservation.Trip.StatusId is 4 or 5)
        {
            throw new InvalidOperationException("No se puede pagar un viaje finalizado o cancelado.");
        }

        var method = ResolvePaymentMethod(dto.PaymentMethodId);

        var existingPending = await _context.Payments
            .Include(p => p.UserPaymentMethod)
            .Where(p => p.ReservationId == dto.ReservationId && p.Status == PaymentStatus.Pending)
            .ToListAsync();
        await CheckAndExpirePaymentsAsync(existingPending);

        var hasOpenPayment = await _context.Payments.AnyAsync(p =>
            p.ReservationId == dto.ReservationId &&
            p.Status != PaymentStatus.Rejected &&
            p.Status != PaymentStatus.Cancelled &&
            p.Status != PaymentStatus.Expired);

        if (hasOpenPayment)
        {
            throw new InvalidOperationException("La reserva ya tiene un pago activo.");
        }

        var userMethod = await ResolveUserPaymentMethodAsync(dto.UserPaymentMethodId, method, reservation, userId);
        var isCash = method.Type == PaymentMethodType.Cash;

        var payment = new Payment
        {
            Id = Guid.NewGuid(),
            ReservationId = dto.ReservationId,
            UserPaymentMethodId = userMethod.Id,
            UserPaymentMethod = userMethod,
            Amount = reservation.Trip.FareAmount * reservation.SeatsReserved,
            Currency = currency,
            Status = method.RequiresManualConfirmation ? PaymentStatus.Pending : PaymentStatus.Approved,
            Description = dto.Description?.Trim(),
            ExternalReference = $"PAY-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
            ExpiresAt = method.RequiresManualConfirmation && !isCash ? DateTime.UtcNow.AddMinutes(60) : null,
            PaidAt = method.RequiresManualConfirmation ? null : DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = method.RequiresManualConfirmation ? null : DateTime.UtcNow
        };

        payment.Transactions.Add(new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            TransactionType = PaymentTransactionType.Payment,
            Status = method.RequiresManualConfirmation ? PaymentTransactionStatus.Pending : PaymentTransactionStatus.Success,
            Amount = payment.Amount,
            Provider = method.Type == PaymentMethodType.Simulated ? "SIMULATED_GATEWAY" : method.Code,
            ResponseMessage = isCash
                ? "Pago en efectivo registrado (se realizara en persona al finalizar el viaje)."
                : (method.RequiresManualConfirmation
                    ? "Pago pendiente de confirmacion manual."
                    : "Pago creado en ambiente simulado."),
            CreatedAt = DateTime.UtcNow
        });

        _context.Payments.Add(payment);
        await _context.SaveChangesAsync();

        payment.Reservation = reservation;
        if (payment.Status == PaymentStatus.Pending && reservation.Trip.DriverUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                reservation.Trip.DriverUserId.Value,
                "Pago pendiente de confirmacion",
                $"Tienes un pago de {payment.Amount:0.00} {payment.Currency} pendiente por confirmar.",
                BuildPaymentData(payment, "payment_pending"));
            await _notificationService.SendNotificationAsync(
                reservation.PassengerUserId,
                "Pago enviado",
                "Tu pago quedo pendiente de confirmacion del conductor.",
                BuildPaymentData(payment, "payment_pending"));
        }
        else if (payment.Status == PaymentStatus.Approved && reservation.Trip.DriverUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                reservation.Trip.DriverUserId.Value,
                "Pago recibido",
                $"Se registro un pago aprobado de {payment.Amount:0.00} {payment.Currency}.",
                BuildPaymentData(payment, "payment_approved"));
            await _notificationService.SendNotificationAsync(
                reservation.PassengerUserId,
                "Pago aprobado",
                $"Tu pago de {payment.Amount:0.00} {payment.Currency} fue aprobado.",
                BuildPaymentData(payment, "payment_approved"));
        }

        return await GetPaymentForUserAsync(userId, payment.Id);
    }

    public async Task<List<PaymentResponseDto>> ListPaymentsForUserAsync(Guid userId)
    {
        await EnsureUserExistsAsync(userId);

        var payments = await PaymentDetailsQuery()
            .Where(p => p.Reservation.PassengerUserId == userId ||
                        p.Reservation.Trip.DriverUserId == userId ||
                        p.ConfirmedByUserId == userId)
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync();

        await CheckAndExpirePaymentsAsync(payments);

        return payments.Select(PaymentResponseDto.FromEntity).ToList();
    }

    public async Task<List<PaymentResponseDto>> ListPaymentsForReservationAsync(Guid userId, Guid reservationId)
    {
        var reservation = await _context.Reservations
            .AsNoTracking()
            .Include(r => r.Trip)
            .FirstOrDefaultAsync(r => r.Id == reservationId)
            ?? throw new KeyNotFoundException("Reserva no encontrada.");

        if (reservation.PassengerUserId != userId && reservation.Trip.DriverUserId != userId)
        {
            throw new InvalidOperationException("No tienes permisos para ver los pagos de esta reserva.");
        }

        var payments = await PaymentDetailsQuery()
            .Where(p => p.ReservationId == reservationId)
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync();

        await CheckAndExpirePaymentsAsync(payments);

        return payments.Select(PaymentResponseDto.FromEntity).ToList();
    }

    public async Task<PaymentResponseDto> GetPaymentForUserAsync(Guid userId, Guid paymentId)
    {
        var payment = await PaymentDetailsQuery()
            .FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        await CheckAndExpirePaymentsAsync(new List<Payment> { payment });

        EnsureCanAccessPayment(userId, payment);
        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<PaymentResponseDto> SimulatePaymentAsync(Guid userId, Guid paymentId, SimulatePaymentDto dto)
    {
        var payment = await PaymentDetailsQuery().FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.Reservation.PassengerUserId != userId)
        {
            throw new InvalidOperationException("Solo el pasajero puede simular este pago.");
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidOperationException("Solo se pueden simular pagos pendientes.");
        }

        if (payment.UserPaymentMethod.RequiresManualConfirmation)
        {
            throw new InvalidOperationException("Este metodo requiere confirmacion manual del conductor.");
        }

        var now = DateTime.UtcNow;
        payment.Status = dto.Approve ? PaymentStatus.Approved : PaymentStatus.Rejected;
        payment.FailureReason = dto.Approve ? null : dto.ResponseMessage?.Trim() ?? "Pago rechazado en simulacion.";
        payment.PaidAt = dto.Approve ? now : null;
        payment.UpdatedAt = now;

        AddTransaction(payment, PaymentTransactionType.Payment,
            dto.Approve ? PaymentTransactionStatus.Success : PaymentTransactionStatus.Failed,
            payment.Amount,
            "SIMULATED_GATEWAY",
            dto.ResponseCode ?? (dto.Approve ? "APPROVED" : "REJECTED"),
            dto.ResponseMessage ?? (dto.Approve ? "Pago aprobado en ambiente simulado." : "Pago rechazado en ambiente simulado."));

        await _context.SaveChangesAsync();

        if (payment.Reservation.Trip.DriverUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                payment.Reservation.Trip.DriverUserId.Value,
                dto.Approve ? "Pago aprobado" : "Pago rechazado",
                dto.Approve
                    ? $"El pasajero completo un pago de {payment.Amount:0.00} {payment.Currency}."
                    : "El pago del pasajero fue rechazado.",
                BuildPaymentData(payment, dto.Approve ? "payment_approved" : "payment_rejected"));
        }
        await _notificationService.SendNotificationAsync(
            payment.Reservation.PassengerUserId,
            dto.Approve ? "Pago aprobado" : "Pago rechazado",
            dto.Approve
                ? $"Tu pago de {payment.Amount:0.00} {payment.Currency} fue aprobado."
                : "Tu pago fue rechazado.",
            BuildPaymentData(payment, dto.Approve ? "payment_approved" : "payment_rejected"));

        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<PaymentResponseDto> ConfirmManualPaymentAsync(Guid userId, Guid paymentId, ConfirmPaymentDto dto)
    {
        var payment = await _context.Payments
            .Include(p => p.UserPaymentMethod)
            .Include(p => p.Reservation).ThenInclude(r => r.Trip)
            .FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.Reservation.Trip.DriverUserId != userId)
        {
            throw new InvalidOperationException("Solo el conductor del viaje puede confirmar este pago.");
        }

        if (payment.Status == PaymentStatus.Approved)
        {
            return await GetPaymentForUserAsync(userId, paymentId);
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidOperationException("Solo se pueden confirmar pagos pendientes.");
        }

        if (!payment.UserPaymentMethod.RequiresManualConfirmation)
        {
            throw new InvalidOperationException("Este metodo no requiere confirmacion manual.");
        }

        var now = DateTime.UtcNow;
        payment.Status = PaymentStatus.Approved;
        payment.ConfirmedByUserId = userId;
        payment.ConfirmedAt = now;
        payment.ConfirmationNotes = dto.Notes?.Trim();
        payment.ConfirmationEvidenceUrl = dto.EvidenceUrl?.Trim();
        payment.PaidAt = now;
        payment.UpdatedAt = now;

        var originalTransaction = await _context.PaymentTransactions
            .Where(t => t.PaymentId == paymentId && t.TransactionType == PaymentTransactionType.Payment)
            .OrderBy(t => t.CreatedAt)
            .FirstOrDefaultAsync();

        if (originalTransaction != null && originalTransaction.Status == PaymentTransactionStatus.Pending)
        {
            originalTransaction.Status = PaymentTransactionStatus.Success;
            originalTransaction.ResponseCode = "CONFIRMED";
            originalTransaction.ResponseMessage = "Pago confirmado manualmente por el conductor.";
            originalTransaction.ProcessedAt = now;
        }

        _context.PaymentTransactions.Add(new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            TransactionType = PaymentTransactionType.Confirmation,
            Status = PaymentTransactionStatus.Success,
            Amount = payment.Amount,
            Provider = payment.UserPaymentMethod.PaymentMethodCode,
            ProviderTransactionId = $"TX-{now:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
            ResponseCode = "CONFIRMED",
            ResponseMessage = "Pago confirmado manualmente por el conductor.",
            ProcessedAt = now,
            CreatedAt = now
        });

        await _context.SaveChangesAsync();
        await _notificationService.SendNotificationAsync(
            payment.Reservation.PassengerUserId,
            "Pago confirmado",
            $"Tu pago de {payment.Amount:0.00} {payment.Currency} fue confirmado por el conductor.",
            BuildPaymentData(payment, "payment_confirmed"));

        return await GetPaymentForUserAsync(userId, paymentId);
    }

    public async Task<PaymentResponseDto> CancelPaymentAsync(Guid userId, Guid paymentId)
    {
        var payment = await PaymentDetailsQuery().FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.Reservation.PassengerUserId != userId && payment.Reservation.Trip.DriverUserId != userId)
        {
            throw new InvalidOperationException("No tienes permisos para cancelar este pago.");
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidOperationException("Solo se pueden cancelar pagos pendientes.");
        }

        payment.Status = PaymentStatus.Cancelled;
        payment.UpdatedAt = DateTime.UtcNow;
        AddTransaction(payment, PaymentTransactionType.Cancellation, PaymentTransactionStatus.Success,
            payment.Amount, payment.UserPaymentMethod.PaymentMethodCode, "CANCELLED", "Pago cancelado.");

        await _context.SaveChangesAsync();
        var otherUserId = payment.Reservation.PassengerUserId == userId
            ? payment.Reservation.Trip.DriverUserId
            : payment.Reservation.PassengerUserId;
        if (otherUserId.HasValue)
        {
            await _notificationService.SendNotificationAsync(
                otherUserId.Value,
                "Pago cancelado",
                $"Se cancelo un pago de {payment.Amount:0.00} {payment.Currency}.",
                BuildPaymentData(payment, "payment_cancelled"));
        }

        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<List<PaymentResponseDto>> ListAllPaymentsAsync()
    {
        var payments = await PaymentDetailsQuery()
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync();

        await CheckAndExpirePaymentsAsync(payments);

        return payments.Select(PaymentResponseDto.FromEntity).ToList();
    }

    private IQueryable<Payment> PaymentDetailsQuery()
    {
        return _context.Payments
            .Include(p => p.ConfirmedByUser)
            .Include(p => p.UserPaymentMethod)
            .Include(p => p.Reservation).ThenInclude(r => r.Trip)
            .Include(p => p.Reservation).ThenInclude(r => r.PassengerUser)
            .Include(p => p.Transactions);
    }

    private async Task EnsureUserExistsAsync(Guid userId)
    {
        var exists = await _context.Users.AnyAsync(u => u.Id == userId);
        if (!exists)
        {
            throw new KeyNotFoundException("Usuario no encontrado.");
        }
    }

    private async Task ClearDefaultUserPaymentMethodsAsync(Guid userId, int paymentMethodId)
    {
        var defaults = await _context.UserPaymentMethods
            .Where(m => m.UserId == userId && m.PaymentMethodId == paymentMethodId && m.IsDefault)
            .ToListAsync();

        foreach (var method in defaults)
        {
            method.IsDefault = false;
            method.UpdatedAt = DateTime.UtcNow;
        }
    }

    private async Task<UserPaymentMethod> ResolveUserPaymentMethodAsync(
        Guid? userPaymentMethodId,
        PaymentMethodDefinition method,
        Reservation reservation,
        Guid passengerUserId)
    {
        if (userPaymentMethodId.HasValue)
        {
            var userMethod = await _context.UserPaymentMethods
                .FirstOrDefaultAsync(m => m.Id == userPaymentMethodId.Value && m.IsActive)
                ?? throw new KeyNotFoundException("Metodo de pago guardado no encontrado.");

            ValidateUserPaymentMethodOwner(userMethod, method, reservation, passengerUserId);
            return userMethod;
        }

        if (method.Type == PaymentMethodType.BankQr)
        {
            var driverUserId = reservation.Trip.DriverUserId
                ?? throw new InvalidOperationException("La reserva no tiene conductor asignado.");

            return await _context.UserPaymentMethods
                .Where(m => m.UserId == driverUserId && m.PaymentMethodId == method.Id && m.IsActive)
                .OrderByDescending(m => m.IsDefault)
                .ThenByDescending(m => m.CreatedAt)
                .FirstOrDefaultAsync()
                ?? throw new InvalidOperationException("El conductor aun no registro un QR bancario.");
        }

        var ownerUserId = method.Type == PaymentMethodType.Cash
            ? reservation.Trip.DriverUserId ?? throw new InvalidOperationException("La reserva no tiene conductor asignado.")
            : passengerUserId;

        return await GetOrCreateRuntimeUserPaymentMethodAsync(ownerUserId, method);
    }

    private static void ValidateUserPaymentMethodOwner(
        UserPaymentMethod userMethod,
        PaymentMethodDefinition method,
        Reservation reservation,
        Guid passengerUserId)
    {
        if (userMethod.PaymentMethodId != method.Id)
        {
            throw new InvalidOperationException("El metodo guardado no coincide con el metodo de pago seleccionado.");
        }

        var expectedOwnerId = method.Type is PaymentMethodType.BankQr or PaymentMethodType.Cash
            ? reservation.Trip.DriverUserId
            : passengerUserId;

        if (!expectedOwnerId.HasValue || userMethod.UserId != expectedOwnerId.Value)
        {
            throw new InvalidOperationException("El metodo de pago guardado no pertenece al usuario correspondiente.");
        }
    }

    private async Task<UserPaymentMethod> GetOrCreateRuntimeUserPaymentMethodAsync(Guid userId, PaymentMethodDefinition method)
    {
        var existing = await _context.UserPaymentMethods
            .Where(m => m.UserId == userId && m.PaymentMethodId == method.Id && m.IsActive)
            .OrderByDescending(m => m.IsDefault)
            .ThenByDescending(m => m.CreatedAt)
            .FirstOrDefaultAsync();

        if (existing is not null)
        {
            return existing;
        }

        var created = BuildUserPaymentMethod(userId, method, isDefault: false);
        created.Alias = method.Name;
        _context.UserPaymentMethods.Add(created);
        await _context.SaveChangesAsync();
        return created;
    }

    private static UserPaymentMethod BuildUserPaymentMethod(Guid userId, PaymentMethodDefinition method, bool isDefault)
    {
        return new UserPaymentMethod
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            PaymentMethodId = method.Id,
            PaymentMethodCode = method.Code,
            PaymentMethodName = method.Name,
            PaymentMethodDescription = method.Description,
            Type = method.Type,
            RequiresManualConfirmation = method.RequiresManualConfirmation,
            IsDefault = isDefault,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };
    }

    private static PaymentMethodDefinition ResolvePaymentMethod(int paymentMethodId)
    {
        return PaymentMethodCatalog.Find(paymentMethodId)
            ?? throw new KeyNotFoundException("Metodo de pago no encontrado.");
    }

    private static void EnsureCanAccessPayment(Guid userId, Payment payment)
    {
        if (payment.Reservation.PassengerUserId == userId ||
            payment.Reservation.Trip.DriverUserId == userId ||
            payment.ConfirmedByUserId == userId)
        {
            return;
        }

        throw new InvalidOperationException("No tienes permisos para ver este pago.");
    }

    private static string NormalizeCurrency(string? currency)
    {
        var normalized = string.IsNullOrWhiteSpace(currency) ? "BOB" : currency.Trim().ToUpperInvariant();
        if (normalized.Length != 3)
        {
            throw new InvalidOperationException("La moneda debe tener formato ISO de 3 letras.");
        }

        return normalized;
    }

    private static Dictionary<string, string> BuildPaymentData(Payment payment, string type)
    {
        var data = new Dictionary<string, string>
        {
            ["type"] = type,
            ["paymentId"] = payment.Id.ToString(),
            ["reservationId"] = payment.ReservationId.ToString()
        };

        if (payment.Reservation?.TripId is Guid tripId)
        {
            data["tripId"] = tripId.ToString();
        }

        return data;
    }

    private static void AddTransaction(
        Payment payment,
        PaymentTransactionType type,
        PaymentTransactionStatus status,
        decimal amount,
        string provider,
        string responseCode,
        string responseMessage)
    {
        payment.Transactions.Add(new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            TransactionType = type,
            Status = status,
            Amount = amount,
            Provider = provider,
            ProviderTransactionId = $"TX-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
            ResponseCode = responseCode,
            ResponseMessage = responseMessage,
            ProcessedAt = DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow
        });
    }

    private async Task CheckAndExpirePaymentsAsync(List<Payment> payments)
    {
        var now = DateTime.UtcNow;
        var changed = false;
        foreach (var payment in payments)
        {
            if (payment.Status == PaymentStatus.Pending && payment.ExpiresAt.HasValue && payment.ExpiresAt.Value < now)
            {
                payment.Status = PaymentStatus.Expired;
                payment.UpdatedAt = now;
                payment.Transactions.Add(new PaymentTransaction
                {
                    Id = Guid.NewGuid(),
                    PaymentId = payment.Id,
                    TransactionType = PaymentTransactionType.Cancellation,
                    Status = PaymentTransactionStatus.Failed,
                    Amount = payment.Amount,
                    Provider = payment.UserPaymentMethod?.PaymentMethodCode ?? "SYSTEM",
                    ProviderTransactionId = $"TX-EXP-{now:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
                    ResponseCode = "EXPIRED",
                    ResponseMessage = "Pago expirado automaticamente por limite de tiempo.",
                    ProcessedAt = now,
                    CreatedAt = now
                });
                changed = true;
            }
        }

        if (changed)
        {
            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateConcurrencyException)
            {
                // Concurrency conflicts during auto-expiration can be safely ignored.
            }
        }
    }
}
