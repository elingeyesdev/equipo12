using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddTripFareAmount : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<decimal>(
                name: "FareAmount",
                table: "Trips",
                type: "decimal(10,2)",
                nullable: false,
                defaultValue: 10m);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "FareAmount",
                table: "Trips");
        }
    }
}
