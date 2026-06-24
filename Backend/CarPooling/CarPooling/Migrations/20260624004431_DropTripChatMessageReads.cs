using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class DropTripChatMessageReads : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "TripChatMessageReads");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "TripChatMessageReads",
                columns: table => new
                {
                    MessageId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ReadAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_TripChatMessageReads", x => new { x.MessageId, x.UserId });
                    table.ForeignKey(
                        name: "FK_TripChatMessageReads_TripChatMessages_MessageId",
                        column: x => x.MessageId,
                        principalTable: "TripChatMessages",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_TripChatMessageReads_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_TripChatMessageReads_UserId",
                table: "TripChatMessageReads",
                column: "UserId");
        }
    }
}
