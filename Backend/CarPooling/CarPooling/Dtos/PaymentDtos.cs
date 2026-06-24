using CarPooling.Models;

namespace CarPooling.Dtos;

public class PaymentMethodResponseDto
{
    public int Id { get; set; }
    public string Code { get; set; } = "";
    public string Name { get; set; } = "";
    public string Description { get; set; } = "";
    public PaymentMethodType Type { get; set; }
    public bool RequiresManualConfirmation { get; set; }
    public bool IsActive { get; set; }

    public static PaymentMethodResponseDto FromDefinition(PaymentMethodDefinition method) => new()
    {
        Id = method.Id,
        Code = method.Code,
        Name = method.Name,
        Description = method.Description,
        Type = method.Type,
        RequiresManualConfirmation = method.RequiresManualConfirmation,
        IsActive = true
    };
}

public class CreateUserPaymentMethodDto
{
    public int PaymentMethodId { get; set; }
    public string? Alias { get; set; }
    public string? MaskedValue { get; set; }
    public string? ProviderToken { get; set; }
    public string? QrImageUrl { get; set; }
    public string? BankName { get; set; }
    public string? AccountHolderName { get; set; }
    public bool IsDefault { get; set; }
}

public class UserPaymentMethodResponseDto
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public int PaymentMethodId { get; set; }
    public string PaymentMethodCode { get; set; } = "";
    public string PaymentMethodName { get; set; } = "";
    public PaymentMethodType Type { get; set; }
    public bool RequiresManualConfirmation { get; set; }
    public string Alias { get; set; } = "";
    public string MaskedValue { get; set; } = "";
    public string QrImageUrl { get; set; } = "";
    public string BankName { get; set; } = "";
    public string AccountHolderName { get; set; } = "";
    public bool IsDefault { get; set; }
    public bool IsActive { get; set; }

    public static UserPaymentMethodResponseDto FromEntity(UserPaymentMethod method) => new()
    {
        Id = method.Id,
        UserId = method.UserId,
        PaymentMethodId = method.PaymentMethodId,
        PaymentMethodCode = method.PaymentMethodCode,
        PaymentMethodName = method.PaymentMethodName,
        Type = method.Type,
        RequiresManualConfirmation = method.RequiresManualConfirmation,
        Alias = method.Alias ?? "",
        MaskedValue = method.MaskedValue ?? "",
        QrImageUrl = method.QrImageUrl ?? "",
        BankName = method.BankName ?? "",
        AccountHolderName = method.AccountHolderName ?? "",
        IsDefault = method.IsDefault,
        IsActive = method.IsActive
    };
}

public class CreatePaymentDto
{
    public Guid ReservationId { get; set; }
    public int PaymentMethodId { get; set; }
    public Guid? UserPaymentMethodId { get; set; }
    public string Currency { get; set; } = "BOB";
    public string? Description { get; set; }
}

public class ConfirmPaymentDto
{
    public string? Notes { get; set; }
    public string? EvidenceUrl { get; set; }
}

public class SimulatePaymentDto
{
    public bool Approve { get; set; } = true;
    public string? ResponseCode { get; set; }
    public string? ResponseMessage { get; set; }
}

public class PaymentResponseDto
{
    public Guid Id { get; set; }
    public Guid ReservationId { get; set; }
    public Guid PassengerUserId { get; set; }
    public string PassengerName { get; set; } = "";
    public Guid TripId { get; set; }
    public Guid? DriverUserId { get; set; }
    public string DriverName { get; set; } = "";
    public int PaymentMethodId { get; set; }
    public string PaymentMethodCode { get; set; } = "";
    public string PaymentMethodName { get; set; } = "";
    public Guid UserPaymentMethodId { get; set; }
    public decimal Amount { get; set; }
    public string Currency { get; set; } = "";
    public PaymentStatus Status { get; set; }
    public string Description { get; set; } = "";
    public string ExternalReference { get; set; } = "";
    public string FailureReason { get; set; } = "";
    public Guid? ConfirmedByUserId { get; set; }
    public string ConfirmedByName { get; set; } = "";
    public DateTime? ConfirmedAt { get; set; }
    public string ConfirmationNotes { get; set; } = "";
    public string ConfirmationEvidenceUrl { get; set; } = "";
    public DateTime? PaidAt { get; set; }
    public DateTime? ExpiresAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public List<PaymentTransactionResponseDto> Transactions { get; set; } = [];

    public static PaymentResponseDto FromEntity(Payment payment) => new()
    {
        Id = payment.Id,
        ReservationId = payment.ReservationId,
        PassengerUserId = payment.Reservation?.PassengerUserId ?? Guid.Empty,
        PassengerName = payment.Reservation?.PassengerUser?.FullName ?? "",
        TripId = payment.Reservation?.TripId ?? Guid.Empty,
        DriverUserId = payment.Reservation?.Trip?.DriverUserId,
        DriverName = payment.Reservation?.Trip?.DriverName ?? "",
        PaymentMethodId = payment.UserPaymentMethod?.PaymentMethodId ?? 0,
        PaymentMethodCode = payment.UserPaymentMethod?.PaymentMethodCode ?? "",
        PaymentMethodName = payment.UserPaymentMethod?.PaymentMethodName ?? "",
        UserPaymentMethodId = payment.UserPaymentMethodId,
        Amount = payment.Amount,
        Currency = payment.Currency,
        Status = payment.Status,
        Description = payment.Description ?? "",
        ExternalReference = payment.ExternalReference ?? "",
        FailureReason = payment.FailureReason ?? "",
        ConfirmedByUserId = payment.ConfirmedByUserId,
        ConfirmedByName = payment.ConfirmedByUser?.FullName ?? "",
        ConfirmedAt = payment.ConfirmedAt,
        ConfirmationNotes = payment.ConfirmationNotes ?? "",
        ConfirmationEvidenceUrl = payment.ConfirmationEvidenceUrl ?? "",
        PaidAt = payment.PaidAt,
        ExpiresAt = payment.ExpiresAt,
        CreatedAt = payment.CreatedAt,
        UpdatedAt = payment.UpdatedAt,
        Transactions = payment.Transactions
            .OrderByDescending(t => t.CreatedAt)
            .Select(PaymentTransactionResponseDto.FromEntity)
            .ToList()
    };
}

public class PaymentTransactionResponseDto
{
    public Guid Id { get; set; }
    public PaymentTransactionType TransactionType { get; set; }
    public PaymentTransactionStatus Status { get; set; }
    public decimal Amount { get; set; }
    public string Provider { get; set; } = "";
    public string ProviderTransactionId { get; set; } = "";
    public string AuthorizationCode { get; set; } = "";
    public string ResponseCode { get; set; } = "";
    public string ResponseMessage { get; set; } = "";
    public DateTime? ProcessedAt { get; set; }
    public DateTime CreatedAt { get; set; }

    public static PaymentTransactionResponseDto FromEntity(PaymentTransaction transaction) => new()
    {
        Id = transaction.Id,
        TransactionType = transaction.TransactionType,
        Status = transaction.Status,
        Amount = transaction.Amount,
        Provider = transaction.Provider ?? "",
        ProviderTransactionId = transaction.ProviderTransactionId ?? "",
        AuthorizationCode = transaction.AuthorizationCode ?? "",
        ResponseCode = transaction.ResponseCode ?? "",
        ResponseMessage = transaction.ResponseMessage ?? "",
        ProcessedAt = transaction.ProcessedAt,
        CreatedAt = transaction.CreatedAt
    };
}
