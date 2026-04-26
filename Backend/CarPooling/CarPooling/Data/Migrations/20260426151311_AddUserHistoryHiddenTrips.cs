using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddUserHistoryHiddenTrips : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "UserHistoryHiddenTrips",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TripId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    HiddenAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_UserHistoryHiddenTrips", x => x.Id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_UserHistoryHiddenTrips_TripId",
                table: "UserHistoryHiddenTrips",
                column: "TripId");

            migrationBuilder.CreateIndex(
                name: "IX_UserHistoryHiddenTrips_UserId",
                table: "UserHistoryHiddenTrips",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_UserHistoryHiddenTrips_UserId_TripId",
                table: "UserHistoryHiddenTrips",
                columns: new[] { "UserId", "TripId" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "UserHistoryHiddenTrips");
        }
    }
}
