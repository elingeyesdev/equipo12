using System.Security.Cryptography;
using System.Text;
using CarPooling.Models;
using CarPooling.Security;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Data;

public static class DevelopmentDataSeeder
{
    private static readonly Guid AdminId = Guid.Parse("11111111-1111-1111-1111-111111111111");
    private static readonly Guid DriverOneId = Guid.Parse("22222222-2222-2222-2222-222222222222");
    private static readonly Guid DriverTwoId = Guid.Parse("33333333-3333-3333-3333-333333333333");
    private static readonly Guid DriverThreeId = Guid.Parse("33333333-3333-3333-3333-333333333334");
    private static readonly Guid PassengerOneId = Guid.Parse("44444444-4444-4444-4444-444444444444");
    private static readonly Guid PassengerTwoId = Guid.Parse("55555555-5555-5555-5555-555555555555");
    private static readonly Guid PassengerThreeId = Guid.Parse("66666666-6666-6666-6666-666666666666");
    private static readonly Guid PassengerFourId = Guid.Parse("66666666-6666-6666-6666-666666666667");
    private static readonly Guid PassengerFiveId = Guid.Parse("66666666-6666-6666-6666-666666666668");
    private static readonly Guid AnalystId = Guid.Parse("77777777-7777-7777-7777-777777777777");


    private static readonly Guid VehicleOneId = Guid.Parse("99999999-9999-9999-9999-999999999991");
    private static readonly Guid VehicleTwoId = Guid.Parse("99999999-9999-9999-9999-999999999992");
    private static readonly Guid VehicleThreeId = Guid.Parse("99999999-9999-9999-9999-999999999993");

    private static readonly Guid LocUnivalleId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static readonly Guid LocPlazaId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static readonly Guid LocEquipetrolId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static readonly Guid LocBuschId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4");
    private static readonly Guid LocBimodalId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5");
    private static readonly Guid LocVenturaId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6");
    private static readonly Guid LocCristoId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7");
    private static readonly Guid LocLasPalmasId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8");
    private static readonly Guid LocPlan3000Id = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9");
    private static readonly Guid LocHamacasId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10");
    private static readonly Guid LocLibraryBookmarkId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11");
    private static readonly Guid LocHomeBookmarkId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12");

    private static readonly Guid ZoneUnivalleId = Guid.Parse("abababab-abab-abab-abab-ababababab01");
    private static readonly Guid ZonePlazaId = Guid.Parse("abababab-abab-abab-abab-ababababab02");
    private static readonly Guid ZoneEquipetrolId = Guid.Parse("abababab-abab-abab-abab-ababababab03");
    private static readonly Guid ZoneBimodalId = Guid.Parse("abababab-abab-abab-abab-ababababab04");
    private static readonly Guid ZoneVenturaId = Guid.Parse("abababab-abab-abab-abab-ababababab05");
    private static readonly Guid ZoneCristoId = Guid.Parse("abababab-abab-abab-abab-ababababab06");
    private static readonly Guid ZoneLasPalmasId = Guid.Parse("abababab-abab-abab-abab-ababababab07");
    private static readonly Guid ZonePlan3000Id = Guid.Parse("abababab-abab-abab-abab-ababababab08");

    private static readonly Guid TripScheduledId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
    private static readonly Guid TripReadyId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private static readonly Guid TripInProgressId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");
    private static readonly Guid TripFinishedId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4");
    private static readonly Guid TripCancelledId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5");
    private static readonly Guid TripRefundableId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6");
    private static readonly Guid TripFullId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7");
    private static readonly Guid TripMorningFinishedId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8");
    private static readonly Guid BookmarkPlaceId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbf1");
    private static readonly Guid BookmarkRouteId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbf2");

    private static readonly Guid ResOneId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc1");
    private static readonly Guid ResTwoId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc2");
    private static readonly Guid ResThreeId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc3");
    private static readonly Guid ResFourId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc4");
    private static readonly Guid ResFiveId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc5");
    private static readonly Guid ResSixId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc6");
    private static readonly Guid ResSevenId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc7");
    private static readonly Guid ResEightId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc8");
    private static readonly Guid ResNineId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc9");
    private static readonly Guid ResTenId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc10");
    private static readonly Guid ResElevenId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc11");
    private static readonly Guid ResTwelveId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc12");
    private static readonly Guid ResThirteenId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc13");
    private static readonly Guid ResFourteenId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc14");
    private static readonly Guid ResFifteenId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccc15");

    private static readonly Guid DriverOneQrMethodId = Guid.Parse("abab1111-1111-1111-1111-111111111111");
    private static readonly Guid DriverTwoQrMethodId = Guid.Parse("abab2222-2222-2222-2222-222222222222");
    private static readonly Guid PassengerCardMethodId = Guid.Parse("abab3333-3333-3333-3333-333333333333");
    private static readonly Guid PassengerWalletMethodId = Guid.Parse("abab4444-4444-4444-4444-444444444444");

    private static readonly Guid PayOneId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static readonly Guid PayTwoId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd2");
    private static readonly Guid PayThreeId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd3");
    private static readonly Guid PayFourId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd4");
    private static readonly Guid PayFiveId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd5");
    private static readonly Guid PaySixId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd6");
    private static readonly Guid PaySevenId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd7");
    private static readonly Guid RefundOneId = Guid.Parse("dadadada-dada-dada-dada-dadadadada01");

    private static readonly Guid ChatActiveId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
    private static readonly Guid ChatFinishedId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2");

    private static readonly Guid TicketOneId = Guid.Parse("22223333-4444-5555-6666-777788889991");
    private static readonly Guid TicketTwoId = Guid.Parse("22223333-4444-5555-6666-777788889992");
    private static readonly Guid TicketThreeId = Guid.Parse("22223333-4444-5555-6666-777788889993");

