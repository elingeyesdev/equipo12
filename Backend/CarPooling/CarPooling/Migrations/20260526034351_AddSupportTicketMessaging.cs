using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace CarPooling.Migrations
{
    /// <inheritdoc />
    public partial class AddSupportTicketMessaging : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<Guid>(
                name: "AssignedAdminUserId",
                table: "SupportTickets",
                type: "uniqueidentifier",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "ClosedAt",
                table: "SupportTickets",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "FirstAdminReplyAt",
                table: "SupportTickets",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "LastMessageAt",
                table: "SupportTickets",
                type: "datetime2",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "SupportTicketMessages",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    TicketId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    SenderUserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    SenderKind = table.Column<int>(type: "int", nullable: false),
                    MessageText = table.Column<string>(type: "nvarchar(2000)", maxLength: 2000, nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SupportTicketMessages", x => x.Id);
                    table.ForeignKey(
                        name: "FK_SupportTicketMessages_SupportTickets_TicketId",
                        column: x => x.TicketId,
                        principalTable: "SupportTickets",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_SupportTicketMessages_Users_SenderUserId",
                        column: x => x.SenderUserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "SupportTicketMessageReads",
                columns: table => new
                {
                    MessageId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    UserId = table.Column<Guid>(type: "uniqueidentifier", nullable: false),
                    ReadAt = table.Column<DateTime>(type: "datetime2", nullable: false, defaultValueSql: "GETUTCDATE()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SupportTicketMessageReads", x => new { x.MessageId, x.UserId });
                    table.ForeignKey(
                        name: "FK_SupportTicketMessageReads_SupportTicketMessages_MessageId",
                        column: x => x.MessageId,
                        principalTable: "SupportTicketMessages",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_SupportTicketMessageReads_Users_UserId",
                        column: x => x.UserId,
                        principalTable: "Users",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "IX_SupportTickets_AssignedAdminUserId",
                table: "SupportTickets",
                column: "AssignedAdminUserId");

            migrationBuilder.CreateIndex(
                name: "IX_SupportTickets_UserId_FirstAdminReplyAt",
                table: "SupportTickets",
                columns: new[] { "UserId", "FirstAdminReplyAt" });

            migrationBuilder.CreateIndex(
                name: "IX_SupportTicketMessageReads_UserId",
                table: "SupportTicketMessageReads",
                column: "UserId");

            migrationBuilder.CreateIndex(
                name: "IX_SupportTicketMessages_SenderUserId",
                table: "SupportTicketMessages",
                column: "SenderUserId");

            migrationBuilder.CreateIndex(
                name: "IX_SupportTicketMessages_TicketId",
                table: "SupportTicketMessages",
                column: "TicketId");

            migrationBuilder.CreateIndex(
                name: "IX_SupportTicketMessages_TicketId_CreatedAt",
                table: "SupportTicketMessages",
                columns: new[] { "TicketId", "CreatedAt" });

            migrationBuilder.AddForeignKey(
                name: "FK_SupportTickets_Users_AssignedAdminUserId",
                table: "SupportTickets",
                column: "AssignedAdminUserId",
                principalTable: "Users",
                principalColumn: "Id",
                onDelete: ReferentialAction.SetNull);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_SupportTickets_Users_AssignedAdminUserId",
                table: "SupportTickets");

            migrationBuilder.DropTable(
                name: "SupportTicketMessageReads");

            migrationBuilder.DropTable(
                name: "SupportTicketMessages");

            migrationBuilder.DropIndex(
                name: "IX_SupportTickets_AssignedAdminUserId",
                table: "SupportTickets");

            migrationBuilder.DropIndex(
                name: "IX_SupportTickets_UserId_FirstAdminReplyAt",
                table: "SupportTickets");

            migrationBuilder.DropColumn(
                name: "AssignedAdminUserId",
                table: "SupportTickets");

            migrationBuilder.DropColumn(
                name: "ClosedAt",
                table: "SupportTickets");

            migrationBuilder.DropColumn(
                name: "FirstAdminReplyAt",
                table: "SupportTickets");

            migrationBuilder.DropColumn(
                name: "LastMessageAt",
                table: "SupportTickets");
        }
    }
}
