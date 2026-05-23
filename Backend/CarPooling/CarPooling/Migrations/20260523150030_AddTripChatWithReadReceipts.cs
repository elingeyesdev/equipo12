using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddTripChatWithReadReceipts : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "TripChats",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TripId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_TripChats", x => x.Id);
                    table.ForeignKey(
                        name: "FK_TripChats_Trips_TripId",
                        column: x => x.TripId,
                        principalTable: "Trips",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "TripChatMessages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ChatId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    SenderUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    MessageText = table.Column<string>(type: "nvarchar(max)", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_TripChatMessages", x => x.Id);
                    table.ForeignKey(
                        name: "FK_TripChatMessages_TripChats_ChatId",
                        column: x => x.ChatId,
                        principalTable: "TripChats",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_TripChatMessages_Users_SenderUserId",
                        column: x => x.SenderUserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

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

            migrationBuilder.CreateIndex(
                name: "IX_TripChatMessages_ChatId",
                table: "TripChatMessages",
                column: "ChatId");

            migrationBuilder.CreateIndex(
                name: "IX_TripChatMessages_CreatedAt",
                table: "TripChatMessages",
                column: "CreatedAt");

            migrationBuilder.CreateIndex(
                name: "IX_TripChatMessages_SenderUserId",
                table: "TripChatMessages",
                column: "SenderUserId");

            migrationBuilder.CreateIndex(
                name: "IX_TripChats_TripId",
                table: "TripChats",
                column: "TripId",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "TripChatMessageReads");

            migrationBuilder.DropTable(
                name: "TripChatMessages");

            migrationBuilder.DropTable(
                name: "TripChats");
        }
    }
}