    public static async Task SeedAsync(IServiceProvider services)
    {
        using var scope = services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();

        if (context.Database.IsRelational())
        {
            await context.Database.MigrateAsync();
        }
        else
        {
            await context.Database.EnsureCreatedAsync();
        }

        await SeedPermissionsAndRolesAsync(context);
        await ClearDemoDataAsync(context);
        await SeedDemoDataAsync(context);
        await SeedDefaultThemeAsync(context);
    }

    private static async Task SeedPermissionsAndRolesAsync(CarPoolingContext context)
    {
        foreach (var permission in AppPermissions.AllPermissions)
        {
            if (!await context.Permissions.AnyAsync(p => p.Id == permission.Id))
            {
                context.Permissions.Add(new Permission
                {
                    Id = permission.Id,
                    Name = permission.Name,
                    GroupName = permission.GroupName
                });
            }
        }

        await context.SaveChangesAsync();

        var rolesToSeed = new[]
        {
            new { Name = "SuperAdmin", Description = "Acceso total al sistema", IsSystemRole = true },
            new { Name = "Student", Description = "Rol por defecto para estudiantes", IsSystemRole = true },
            new { Name = "Driver", Description = "Rol por defecto para conductores", IsSystemRole = true },
            new { Name = "Analyst", Description = "Rol limitado para visualizar metricas", IsSystemRole = false }
        };

        foreach (var roleSeed in rolesToSeed)
        {
            var role = await context.Roles
                .Include(r => r.RolePermissions)
                .FirstOrDefaultAsync(r => r.Name == roleSeed.Name);

            if (role is null)
            {
                role = new Role
                {
                    Id = Guid.NewGuid(),
                    Name = roleSeed.Name,
                    Description = roleSeed.Description,
                    IsSystemRole = roleSeed.IsSystemRole
                };
                context.Roles.Add(role);
                await context.SaveChangesAsync();
            }

            if (roleSeed.Name == "SuperAdmin")
            {
                var permissions = await context.Permissions.ToListAsync();
                foreach (var permission in permissions)
                {
                    if (!role.RolePermissions.Any(rp => rp.PermissionId == permission.Id))
                    {
                        context.RolePermissions.Add(new RolePermission
                        {
                            RoleId = role.Id,
                            PermissionId = permission.Id
                        });
                    }
                }
            }
            else if (roleSeed.Name == "Analyst")
            {
                var analystPermissions = new[] { AppPermissions.ViewMetrics, AppPermissions.ReadUsers, AppPermissions.ReadTrips };
                foreach (var permissionId in analystPermissions)
                {
                    if (!role.RolePermissions.Any(rp => rp.PermissionId == permissionId))
                    {
                        context.RolePermissions.Add(new RolePermission
                        {
                            RoleId = role.Id,
                            PermissionId = permissionId
                        });
                    }
                }
            }
        }

        await context.SaveChangesAsync();
    }

    private static async Task ClearDemoDataAsync(CarPoolingContext context)
    {
        context.Refunds.RemoveRange(context.Refunds);
        context.PaymentTransactions.RemoveRange(context.PaymentTransactions);
        context.Payments.RemoveRange(context.Payments);
        context.SupportTicketMessages.RemoveRange(context.SupportTicketMessages);
        context.SupportTickets.RemoveRange(context.SupportTickets);
        context.TripRatings.RemoveRange(context.TripRatings);
        context.TripChatMessageReads.RemoveRange(context.TripChatMessageReads);
        context.TripChatMessages.RemoveRange(context.TripChatMessages);
        context.TripChats.RemoveRange(context.TripChats);
        context.Reservations.RemoveRange(context.Reservations);
        context.Trips.RemoveRange(context.Trips);
        context.UserDevices.RemoveRange(context.UserDevices);
        context.UserPaymentMethods.RemoveRange(context.UserPaymentMethods);
        context.Vehicles.RemoveRange(context.Vehicles);
        context.UserRoles.RemoveRange(context.UserRoles);
        context.Users.RemoveRange(context.Users);
        context.SafeZones.RemoveRange(context.SafeZones);
        context.Locations.RemoveRange(context.Locations);
        context.UserBookmarks.RemoveRange(context.UserBookmarks);

        await context.SaveChangesAsync();
    }

