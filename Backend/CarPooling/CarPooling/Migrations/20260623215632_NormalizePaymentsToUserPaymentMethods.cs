using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class NormalizePaymentsToUserPaymentMethods : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Payments_PaymentMethods_PaymentMethodId",
                table: "Payments");

            migrationBuilder.DropForeignKey(
                name: "FK_Payments_UserPaymentMethods_UserPaymentMethodId",
                table: "Payments");

            migrationBuilder.DropForeignKey(
                name: "FK_UserPaymentMethods_PaymentMethods_PaymentMethodId",
                table: "UserPaymentMethods");

            migrationBuilder.DropTable(
                name: "Refunds");

            migrationBuilder.DropIndex(
                name: "IX_UserPaymentMethods_PaymentMethodId",
                table: "UserPaymentMethods");

            migrationBuilder.DropIndex(
                name: "IX_Payments_PaymentMethodId",
                table: "Payments");

            migrationBuilder.AddColumn<string>(
                name: "PaymentMethodCode",
                table: "UserPaymentMethods",
                type: "nvarchar(30)",
                maxLength: 30,
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<string>(
                name: "PaymentMethodDescription",
                table: "UserPaymentMethods",
                type: "nvarchar(300)",
                maxLength: 300,
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "PaymentMethodName",
                table: "UserPaymentMethods",
                type: "nvarchar(80)",
                maxLength: 80,
                nullable: false,
                defaultValue: "");

            migrationBuilder.AddColumn<bool>(
                name: "RequiresManualConfirmation",
                table: "UserPaymentMethods",
                type: "bit",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "Type",
                table: "UserPaymentMethods",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.Sql(@"
UPDATE upm
SET
    PaymentMethodCode = pm.Code,
    PaymentMethodName = pm.Name,
    PaymentMethodDescription = pm.Description,
    Type = pm.Type,
    RequiresManualConfirmation = pm.RequiresManualConfirmation
FROM UserPaymentMethods upm
INNER JOIN PaymentMethods pm ON pm.Id = upm.PaymentMethodId;

DECLARE @MissingPaymentMethods TABLE
(
    PaymentId uniqueidentifier PRIMARY KEY,
    UserPaymentMethodId uniqueidentifier NOT NULL,
    UserId uniqueidentifier NOT NULL,
    PaymentMethodId int NOT NULL,
    PaymentMethodCode nvarchar(30) NOT NULL,
    PaymentMethodName nvarchar(80) NOT NULL,
    PaymentMethodDescription nvarchar(300) NULL,
    Type int NOT NULL,
    RequiresManualConfirmation bit NOT NULL
);

INSERT INTO @MissingPaymentMethods
(
    PaymentId,
    UserPaymentMethodId,
    UserId,
    PaymentMethodId,
    PaymentMethodCode,
    PaymentMethodName,
    PaymentMethodDescription,
    Type,
    RequiresManualConfirmation
)
SELECT
    p.Id,
    NEWID(),
    CASE
        WHEN pm.Type IN (1, 3) THEN COALESCE(t.DriverUserId, r.PassengerUserId)
        ELSE r.PassengerUserId
    END,
    pm.Id,
    pm.Code,
    pm.Name,
    pm.Description,
    pm.Type,
    pm.RequiresManualConfirmation
FROM Payments p
INNER JOIN PaymentMethods pm ON pm.Id = p.PaymentMethodId
INNER JOIN Reservations r ON r.Id = p.ReservationId
LEFT JOIN Trips t ON t.Id = r.TripId
WHERE p.UserPaymentMethodId IS NULL
  AND CASE
        WHEN pm.Type IN (1, 3) THEN COALESCE(t.DriverUserId, r.PassengerUserId)
        ELSE r.PassengerUserId
      END IS NOT NULL;

INSERT INTO UserPaymentMethods
(
    Id,
    UserId,
    PaymentMethodId,
    PaymentMethodCode,
    PaymentMethodName,
    PaymentMethodDescription,
    Type,
    RequiresManualConfirmation,
    Alias,
    IsDefault,
    IsActive,
    CreatedAt
)
SELECT
    UserPaymentMethodId,
    UserId,
    PaymentMethodId,
    PaymentMethodCode,
    PaymentMethodName,
    PaymentMethodDescription,
    Type,
    RequiresManualConfirmation,
    PaymentMethodName,
    0,
    1,
    GETUTCDATE()
FROM @MissingPaymentMethods;

UPDATE p
SET UserPaymentMethodId = m.UserPaymentMethodId
FROM Payments p
INNER JOIN @MissingPaymentMethods m ON m.PaymentId = p.Id;

IF EXISTS (SELECT 1 FROM Payments WHERE UserPaymentMethodId IS NULL)
BEGIN
    THROW 50001, 'No se pudo asociar todos los pagos existentes a UserPaymentMethods.', 1;
END;

UPDATE Payments SET Status = 4 WHERE Status = 6;
UPDATE Payments SET Status = 2 WHERE Status = 7;
UPDATE PaymentTransactions SET TransactionType = 3 WHERE TransactionType IN (3, 4);
");

            migrationBuilder.AlterColumn<Guid>(
                name: "UserPaymentMethodId",
                table: "Payments",
                type: "uniqueidentifier",
                nullable: false,
                oldClrType: typeof(Guid),
                oldType: "uniqueidentifier",
                oldNullable: true);

            migrationBuilder.DropColumn(
                name: "PaymentMethodId",
                table: "Payments");

            migrationBuilder.DropColumn(
                name: "RefundedAmount",
                table: "Payments");

            migrationBuilder.DropTable(
                name: "PaymentMethods");

            migrationBuilder.CreateIndex(
                name: "IX_UserPaymentMethods_UserId_PaymentMethodId_IsDefault",
                table: "UserPaymentMethods",
                columns: new[] { "UserId", "PaymentMethodId", "IsDefault" });

            migrationBuilder.AddForeignKey(
                name: "FK_Payments_UserPaymentMethods_UserPaymentMethodId",
                table: "Payments",
                column: "UserPaymentMethodId",
                principalTable: "UserPaymentMethods",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Payments_UserPaymentMethods_UserPaymentMethodId",
                table: "Payments");

            migrationBuilder.DropIndex(
                name: "IX_UserPaymentMethods_UserId_PaymentMethodId_IsDefault",
                table: "UserPaymentMethods");

            migrationBuilder.DropColumn(
                name: "PaymentMethodCode",
                table: "UserPaymentMethods");

            migrationBuilder.DropColumn(
                name: "PaymentMethodDescription",
                table: "UserPaymentMethods");

            migrationBuilder.DropColumn(
                name: "PaymentMethodName",
                table: "UserPaymentMethods");

            migrationBuilder.DropColumn(
                name: "RequiresManualConfirmation",
                table: "UserPaymentMethods");

            migrationBuilder.DropColumn(
                name: "Type",
                table: "UserPaymentMethods");

            migrationBuilder.AlterColumn<Guid>(
                name: "UserPaymentMethodId",
                table: "Payments",
                type: "uniqueidentifier",
                nullable: true,
                oldClrType: typeof(Guid),
                oldType: "uniqueidentifier");

            migrationBuilder.AddColumn<int>(
                name: "PaymentMethodId",
                table: "Payments",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<decimal>(
                name: "RefundedAmount",
                table: "Payments",
                type: "decimal(10,2)",
                nullable: false,
                defaultValue: 0m);

            migrationBuilder.CreateTable(
                name: "PaymentMethods",
                columns: table => new
                {
                    Id = table.Column<int>(type: "int", nullable: false)
                        .Annotation("SqlServer:Identity", "1, 1"),
                    Code = table.Column<string>(type: "nvarchar(30)", maxLength: 30, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()"),
                    Description = table.Column<string>(type: "nvarchar(300)", maxLength: 300, nullable: true),
                    IsActive = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    Name = table.Column<string>(type: "nvarchar(80)", maxLength: 80, nullable: false),
                    RequiresManualConfirmation = table.Column<bool>(type: "bit", nullable: false, defaultValue: false),
                    SupportsRefunds = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    Type = table.Column<int>(type: "int", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "datetime2", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_PaymentMethods", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "Refunds",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    PaymentId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ProcessedByUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    RequestedByUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    Amount = table.Column<decimal>(type: "decimal(10,2)", nullable: false),
                    CancellationDeadline = table.Column<DateTime>(type: "datetime2", nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()"),
                    IsWithinCancellationWindow = table.Column<bool>(type: "bit", nullable: false, defaultValue: true),
                    MinutesBeforeTripStart = table.Column<int>(type: "int", nullable: true),
                    ProcessedAt = table.Column<DateTime>(type: "datetime2", nullable: true),
                    Reason = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    RejectionReason = table.Column<string>(type: "nvarchar(500)", maxLength: 500, nullable: true),
                    RequestedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()"),
                    Status = table.Column<int>(type: "int", nullable: false, defaultValue: 1)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Refunds", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Refunds_Payments_PaymentId",
                        column: x => x.PaymentId,
                        principalTable: "Payments",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_Refunds_Users_ProcessedByUserId",
                        column: x => x.ProcessedByUserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_Refunds_Users_RequestedByUserId",
                        column: x => x.RequestedByUserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.InsertData(
                table: "PaymentMethods",
                columns: new[] { "Id", "Code", "CreatedAt", "Description", "IsActive", "Name", "RequiresManualConfirmation", "Type", "UpdatedAt" },
                values: new object[] { 1, "CASH", new DateTime(2026, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc), "Pago en efectivo confirmado por el conductor.", true, "Efectivo", true, 1, null });

            migrationBuilder.InsertData(
                table: "PaymentMethods",
                columns: new[] { "Id", "Code", "CreatedAt", "Description", "IsActive", "Name", "SupportsRefunds", "Type", "UpdatedAt" },
                values: new object[] { 2, "CARD_SIM", new DateTime(2026, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc), "Pago con tarjeta en ambiente simulado para fines academicos.", true, "Tarjeta simulada", true, 2, null });

            migrationBuilder.InsertData(
                table: "PaymentMethods",
                columns: new[] { "Id", "Code", "CreatedAt", "Description", "IsActive", "Name", "RequiresManualConfirmation", "SupportsRefunds", "Type", "UpdatedAt" },
                values: new object[] { 3, "QR_BANK", new DateTime(2026, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc), "Pago mediante QR bancario del conductor, confirmado manualmente.", true, "QR bancario", true, true, 3, null });

            migrationBuilder.InsertData(
                table: "PaymentMethods",
                columns: new[] { "Id", "Code", "CreatedAt", "Description", "IsActive", "Name", "SupportsRefunds", "Type", "UpdatedAt" },
                values: new object[] { 4, "WALLET_SIM", new DateTime(2026, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc), "Saldo interno simulado para fines academicos.", true, "Billetera simulada", true, 4, null });

            migrationBuilder.CreateIndex(
                name: "IX_UserPaymentMethods_PaymentMethodId",
                table: "UserPaymentMethods",
                column: "PaymentMethodId");

            migrationBuilder.CreateIndex(
                name: "IX_Payments_PaymentMethodId",
                table: "Payments",
                column: "PaymentMethodId");

            migrationBuilder.CreateIndex(
                name: "IX_PaymentMethods_Code",
                table: "PaymentMethods",
                column: "Code",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Refunds_PaymentId",
                table: "Refunds",
                column: "PaymentId");

            migrationBuilder.CreateIndex(
                name: "IX_Refunds_ProcessedByUserId",
                table: "Refunds",
                column: "ProcessedByUserId");

            migrationBuilder.CreateIndex(
                name: "IX_Refunds_RequestedAt",
                table: "Refunds",
                column: "RequestedAt");

            migrationBuilder.CreateIndex(
                name: "IX_Refunds_RequestedByUserId",
                table: "Refunds",
                column: "RequestedByUserId");

            migrationBuilder.CreateIndex(
                name: "IX_Refunds_Status",
                table: "Refunds",
                column: "Status");

            migrationBuilder.AddForeignKey(
                name: "FK_Payments_PaymentMethods_PaymentMethodId",
                table: "Payments",
                column: "PaymentMethodId",
                principalTable: "PaymentMethods",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);

            migrationBuilder.AddForeignKey(
                name: "FK_Payments_UserPaymentMethods_UserPaymentMethodId",
                table: "Payments",
                column: "UserPaymentMethodId",
                principalTable: "UserPaymentMethods",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);

            migrationBuilder.AddForeignKey(
                name: "FK_UserPaymentMethods_PaymentMethods_PaymentMethodId",
                table: "UserPaymentMethods",
                column: "PaymentMethodId",
                principalTable: "PaymentMethods",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }
    }
}
