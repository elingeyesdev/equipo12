using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddSupportTicketReservationId : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<Guid>(
                name: "ReservationId",
                table: "SupportTickets",
                type: "uniqueidentifier",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_SupportTickets_ReservationId",
                table: "SupportTickets",
                column: "ReservationId");

            migrationBuilder.CreateIndex(
                name: "IX_SupportTickets_UserId_Category_ReservationId_Status",
                table: "SupportTickets",
                columns: new[] { "UserId", "Category", "ReservationId", "Status" });

            migrationBuilder.CreateIndex(
                name: "IX_SupportTickets_UserId_Category_TripId_Status",
                table: "SupportTickets",
                columns: new[] { "UserId", "Category", "TripId", "Status" });

            migrationBuilder.AddForeignKey(
                name: "FK_SupportTickets_Reservations_ReservationId",
                table: "SupportTickets",
                column: "ReservationId",
                principalTable: "Reservations",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_SupportTickets_Reservations_ReservationId",
                table: "SupportTickets");

            migrationBuilder.DropIndex(
                name: "IX_SupportTickets_ReservationId",
                table: "SupportTickets");

            migrationBuilder.DropIndex(
                name: "IX_SupportTickets_UserId_Category_ReservationId_Status",
                table: "SupportTickets");

            migrationBuilder.DropIndex(
                name: "IX_SupportTickets_UserId_Category_TripId_Status",
                table: "SupportTickets");

            migrationBuilder.DropColumn(
                name: "ReservationId",
                table: "SupportTickets");
        }
    }
}
