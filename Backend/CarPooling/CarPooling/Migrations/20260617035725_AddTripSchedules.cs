using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddTripSchedules : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "ScheduledDate",
                table: "Trips",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "TripScheduleId",
                table: "Trips",
                type: "uniqueidentifier",
                nullable: true);

            migrationBuilder.AddColumn<Guid>(
                name: "RecurringReservationId",
                table: "Reservations",
                type: "uniqueidentifier",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "TripSchedules",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DriverUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    OriginLocationId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DestinationLocationId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    DepartureTime = table.Column<TimeSpan>(type: "time", nullable: false),
                    DaysOfWeek = table.Column<string>(type: "nvarchar(50)", maxLength: 50, nullable: false),
                    StartDate = table.Column<DateTime>(type: "datetime2", nullable: false),
                    EndDate = table.Column<DateTime>(type: "datetime2", nullable: true),
                    VehicleId = table.Column<Guid>(type: "uniqueidentifier", nullable: true),
                    OfferedSeats = table.Column<int>(type: "int", nullable: false),
                    FareAmount = table.Column<decimal>(type: "decimal(10,2)", nullable: false),
                    IsActive = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_TripSchedules", x => x.Id);
                    table.ForeignKey(
                        name: "FK_TripSchedules_Locations_DestinationLocationId",
                        column: x => x.DestinationLocationId,
                        principalTable: "Locations",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_TripSchedules_Locations_OriginLocationId",
                        column: x => x.OriginLocationId,
                        principalTable: "Locations",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_TripSchedules_Users_DriverUserId",
                        column: x => x.DriverUserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                    table.ForeignKey(
                        name: "FK_TripSchedules_Vehicles_VehicleId",
                        column: x => x.VehicleId,
                        principalTable: "Vehicles",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateTable(
                name: "RecurringReservations",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TripScheduleId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    PassengerUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    SeatsReserved = table.Column<int>(type: "int", nullable: false),
                    IsActive = table.Column<bool>(type: "bit", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RecurringReservations", x => x.Id);
                    table.ForeignKey(
                        name: "FK_RecurringReservations_TripSchedules_TripScheduleId",
                        column: x => x.TripScheduleId,
                        principalTable: "TripSchedules",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_RecurringReservations_Users_PassengerUserId",
                        column: x => x.PassengerUserId,
                        principalTable: "Users",
                        principalColumn: "Id");
                });

            migrationBuilder.CreateIndex(
                name: "IX_Trips_TripScheduleId",
                table: "Trips",
                column: "TripScheduleId");

            migrationBuilder.CreateIndex(
                name: "IX_Reservations_RecurringReservationId",
                table: "Reservations",
                column: "RecurringReservationId");

            migrationBuilder.CreateIndex(
                name: "IX_RecurringReservations_PassengerUserId",
                table: "RecurringReservations",
                column: "PassengerUserId");

            migrationBuilder.CreateIndex(
                name: "IX_RecurringReservations_TripScheduleId",
                table: "RecurringReservations",
                column: "TripScheduleId");

            migrationBuilder.CreateIndex(
                name: "IX_TripSchedules_DestinationLocationId",
                table: "TripSchedules",
                column: "DestinationLocationId");

            migrationBuilder.CreateIndex(
                name: "IX_TripSchedules_DriverUserId",
                table: "TripSchedules",
                column: "DriverUserId");

            migrationBuilder.CreateIndex(
                name: "IX_TripSchedules_OriginLocationId",
                table: "TripSchedules",
                column: "OriginLocationId");

            migrationBuilder.CreateIndex(
                name: "IX_TripSchedules_VehicleId",
                table: "TripSchedules",
                column: "VehicleId");

            migrationBuilder.AddForeignKey(
                name: "FK_Reservations_RecurringReservations_RecurringReservationId",
                table: "Reservations",
                column: "RecurringReservationId",
                principalTable: "RecurringReservations",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);

            migrationBuilder.AddForeignKey(
                name: "FK_Trips_TripSchedules_TripScheduleId",
                table: "Trips",
                column: "TripScheduleId",
                principalTable: "TripSchedules",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Reservations_RecurringReservations_RecurringReservationId",
                table: "Reservations");

            migrationBuilder.DropForeignKey(
                name: "FK_Trips_TripSchedules_TripScheduleId",
                table: "Trips");

            migrationBuilder.DropTable(
                name: "RecurringReservations");

            migrationBuilder.DropTable(
                name: "TripSchedules");

            migrationBuilder.DropIndex(
                name: "IX_Trips_TripScheduleId",
                table: "Trips");

            migrationBuilder.DropIndex(
                name: "IX_Reservations_RecurringReservationId",
                table: "Reservations");

            migrationBuilder.DropColumn(
                name: "ScheduledDate",
                table: "Trips");

            migrationBuilder.DropColumn(
                name: "TripScheduleId",
                table: "Trips");

            migrationBuilder.DropColumn(
                name: "RecurringReservationId",
                table: "Reservations");
        }
    }
}
