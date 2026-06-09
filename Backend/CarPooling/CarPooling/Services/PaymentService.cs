using CarPooling.Data;
using CarPooling.Dtos;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Services;

public class PaymentService(CarPoolingContext context)
{
    private readonly CarPoolingContext _context = context;

    public async Task<List<PaymentMethodResponseDto>> ListMethodsAsync()
    {
        var methods = await _context.PaymentMethods
            .AsNoTracking()
            .Where(m => m.IsActive)
            .OrderBy(m => m.Id)
            .ToListAsync();

        return methods.Select(PaymentMethodResponseDto.FromEntity).ToList();
    }

    public async Task<UserPaymentMethodResponseDto> CreateUserPaymentMethodAsync(
        Guid userId,
        CreateUserPaymentMethodDto dto)
    {
        await EnsureUserExistsAsync(userId);

        var method = await _context.PaymentMethods.FirstOrDefaultAsync(m => m.Id == dto.PaymentMethodId && m.IsActive)
            ?? throw new KeyNotFoundException("Metodo de pago no encontrado.");

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
            await ClearDefaultUserPaymentMethodsAsync(userId, dto.PaymentMethodId);
        }

        var userMethod = new UserPaymentMethod
        {
            Id = Guid.NewGuid(),
            UserId = userId,
            PaymentMethodId = dto.PaymentMethodId,
            Alias = dto.Alias?.Trim(),
            MaskedValue = dto.MaskedValue?.Trim(),
            ProviderToken = dto.ProviderToken?.Trim(),
            QrImageUrl = dto.QrImageUrl?.Trim(),
            BankName = dto.BankName?.Trim(),
            AccountHolderName = dto.AccountHolderName?.Trim(),
            IsDefault = dto.IsDefault,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        _context.UserPaymentMethods.Add(userMethod);
        await _context.SaveChangesAsync();

        userMethod.PaymentMethod = method;
        return UserPaymentMethodResponseDto.FromEntity(userMethod);
    }

    public async Task<List<UserPaymentMethodResponseDto>> ListUserPaymentMethodsAsync(Guid userId)
    {
        await EnsureUserExistsAsync(userId);

        var methods = await _context.UserPaymentMethods
            .AsNoTracking()
            .Include(m => m.PaymentMethod)
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
            .Include(m => m.PaymentMethod)
            .Where(m => m.UserId == reservation.Trip.DriverUserId.Value &&
                        m.IsActive &&
                        m.PaymentMethod.IsActive &&
                        (m.PaymentMethod.Type == PaymentMethodType.BankQr ||
                         m.PaymentMethod.Type == PaymentMethodType.Cash))
            .OrderByDescending(m => m.IsDefault)
            .ThenByDescending(m => m.CreatedAt)
            .ToListAsync();

        return methods.Select(UserPaymentMethodResponseDto.FromEntity).ToList();
    }

