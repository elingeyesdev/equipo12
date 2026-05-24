using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddTripRatingTags : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "Tags",
                table: "TripRatings",
                type: "nvarchar(500)",
                maxLength: 500,
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Tags",
                table: "TripRatings");
        }
    }
}