    private static async Task SeedDemoDataAsync(CarPoolingContext context)
    {
        var now = DateTime.UtcNow;
        var passwordHash = HashPassword("123456");

        var roleSuperAdmin = await context.Roles.FirstAsync(r => r.Name == "SuperAdmin");
        var roleDriver = await context.Roles.FirstAsync(r => r.Name == "Driver");
        var roleStudent = await context.Roles.FirstAsync(r => r.Name == "Student");
        var roleAnalyst = await context.Roles.FirstAsync(r => r.Name == "Analyst");

        var users = new[]
        {
            DemoUser(AdminId, "Administrador Univalle", "admin@univalle.edu", "71011111", passwordHash, now.AddMonths(-5)),
            DemoUser(DriverOneId, "Juan Perez", "conductor1@univalle.edu", "71020304", passwordHash, now.AddMonths(-4)),
            DemoUser(DriverTwoId, "Maria Gomez", "conductor2@univalle.edu", "72030405", passwordHash, now.AddMonths(-4).AddDays(2)),
            DemoUser(DriverThreeId, "Rodrigo Vaca", "conductor3@univalle.edu", "72030406", passwordHash, now.AddMonths(-2)),
            DemoUser(PassengerOneId, "Carlos Rojas", "estudiante1@univalle.edu", "73040506", passwordHash, now.AddMonths(-3)),
            DemoUser(PassengerTwoId, "Ana Torres", "estudiante2@univalle.edu", "74050607", passwordHash, now.AddMonths(-3).AddDays(4)),
            DemoUser(PassengerThreeId, "Luis Castro", "estudiante3@univalle.edu", "75060708", passwordHash, now.AddMonths(-2)),
            DemoUser(PassengerFourId, "Valeria Suarez", "estudiante4@univalle.edu", "76011122", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerFiveId, "Diego Molina", "estudiante5@univalle.edu", "76033344", passwordHash, now.AddMonths(-1).AddDays(5)),
            DemoUser(AnalystId, "Sofia Morales", "analista@univalle.edu", "76070809", passwordHash, now.AddMonths(-2).AddDays(8))
        };

        context.Users.AddRange(users);
        await context.SaveChangesAsync();

        context.UserRoles.AddRange(
            new UserRole { UserId = AdminId, RoleId = roleSuperAdmin.Id },
            new UserRole { UserId = DriverOneId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverTwoId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverThreeId, RoleId = roleDriver.Id },
            new UserRole { UserId = PassengerOneId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerTwoId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerThreeId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerFourId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerFiveId, RoleId = roleStudent.Id },
            new UserRole { UserId = AnalystId, RoleId = roleAnalyst.Id });

        context.Vehicles.AddRange(
            new Vehicle
            {
                Id = VehicleOneId,
                OwnerUserId = DriverOneId,
                LicensePlate = "4567-XYZ",
                Brand = "Toyota",
                Model = "Corolla",
                Color = "Blanco",
                VehicleYear = 2021,
                TotalSeats = 4,
                IsActive = true,
                IsVerified = true,
                CreatedAt = now.AddDays(-78)
            },
            new Vehicle
            {
                Id = VehicleTwoId,
                OwnerUserId = DriverTwoId,
                LicensePlate = "1234-ABC",
                Brand = "Suzuki",
                Model = "Swift",
                Color = "Gris",
                VehicleYear = 2020,
                TotalSeats = 4,
                IsActive = true,
                IsVerified = true,
                CreatedAt = now.AddDays(-62)
            },
            new Vehicle
            {
                Id = VehicleThreeId,
                OwnerUserId = DriverThreeId,
                LicensePlate = "7890-TRD",
                Brand = "Nissan",
                Model = "Kicks",
                Color = "Rojo",
                VehicleYear = 2022,
                TotalSeats = 4,
                IsActive = true,
                IsVerified = true,
                CreatedAt = now.AddDays(-28)
            });

        await context.SaveChangesAsync();

        SeedLocationsAndSafeZones(context, now);
        await context.SaveChangesAsync();

        SeedTrips(context, now);
        await context.SaveChangesAsync();

        SeedReservations(context, now);
        await context.SaveChangesAsync();

        SeedPaymentMethodsForUsers(context, now);
        await context.SaveChangesAsync();

        SeedPayments(context, now);
        await context.SaveChangesAsync();

        SeedChats(context, now);
        await context.SaveChangesAsync();

        SeedRatings(context, now);
        await context.SaveChangesAsync();

        SeedSupport(context, now);
        SeedDevices(context, now);
        await context.SaveChangesAsync();
    }

    private static User DemoUser(Guid id, string fullName, string email, string phone, string passwordHash, DateTime createdAt)
    {
        return new User
        {
            Id = id,
            FullName = fullName,
            Email = email,
            PasswordHash = passwordHash,
            PhoneNumber = phone,
            CreatedAt = createdAt
        };
    }


    private static void SeedLocationsAndSafeZones(CarPoolingContext context, DateTime now)
    {
        context.Locations.AddRange(
            new Location { Id = LocUnivalleId, Latitude = -17.74797, Longitude = -63.16611, AddressLabel = "Campus Univalle Santa Cruz, Av. Banzer Km 8", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocPlazaId, Latitude = -17.78330, Longitude = -63.18210, AddressLabel = "Plaza 24 de Septiembre, Centro", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocEquipetrolId, Latitude = -17.76560, Longitude = -63.19320, AddressLabel = "Av. San Martin, Barrio Equipetrol", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocBuschId, Latitude = -17.77120, Longitude = -63.18540, AddressLabel = "Avenida Busch y Segundo Anillo", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocBimodalId, Latitude = -17.79410, Longitude = -63.16640, AddressLabel = "Terminal Bimodal, Santa Cruz", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocVenturaId, Latitude = -17.75490, Longitude = -63.19820, AddressLabel = "Ventura Mall, Cuarto Anillo", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocCristoId, Latitude = -17.76940, Longitude = -63.18070, AddressLabel = "Monumento Cristo Redentor", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocLasPalmasId, Latitude = -17.79020, Longitude = -63.20520, AddressLabel = "Barrio Las Palmas", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocPlan3000Id, Latitude = -17.83050, Longitude = -63.13810, AddressLabel = "Plan 3000, Rotonda principal", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocHamacasId, Latitude = -17.73650, Longitude = -63.18100, AddressLabel = "Zona Hamacas, Av. Banzer", CreatedAt = now.AddDays(-20) },
            new Location { Id = LocLibraryBookmarkId, Latitude = -17.74880, Longitude = -63.16560, AddressLabel = "Biblioteca central Univalle", CreatedAt = now.AddDays(-10) },
            new Location { Id = LocHomeBookmarkId, Latitude = -17.78120, Longitude = -63.17510, AddressLabel = "Casa de Carlos, zona Centro", CreatedAt = now.AddDays(-10) });

        context.SafeZones.AddRange(
            DemoSafeZone(ZoneUnivalleId, "Campus Univalle Banzer", "Puerta principal del campus para recoger y dejar pasajeros.", -17.74797, -63.16611, "Campus", 1),
            DemoSafeZone(ZonePlazaId, "Plaza 24 de Septiembre", "Punto de encuentro visible sobre Libertad y Ayacucho.", -17.78330, -63.18210, "Centro", 2),
            DemoSafeZone(ZoneEquipetrolId, "Zona Equipetrol", "Parada segura cerca de Av. San Martin y Tercer Anillo.", -17.76560, -63.19320, "Equipetrol", 3),
            DemoSafeZone(ZoneBimodalId, "Terminal Bimodal", "Encuentro frente al acceso principal de boleterias.", -17.79410, -63.16640, "Bimodal", 4),
            DemoSafeZone(ZoneVenturaId, "Ventura Mall", "Zona iluminada de recojo sobre el Cuarto Anillo.", -17.75490, -63.19820, "Norte", 5),
            DemoSafeZone(ZoneCristoId, "Cristo Redentor", "Punto de referencia central para rutas cortas.", -17.76940, -63.18070, "Centro Norte", 6),
            DemoSafeZone(ZoneLasPalmasId, "Las Palmas", "Parada recomendada para estudiantes del suroeste.", -17.79020, -63.20520, "Suroeste", 7),
            DemoSafeZone(ZonePlan3000Id, "Plan 3000", "Punto seguro con alta demanda de pasajeros.", -17.83050, -63.13810, "Este", 8));
    }

