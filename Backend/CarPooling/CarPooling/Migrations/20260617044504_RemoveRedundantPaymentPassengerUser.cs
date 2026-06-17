using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class RemoveRedundantPaymentPassengerUser : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Payments_Users_PassengerUserId",
                table: "Payments");

            migrationBuilder.DropIndex(
                name: "IX_Payments_PassengerUserId",
                table: "Payments");

            migrationBuilder.DropColumn(
                name: "PassengerUserId",
                table: "Payments");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<Guid>(
                name: "PassengerUserId",
                table: "Payments",
                type: "uniqueidentifier",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.CreateIndex(
                name: "IX_Payments_PassengerUserId",
                table: "Payments",
                column: "PassengerUserId");

            migrationBuilder.AddForeignKey(
                name: "FK_Payments_Users_PassengerUserId",
                table: "Payments",
                column: "PassengerUserId",
                principalTable: "Users",
                principalColumn: "Id",
                onDelete: ReferentialAction.Restrict);
        }
    }
}
