/*
  CarPoolingDB ya existe (Users, Trips, etc.) pero "dotnet ef database update"
  falla al intentar CREATE TABLE [Users] otra vez.

  Causa: la tabla __EFMigrationsHistory no tiene registradas las migraciones
  anteriores, o esta vacia. EF vuelve a ejecutar InitialCreate.

  Este script:
  1) Marca como aplicadas las migraciones previas (si faltan en el historial).
  2) Crea la tabla SafeZones si no existe.
  3) Registra la migracion AddSafeZones.

  Ejecutar en SSMS o Azure Data Studio contra CarPoolingDB (LocalDB).
*/

USE [CarPoolingDB];
GO

SET NOCOUNT ON;

DECLARE @ProductVersion NVARCHAR(32) = N'10.0.5';

-- Solo rellenar historial si la BD ya tiene Users (esquema viejo) y falta InitialCreate
IF OBJECT_ID(N'dbo.Users', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM [dbo].[__EFMigrationsHistory] WHERE [MigrationId] = N'20260517135431_InitialCreate')
BEGIN
    PRINT 'Sincronizando __EFMigrationsHistory con migraciones ya aplicadas en esta BD...';

    INSERT INTO [dbo].[__EFMigrationsHistory] ([MigrationId], [ProductVersion])
    SELECT v.[MigrationId], @ProductVersion
    FROM (VALUES
        (N'20260517135431_InitialCreate'),
        (N'20260517231144_InitialClean'),
        (N'20260518005148_InitialSchema'),
        (N'20260523150030_AddTripChatWithReadReceipts'),
        (N'20260523225750_AddTripRatingsTable'),
        (N'20260524030725_AddTripRatingTags'),
        (N'20260525030355_AddSupportTickets'),
        (N'20260525050134_AddSupportTicketReservationId'),
        (N'20260529005405_AddAppSettings')
    ) AS v([MigrationId])
    WHERE NOT EXISTS (
        SELECT 1 FROM [dbo].[__EFMigrationsHistory] h WHERE h.[MigrationId] = v.[MigrationId]
    );
END
GO

IF OBJECT_ID(N'dbo.SafeZones', N'U') IS NULL
BEGIN
    PRINT 'Creando tabla SafeZones...';

    CREATE TABLE [dbo].[SafeZones] (
        [Id] uniqueidentifier NOT NULL,
        [Name] nvarchar(120) NOT NULL,
        [Description] nvarchar(400) NULL,
        [Latitude] float NOT NULL,
        [Longitude] float NOT NULL,
        [AddressLabel] nvarchar(200) NULL,
        [Purpose] int NOT NULL CONSTRAINT [DF_SafeZones_Purpose] DEFAULT 0,
        [IsActive] bit NOT NULL CONSTRAINT [DF_SafeZones_IsActive] DEFAULT CAST(1 AS bit),
        [DisplayOrder] int NOT NULL CONSTRAINT [DF_SafeZones_DisplayOrder] DEFAULT 0,
        [CampusArea] nvarchar(80) NULL,
        [CreatedAt] datetime2 NOT NULL CONSTRAINT [DF_SafeZones_CreatedAt] DEFAULT (GETUTCDATE()),
        [UpdatedAt] datetime2 NULL,
        CONSTRAINT [PK_SafeZones] PRIMARY KEY ([Id])
    );

    CREATE INDEX [IX_SafeZones_DisplayOrder] ON [dbo].[SafeZones] ([DisplayOrder]);
    CREATE INDEX [IX_SafeZones_IsActive] ON [dbo].[SafeZones] ([IsActive]);
END
ELSE
    PRINT 'La tabla SafeZones ya existe.';
GO

IF NOT EXISTS (SELECT 1 FROM [dbo].[__EFMigrationsHistory] WHERE [MigrationId] = N'20260601232805_AddSafeZones')
BEGIN
    INSERT INTO [dbo].[__EFMigrationsHistory] ([MigrationId], [ProductVersion])
    VALUES (N'20260601232805_AddSafeZones', N'10.0.5');
    PRINT 'Migracion AddSafeZones registrada.';
END
GO

PRINT 'Listo. Verifica:';
SELECT [MigrationId] FROM [dbo].[__EFMigrationsHistory] ORDER BY [MigrationId];
GO