    private static SafeZone DemoSafeZone(Guid id, string name, string description, double lat, double lon, string area, int order)
    {
        return new SafeZone
        {
            Id = id,
            Name = name,
            Description = description,
            Latitude = lat,
            Longitude = lon,
            AddressLabel = name,
            Purpose = SafeZonePurpose.Both,
            IsActive = true,
            DisplayOrder = order,
            CampusArea = area,
            CreatedAt = DateTime.UtcNow.AddDays(-20)
        };
    }

    private static void SeedTrips(CarPoolingContext context, DateTime now)
    {
        context.Trips.AddRange(
            new Trip
            {
                Id = TripScheduledId,
                OriginLocationId = LocPlazaId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 1,
                DriverUserId = DriverOneId,
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 3,
                FareAmount = 10m,
                CreatedAt = now.AddMinutes(-75),
                UpdatedAt = now.AddMinutes(-72)
            },
            new Trip
            {
                Id = TripReadyId,
                OriginLocationId = LocEquipetrolId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2,
                DriverUserId = DriverTwoId,
                DriverName = "Maria Gomez",
                VehicleId = VehicleTwoId,
                OfferedSeats = 4,
                AvailableSeats = 3,
                FareAmount = 8m,
                CreatedAt = now.AddHours(-2),
                UpdatedAt = now.AddHours(-1).AddMinutes(-50)
            },
            new Trip
            {
                Id = TripInProgressId,
                OriginLocationId = LocUnivalleId,
                DestinationLocationId = LocBuschId,
                StatusId = 3,
                DriverUserId = DriverOneId,
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 2,
                FareAmount = 10m,
                CreatedAt = now.AddMinutes(-55),
                UpdatedAt = now.AddMinutes(-16),
                StartedAt = now.AddMinutes(-15)
            },
            new Trip
            {
                Id = TripFinishedId,
                OriginLocationId = LocBimodalId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 4,
                DriverUserId = DriverTwoId,
                DriverName = "Maria Gomez",
                VehicleId = VehicleTwoId,
                OfferedSeats = 4,
                AvailableSeats = 2,
                FareAmount = 12m,
                CreatedAt = now.AddHours(-4),
                UpdatedAt = now.AddHours(-2),
                StartedAt = now.AddHours(-3),
                FinishedAt = now.AddHours(-2)
            },
            new Trip
            {
                Id = TripCancelledId,
                OriginLocationId = LocUnivalleId,
                DestinationLocationId = LocPlazaId,
                StatusId = 5,
                DriverUserId = DriverOneId,
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 4,
                FareAmount = 10m,
                CreatedAt = now.AddHours(-6),
                UpdatedAt = now.AddHours(-5),
                CancelledAt = now.AddHours(-5)
            },
            new Trip
            {
                Id = TripRefundableId,
                OriginLocationId = LocVenturaId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2,
                DriverUserId = DriverThreeId,
                DriverName = "Rodrigo Vaca",
                VehicleId = VehicleThreeId,
                OfferedSeats = 4,
                AvailableSeats = 3,
                FareAmount = 9m,
                CreatedAt = now.AddMinutes(-35),
                UpdatedAt = now.AddMinutes(-25)
            },
            new Trip
            {
                Id = TripFullId,
                OriginLocationId = LocPlan3000Id,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2,
                DriverUserId = DriverThreeId,
                DriverName = "Rodrigo Vaca",
                VehicleId = VehicleThreeId,
                OfferedSeats = 4,
                AvailableSeats = 0,
                FareAmount = 11m,
                CreatedAt = now.AddHours(-1).AddMinutes(-25),
                UpdatedAt = now.AddHours(-1).AddMinutes(-10)
            },
            new Trip
            {
                Id = TripMorningFinishedId,
                OriginLocationId = LocHamacasId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 4,
                DriverUserId = DriverOneId,
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 1,
                FareAmount = 7m,
                CreatedAt = now.AddDays(-1).AddHours(-8),
                UpdatedAt = now.AddDays(-1).AddHours(-6),
                StartedAt = now.AddDays(-1).AddHours(-7),
                FinishedAt = now.AddDays(-1).AddHours(-6)
            });

        context.UserBookmarks.AddRange(
            new UserBookmark
            {
                Id = BookmarkPlaceId,
                Kind = "place",
                OriginLocationId = LocLibraryBookmarkId,
                DestinationLocationId = null,
                Title = "Biblioteca central",
                UserId = PassengerOneId,
                CreatedAt = now.AddDays(-9),
                UseCount = 5,
                LastUsedAt = now.AddHours(-5)
            },
            new UserBookmark
            {
                Id = BookmarkRouteId,
                Kind = "route",
                OriginLocationId = LocHomeBookmarkId,
                DestinationLocationId = LocUnivalleId,
                Title = "Casa - Campus",
                UserId = PassengerOneId,
                CreatedAt = now.AddDays(-8),
                UseCount = 3,
                LastUsedAt = now.AddDays(-1)
            });
    }