    public async Task<UserPaymentMethodResponseDto> DisableUserPaymentMethodAsync(Guid userId, Guid methodId)
    {
        var method = await _context.UserPaymentMethods
            .Include(m => m.PaymentMethod)
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

        var method = await _context.PaymentMethods.FirstOrDefaultAsync(m => m.Id == dto.PaymentMethodId && m.IsActive)
            ?? throw new KeyNotFoundException("Metodo de pago no encontrado.");

        await ValidateUserPaymentMethodAsync(dto.UserPaymentMethodId, method, reservation, userId);

        var existingPending = await _context.Payments
            .Where(p => p.ReservationId == dto.ReservationId && p.Status == PaymentStatus.Pending)
            .ToListAsync();
        await CheckAndExpirePaymentsAsync(existingPending);

        var hasOpenPayment = await _context.Payments.AnyAsync(p =>
            p.ReservationId == dto.ReservationId &&
            p.Status != PaymentStatus.Rejected &&
            p.Status != PaymentStatus.Cancelled &&
            p.Status != PaymentStatus.Expired &&
            p.Status != PaymentStatus.Refunded);

        if (hasOpenPayment)
        {
            throw new InvalidOperationException("La reserva ya tiene un pago activo.");
        }

        var payment = new Payment
        {
            Id = Guid.NewGuid(),
            ReservationId = dto.ReservationId,
            PassengerUserId = userId,
            PaymentMethodId = dto.PaymentMethodId,
            UserPaymentMethodId = dto.UserPaymentMethodId,
            Amount = reservation.Trip.FareAmount * reservation.SeatsReserved,
            Currency = currency,
            Status = PaymentStatus.Pending,
            Description = dto.Description?.Trim(),
            ExternalReference = $"PAY-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
            ExpiresAt = DateTime.UtcNow.AddMinutes(method.RequiresManualConfirmation ? 60 : 15),
            CreatedAt = DateTime.UtcNow
        };

        payment.Transactions.Add(new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            TransactionType = PaymentTransactionType.Payment,
            Status = PaymentTransactionStatus.Pending,
            Amount = payment.Amount,
            Provider = method.Type == PaymentMethodType.Simulated ? "SIMULATED_GATEWAY" : method.Code,
            ResponseMessage = method.RequiresManualConfirmation
                ? "Pago pendiente de confirmacion manual."
                : "Pago creado en ambiente simulado.",
            CreatedAt = DateTime.UtcNow
        });

        _context.Payments.Add(payment);
        await _context.SaveChangesAsync();

