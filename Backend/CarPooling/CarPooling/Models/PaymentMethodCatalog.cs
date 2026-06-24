namespace CarPooling.Models;

public sealed record PaymentMethodDefinition(
    int Id,
    string Code,
    string Name,
    string Description,
    PaymentMethodType Type,
    bool RequiresManualConfirmation);

public static class PaymentMethodCatalog
{
    public static readonly IReadOnlyList<PaymentMethodDefinition> All =
    [
        new(1, "CASH", "Efectivo", "Pago en efectivo confirmado por el conductor.", PaymentMethodType.Cash, true),
        new(2, "CARD_SIM", "Tarjeta simulada", "Pago con tarjeta en ambiente simulado para fines academicos.", PaymentMethodType.Simulated, false),
        new(3, "QR_BANK", "QR bancario", "Pago mediante QR bancario del conductor, confirmado manualmente.", PaymentMethodType.BankQr, true),
        new(4, "WALLET_SIM", "Billetera simulada", "Saldo interno simulado para fines academicos.", PaymentMethodType.Wallet, false)
    ];

    public static PaymentMethodDefinition? Find(int id) => All.FirstOrDefault(m => m.Id == id);

    public static PaymentMethodDefinition? FindByCode(string code) =>
        All.FirstOrDefault(m => string.Equals(m.Code, code, StringComparison.OrdinalIgnoreCase));
}