    private static void SeedReservations(CarPoolingContext context, DateTime now)
    {
        context.Reservations.AddRange(
            new Reservation { Id = ResOneId, TripId = TripScheduledId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1111", CreatedAt = now.AddMinutes(-30) },
            new Reservation { Id = ResTwoId, TripId = TripReadyId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 2, BoardingCode = "2222", CreatedAt = now.AddMinutes(-90) },
            new Reservation { Id = ResThreeId, TripId = TripReadyId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 1, BoardingCode = "3333", CreatedAt = now.AddMinutes(-40) },
            new Reservation { Id = ResFourId, TripId = TripInProgressId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 3, BoardingCode = "4444", CreatedAt = now.AddMinutes(-35) },
            new Reservation { Id = ResFiveId, TripId = TripInProgressId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "5555", CreatedAt = now.AddMinutes(-30) },
            new Reservation { Id = ResSixId, TripId = TripFinishedId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "6666", CreatedAt = now.AddHours(-3).AddMinutes(-30) },
            new Reservation { Id = ResSevenId, TripId = TripFinishedId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 3, BoardingCode = "7777", CreatedAt = now.AddHours(-3).AddMinutes(-10) },
            new Reservation { Id = ResEightId, TripId = TripFinishedId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 4, BoardingCode = "8888", CreatedAt = now.AddHours(-3).AddMinutes(-5) },
            new Reservation { Id = ResNineId, TripId = TripRefundableId, PassengerUserId = PassengerFourId, SeatsReserved = 1, StatusId = 2, BoardingCode = "9090", CreatedAt = now.AddMinutes(-28) },
            new Reservation { Id = ResTenId, TripId = TripFullId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1010", CreatedAt = now.AddHours(-1).AddMinutes(-18) },
            new Reservation { Id = ResElevenId, TripId = TripFullId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1110", CreatedAt = now.AddHours(-1).AddMinutes(-17) },
            new Reservation { Id = ResTwelveId, TripId = TripFullId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1212", CreatedAt = now.AddHours(-1).AddMinutes(-16) },
            new Reservation { Id = ResThirteenId, TripId = TripFullId, PassengerUserId = PassengerFiveId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1313", CreatedAt = now.AddHours(-1).AddMinutes(-15) },
            new Reservation { Id = ResFourteenId, TripId = TripMorningFinishedId, PassengerUserId = PassengerFourId, SeatsReserved = 1, StatusId = 3, BoardingCode = "1414", CreatedAt = now.AddDays(-1).AddHours(-7).AddMinutes(-40) },
            new Reservation { Id = ResFifteenId, TripId = TripCancelledId, PassengerUserId = PassengerFiveId, SeatsReserved = 1, StatusId = 4, BoardingCode = "1515", CreatedAt = now.AddHours(-5).AddMinutes(-45) });
    }

    private static void SeedPaymentMethodsForUsers(CarPoolingContext context, DateTime now)
    {
        const string demoQr = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

        context.UserPaymentMethods.AddRange(
            new UserPaymentMethod
            {
                Id = DriverOneQrMethodId,
                UserId = DriverOneId,
                PaymentMethodId = 3,
                Alias = "QR Banco Union",
                QrImageUrl = demoQr,
                BankName = "Banco Union",
                AccountHolderName = "Juan Perez",
                IsDefault = true,
                IsActive = true,
                CreatedAt = now.AddDays(-15)
            },
            new UserPaymentMethod
            {
                Id = DriverTwoQrMethodId,
                UserId = DriverTwoId,
                PaymentMethodId = 3,
                Alias = "QR BNB Personal",
                QrImageUrl = demoQr,
                BankName = "Banco Nacional de Bolivia",
                AccountHolderName = "Maria Gomez Vaca",
                IsDefault = true,
                IsActive = true,
                CreatedAt = now.AddDays(-14)
            },
            new UserPaymentMethod
            {
                Id = PassengerCardMethodId,
                UserId = PassengerFourId,
                PaymentMethodId = 2,
                Alias = "Tarjeta demo",
                MaskedValue = "**** **** **** 4242",
                ProviderToken = "tok_demo_4242",
                IsDefault = true,
                IsActive = true,
                CreatedAt = now.AddDays(-5)
            },
            new UserPaymentMethod
            {
                Id = PassengerWalletMethodId,
                UserId = PassengerOneId,
                PaymentMethodId = 4,
                Alias = "Billetera Univalle",
                MaskedValue = "Saldo demo 75 BOB",
                ProviderToken = "wallet_demo_carlos",
                IsDefault = true,
                IsActive = true,
                CreatedAt = now.AddDays(-6)
            });
    }

    private static void SeedPayments(CarPoolingContext context, DateTime now)
    {
        context.Payments.AddRange(
            new Payment
            {
                Id = PayOneId,
                ReservationId = ResFourId,
                PaymentMethodId = 2,
                Amount = 10m,
                Currency = "BOB",
                Status = PaymentStatus.Approved,
                Description = "Pago de pasaje simulado por tarjeta de debito",
                ExternalReference = "PAY-DEMO-CARD-0001",
                PaidAt = now.AddMinutes(-15),
                CreatedAt = now.AddMinutes(-18),
                UpdatedAt = now.AddMinutes(-15)
            },
            new Payment
            {
                Id = PayTwoId,
                ReservationId = ResFiveId,
                PaymentMethodId = 1,
                Amount = 10m,
                Currency = "BOB",
                Status = PaymentStatus.Pending,
                Description = "Pago en efectivo pendiente de confirmacion del conductor",
                ExternalReference = "PAY-DEMO-CASH-0002",
                ExpiresAt = now.AddMinutes(45),
                CreatedAt = now.AddMinutes(-12)
            },
            new Payment
            {
                Id = PayThreeId,
                ReservationId = ResSixId,
                PaymentMethodId = 3,
                UserPaymentMethodId = DriverTwoQrMethodId,
                Amount = 12m,
                Currency = "BOB",
                Status = PaymentStatus.Approved,
                Description = "Pago verificado por QR",
                ExternalReference = "PAY-DEMO-QR-0003",
                ConfirmedByUserId = DriverTwoId,
                ConfirmedAt = now.AddHours(-2).AddMinutes(-30),
                ConfirmationNotes = "QR verificado en cuenta BNB",
                PaidAt = now.AddHours(-2).AddMinutes(-40),
                CreatedAt = now.AddHours(-2).AddMinutes(-50),
                UpdatedAt = now.AddHours(-2).AddMinutes(-30)
            },
            new Payment
            {
                Id = PayFourId,
                ReservationId = ResSevenId,
                PaymentMethodId = 1,
                Amount = 12m,
                Currency = "BOB",
                Status = PaymentStatus.Approved,
                Description = "Pago en efectivo entregado a conductora",
                ExternalReference = "PAY-DEMO-CASH-0004",
                ConfirmedByUserId = DriverTwoId,
                ConfirmedAt = now.AddHours(-2),
                PaidAt = now.AddHours(-2),
                CreatedAt = now.AddHours(-2).AddMinutes(-15),
                UpdatedAt = now.AddHours(-2)
            },
            new Payment
            {
                Id = PayFiveId,
                ReservationId = ResNineId,
                PaymentMethodId = 2,
                UserPaymentMethodId = PassengerCardMethodId,
                Amount = 9m,
                Currency = "BOB",
                RefundedAmount = 4m,
                Status = PaymentStatus.PartiallyRefunded,
                Description = "Pago parcial con devolucion solicitada antes de iniciar",
                ExternalReference = "PAY-DEMO-REFUND-0005",
                PaidAt = now.AddMinutes(-24),
                CreatedAt = now.AddMinutes(-26),
                UpdatedAt = now.AddMinutes(-8)
            },
            new Payment
            {
                Id = PaySixId,
                ReservationId = ResTenId,
                PaymentMethodId = 4,
                UserPaymentMethodId = PassengerWalletMethodId,
                Amount = 11m,
                Currency = "BOB",
                Status = PaymentStatus.Approved,
                Description = "Pago con billetera simulada",
                ExternalReference = "PAY-DEMO-WALLET-0006",
                PaidAt = now.AddHours(-1),
                CreatedAt = now.AddHours(-1).AddMinutes(-5),
                UpdatedAt = now.AddHours(-1)
            },
            new Payment
            {
                Id = PaySevenId,
                ReservationId = ResFifteenId,
                PaymentMethodId = 2,
                Amount = 10m,
                Currency = "BOB",
                RefundedAmount = 10m,
                Status = PaymentStatus.Refunded,
                Description = "Pago devuelto por cancelacion del viaje",
                ExternalReference = "PAY-DEMO-CANCEL-0007",
                PaidAt = now.AddHours(-5).AddMinutes(-35),
                CreatedAt = now.AddHours(-5).AddMinutes(-40),
                UpdatedAt = now.AddHours(-5)
            });

        context.PaymentTransactions.AddRange(
            DemoTransaction(PayOneId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 10m, "SIMULATED_GATEWAY", "TX-CARD-987654", "00", "Aprobado exitosamente", now.AddMinutes(-15)),
            DemoTransaction(PayTwoId, PaymentTransactionType.Payment, PaymentTransactionStatus.Pending, 10m, "CASH", "TX-CASH-PENDING", "PENDING", "Esperando confirmacion del conductor", null),
            DemoTransaction(PayThreeId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 12m, "QR_BANK", "TX-QR-112233", "CONFIRMED", "Verificacion manual de QR exitosa", now.AddHours(-2).AddMinutes(-30)),
            DemoTransaction(PayFourId, PaymentTransactionType.Confirmation, PaymentTransactionStatus.Success, 12m, "CASH", "TX-CASH-445566", "CONFIRMED", "Pago en efectivo confirmado", now.AddHours(-2)),
            DemoTransaction(PayFiveId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 9m, "SIMULATED_GATEWAY", "TX-CARD-REFUND", "00", "Pago aprobado en simulador", now.AddMinutes(-24)),
            DemoTransaction(PayFiveId, PaymentTransactionType.Refund, PaymentTransactionStatus.Success, 4m, "CARD_SIM", "TX-REFUND-PARTIAL", "REFUNDED", "Devolucion parcial procesada", now.AddMinutes(-8)),
            DemoTransaction(PaySixId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 11m, "WALLET_SIM", "TX-WALLET-0006", "00", "Debito de billetera simulado", now.AddHours(-1)),
            DemoTransaction(PaySevenId, PaymentTransactionType.Refund, PaymentTransactionStatus.Success, 10m, "SIMULATED_GATEWAY", "TX-REFUND-CANCEL", "REFUNDED", "Devolucion completa por cancelacion", now.AddHours(-5)));

        context.Refunds.Add(new Refund
        {
            Id = RefundOneId,
            PaymentId = PayFiveId,
            Amount = 4m,
            Status = RefundStatus.Processed,
            RequestedByUserId = PassengerFourId,
            ProcessedByUserId = DriverThreeId,
            Reason = "El pasajero cambio a un punto de encuentro mas cercano.",
            IsWithinCancellationWindow = true,
            CancellationDeadline = now.AddMinutes(20),
            MinutesBeforeTripStart = 40,
            RequestedAt = now.AddMinutes(-10),
            ProcessedAt = now.AddMinutes(-8),
            CreatedAt = now.AddMinutes(-10)
        });
    }

    private static PaymentTransaction DemoTransaction(
        Guid paymentId,
        PaymentTransactionType type,
        PaymentTransactionStatus status,
        decimal amount,
        string provider,
        string providerTransactionId,
        string responseCode,
        string responseMessage,
        DateTime? processedAt)
    {
        return new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = paymentId,
            TransactionType = type,
            Status = status,
            Amount = amount,
            Provider = provider,
            ProviderTransactionId = providerTransactionId,
            ResponseCode = responseCode,
            ResponseMessage = responseMessage,
            ProcessedAt = processedAt,
            CreatedAt = processedAt ?? DateTime.UtcNow
        };
    }

    private static void SeedChats(CarPoolingContext context, DateTime now)
    {
        context.TripChats.AddRange(
            new TripChat { Id = ChatActiveId, TripId = TripInProgressId, CreatedAt = now.AddMinutes(-40) },
            new TripChat { Id = ChatFinishedId, TripId = TripFinishedId, CreatedAt = now.AddHours(-3) });

        var activeMessages = new[]
        {
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff1"), ChatId = ChatActiveId, SenderUserId = DriverOneId, MessageText = "Hola a todos. Estoy parqueado en la salida del campus, frente al porton principal.", CreatedAt = now.AddMinutes(-15) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff2"), ChatId = ChatActiveId, SenderUserId = PassengerTwoId, MessageText = "Hola Juan, salgo de mi clase del edificio de Ingenieria. Llego en 3 minutos.", CreatedAt = now.AddMinutes(-12) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff3"), ChatId = ChatActiveId, SenderUserId = PassengerOneId, MessageText = "Listo, tambien voy bajando por las escaleras principales.", CreatedAt = now.AddMinutes(-10) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff4"), ChatId = ChatActiveId, SenderUserId = DriverOneId, MessageText = "Excelente, los espero en el Corolla blanco.", CreatedAt = now.AddMinutes(-8) }
        };

        var finishedMessages = new[]
        {
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff5"), ChatId = ChatFinishedId, SenderUserId = DriverTwoId, MessageText = "Gracias por viajar conmigo. Ya finalice el recorrido.", CreatedAt = now.AddHours(-2).AddMinutes(-5) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff6"), ChatId = ChatFinishedId, SenderUserId = PassengerOneId, MessageText = "Gracias Maria, todo bien. Ya deje mi calificacion.", CreatedAt = now.AddHours(-1).AddMinutes(-55) }
        };

