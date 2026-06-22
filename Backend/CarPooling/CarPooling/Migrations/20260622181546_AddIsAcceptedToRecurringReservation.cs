using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddIsAcceptedToRecurringReservation : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "IsAccepted",
                table: "RecurringReservations",
                type: "bit",
                nullable: false,
                defaultValue: false);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "IsAccepted",
                table: "RecurringReservations");
        }
    }
}
