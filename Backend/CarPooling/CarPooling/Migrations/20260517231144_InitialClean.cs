using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class InitialClean : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "UserHistoryHiddenTrips");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "UserHistoryHiddenTrips",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TripId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    HiddenAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_UserHistoryHiddenTrips", x => x.Id);
                    table.ForeignKey(
                        name: "FK_UserHistoryHiddenTrips_Trips_TripId",
                        column: x => x.TripId,
                        principalTable: "Trips",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_UserHistoryHiddenTrips_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
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
    }
}