        context.TripChatMessages.AddRange(activeMessages);
        context.TripChatMessages.AddRange(finishedMessages);

        context.TripChatMessageReads.AddRange(
            new TripChatMessageRead { MessageId = activeMessages[0].Id, UserId = PassengerTwoId, ReadAt = now.AddMinutes(-14) },
            new TripChatMessageRead { MessageId = activeMessages[0].Id, UserId = PassengerOneId, ReadAt = now.AddMinutes(-13) },
            new TripChatMessageRead { MessageId = activeMessages[1].Id, UserId = DriverOneId, ReadAt = now.AddMinutes(-11) },
            new TripChatMessageRead { MessageId = activeMessages[2].Id, UserId = DriverOneId, ReadAt = now.AddMinutes(-9) },
            new TripChatMessageRead { MessageId = finishedMessages[0].Id, UserId = PassengerOneId, ReadAt = now.AddHours(-2) },
            new TripChatMessageRead { MessageId = finishedMessages[1].Id, UserId = DriverTwoId, ReadAt = now.AddHours(-1).AddMinutes(-50) });
    }

    private static void SeedRatings(CarPoolingContext context, DateTime now)
    {
        context.TripRatings.AddRange(
            new TripRating
            {
                Id = Guid.Parse("11112222-3333-4444-5555-666677778881"),
                TripId = TripFinishedId,
                EvaluatorUserId = PassengerOneId,
                EvaluatedUserId = DriverTwoId,
                RatingRole = RatingRole.PassengerToDriver,
                Score = 5,
                Comment = "La conductora fue puntual y el auto estaba limpio y comodo.",
                Tags = "Puntual,Limpio,Amable",
                CreatedAt = now.AddHours(-1).AddMinutes(-50)
            },
            new TripRating
            {
                Id = Guid.Parse("11112222-3333-4444-5555-666677778882"),
                TripId = TripFinishedId,
                EvaluatorUserId = PassengerThreeId,
                EvaluatedUserId = DriverTwoId,
                RatingRole = RatingRole.PassengerToDriver,
                Score = 4,
                Comment = "Buen viaje, habia trafico pero la ruta fue segura.",
                Tags = "Respetuoso,Puntual",
                CreatedAt = now.AddHours(-1).AddMinutes(-45)
            },
            new TripRating
            {
                Id = Guid.Parse("11112222-3333-4444-5555-666677778883"),
                TripId = TripFinishedId,
                EvaluatorUserId = DriverTwoId,
                EvaluatedUserId = PassengerOneId,
                RatingRole = RatingRole.DriverToPassenger,
                Score = 5,
                Comment = "Pasajero educado y puntual al abordar.",
                Tags = "Educado,Puntual",
                CreatedAt = now.AddHours(-1).AddMinutes(-30)
            },
            new TripRating
            {
                Id = Guid.Parse("11112222-3333-4444-5555-666677778884"),
                TripId = TripFinishedId,
                EvaluatorUserId = DriverTwoId,
                EvaluatedUserId = PassengerThreeId,
                RatingRole = RatingRole.DriverToPassenger,
                Score = 5,
                Comment = "Pasajero respetuoso durante todo el trayecto.",
                Tags = "Respetuoso",
                CreatedAt = now.AddHours(-1).AddMinutes(-28)
            },
            new TripRating
            {
                Id = Guid.Parse("11112222-3333-4444-5555-666677778885"),
                TripId = TripMorningFinishedId,
                EvaluatorUserId = PassengerFourId,
                EvaluatedUserId = DriverOneId,
                RatingRole = RatingRole.PassengerToDriver,
                Score = 5,
                Comment = "Llegamos a tiempo para el primer modulo.",
                Tags = "Puntual,Seguro",
                CreatedAt = now.AddDays(-1).AddHours(-5)
            });
    }

    private static void SeedSupport(CarPoolingContext context, DateTime now)
    {
        context.SupportTickets.AddRange(
            new SupportTicket
            {
                Id = TicketOneId,
                UserId = PassengerOneId,
                TripId = TripFinishedId,
                ReservationId = ResSixId,
                Category = SupportTicketCategory.Trip,
                Subject = "Celular olvidado en el vehiculo",
                Description = "Creo que olvide mi celular Samsung con funda azul en el asiento trasero del Suzuki Swift gris.",
                Status = SupportTicketStatus.Open,
                AssignedAdminUserId = AdminId,
                CreatedAt = now.AddHours(-3),
                LastMessageAt = now.AddHours(-2).AddMinutes(-50)
            },
            new SupportTicket
            {
                Id = TicketTwoId,
                UserId = PassengerTwoId,
                Category = SupportTicketCategory.Account,
                Subject = "Problema con saldo de billetera simulada",
                Description = "Realice una recarga de 50 BOB pero el saldo seguia apareciendo en 0 BOB.",
                Status = SupportTicketStatus.Resolved,
                AssignedAdminUserId = AdminId,
                CreatedAt = now.AddDays(-3),
                FirstAdminReplyAt = now.AddDays(-2),
                LastMessageAt = now.AddDays(-1),
                ClosedAt = now.AddDays(-1)
            },
            new SupportTicket
            {
                Id = TicketThreeId,
                UserId = PassengerFourId,
                ReservationId = ResNineId,
                Category = SupportTicketCategory.Payment,
                Subject = "Consulta sobre devolucion parcial",
                Description = "Solicite una devolucion parcial y quiero confirmar que quedo registrada.",
                Status = SupportTicketStatus.InReview,
                AssignedAdminUserId = AdminId,
                CreatedAt = now.AddMinutes(-18),
                FirstAdminReplyAt = now.AddMinutes(-12),
                LastMessageAt = now.AddMinutes(-10)
            });

        var messages = new[]
        {
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990001"), TicketId = TicketOneId, SenderUserId = PassengerOneId, SenderKind = SupportMessageSenderKind.User, MessageText = "Hola, no he podido comunicarme con la conductora. Ojala me puedan ayudar.", CreatedAt = now.AddHours(-2).AddMinutes(-50) },
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990002"), TicketId = TicketTwoId, SenderUserId = PassengerTwoId, SenderKind = SupportMessageSenderKind.User, MessageText = "Ayer hice la recarga pero no se refleja en mi balance.", CreatedAt = now.AddDays(-3).AddHours(1) },
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990003"), TicketId = TicketTwoId, SenderUserId = AdminId, SenderKind = SupportMessageSenderKind.Admin, MessageText = "Hola Ana. Verificamos la transaccion y acreditamos manualmente los 50 BOB.", CreatedAt = now.AddDays(-2) },
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990004"), TicketId = TicketTwoId, SenderUserId = PassengerTwoId, SenderKind = SupportMessageSenderKind.User, MessageText = "Ya aparece el saldo correcto. Muchas gracias.", CreatedAt = now.AddDays(-1) },
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990005"), TicketId = TicketThreeId, SenderUserId = PassengerFourId, SenderKind = SupportMessageSenderKind.User, MessageText = "La devolucion parcial aparece en el detalle del pago, pero quiero confirmar el estado.", CreatedAt = now.AddMinutes(-16) },
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990006"), TicketId = TicketThreeId, SenderUserId = AdminId, SenderKind = SupportMessageSenderKind.Admin, MessageText = "Estamos revisando el movimiento. El estado figura como procesado por el conductor.", CreatedAt = now.AddMinutes(-12) }
        };

        context.SupportTicketMessages.AddRange(messages);
    }

    private static void SeedDevices(CarPoolingContext context, DateTime now)
    {
        context.UserDevices.AddRange(
            new UserDevice { Id = Guid.Parse("12121212-1212-1212-1212-121212121201"), UserId = DriverOneId, FcmToken = "demo-fcm-driver-juan", DeviceName = "Pixel conductor Juan", LastUsedAt = now.AddMinutes(-5) },
            new UserDevice { Id = Guid.Parse("12121212-1212-1212-1212-121212121202"), UserId = PassengerOneId, FcmToken = "demo-fcm-carlos", DeviceName = "Android Carlos", LastUsedAt = now.AddMinutes(-3) },
            new UserDevice { Id = Guid.Parse("12121212-1212-1212-1212-121212121203"), UserId = PassengerTwoId, FcmToken = "demo-fcm-ana", DeviceName = "Android Ana", LastUsedAt = now.AddMinutes(-8) });
    }

    private static async Task SeedDefaultThemeAsync(CarPoolingContext context)
    {
        if (await context.AppSettings.AnyAsync(s => s.Key == "theme"))
        {
            return;
        }

        context.AppSettings.Add(new AppSetting
        {
            Key = "theme",
            Value = "{\"primaryLight\":\"#82254B\",\"secondaryLight\":\"#6E1E3F\",\"textLight\":\"#111827\",\"bgLight\":\"#FFFFFF\",\"cardLight\":\"#F5F5F5\",\"borderLight\":\"#9CA8B0\",\"primaryDark\":\"#82254B\",\"secondaryDark\":\"#6E1E3F\",\"textDark\":\"#ffffff\",\"bgDark\":\"#121011\",\"cardDark\":\"#251a1e\",\"borderDark\":\"#6E1E3F\"}"
        });

        await context.SaveChangesAsync();
    }

    private static string HashPassword(string password)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(bytes);
    }
}
