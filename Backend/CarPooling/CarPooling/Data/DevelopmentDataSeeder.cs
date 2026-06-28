using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
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
    private static readonly Guid DriverFourId = Guid.Parse("33333333-3333-3333-3333-333333333335");
    private static readonly Guid DriverFiveId = Guid.Parse("33333333-3333-3333-3333-333333333336");
    private static readonly Guid PassengerOneId = Guid.Parse("44444444-4444-4444-4444-444444444444");
    private static readonly Guid PassengerTwoId = Guid.Parse("55555555-5555-5555-5555-555555555555");
    private static readonly Guid PassengerThreeId = Guid.Parse("66666666-6666-6666-6666-666666666666");
    private static readonly Guid PassengerFourId = Guid.Parse("66666666-6666-6666-6666-666666666667");
    private static readonly Guid PassengerFiveId = Guid.Parse("66666666-6666-6666-6666-666666666668");
    private static readonly Guid PassengerSixId = Guid.Parse("66666666-6666-6666-6666-666666666669");
    private static readonly Guid PassengerSevenId = Guid.Parse("66666666-6666-6666-6666-666666666670");
    private static readonly Guid PassengerEightId = Guid.Parse("66666666-6666-6666-6666-666666666671");
    private static readonly Guid PassengerNineId = Guid.Parse("66666666-6666-6666-6666-666666666672");
    private static readonly Guid PassengerTenId = Guid.Parse("66666666-6666-6666-6666-666666666673");
    private static readonly Guid PassengerElevenId = Guid.Parse("66666666-6666-6666-6666-666666666674");
    private static readonly Guid AnalystId = Guid.Parse("77777777-7777-7777-7777-777777777777");

    private static readonly Guid VehicleOneId = Guid.Parse("99999999-9999-9999-9999-999999999991");
    private static readonly Guid VehicleTwoId = Guid.Parse("99999999-9999-9999-9999-999999999992");
    private static readonly Guid VehicleThreeId = Guid.Parse("99999999-9999-9999-9999-999999999993");
    private static readonly Guid VehicleFourId = Guid.Parse("99999999-9999-9999-9999-999999999994");
    private static readonly Guid VehicleFiveId = Guid.Parse("99999999-9999-9999-9999-999999999995");

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
    private static readonly Guid TripExtraPaymentId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6");
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
    private static readonly Guid DriverOneCashMethodId = Guid.Parse("abab5555-5555-5555-5555-555555555555");
    private static readonly Guid DriverTwoCashMethodId = Guid.Parse("abab6666-6666-6666-6666-666666666666");
    private static readonly Guid PassengerSixCardMethodId = Guid.Parse("abab7777-7777-7777-7777-777777777777");
    private static readonly Guid PassengerFiveCardMethodId = Guid.Parse("abab8888-8888-8888-8888-888888888888");

    private static readonly Guid PayOneId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static readonly Guid PayTwoId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd2");
    private static readonly Guid PayThreeId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd3");
    private static readonly Guid PayFourId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd4");
    private static readonly Guid PayFiveId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd5");
    private static readonly Guid PaySixId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd6");
    private static readonly Guid PaySevenId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd7");

    private static readonly Guid ChatActiveId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
    private static readonly Guid ChatFinishedId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2");

    private static readonly Guid TicketOneId = Guid.Parse("22223333-4444-5555-6666-777788889991");
    private static readonly Guid TicketTwoId = Guid.Parse("22223333-4444-5555-6666-777788889992");
    private static readonly Guid TicketThreeId = Guid.Parse("22223333-4444-5555-6666-777788889993");
    private const string AuditDemoMarker = "SANTA_CRUZ_AUDIT_SCENARIO_V2";
    private static readonly string[] AuditDemoMarkers = ["AUDIT_DEMO_SCENARIO_V1", AuditDemoMarker];

    private static readonly Guid ScheduleOneId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbba1");
    private static readonly Guid ScheduleTwoId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbba2");
    private static readonly Guid RecurringResOneId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccca");
    private static readonly Guid RecurringResTwoId = Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccccb");

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
         await SeedDemoDataAsync(context);
        // await SeedAuditScenarioAsync(context);
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
                var analystPermissions = new[] { AppPermissions.ViewMetrics, AppPermissions.ReadUsers, AppPermissions.ReadTrips, AppPermissions.ReadAudit, AppPermissions.ExportAudit };
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

    private static async Task SeedDemoDataAsync(CarPoolingContext context)
    {
        if (await context.Users.AnyAsync(u => u.Id == AdminId || u.Email == "admin@univalle.edu"))
        {
            return;
        }

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
            DemoUser(DriverFourId, "Pedro Ortiz", "conductor4@univalle.edu", "72030407", passwordHash, now.AddMonths(-2)),
            DemoUser(DriverFiveId, "Laura Mendez", "conductor5@univalle.edu", "72030408", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerOneId, "Carlos Rojas", "estudiante1@univalle.edu", "73040506", passwordHash, now.AddMonths(-3)),
            DemoUser(PassengerTwoId, "Ana Torres", "estudiante2@univalle.edu", "74050607", passwordHash, now.AddMonths(-3).AddDays(4)),
            DemoUser(PassengerThreeId, "Luis Castro", "estudiante3@univalle.edu", "75060708", passwordHash, now.AddMonths(-2)),
            DemoUser(PassengerFourId, "Valeria Suarez", "estudiante4@univalle.edu", "76011122", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerFiveId, "Diego Molina", "estudiante5@univalle.edu", "76033344", passwordHash, now.AddMonths(-1).AddDays(5)),
            DemoUser(PassengerSixId, "Jose Linares", "estudiante6@univalle.edu", "76033345", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerSevenId, "Elena Gomez", "estudiante7@univalle.edu", "76033346", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerEightId, "Miguel Angel", "estudiante8@univalle.edu", "76033347", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerNineId, "Lucia Vargas", "estudiante9@univalle.edu", "76033348", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerTenId, "Fernando Torrez", "estudiante10@univalle.edu", "76033349", passwordHash, now.AddMonths(-1)),
            DemoUser(PassengerElevenId, "Patricia Soto", "estudiante11@univalle.edu", "76033350", passwordHash, now.AddMonths(-1)),
            DemoUser(AnalystId, "Sofia Morales", "analista@univalle.edu", "76070809", passwordHash, now.AddMonths(-2).AddDays(8))
        };

        context.Users.AddRange(users);
        await context.SaveChangesAsync();

        context.UserRoles.AddRange(
            new UserRole { UserId = AdminId, RoleId = roleSuperAdmin.Id },
            new UserRole { UserId = DriverOneId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverTwoId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverThreeId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverFourId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverFiveId, RoleId = roleDriver.Id },
            new UserRole { UserId = PassengerOneId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerTwoId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerThreeId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerFourId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerFiveId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerSixId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerSevenId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerEightId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerNineId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerTenId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerElevenId, RoleId = roleStudent.Id },
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
            },
            new Vehicle
            {
                Id = VehicleFourId,
                OwnerUserId = DriverFourId,
                LicensePlate = "8899-KKK",
                Brand = "Hyundai",
                Model = "Accent",
                Color = "Azul",
                VehicleYear = 2021,
                TotalSeats = 4,
                IsActive = true,
                IsVerified = true,
                CreatedAt = now.AddDays(-20)
            },
            new Vehicle
            {
                Id = VehicleFiveId,
                OwnerUserId = DriverFiveId,
                LicensePlate = "5566-LLL",
                Brand = "Kia",
                Model = "Rio",
                Color = "Plateado",
                VehicleYear = 2022,
                TotalSeats = 4,
                IsActive = true,
                IsVerified = true,
                CreatedAt = now.AddDays(-15)
            });

        await context.SaveChangesAsync();

        SeedLocationsAndSafeZones(context, now);
        await context.SaveChangesAsync();

        SeedTripSchedules(context, now);
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

    private static void SeedTripSchedules(CarPoolingContext context, DateTime now)
    {
        if (context.TripSchedules.Any())
        {
            return;
        }

        context.TripSchedules.AddRange(
            new TripSchedule
            {
                Id = ScheduleOneId,
                DriverUserId = DriverOneId,
                OriginLocationId = LocPlazaId,
                DestinationLocationId = LocUnivalleId,
                DepartureTime = new TimeSpan(7, 15, 0),
                DaysOfWeek = "1,2,3,4,5",
                StartDate = now.AddMonths(-1),
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                FareAmount = 10m,
                IsActive = true,
                CreatedAt = now.AddMonths(-1)
            },
            new TripSchedule
            {
                Id = ScheduleTwoId,
                DriverUserId = DriverTwoId,
                OriginLocationId = LocEquipetrolId,
                DestinationLocationId = LocUnivalleId,
                DepartureTime = new TimeSpan(12, 30, 0),
                DaysOfWeek = "1,3,5",
                StartDate = now.AddMonths(-1),
                VehicleId = VehicleTwoId,
                OfferedSeats = 4,
                FareAmount = 8m,
                IsActive = true,
                CreatedAt = now.AddMonths(-1)
            }
        );

        context.RecurringReservations.AddRange(
            new RecurringReservation
            {
                Id = RecurringResOneId,
                TripScheduleId = ScheduleOneId,
                PassengerUserId = PassengerFiveId,
                SeatsReserved = 1,
                IsActive = true,
                CreatedAt = now.AddMonths(-1)
            },
            new RecurringReservation
            {
                Id = RecurringResTwoId,
                TripScheduleId = ScheduleTwoId,
                PassengerUserId = PassengerFourId,
                SeatsReserved = 1,
                IsActive = true,
                CreatedAt = now.AddMonths(-1)
            }
        );
    }

    private static void SeedTrips(CarPoolingContext context, DateTime now)
    {
        context.Trips.AddRange(
            new Trip
            {
                Id = TripScheduledId,
                OriginLocationId = LocCristoId,
                DestinationLocationId = LocCristoId, // En estado 1 (scheduled) el destino es igual al origen
                StatusId = 1, // scheduled
                DriverUserId = DriverOneId, // Conductor requerido por los tests de Integración (evaluado con DriverOneId)
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 3, // Reservado por PassengerTwoId
                FareAmount = 10m,
                CreatedAt = now.AddMinutes(-75),
                UpdatedAt = now.AddMinutes(-72)
            },
            new Trip
            {
                Id = TripReadyId,
                OriginLocationId = LocEquipetrolId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2, // ready
                DriverUserId = DriverTwoId,
                DriverName = "Maria Gomez",
                VehicleId = VehicleTwoId,
                OfferedSeats = 4,
                AvailableSeats = 3, // 1 confirmado (Elena Gomez), 1 pendiente (Luis Castro)
                FareAmount = 8m,
                CreatedAt = now.AddHours(-2),
                UpdatedAt = now.AddHours(-1).AddMinutes(-50)
            },
            new Trip
            {
                Id = TripInProgressId,
                OriginLocationId = LocUnivalleId,
                DestinationLocationId = LocBuschId,
                StatusId = 3, // in_progress
                DriverUserId = DriverFiveId, // Laura Mendez
                DriverName = "Laura Mendez",
                VehicleId = VehicleFiveId,
                OfferedSeats = 4,
                AvailableSeats = 2, // 2 abordados (Carlos Rojas, Jose Linares)
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
                StatusId = 4, // finished
                DriverUserId = DriverTwoId,
                DriverName = "Maria Gomez",
                VehicleId = VehicleTwoId,
                OfferedSeats = 4,
                AvailableSeats = 2, // 2 abordados (Carlos Rojas, Luis Castro)
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
                StatusId = 5, // cancelled
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
                Id = TripExtraPaymentId,
                OriginLocationId = LocVenturaId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2, // ready
                DriverUserId = DriverFourId,
                DriverName = "Pedro Ortiz",
                VehicleId = VehicleFourId,
                OfferedSeats = 4,
                AvailableSeats = 3, // 1 confirmado (Valeria Suarez)
                FareAmount = 9m,
                CreatedAt = now.AddMinutes(-35),
                UpdatedAt = now.AddMinutes(-25)
            },
            new Trip
            {
                Id = TripFullId,
                OriginLocationId = LocPlan3000Id,
                DestinationLocationId = LocUnivalleId,
                StatusId = 2, // ready
                DriverUserId = DriverThreeId,
                DriverName = "Rodrigo Vaca",
                VehicleId = VehicleThreeId,
                OfferedSeats = 4,
                AvailableSeats = 0, // 4 confirmados (Elena, Miguel, Lucia, Fernando)
                FareAmount = 11m,
                CreatedAt = now.AddHours(-1).AddMinutes(-25),
                UpdatedAt = now.AddHours(-1).AddMinutes(-10)
            },
            new Trip
            {
                Id = TripMorningFinishedId,
                OriginLocationId = LocHamacasId,
                DestinationLocationId = LocUnivalleId,
                StatusId = 4, // finished
                DriverUserId = DriverOneId,
                DriverName = "Juan Perez",
                VehicleId = VehicleOneId,
                OfferedSeats = 4,
                AvailableSeats = 3, // 1 abordado (Diego Molina)
                FareAmount = 7m,
                TripScheduleId = ScheduleOneId, // Enlace con plantilla programada
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
            new Reservation { Id = ResOneId, TripId = TripScheduledId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1111", CreatedAt = now.AddMinutes(-30) }, // Requerida para test de Integración (PassengerTwoId confirmada)
            new Reservation { Id = ResTwoId, TripId = TripReadyId, PassengerUserId = PassengerSevenId, SeatsReserved = 1, StatusId = 2, BoardingCode = "2222", CreatedAt = now.AddMinutes(-90) }, // Elena Gomez
            new Reservation { Id = ResThreeId, TripId = TripReadyId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 1, BoardingCode = "3333", CreatedAt = now.AddMinutes(-40) }, // Luis Castro
            new Reservation { Id = ResFourId, TripId = TripInProgressId, PassengerUserId = PassengerSixId, SeatsReserved = 1, StatusId = 3, BoardingCode = "4444", CreatedAt = now.AddMinutes(-35) }, // Jose Linares
            new Reservation { Id = ResFiveId, TripId = TripInProgressId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "5555", CreatedAt = now.AddMinutes(-30) }, // Carlos Rojas
            new Reservation { Id = ResSixId, TripId = TripFinishedId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "6666", CreatedAt = now.AddHours(-3).AddMinutes(-30) },
            new Reservation { Id = ResSevenId, TripId = TripFinishedId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 3, BoardingCode = "7777", CreatedAt = now.AddHours(-3).AddMinutes(-10) },
            new Reservation { Id = ResEightId, TripId = TripFinishedId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 4, BoardingCode = "8888", CreatedAt = now.AddHours(-3).AddMinutes(-5) },
            new Reservation { Id = ResNineId, TripId = TripExtraPaymentId, PassengerUserId = PassengerFourId, SeatsReserved = 1, StatusId = 2, BoardingCode = "9090", CreatedAt = now.AddMinutes(-28) },
            new Reservation { Id = ResTenId, TripId = TripFullId, PassengerUserId = PassengerEightId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1010", CreatedAt = now.AddHours(-1).AddMinutes(-18) },
            new Reservation { Id = ResElevenId, TripId = TripFullId, PassengerUserId = PassengerNineId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1110", CreatedAt = now.AddHours(-1).AddMinutes(-17) },
            new Reservation { Id = ResTwelveId, TripId = TripFullId, PassengerUserId = PassengerTenId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1212", CreatedAt = now.AddHours(-1).AddMinutes(-16) },
            new Reservation { Id = ResThirteenId, TripId = TripFullId, PassengerUserId = PassengerElevenId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1313", CreatedAt = now.AddHours(-1).AddMinutes(-15) },
            new Reservation { Id = ResFourteenId, TripId = TripMorningFinishedId, PassengerUserId = PassengerFiveId, SeatsReserved = 1, StatusId = 3, BoardingCode = "1414", RecurringReservationId = RecurringResOneId, CreatedAt = now.AddDays(-1).AddHours(-7).AddMinutes(-40) },
            new Reservation { Id = ResFifteenId, TripId = TripCancelledId, PassengerUserId = PassengerFiveId, SeatsReserved = 1, StatusId = 4, BoardingCode = "1515", CreatedAt = now.AddHours(-5).AddMinutes(-45) });
    }

    private static void SeedPaymentMethodsForUsers(CarPoolingContext context, DateTime now)
    {
        const string demoQr = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";
        var cash = PaymentMethodCatalog.Find(1)!;
        var card = PaymentMethodCatalog.Find(2)!;
        var qr = PaymentMethodCatalog.Find(3)!;
        var wallet = PaymentMethodCatalog.Find(4)!;

        context.UserPaymentMethods.AddRange(
            DemoUserPaymentMethod(DriverOneQrMethodId, DriverOneId, qr, "QR Banco Union", null, null, demoQr, "Banco Union", "Juan Perez", true, now.AddDays(-15)),
            DemoUserPaymentMethod(DriverTwoQrMethodId, DriverTwoId, qr, "QR BNB Personal", null, null, demoQr, "Banco Nacional de Bolivia", "Maria Gomez Vaca", true, now.AddDays(-14)),
            DemoUserPaymentMethod(DriverOneCashMethodId, DriverOneId, cash, "Efectivo conductor", null, null, null, null, "Juan Perez", false, now.AddDays(-14)),
            DemoUserPaymentMethod(DriverTwoCashMethodId, DriverTwoId, cash, "Efectivo conductora", null, null, null, null, "Maria Gomez Vaca", false, now.AddDays(-14)),
            DemoUserPaymentMethod(PassengerCardMethodId, PassengerFourId, card, "Tarjeta demo", "**** **** **** 4242", "tok_demo_4242", null, null, null, true, now.AddDays(-5)),
            DemoUserPaymentMethod(PassengerSixCardMethodId, PassengerSixId, card, "Tarjeta demo", "**** **** **** 1111", "tok_demo_1111", null, null, null, true, now.AddDays(-5)),
            DemoUserPaymentMethod(PassengerFiveCardMethodId, PassengerFiveId, card, "Tarjeta demo", "**** **** **** 5555", "tok_demo_5555", null, null, null, true, now.AddDays(-6)),
            DemoUserPaymentMethod(PassengerWalletMethodId, PassengerEightId, wallet, "Billetera Univalle", "Saldo demo 75 BOB", "wallet_demo_miguel", null, null, null, true, now.AddDays(-6)));
    }

    private static void SeedPayments(CarPoolingContext context, DateTime now)
    {
        context.Payments.AddRange(
            new Payment
            {
                Id = PayOneId,
                ReservationId = ResFourId,
                UserPaymentMethodId = PassengerSixCardMethodId,
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
                UserPaymentMethodId = DriverOneCashMethodId,
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
                UserPaymentMethodId = DriverTwoCashMethodId,
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
                UserPaymentMethodId = PassengerCardMethodId,
                Amount = 9m,
                Currency = "BOB",
                Status = PaymentStatus.Approved,
                Description = "Pago simulado aprobado antes de iniciar",
                ExternalReference = "PAY-DEMO-CARD-0005",
                PaidAt = now.AddMinutes(-24),
                CreatedAt = now.AddMinutes(-26),
                UpdatedAt = now.AddMinutes(-24)
            },
            new Payment
            {
                Id = PaySixId,
                ReservationId = ResTenId,
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
                UserPaymentMethodId = PassengerFiveCardMethodId,
                Amount = 10m,
                Currency = "BOB",
                Status = PaymentStatus.Cancelled,
                Description = "Pago cancelado por cancelacion del viaje",
                ExternalReference = "PAY-DEMO-CANCEL-0007",
                CreatedAt = now.AddHours(-5).AddMinutes(-40),
                UpdatedAt = now.AddHours(-5)
            });

        context.PaymentTransactions.AddRange(
            DemoTransaction(PayOneId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 10m, "SIMULATED_GATEWAY", "TX-CARD-987654", "00", "Aprobado exitosamente", now.AddMinutes(-15)),
            DemoTransaction(PayTwoId, PaymentTransactionType.Payment, PaymentTransactionStatus.Pending, 10m, "CASH", "TX-CASH-PENDING", "PENDING", "Esperando confirmacion del conductor", null),
            DemoTransaction(PayThreeId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 12m, "QR_BANK", "TX-QR-112233", "CONFIRMED", "Verificacion manual de QR exitosa", now.AddHours(-2).AddMinutes(-30)),
            DemoTransaction(PayFourId, PaymentTransactionType.Confirmation, PaymentTransactionStatus.Success, 12m, "CASH", "TX-CASH-445566", "CONFIRMED", "Pago en efectivo confirmado", now.AddHours(-2)),
            DemoTransaction(PayFiveId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 9m, "SIMULATED_GATEWAY", "TX-CARD-REFUND", "00", "Pago aprobado en simulador", now.AddMinutes(-24)),
            DemoTransaction(PaySixId, PaymentTransactionType.Payment, PaymentTransactionStatus.Success, 11m, "WALLET_SIM", "TX-WALLET-0006", "00", "Debito de billetera simulado", now.AddHours(-1)),
            DemoTransaction(PaySevenId, PaymentTransactionType.Cancellation, PaymentTransactionStatus.Success, 10m, "SIMULATED_GATEWAY", "TX-CANCEL-0007", "CANCELLED", "Pago cancelado por viaje cancelado", now.AddHours(-5)));
    }

    private static UserPaymentMethod DemoUserPaymentMethod(
        Guid id,
        Guid userId,
        PaymentMethodDefinition method,
        string alias,
        string? maskedValue,
        string? providerToken,
        string? qrImageUrl,
        string? bankName,
        string? accountHolderName,
        bool isDefault,
        DateTime createdAt)
    {
        return new UserPaymentMethod
        {
            Id = id,
            UserId = userId,
            PaymentMethodId = method.Id,
            PaymentMethodCode = method.Code,
            PaymentMethodName = method.Name,
            PaymentMethodDescription = method.Description,
            Type = method.Type,
            RequiresManualConfirmation = method.RequiresManualConfirmation,
            Alias = alias,
            MaskedValue = maskedValue,
            ProviderToken = providerToken,
            QrImageUrl = qrImageUrl,
            BankName = bankName,
            AccountHolderName = accountHolderName,
            IsDefault = isDefault,
            IsActive = true,
            CreatedAt = createdAt
        };
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
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff1"), ChatId = ChatActiveId, SenderUserId = DriverFiveId, MessageText = "Hola a todos. Estoy parqueado en la salida del campus, frente al porton principal.", CreatedAt = now.AddMinutes(-15) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff2"), ChatId = ChatActiveId, SenderUserId = PassengerSixId, MessageText = "Hola Laura, salgo de mi clase del edificio de Ingenieria. Llego en 3 minutos.", CreatedAt = now.AddMinutes(-12) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff3"), ChatId = ChatActiveId, SenderUserId = PassengerOneId, MessageText = "Listo, tambien voy bajando por las escaleras principales.", CreatedAt = now.AddMinutes(-10) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff4"), ChatId = ChatActiveId, SenderUserId = DriverFiveId, MessageText = "Excelente, los espero en el Kia Rio plateado.", CreatedAt = now.AddMinutes(-8) }
        };

        var finishedMessages = new[]
        {
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff5"), ChatId = ChatFinishedId, SenderUserId = DriverTwoId, MessageText = "Gracias por viajar conmigo. Ya finalice el recorrido.", CreatedAt = now.AddHours(-2).AddMinutes(-5) },
            new TripChatMessage { Id = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff6"), ChatId = ChatFinishedId, SenderUserId = PassengerOneId, MessageText = "Gracias Maria, todo bien. Ya deje mi calificacion.", CreatedAt = now.AddHours(-1).AddMinutes(-55) }
        };

        context.TripChatMessages.AddRange(activeMessages);
        context.TripChatMessages.AddRange(finishedMessages);
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
                EvaluatorUserId = PassengerFiveId, // Diego Molina (Pasajero asociado al viaje recurrente)
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
                Subject = "Consulta sobre pago registrado",
                Description = "Solicite una revision del pago y quiero confirmar que quedo registrada.",
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
            new SupportTicketMessage { Id = Guid.Parse("33334444-5555-6666-7777-888899990005"), TicketId = TicketThreeId, SenderUserId = PassengerFourId, SenderKind = SupportMessageSenderKind.User, MessageText = "La revision aparece en el detalle del pago, pero quiero confirmar el estado.", CreatedAt = now.AddMinutes(-16) },
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

    private static async Task SeedAuditScenarioAsync(CarPoolingContext context)
    {
        if (await context.AuditLogs.AnyAsync(l => l.UserAgent == AuditDemoMarker))
        {
            return;
        }

        await ClearAuditScenarioAsync(context);

        var now = DateTime.UtcNow;
        var passwordHash = HashPassword("123456");
        var roleDriver = await context.Roles.FirstAsync(r => r.Name == "Driver");
        var roleStudent = await context.Roles.FirstAsync(r => r.Name == "Student");
        var admin = await context.Users.FirstOrDefaultAsync(u => u.Id == AdminId || u.Email == "admin@univalle.edu");
        var adminId = admin?.Id ?? AdminId;
        var adminEmail = admin?.Email ?? "admin@univalle.edu";

        var driverIds = Enumerable.Range(1, 6).Select(i => StableGuid($"audit-driver-{i:00}")).ToList();
        var passengerIds = Enumerable.Range(1, 14).Select(i => StableGuid($"audit-passenger-{i:00}")).ToList();
        var allUserIds = driverIds.Concat(passengerIds).ToList();

        var driverNames = new[]
        {
            "Carlos Mendoza",
            "Gabriela Espinoza",
            "Fernando Salvatierra",
            "Diana Gutierrez",
            "Mauricio Vargas",
            "Andrea Ortiz"
        };
        var driverEmails = new[]
        {
            "carlos.mendoza@univalle.edu",
            "gabriela.espinoza@univalle.edu",
            "fernando.salvatierra@univalle.edu",
            "diana.gutierrez@univalle.edu",
            "mauricio.vargas@univalle.edu",
            "andrea.ortiz@univalle.edu"
        };

        var passengerNames = new[]
        {
            "Lucas Rivero",
            "Camila Alarcon",
            "Mateo Flores",
            "Sofia Justiniano",
            "Sebastian Melgar",
            "Natalia Aguilera",
            "Alejandro Roca",
            "Valentina Suarez",
            "Daniel Chavez",
            "Isabella Guzman",
            "Santiago Banegas",
            "Luciana Prado",
            "Nicolas Hurtado",
            "Mariana Pinto"
        };
        var passengerEmails = new[]
        {
            "lucas.rivero@univalle.edu",
            "camila.alarcon@univalle.edu",
            "mateo.flores@univalle.edu",
            "sofia.justiniano@univalle.edu",
            "sebastian.melgar@univalle.edu",
            "natalia.aguilera@univalle.edu",
            "alejandro.roca@univalle.edu",
            "valentina.suarez@univalle.edu",
            "daniel.chavez@univalle.edu",
            "isabella.guzman@univalle.edu",
            "santiago.banegas@univalle.edu",
            "luciana.prado@univalle.edu",
            "nicolas.hurtado@univalle.edu",
            "mariana.pinto@univalle.edu"
        };

        var users = new List<User>();
        for (var i = 0; i < driverIds.Count; i++)
        {
            users.Add(DemoUser(
                driverIds[i],
                driverNames[i],
                driverEmails[i],
                $"76010{i + 1:000}",
                passwordHash,
                now.AddDays(-28 + i)));
        }

        for (var i = 0; i < passengerIds.Count; i++)
        {
            var user = DemoUser(
                passengerIds[i],
                passengerNames[i],
                passengerEmails[i],
                $"77020{i + 1:000}",
                passwordHash,
                now.AddDays(-24 + i));
            user.IsActive = i != passengerIds.Count - 1;
            users.Add(user);
        }

        var existingUserIds = await context.Users
            .Where(u => allUserIds.Contains(u.Id))
            .Select(u => u.Id)
            .ToListAsync();
        context.Users.AddRange(users.Where(u => !existingUserIds.Contains(u.Id)));
        await context.SaveChangesAsync();

        foreach (var driverId in driverIds)
        {
            if (!await context.UserRoles.AnyAsync(ur => ur.UserId == driverId && ur.RoleId == roleDriver.Id))
            {
                context.UserRoles.Add(new UserRole { UserId = driverId, RoleId = roleDriver.Id });
            }
        }

        foreach (var passengerId in passengerIds)
        {
            if (!await context.UserRoles.AnyAsync(ur => ur.UserId == passengerId && ur.RoleId == roleStudent.Id))
            {
                context.UserRoles.Add(new UserRole { UserId = passengerId, RoleId = roleStudent.Id });
            }
        }

        await context.SaveChangesAsync();

        var usersById = await context.Users
            .Where(u => allUserIds.Contains(u.Id) || u.Id == adminId)
            .ToDictionaryAsync(u => u.Id);

        var vehicles = driverIds.Select((driverId, index) => new Vehicle
        {
            Id = StableGuid($"audit-vehicle-{index + 1:00}"),
            OwnerUserId = driverId,
            LicensePlate = $"{((index + 1) * 739 + 1000)}-ABC",
            Brand = new[] { "Toyota", "Suzuki", "Nissan", "Hyundai", "Kia", "Chevrolet" }[index],
            Model = new[] { "Yaris", "Swift", "Kicks", "Accent", "Rio", "Onix" }[index],
            Color = new[] { "Blanco", "Gris", "Rojo", "Azul", "Negro", "Plata" }[index],
            VehicleYear = 2019 + index,
            TotalSeats = 4,
            IsActive = true,
            IsVerified = true,
            CreatedAt = now.AddDays(-20 + index)
        }).ToList();

        var vehicleIds = vehicles.Select(v => v.Id).ToList();
        var existingVehicleIds = await context.Vehicles
            .Where(v => vehicleIds.Contains(v.Id))
            .Select(v => v.Id)
            .ToListAsync();
        context.Vehicles.AddRange(vehicles.Where(v => !existingVehicleIds.Contains(v.Id)));

        var locations = new[]
        {
            new Location { Id = StableGuid("audit-location-campus"), Latitude = -17.74797, Longitude = -63.16611, AddressLabel = "Campus Univalle Santa Cruz - Av. Banzer Km 8", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-centro"), Latitude = -17.78330, Longitude = -63.18210, AddressLabel = "Plaza 24 de Septiembre - Centro historico", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-equipetrol"), Latitude = -17.76560, Longitude = -63.19320, AddressLabel = "Equipetrol - Av. San Martin y 3er Anillo", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-bimodal"), Latitude = -17.788889, Longitude = -63.161111, AddressLabel = "Terminal Bimodal Castulo Chavez - Av. Interradial", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-ventura"), Latitude = -17.75490, Longitude = -63.19820, AddressLabel = "Ventura Mall - Av. San Martin y 4to Anillo", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-cristo"), Latitude = -17.76940, Longitude = -63.18070, AddressLabel = "Monumento Cristo Redentor - 2do Anillo", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-palmas"), Latitude = -17.79020, Longitude = -63.20520, AddressLabel = "Barrio Las Palmas - Av. Pirai", CreatedAt = now.AddDays(-20) },
            new Location { Id = StableGuid("audit-location-plan"), Latitude = -17.83050, Longitude = -63.13810, AddressLabel = "Plan 3000 - Rotonda principal", CreatedAt = now.AddDays(-20) }
        }.ToList();

        var locationIds = locations.Select(l => l.Id).ToList();
        var existingLocationIds = await context.Locations
            .Where(l => locationIds.Contains(l.Id))
            .Select(l => l.Id)
            .ToListAsync();
        context.Locations.AddRange(locations.Where(l => !existingLocationIds.Contains(l.Id)));
        await context.SaveChangesAsync();

        var today = now.Date;
        var tripSeeds = new[]
        {
            new { StatusId = 4, DriverIndex = 0, OriginIndex = 1, ScheduledAt = today.AddDays(-12).AddHours(7).AddMinutes(15), Fare = 10m },
            new { StatusId = 4, DriverIndex = 1, OriginIndex = 2, ScheduledAt = today.AddDays(-10).AddHours(12), Fare = 11m },
            new { StatusId = 4, DriverIndex = 2, OriginIndex = 3, ScheduledAt = today.AddDays(-8).AddHours(18).AddMinutes(20), Fare = 12m },
            new { StatusId = 4, DriverIndex = 3, OriginIndex = 4, ScheduledAt = today.AddDays(-6).AddHours(6).AddMinutes(50), Fare = 9m },
            new { StatusId = 5, DriverIndex = 0, OriginIndex = 5, ScheduledAt = today.AddDays(-5).AddHours(16).AddMinutes(30), Fare = 10m },
            new { StatusId = 5, DriverIndex = 0, OriginIndex = 6, ScheduledAt = today.AddDays(-3).AddHours(7).AddMinutes(40), Fare = 10m },
            new { StatusId = 5, DriverIndex = 0, OriginIndex = 7, ScheduledAt = today.AddDays(-1).AddHours(13).AddMinutes(15), Fare = 12m },
            new { StatusId = 5, DriverIndex = 1, OriginIndex = 1, ScheduledAt = today.AddDays(-4).AddHours(18), Fare = 11m },
            new { StatusId = 5, DriverIndex = 1, OriginIndex = 2, ScheduledAt = today.AddDays(-2).AddHours(6).AddMinutes(45), Fare = 9m },
            new { StatusId = 2, DriverIndex = 3, OriginIndex = 3, ScheduledAt = now.AddHours(1).AddMinutes(20), Fare = 10m },
            new { StatusId = 2, DriverIndex = 4, OriginIndex = 4, ScheduledAt = now.AddHours(2).AddMinutes(5), Fare = 12m },
            new { StatusId = 3, DriverIndex = 5, OriginIndex = 5, ScheduledAt = now.AddMinutes(-25), Fare = 9m },
            new { StatusId = 1, DriverIndex = 2, OriginIndex = 6, ScheduledAt = today.AddDays(1).AddHours(7).AddMinutes(10), Fare = 10m },
            new { StatusId = 4, DriverIndex = 4, OriginIndex = 7, ScheduledAt = today.AddDays(-9).AddHours(17).AddMinutes(45), Fare = 11m },
            new { StatusId = 4, DriverIndex = 5, OriginIndex = 2, ScheduledAt = today.AddDays(-7).AddHours(11).AddMinutes(30), Fare = 12m },
            new { StatusId = 2, DriverIndex = 0, OriginIndex = 1, ScheduledAt = now.AddHours(3).AddMinutes(10), Fare = 10m },
            new { StatusId = 5, DriverIndex = 0, OriginIndex = 4, ScheduledAt = today.AddDays(-14).AddHours(7).AddMinutes(25), Fare = 9m },
            new { StatusId = 4, DriverIndex = 2, OriginIndex = 5, ScheduledAt = today.AddDays(-15).AddHours(18).AddMinutes(15), Fare = 12m }
        };

        var trips = tripSeeds.Select((seed, index) =>
        {
            var createdAt = seed.ScheduledAt.AddHours(-2).AddMinutes(-(index % 4) * 8);
            var startedAt = seed.StatusId is 3 or 4 ? seed.ScheduledAt.AddMinutes(seed.StatusId == 3 ? 2 : 5) : (DateTime?)null;
            var finishedAt = seed.StatusId == 4 ? seed.ScheduledAt.AddHours(1).AddMinutes(5 + index % 4) : (DateTime?)null;
            var cancelledAt = seed.StatusId == 5 ? seed.ScheduledAt.AddMinutes(-(10 + index % 3 * 7)) : (DateTime?)null;
            return new Trip
            {
                Id = StableGuid($"audit-trip-{index + 1:00}"),
                OriginLocationId = locations[seed.OriginIndex].Id,
                DestinationLocationId = locations[0].Id,
                StatusId = seed.StatusId,
                DriverUserId = driverIds[seed.DriverIndex],
                DriverName = usersById[driverIds[seed.DriverIndex]].FullName,
                VehicleId = vehicles[seed.DriverIndex].Id,
                OfferedSeats = 4,
                AvailableSeats = 4,
                FareAmount = seed.Fare,
                ScheduledDate = seed.ScheduledAt,
                CreatedAt = createdAt,
                UpdatedAt = createdAt.AddMinutes(15),
                StartedAt = startedAt,
                FinishedAt = finishedAt,
                CancelledAt = cancelledAt
            };
        }).ToList();

        var tripIds = trips.Select(t => t.Id).ToList();
        var existingTripIds = await context.Trips
            .Where(t => tripIds.Contains(t.Id))
            .Select(t => t.Id)
            .ToListAsync();
        context.Trips.AddRange(trips.Where(t => !existingTripIds.Contains(t.Id)));
        await context.SaveChangesAsync();

        var reservations = new List<Reservation>();
        void AddReservation(int index, int tripNumber, int passengerNumber, int statusId, int minutesAfterTripCreation)
        {
            var trip = trips[tripNumber - 1];
            reservations.Add(new Reservation
            {
                Id = StableGuid($"audit-reservation-{index:00}"),
                TripId = trip.Id,
                PassengerUserId = passengerIds[passengerNumber - 1],
                SeatsReserved = 1,
                StatusId = statusId,
                BoardingCode = $"{8000 + index}",
                CreatedAt = trip.CreatedAt.AddMinutes(minutesAfterTripCreation)
            });
        }

        AddReservation(1, 1, 1, 3, 8);
        AddReservation(2, 1, 2, 3, 10);
        AddReservation(3, 1, 3, 2, 12);
        AddReservation(4, 1, 4, 4, 16);
        AddReservation(5, 2, 3, 2, 9);
        AddReservation(6, 2, 5, 3, 11);
        AddReservation(7, 2, 6, 4, 14);
        AddReservation(8, 3, 3, 2, 8);
        AddReservation(9, 3, 7, 3, 12);
        AddReservation(10, 3, 8, 3, 14);
        AddReservation(11, 4, 4, 4, 7);
        AddReservation(12, 4, 6, 4, 11);
        AddReservation(13, 5, 9, 4, 6);
        AddReservation(14, 6, 4, 4, 6);
        AddReservation(15, 7, 10, 4, 6);
        AddReservation(16, 10, 11, 2, 5);
        AddReservation(17, 10, 12, 1, 7);
        AddReservation(18, 11, 13, 2, 6);
        AddReservation(19, 12, 1, 3, 8);
        AddReservation(20, 13, 2, 1, 5);
        AddReservation(21, 14, 5, 3, 9);
        AddReservation(22, 14, 3, 2, 12);
        AddReservation(23, 15, 6, 3, 8);
        AddReservation(24, 16, 7, 2, 6);
        AddReservation(25, 17, 4, 4, 7);
        AddReservation(26, 18, 8, 3, 9);

        foreach (var trip in trips)
        {
            var occupiedSeats = reservations
                .Where(r => r.TripId == trip.Id && r.StatusId is 2 or 3)
                .Sum(r => r.SeatsReserved);
            trip.AvailableSeats = trip.StatusId == 5
                ? trip.OfferedSeats
                : Math.Max(0, trip.OfferedSeats - occupiedSeats);
        }

        ValidateAuditScenario(trips, reservations);

        var reservationIds = reservations.Select(r => r.Id).ToList();
        var existingReservationIds = await context.Reservations
            .Where(r => reservationIds.Contains(r.Id))
            .Select(r => r.Id)
            .ToListAsync();
        context.Reservations.AddRange(reservations.Where(r => !existingReservationIds.Contains(r.Id)));
        await context.SaveChangesAsync();

        var tickets = new List<SupportTicket>();
        for (var i = 0; i < 10; i++)
        {
            var trip = trips[i % 9];
            var passengerId = passengerIds[(i + 2) % passengerIds.Count];
            tickets.Add(new SupportTicket
            {
                Id = StableGuid($"audit-ticket-{i + 1:00}"),
                UserId = passengerId,
                TripId = trip.Id,
                ReservationId = reservations.FirstOrDefault(r => r.TripId == trip.Id)?.Id,
                Category = SupportTicketCategory.Trip,
                Subject = i % 3 == 0 ? "Conductor cancelo con poca anticipacion" : "Incidente reportado en viaje",
                Description = i % 3 == 0 ? "El conductor cancelo el viaje pocos minutos antes de la hora de partida. Solicito revision del caso." : "Tuve un inconveniente con el conductor durante el recorrido por la avenida principal.",
                Status = i % 4 == 0 ? SupportTicketStatus.Resolved : i % 4 == 1 ? SupportTicketStatus.InReview : SupportTicketStatus.Open,
                AssignedAdminUserId = adminId,
                CreatedAt = trip.CreatedAt.AddHours(2),
                UpdatedAt = trip.CreatedAt.AddHours(3),
                FirstAdminReplyAt = i % 2 == 0 ? trip.CreatedAt.AddHours(2).AddMinutes(30) : null,
                LastMessageAt = trip.CreatedAt.AddHours(3),
                ClosedAt = i % 4 == 0 ? trip.CreatedAt.AddHours(4) : null
            });
        }

        var ticketIds = tickets.Select(t => t.Id).ToList();
        var existingTicketIds = await context.SupportTickets
            .Where(t => ticketIds.Contains(t.Id))
            .Select(t => t.Id)
            .ToListAsync();
        context.SupportTickets.AddRange(tickets.Where(t => !existingTicketIds.Contains(t.Id)));
        await context.SaveChangesAsync();

        var messages = tickets.Select((ticket, index) => new SupportTicketMessage
        {
            Id = StableGuid($"audit-ticket-message-{index + 1:00}"),
            TicketId = ticket.Id,
            SenderUserId = index % 2 == 0 ? ticket.UserId : adminId,
            SenderKind = index % 2 == 0 ? SupportMessageSenderKind.User : SupportMessageSenderKind.Admin,
            MessageText = index % 2 == 0
                ? "Por favor, requiero que se revise la situacion con urgencia, gracias."
                : "Hola, hemos recibido tu reporte. El caso esta siendo revisado por soporte estudiantil.",
            CreatedAt = ticket.CreatedAt.AddMinutes(15)
        }).ToList();

        var messageIds = messages.Select(m => m.Id).ToList();
        var existingMessageIds = await context.SupportTicketMessages
            .Where(m => messageIds.Contains(m.Id))
            .Select(m => m.Id)
            .ToListAsync();
        context.SupportTicketMessages.AddRange(messages.Where(m => !existingMessageIds.Contains(m.Id)));

        var ratings = trips
            .Where(t => t.StatusId == 4)
            .Take(6)
            .Select((trip, index) => new TripRating
            {
                Id = StableGuid($"audit-rating-{index + 1:00}"),
                TripId = trip.Id,
                EvaluatorUserId = passengerIds[index % passengerIds.Count],
                EvaluatedUserId = trip.DriverUserId!.Value,
                RatingRole = RatingRole.PassengerToDriver,
                Score = index % 5 == 0 ? 3 : 5,
                Comment = index % 5 == 0 ? "El conductor tardo unos 10 minutos en llegar al punto de encuentro." : "Excelente viaje, el conductor fue amable y manejo con precaucion.",
                Tags = index % 5 == 0 ? "Demora" : "Puntual,Seguro",
                CreatedAt = trip.FinishedAt?.AddMinutes(20) ?? now.AddDays(-1)
            })
            .ToList();
        var ratingIds = ratings.Select(r => r.Id).ToList();
        var existingRatingIds = await context.TripRatings
            .Where(r => ratingIds.Contains(r.Id))
            .Select(r => r.Id)
            .ToListAsync();
        context.TripRatings.AddRange(ratings.Where(r => !existingRatingIds.Contains(r.Id)));
        await context.SaveChangesAsync();

        var auditLogs = new List<AuditLog>();
        foreach (var user in users)
        {
            auditLogs.Add(DemoAuditLog(
                $"audit-log-user-registered-{user.Id}",
                user.Id,
                user.Email,
                "UserRegistered",
                "Users",
                "User",
                user.Id.ToString(),
                null,
                new { user.FullName, user.Email, user.PhoneNumber, user.IsActive },
                new[] { "FullName", "Email", "PhoneNumber", "IsActive" },
                user.CreatedAt,
                "Usuario registrado en la plataforma."));
        }

        foreach (var user in users.Take(12))
        {
            auditLogs.Add(DemoAuditLog(
                $"audit-log-login-{user.Id}",
                user.Id,
                user.Email,
                "UserLoggedIn",
                "Authentication",
                "User",
                user.Id.ToString(),
                null,
                new { Result = "Success" },
                new[] { "Session" },
                now.AddDays(-3).AddMinutes(users.IndexOf(user) * 7),
                "Inicio de sesión exitoso."));
        }

        foreach (var trip in trips)
        {
            auditLogs.Add(DemoAuditLog(
                $"audit-log-trip-created-{trip.Id}",
                trip.DriverUserId,
                usersById[trip.DriverUserId!.Value].Email,
                "UserCreatedTrip",
                "Trips",
                "Trip",
                trip.Id.ToString(),
                null,
                new { trip.DriverName, trip.StatusId, trip.OfferedSeats, trip.AvailableSeats, trip.FareAmount },
                new[] { "DriverName", "StatusId", "OfferedSeats", "AvailableSeats", "FareAmount" },
                trip.CreatedAt,
                "El conductor creó un nuevo viaje."));

            if (trip.StatusId == 5)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-trip-cancelled-{trip.Id}",
                    trip.DriverUserId,
                    usersById[trip.DriverUserId!.Value].Email,
                    "UserCancelledTrip",
                    "Trips",
                    "Trip",
                    trip.Id.ToString(),
                    new { StatusId = 1 },
                    new { trip.StatusId, trip.CancelledAt },
                    new[] { "StatusId", "CancelledAt" },
                    trip.CancelledAt ?? trip.CreatedAt.AddMinutes(25),
                    "El conductor canceló el viaje."));
            }
            else if (trip.StatusId is 3 or 4)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-trip-started-{trip.Id}",
                    trip.DriverUserId,
                    usersById[trip.DriverUserId!.Value].Email,
                    "UserStartedTrip",
                    "Trips",
                    "Trip",
                    trip.Id.ToString(),
                    new { StatusId = 2 },
                    new { StatusId = 3, trip.StartedAt },
                    new[] { "StatusId", "StartedAt" },
                    trip.StartedAt ?? trip.CreatedAt.AddMinutes(40),
                    "El conductor inicio el viaje."));
            }

            if (trip.StatusId == 4)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-trip-finished-{trip.Id}",
                    trip.DriverUserId,
                    usersById[trip.DriverUserId!.Value].Email,
                    "UserFinishedTrip",
                    "Trips",
                    "Trip",
                    trip.Id.ToString(),
                    new { StatusId = 3 },
                    new { trip.StatusId, trip.FinishedAt },
                    new[] { "StatusId", "FinishedAt" },
                    trip.FinishedAt ?? trip.CreatedAt.AddHours(1),
                    "El conductor finalizó el viaje."));
            }
        }

        foreach (var reservation in reservations)
        {
            var passenger = usersById[reservation.PassengerUserId];
            var reservationTrip = trips.First(t => t.Id == reservation.TripId);
            auditLogs.Add(DemoAuditLog(
                $"audit-log-reservation-created-{reservation.Id}",
                reservation.PassengerUserId,
                passenger.Email,
                "UserReservedSeat",
                "Reservations",
                "Reservation",
                reservation.Id.ToString(),
                null,
                new { reservation.TripId, reservation.PassengerUserId, reservation.StatusId, reservation.SeatsReserved },
                new[] { "TripId", "PassengerUserId", "StatusId", "SeatsReserved" },
                reservation.CreatedAt,
                "El pasajero realizó una reserva de asiento."));

            if (reservation.StatusId is 2 or 3 or 4)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-reservation-accepted-{reservation.Id}",
                    reservationTrip.DriverUserId,
                    usersById[reservationTrip.DriverUserId!.Value].Email,
                    "DriverAcceptedReservation",
                    "Reservations",
                    "Reservation",
                    reservation.Id.ToString(),
                    new { StatusId = 1 },
                    new { StatusId = 2 },
                    new[] { "StatusId" },
                    reservation.CreatedAt.AddMinutes(10),
                    "El conductor acepto la reserva."));
            }

            if (reservation.StatusId == 3)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-reservation-boarded-{reservation.Id}",
                    reservation.PassengerUserId,
                    passenger.Email,
                    "UserBoardedTrip",
                    "Reservations",
                    "Reservation",
                    reservation.Id.ToString(),
                    new { StatusId = 2 },
                    new { reservation.StatusId },
                    new[] { "StatusId" },
                    reservation.CreatedAt.AddMinutes(35),
                    "El pasajero abordó el vehículo."));
            }
            else if (reservation.StatusId == 4)
            {
                auditLogs.Add(DemoAuditLog(
                    $"audit-log-reservation-cancelled-{reservation.Id}",
                    reservation.PassengerUserId,
                    passenger.Email,
                    "UserCancelledReservation",
                    "Reservations",
                    "Reservation",
                    reservation.Id.ToString(),
                    new { StatusId = 2 },
                    new { reservation.StatusId },
                    new[] { "StatusId" },
                    reservation.CreatedAt.AddMinutes(18),
                    "El pasajero canceló su reserva."));
            }
        }

        foreach (var ticket in tickets)
        {
            var reporter = usersById[ticket.UserId];
            auditLogs.Add(DemoAuditLog(
                $"audit-log-ticket-created-{ticket.Id}",
                ticket.UserId,
                reporter.Email,
                "UserCreatedReport",
                "Support",
                "SupportTicket",
                ticket.Id.ToString(),
                null,
                new { ticket.Subject, ticket.Description, ticket.Status, ticket.TripId, ticket.ReservationId },
                new[] { "Subject", "Description", "Status", "TripId", "ReservationId" },
                ticket.CreatedAt,
                "El usuario reportó un inconveniente al soporte."));

            auditLogs.Add(DemoAuditLog(
                $"audit-log-ticket-admin-{ticket.Id}",
                adminId,
                adminEmail,
                "AdminUpdatedReportStatus",
                "Admin",
                "SupportTicket",
                ticket.Id.ToString(),
                new { Status = SupportTicketStatus.Open },
                new { ticket.Status, ticket.AssignedAdminUserId },
                new[] { "Status", "AssignedAdminUserId" },
                ticket.UpdatedAt ?? ticket.CreatedAt.AddHours(1),
                "El administrador revisó el reporte de soporte."));
        }

        var suspendedUser = users.Last();
        auditLogs.Add(DemoAuditLog(
            "audit-log-admin-disabled-demo-user",
            adminId,
            adminEmail,
            "AdminDisabledUser",
            "Admin",
            "User",
            suspendedUser.Id.ToString(),
            new { IsActive = true },
            new { IsActive = false },
            new[] { "IsActive" },
            now.AddDays(-2),
            "El administrador desactivó la cuenta de usuario por reporte."));

        var auditLogIds = auditLogs.Select(l => l.Id).ToList();
        var existingAuditLogIds = await context.AuditLogs
            .Where(l => auditLogIds.Contains(l.Id))
            .Select(l => l.Id)
            .ToListAsync();
        context.AuditLogs.AddRange(auditLogs.Where(l => !existingAuditLogIds.Contains(l.Id)));
        await context.SaveChangesAsync();
    }

    private static async Task ClearAuditScenarioAsync(CarPoolingContext context)
    {
        var driverIds = Enumerable.Range(1, 6).Select(i => StableGuid($"audit-driver-{i:00}")).ToList();
        var passengerIds = Enumerable.Range(1, 14).Select(i => StableGuid($"audit-passenger-{i:00}")).ToList();
        var userIds = driverIds.Concat(passengerIds).ToList();
        var vehicleIds = Enumerable.Range(1, 6).Select(i => StableGuid($"audit-vehicle-{i:00}")).ToList();
        var locationIds = new[]
        {
            StableGuid("audit-location-campus"),
            StableGuid("audit-location-centro"),
            StableGuid("audit-location-equipetrol"),
            StableGuid("audit-location-bimodal"),
            StableGuid("audit-location-ventura"),
            StableGuid("audit-location-cristo"),
            StableGuid("audit-location-palmas"),
            StableGuid("audit-location-plan")
        }.ToList();
        var tripIds = Enumerable.Range(1, 18).Select(i => StableGuid($"audit-trip-{i:00}")).ToList();
        var reservationIds = Enumerable.Range(1, 26).Select(i => StableGuid($"audit-reservation-{i:00}")).ToList();
        var ticketIds = Enumerable.Range(1, 10).Select(i => StableGuid($"audit-ticket-{i:00}")).ToList();
        var ticketMessageIds = Enumerable.Range(1, 10).Select(i => StableGuid($"audit-ticket-message-{i:00}")).ToList();
        var ratingIds = Enumerable.Range(1, 6).Select(i => StableGuid($"audit-rating-{i:00}")).ToList();

        var auditLogs = await context.AuditLogs
            .Where(l => l.UserAgent != null && AuditDemoMarkers.Contains(l.UserAgent))
            .ToListAsync();
        context.AuditLogs.RemoveRange(auditLogs);

        context.TripRatings.RemoveRange(await context.TripRatings.Where(r => ratingIds.Contains(r.Id)).ToListAsync());
        context.SupportTicketMessages.RemoveRange(await context.SupportTicketMessages.Where(m => ticketMessageIds.Contains(m.Id)).ToListAsync());
        context.SupportTickets.RemoveRange(await context.SupportTickets.Where(t => ticketIds.Contains(t.Id)).ToListAsync());
        context.Reservations.RemoveRange(await context.Reservations.Where(r => reservationIds.Contains(r.Id)).ToListAsync());
        context.Trips.RemoveRange(await context.Trips.Where(t => tripIds.Contains(t.Id)).ToListAsync());
        context.Vehicles.RemoveRange(await context.Vehicles.Where(v => vehicleIds.Contains(v.Id)).ToListAsync());
        context.UserRoles.RemoveRange(await context.UserRoles.Where(ur => userIds.Contains(ur.UserId)).ToListAsync());
        context.Users.RemoveRange(await context.Users.Where(u => userIds.Contains(u.Id)).ToListAsync());
        context.Locations.RemoveRange(await context.Locations.Where(l => locationIds.Contains(l.Id)).ToListAsync());

        await context.SaveChangesAsync();
    }

    private static void ValidateAuditScenario(IReadOnlyCollection<Trip> trips, IReadOnlyCollection<Reservation> reservations)
    {
        var activeTripsByDriver = trips
            .Where(t => t.DriverUserId.HasValue && t.StatusId is not 4 and not 5)
            .GroupBy(t => t.DriverUserId!.Value)
            .Where(g => g.Count() > 1)
            .ToList();

        if (activeTripsByDriver.Count > 0)
        {
            throw new InvalidOperationException("El seeder de auditoria no puede crear mas de un viaje activo por conductor.");
        }

        var activeTripIds = trips
            .Where(t => t.StatusId is not 4 and not 5)
            .Select(t => t.Id)
            .ToHashSet();

        var activeReservationsByPassenger = reservations
            .Where(r => r.StatusId != 4 && activeTripIds.Contains(r.TripId))
            .GroupBy(r => r.PassengerUserId)
            .Where(g => g.Count() > 1)
            .ToList();

        if (activeReservationsByPassenger.Count > 0)
        {
            throw new InvalidOperationException("El seeder de auditoria no puede crear mas de una reserva activa por pasajero.");
        }

        var overbookedTrips = trips
            .Where(t => reservations
                .Where(r => r.TripId == t.Id && r.StatusId is 2 or 3)
                .Sum(r => r.SeatsReserved) > t.OfferedSeats)
            .ToList();

        if (overbookedTrips.Count > 0)
        {
            throw new InvalidOperationException("El seeder de auditoria no puede crear viajes con mas reservas confirmadas que asientos ofrecidos.");
        }
    }

    private static AuditLog DemoAuditLog(
        string seed,
        Guid? actorUserId,
        string? actorEmail,
        string actionType,
        string module,
        string entityName,
        string? entityId,
        object? oldValues,
        object? newValues,
        string[] changedFields,
        DateTime createdAt,
        string description)
    {
        return new AuditLog
        {
            Id = StableGuid(seed),
            ActorUserId = actorUserId,
            ActorEmailSnapshot = actorEmail,
            ActionType = actionType,
            Module = module,
            EntityName = entityName,
            EntityId = entityId,
            OldValuesJson = oldValues is null ? null : JsonSerializer.Serialize(oldValues),
            NewValuesJson = newValues is null ? null : JsonSerializer.Serialize(newValues),
            ChangedFieldsJson = JsonSerializer.Serialize(changedFields),
            Result = "Success",
            Description = description,
            IpAddress = "127.0.0.1",
            UserAgent = AuditDemoMarker,
            RequestPath = "/seed/audit-demo",
            CreatedAt = createdAt
        };
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

    private static Guid StableGuid(string seed)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(seed));
        var bytes = new byte[16];
        Array.Copy(hash, bytes, bytes.Length);
        return new Guid(bytes);
    }
}