        return await GetPaymentForUserAsync(userId, payment.Id);
    }

    public async Task<List<PaymentResponseDto>> ListPaymentsForUserAsync(Guid userId)
    {
        await EnsureUserExistsAsync(userId);

        var payments = await PaymentDetailsQuery()
            .Where(p => p.PassengerUserId == userId ||
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

        if (payment.PassengerUserId != userId)
        {
            throw new InvalidOperationException("Solo el pasajero puede simular este pago.");
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidOperationException("Solo se pueden simular pagos pendientes.");
        }

        if (payment.PaymentMethod.RequiresManualConfirmation)
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

        if (dto.Approve)
        {
            EnsureReceipt(payment);
        }

        await _context.SaveChangesAsync();
        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<PaymentResponseDto> ConfirmManualPaymentAsync(Guid userId, Guid paymentId, ConfirmPaymentDto dto)
    {
        var payment = await PaymentDetailsQuery().FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.Reservation.Trip.DriverUserId != userId)
        {
            throw new InvalidOperationException("Solo el conductor del viaje puede confirmar este pago.");
        }

        if (payment.Status != PaymentStatus.Pending)
        {
            throw new InvalidOperationException("Solo se pueden confirmar pagos pendientes.");
        }

        if (!payment.PaymentMethod.RequiresManualConfirmation)
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

        AddTransaction(payment, PaymentTransactionType.Confirmation, PaymentTransactionStatus.Success,
            payment.Amount, payment.PaymentMethod.Code, "CONFIRMED",
            "Pago confirmado manualmente por el conductor.");
        EnsureReceipt(payment);

        await _context.SaveChangesAsync();
        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<PaymentResponseDto> CancelPaymentAsync(Guid userId, Guid paymentId)
    {
        var payment = await PaymentDetailsQuery().FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.PassengerUserId != userId && payment.Reservation.Trip.DriverUserId != userId)
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
            payment.Amount, payment.PaymentMethod.Code, "CANCELLED", "Pago cancelado.");

        await _context.SaveChangesAsync();
        return PaymentResponseDto.FromEntity(payment);
    }

    public async Task<PaymentResponseDto> RequestRefundAsync(Guid userId, Guid paymentId, CreateRefundDto dto)
    {
        var payment = await PaymentDetailsQuery().FirstOrDefaultAsync(p => p.Id == paymentId)
            ?? throw new KeyNotFoundException("Pago no encontrado.");

        if (payment.PassengerUserId != userId)
        {
            throw new InvalidOperationException("Solo el pasajero puede solicitar devolucion.");
        }

        if (payment.Status is not PaymentStatus.Approved and not PaymentStatus.PartiallyRefunded)
        {
            throw new InvalidOperationException("Solo se puede solicitar devolucion de pagos aprobados.");
        }

        if (!payment.PaymentMethod.SupportsRefunds)
        {
            throw new InvalidOperationException("Este metodo de pago no admite devoluciones.");
        }

        if (payment.Reservation.Trip.StartedAt.HasValue || payment.Reservation.Trip.StatusId == 3)
        {
            throw new InvalidOperationException("El viaje ya inicio; el pago no puede ser devuelto.");
        }

        var remainingAmount = payment.Amount - payment.RefundedAmount;
        if (dto.Amount <= 0 || dto.Amount > remainingAmount)
        {
            throw new InvalidOperationException("El monto de devolucion no es valido.");
        }

        var refund = new Refund
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            Amount = dto.Amount,
            Status = RefundStatus.Requested,
            RequestedByUserId = userId,
            Reason = dto.Reason?.Trim(),
            IsWithinCancellationWindow = true,
            RequestedAt = DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow
        };

        payment.Refunds.Add(refund);
        payment.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return await GetPaymentForUserAsync(userId, paymentId);
    }

    public async Task<RefundResponseDto> ApproveRefundAsync(Guid userId, Guid refundId)
    {
        var refund = await RefundDetailsQuery().FirstOrDefaultAsync(r => r.Id == refundId)
            ?? throw new KeyNotFoundException("Devolucion no encontrada.");

        EnsureCanProcessRefund(userId, refund);

        if (refund.Status != RefundStatus.Requested)
        {
            throw new InvalidOperationException("Solo se pueden aprobar devoluciones solicitadas.");
        }

        var payment = refund.Payment;
        var remainingAmount = payment.Amount - payment.RefundedAmount;
        if (refund.Amount > remainingAmount)
        {
            throw new InvalidOperationException("El monto de devolucion excede el saldo disponible.");
        }

        refund.Status = RefundStatus.Processed;
        refund.ProcessedByUserId = userId;
        refund.ProcessedAt = DateTime.UtcNow;

        payment.RefundedAmount += refund.Amount;
        payment.Status = payment.RefundedAmount >= payment.Amount
            ? PaymentStatus.Refunded
            : PaymentStatus.PartiallyRefunded;
        payment.UpdatedAt = DateTime.UtcNow;

        AddTransaction(payment, PaymentTransactionType.Refund, PaymentTransactionStatus.Success,
            refund.Amount, payment.PaymentMethod.Code, "REFUNDED", "Devolucion procesada.");

        await _context.SaveChangesAsync();
        return RefundResponseDto.FromEntity(refund);
    }

    public async Task<RefundResponseDto> RejectRefundAsync(Guid userId, Guid refundId, ProcessRefundDto dto)
    {
        var refund = await RefundDetailsQuery().FirstOrDefaultAsync(r => r.Id == refundId)
            ?? throw new KeyNotFoundException("Devolucion no encontrada.");

        EnsureCanProcessRefund(userId, refund);

        if (refund.Status != RefundStatus.Requested)
        {
            throw new InvalidOperationException("Solo se pueden rechazar devoluciones solicitadas.");
        }

        refund.Status = RefundStatus.Rejected;
        refund.ProcessedByUserId = userId;
        refund.ProcessedAt = DateTime.UtcNow;
        refund.RejectionReason = dto.Notes?.Trim() ?? "Devolucion rechazada.";

        await _context.SaveChangesAsync();
        return RefundResponseDto.FromEntity(refund);
    }

    private IQueryable<Payment> PaymentDetailsQuery()
    {
        return _context.Payments
            .Include(p => p.PassengerUser)
            .Include(p => p.ConfirmedByUser)
            .Include(p => p.PaymentMethod)
            .Include(p => p.UserPaymentMethod)
            .Include(p => p.Reservation).ThenInclude(r => r.Trip)
            .Include(p => p.Transactions)
            .Include(p => p.Receipt)
            .Include(p => p.Refunds);
    }

    private IQueryable<Refund> RefundDetailsQuery()
    {
        return _context.Refunds
            .Include(r => r.Payment).ThenInclude(p => p.Reservation).ThenInclude(r => r.Trip)
            .Include(r => r.Payment).ThenInclude(p => p.PaymentMethod)
            .Include(r => r.Payment).ThenInclude(p => p.Transactions);
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

    private async Task ValidateUserPaymentMethodAsync(
        Guid? userPaymentMethodId,
        PaymentMethod method,
        Reservation reservation,
        Guid passengerUserId)
    {
        if (!userPaymentMethodId.HasValue)
        {
            return;
        }

        var userMethod = await _context.UserPaymentMethods
            .AsNoTracking()
            .FirstOrDefaultAsync(m => m.Id == userPaymentMethodId.Value && m.IsActive)
            ?? throw new KeyNotFoundException("Metodo de pago guardado no encontrado.");

        if (userMethod.PaymentMethodId != method.Id)
        {
            throw new InvalidOperationException("El metodo guardado no coincide con el metodo de pago seleccionado.");
        }

        var expectedOwnerId = method.Type == PaymentMethodType.BankQr
            ? reservation.Trip.DriverUserId
            : passengerUserId;

        if (!expectedOwnerId.HasValue || userMethod.UserId != expectedOwnerId.Value)
        {
            throw new InvalidOperationException("El metodo de pago guardado no pertenece al usuario correspondiente.");
        }
    }

    private static void EnsureCanAccessPayment(Guid userId, Payment payment)
    {
        if (payment.PassengerUserId == userId ||
            payment.Reservation.Trip.DriverUserId == userId ||
            payment.ConfirmedByUserId == userId)
        {
            return;
        }

        throw new InvalidOperationException("No tienes permisos para ver este pago.");
    }

    private static void EnsureCanProcessRefund(Guid userId, Refund refund)
    {
        if (refund.Payment.Reservation.Trip.DriverUserId == userId || refund.Payment.ConfirmedByUserId == userId)
        {
            return;
        }

        throw new InvalidOperationException("Solo el conductor puede procesar esta devolucion.");
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

    private static void EnsureReceipt(Payment payment)
    {
        if (payment.Receipt is not null)
        {
            return;
        }

        payment.Receipt = new PaymentReceipt
        {
            Id = Guid.NewGuid(),
            PaymentId = payment.Id,
            ReceiptNumber = $"RC-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..6]}",
            QrCodeValue = payment.ExternalReference,
            IssuedAt = DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow
        };
    }

    public async Task<List<PaymentResponseDto>> ListAllPaymentsAsync()
    {
        var payments = await PaymentDetailsQuery()
            .OrderByDescending(p => p.CreatedAt)
            .ToListAsync();

        await CheckAndExpirePaymentsAsync(payments);

        return payments.Select(PaymentResponseDto.FromEntity).ToList();
    }

    private async Task CheckAndExpirePaymentsAsync(List<Payment> payments)
    {
        var now = DateTime.UtcNow;
        bool changed = false;
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
                    Provider = payment.PaymentMethod?.Code ?? "SYSTEM",
                    ProviderTransactionId = $"TX-EXP-{now:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N")[..8]}",
                    ResponseCode = "EXPIRED",
                    ResponseMessage = "Pago expirado automáticamente por límite de tiempo.",
                    ProcessedAt = now,
                    CreatedAt = now
                });
                changed = true;
            }
        }
        if (changed)
        {
            await _context.SaveChangesAsync();
        }
    }
}
