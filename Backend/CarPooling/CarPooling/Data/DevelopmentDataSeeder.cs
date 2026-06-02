using System.Security.Cryptography;
using System.Text;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;
using CarPooling.Security;

namespace CarPooling.Data;

public static class DevelopmentDataSeeder
{
    private static readonly Guid AdminId = Guid.Parse("11111111-1111-1111-1111-111111111111");
    
    // Drivers (1-5)
    private static readonly Guid DriverOneId = Guid.Parse("22222222-2222-2222-2222-222222222222");
    private static readonly Guid DriverTwoId = Guid.Parse("33333333-3333-3333-3333-333333333333");
    private static readonly Guid DriverThreeId = Guid.Parse("22222222-2222-2222-2222-222222222223");
    private static readonly Guid DriverFourId = Guid.Parse("22222222-2222-2222-2222-222222222224");
    private static readonly Guid DriverFiveId = Guid.Parse("22222222-2222-2222-2222-222222222225");

    // Students (1-6)
    private static readonly Guid StudentOneId = Guid.Parse("44444444-4444-4444-4444-444444444444");
    private static readonly Guid StudentTwoId = Guid.Parse("55555555-5555-5555-5555-555555555555");
    private static readonly Guid StudentThreeId = Guid.Parse("66666666-6666-6666-6666-666666666666");
    private static readonly Guid StudentFourId = Guid.Parse("44444444-4444-4444-4444-444444444445");
    private static readonly Guid StudentFiveId = Guid.Parse("44444444-4444-4444-4444-444444444446");
    private static readonly Guid StudentSixId = Guid.Parse("44444444-4444-4444-4444-444444444447");

    // Driver Profiles (1-5)
    private static readonly Guid DriverOneProfileId = Guid.Parse("77777777-7777-7777-7777-777777777777");
    private static readonly Guid DriverTwoProfileId = Guid.Parse("88888888-8888-8888-8888-888888888888");
    private static readonly Guid DriverThreeProfileId = Guid.Parse("77777777-7777-7777-7777-777777777778");
    private static readonly Guid DriverFourProfileId = Guid.Parse("77777777-7777-7777-7777-777777777779");
    private static readonly Guid DriverFiveProfileId = Guid.Parse("77777777-7777-7777-7777-777777777780");

    // Vehicles (1-5)
    private static readonly Guid VehicleOneId = Guid.Parse("99999999-9999-9999-9999-999999999991");
    private static readonly Guid VehicleTwoId = Guid.Parse("99999999-9999-9999-9999-999999999992");
    private static readonly Guid VehicleThreeId = Guid.Parse("99999999-9999-9999-9999-999999999993");
    private static readonly Guid VehicleFourId = Guid.Parse("99999999-9999-9999-9999-999999999994");
    private static readonly Guid VehicleFiveId = Guid.Parse("99999999-9999-9999-9999-999999999995");

    // Locations (1-12)
    private static readonly Guid LocationOneId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static readonly Guid LocationTwoId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static readonly Guid LocationThreeId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static readonly Guid LocationFourId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4");
    private static readonly Guid LocationFiveId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5");
    private static readonly Guid LocationSixId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6");
    private static readonly Guid LocationSevenId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7");
    private static readonly Guid LocationEightId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8");
    private static readonly Guid LocationNineId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9");
    private static readonly Guid LocationTenId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10");
    private static readonly Guid LocationElevenId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11");
    private static readonly Guid LocationTwelveId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12");

    // Trips (1-20)
    private static readonly Guid TripOneId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
    private static readonly Guid TripTwoId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private static readonly Guid TripThreeId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");
    private static readonly Guid TripFourId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4");
    private static readonly Guid TripFiveId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5");
    private static readonly Guid TripSixId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb6");
    private static readonly Guid TripSevenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb7");
    private static readonly Guid TripEightId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb8");
    private static readonly Guid TripNineId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb9");
    private static readonly Guid TripTenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb10");
    private static readonly Guid TripElevenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb11");
    private static readonly Guid TripTwelveId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb12");
    private static readonly Guid TripThirteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb13");
    private static readonly Guid TripFourteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb14");
    private static readonly Guid TripFifteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb15");
    private static readonly Guid TripSixteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb16");
    private static readonly Guid TripSeventeenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb17");
    private static readonly Guid TripEighteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb18");
    private static readonly Guid TripNineteenId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb19");
    private static readonly Guid TripTwentyId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb20");

    // Reservations (1-6)
    private static readonly Guid ResOneId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc1");
    private static readonly Guid ResTwoId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccc102");
    private static readonly Guid ResThreeId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccc103");
    private static readonly Guid ResFourId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccc104");
    private static readonly Guid ResFiveId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccc105");
    private static readonly Guid ResSixId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccc106");

    // Chats (1-6)
    private static readonly Guid ChatOneId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static readonly Guid ChatTwoId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd2");
    private static readonly Guid ChatThreeId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd3");
    private static readonly Guid ChatFiveId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd5");
    private static readonly Guid ChatSevenId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd7");
    private static readonly Guid ChatEightId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd8");

    // Messages (1-10)
    private static readonly Guid MsgOneId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
    private static readonly Guid MsgTwoId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2");
    private static readonly Guid MsgThreeId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee3");
    private static readonly Guid MsgFourId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee4");
    private static readonly Guid MsgFiveId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee5");
    private static readonly Guid MsgSixId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee6");
    private static readonly Guid MsgSevenId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee7");
    private static readonly Guid MsgEightId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee8");
    private static readonly Guid MsgNineId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee9");
    private static readonly Guid MsgTenId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeee10");

    // Ratings (1-4)
    private static readonly Guid RatingOneId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff1");
    private static readonly Guid RatingTwoId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff2");
    private static readonly Guid RatingThreeId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff3");
    private static readonly Guid RatingFourId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff4");

    // Tickets (1-3)
    private static readonly Guid TicketOneId = Guid.Parse("11112222-3333-4444-5555-666677778881");
    private static readonly Guid TicketTwoId = Guid.Parse("11112222-3333-4444-5555-666677778882");
    private static readonly Guid TicketThreeId = Guid.Parse("11112222-3333-4444-5555-666677778883");

    public static async Task SeedAsync(IServiceProvider services)
    {
        using var scope = services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<CarPoolingContext>();

        await context.Database.MigrateAsync();

        // 0. Seed Permissions
        foreach (var p in AppPermissions.AllPermissions)
        {
            var existingPermission = await context.Permissions.FirstOrDefaultAsync(dbP => dbP.Id == p.Id);
            if (existingPermission == null)
            {
                context.Permissions.Add(new Permission
                {
                    Id = p.Id,
                    Name = p.Name,
                    GroupName = p.GroupName
                });
            }
        }
        await context.SaveChangesAsync();

        // Seed Roles (SuperAdmin, Student, Driver, Analyst)
        var rolesToSeed = new[]
        {
            new { Name = "SuperAdmin", Description = "Acceso total al sistema", IsSystemRole = true },
            new { Name = "Student", Description = "Rol por defecto para estudiantes", IsSystemRole = true },
            new { Name = "Driver", Description = "Rol por defecto para conductores", IsSystemRole = true },
            new { Name = "Analyst", Description = "Rol limitado para visualizar métricas", IsSystemRole = false }
        };

        foreach (var r in rolesToSeed)
        {
            var existingRole = await context.Roles
                .Include(dbR => dbR.RolePermissions)
                .FirstOrDefaultAsync(dbR => dbR.Name == r.Name);

            if (existingRole == null)
            {
                var newRole = new Role
                {
                    Id = Guid.NewGuid(),
                    Name = r.Name,
                    Description = r.Description,
                    IsSystemRole = r.IsSystemRole
                };
                context.Roles.Add(newRole);
                existingRole = newRole;
                await context.SaveChangesAsync();
            }

            // Assign permissions to SuperAdmin
            if (r.Name == "SuperAdmin")
            {
                var allDbPermissions = await context.Permissions.ToListAsync();
                foreach (var dbP in allDbPermissions)
                {
                    if (!existingRole.RolePermissions.Any(rp => rp.PermissionId == dbP.Id))
                    {
                        context.RolePermissions.Add(new RolePermission { RoleId = existingRole.Id, PermissionId = dbP.Id });
                    }
                }
            }
            // Assign permissions to Analyst
            else if (r.Name == "Analyst")
            {
                var analystPermissions = new[] { AppPermissions.ViewMetrics, AppPermissions.ReadUsers, AppPermissions.ReadTrips };
                foreach (var pId in analystPermissions)
                {
                    if (!existingRole.RolePermissions.Any(rp => rp.PermissionId == pId))
                    {
                        context.RolePermissions.Add(new RolePermission { RoleId = existingRole.Id, PermissionId = pId });
                    }
                }
            }
        }
        await context.SaveChangesAsync();

        var passwordHash = HashPassword("123456");

        // 1. Users (using dynamic role name)
        var admin = await UpsertUserAsync(context, AdminId, "Administrador", "admin@univalle.edu", passwordHash, "SuperAdmin");
        
        // Drivers
        var driverOne = await UpsertUserAsync(context, DriverOneId, "Carlos Rojas", "carlos.rojas@univalle.edu", passwordHash, "Driver", "+57 300 111 2233");
        var driverTwo = await UpsertUserAsync(context, DriverTwoId, "Mariana Torres", "mariana.torres@univalle.edu", passwordHash, "Driver", "+57 300 222 3344");
        var driverThree = await UpsertUserAsync(context, DriverThreeId, "Andrés Castro", "andres.castro@univalle.edu", passwordHash, "Driver", "+57 300 666 7788");
        var driverFour = await UpsertUserAsync(context, DriverFourId, "Diana Morales", "diana.morales@univalle.edu", passwordHash, "Driver", "+57 300 777 8899");
        var driverFive = await UpsertUserAsync(context, DriverFiveId, "Mateo Restrepo", "mateo.restrepo@univalle.edu", passwordHash, "Driver", "+57 300 888 9900");

        // Students
        var studentOne = await UpsertUserAsync(context, StudentOneId, "Valentina Pérez", "valentina.perez@univalle.edu", passwordHash, "Student", "+57 300 333 4455");
        var studentTwo = await UpsertUserAsync(context, StudentTwoId, "Santiago Gómez", "santiago.gomez@univalle.edu", passwordHash, "Student", "+57 300 444 5566");
        var studentThree = await UpsertUserAsync(context, StudentThreeId, "Laura Ramírez", "laura.ramirez@univalle.edu", passwordHash, "Student", "+57 300 555 6677");
        var studentFour = await UpsertUserAsync(context, StudentFourId, "Sofía Ortiz", "sofia.ortiz@univalle.edu", passwordHash, "Student", "+57 300 999 1122");
        var studentFive = await UpsertUserAsync(context, StudentFiveId, "Alejandro Ruiz", "alejandro.ruiz@univalle.edu", passwordHash, "Student", "+57 300 999 2233");
        var studentSix = await UpsertUserAsync(context, StudentSixId, "Gabriela Lugo", "gabriela.lugo@univalle.edu", passwordHash, "Student", "+57 300 999 3344");

        // 2. Driver Profiles
        await UpsertDriverProfileAsync(context, DriverOneProfileId, driverOne.Id, "LIC-DRIVER-001", true);
        await UpsertDriverProfileAsync(context, DriverTwoProfileId, driverTwo.Id, "LIC-DRIVER-002", true);
        await UpsertDriverProfileAsync(context, DriverThreeProfileId, driverThree.Id, "LIC-DRIVER-003", true);
        await UpsertDriverProfileAsync(context, DriverFourProfileId, driverFour.Id, "LIC-DRIVER-004", true);
        await UpsertDriverProfileAsync(context, DriverFiveProfileId, driverFive.Id, "LIC-DRIVER-005", true);

        // 3. Vehicles
        var vehicleOne = await UpsertVehicleAsync(context, VehicleOneId, driverOne.Id, "ABC123", "Kia", "Rio", "Blanco", 2021, 4, true, true);
        var vehicleTwo = await UpsertVehicleAsync(context, VehicleTwoId, driverTwo.Id, "XYZ789", "Chevrolet", "Spark", "Gris", 2020, 4, true, true);
        var vehicleThree = await UpsertVehicleAsync(context, VehicleThreeId, driverThree.Id, "MNO456", "Mazda", "3", "Azul", 2022, 4, true, true);
        var vehicleFour = await UpsertVehicleAsync(context, VehicleFourId, driverFour.Id, "JKL321", "Renault", "Logan", "Rojo", 2019, 4, true, true);
        var vehicleFive = await UpsertVehicleAsync(context, VehicleFiveId, driverFive.Id, "VWX654", "Toyota", "Corolla", "Plateado", 2023, 4, true, true);

        // 4. Locations in Cali, Colombia
        var campus = await UpsertLocationAsync(context, LocationOneId, 3.374, -76.532, "Universidad del Valle, Meléndez");
        var sanFernando = await UpsertLocationAsync(context, LocationTwoId, 3.424, -76.545, "San Fernando, Cali");
        var ingenio = await UpsertLocationAsync(context, LocationThreeId, 3.391, -76.523, "El Ingenio, Cali");
        var oeste = await UpsertLocationAsync(context, LocationFourId, 3.458, -76.544, "Sector Oeste, Cali");
        var norte = await UpsertLocationAsync(context, LocationFiveId, 3.501, -76.509, "Chipichape / Norte, Cali");
        var sur = await UpsertLocationAsync(context, LocationSixId, 3.369, -76.531, "Unicentro / Sur, Cali");
        var ciudadJardin = await UpsertLocationAsync(context, LocationSevenId, 3.352, -76.533, "Ciudad Jardín, Cali");
        var terminal = await UpsertLocationAsync(context, LocationEightId, 3.468, -76.522, "Terminal de Transportes, Cali");
        var menga = await UpsertLocationAsync(context, LocationNineId, 3.491, -76.516, "Menga, Cali");
        var icesi = await UpsertLocationAsync(context, LocationTenId, 3.341, -76.530, "Universidad Icesi, Cali");
        var pance = await UpsertLocationAsync(context, LocationElevenId, 3.328, -76.535, "Pance, Cali");
        var bulevar = await UpsertLocationAsync(context, LocationTwelveId, 3.454, -76.533, "Bulevar del Río, Cali");

        // 5. Trips (Mix of scheduled, ready, in_progress, finished, cancelled)
        // Trip 1: campus -> sanFernando, Programado, driverOne, vehicleOne, 4 offered, 3 available (1 confirmed passenger: studentOne)
        await UpsertTripAsync(context, TripOneId, campus.Id, sanFernando.Id, 1, driverOne.Id, driverOne.FullName, vehicleOne.Id, 4, 3,
            TripKind.Regular, DateTime.UtcNow.AddHours(2), null, null, null);

        // Trip 2: sanFernando -> campus, Programado, driverTwo, vehicleTwo, 3 offered, 2 available (1 confirmed passenger: studentTwo)
        await UpsertTripAsync(context, TripTwoId, sanFernando.Id, campus.Id, 1, driverTwo.Id, driverTwo.FullName, vehicleTwo.Id, 3, 2,
            TripKind.Regular, DateTime.UtcNow.AddHours(3), null, null, null);

        // Trip 3: norte -> campus, Listo, driverOne, vehicleOne, 4 offered, 1 available
        await UpsertTripAsync(context, TripThreeId, norte.Id, campus.Id, 2, driverOne.Id, driverOne.FullName, vehicleOne.Id, 4, 1,
            TripKind.Regular, DateTime.UtcNow.AddMinutes(45), DateTime.UtcNow.AddMinutes(20), null, null);

        // Trip 4: campus -> sur, Programado, driverTwo, vehicleTwo, 4 offered, 4 available
        await UpsertTripAsync(context, TripFourId, campus.Id, sur.Id, 1, driverTwo.Id, driverTwo.FullName, vehicleTwo.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(5), null, null, null);

        // Trip 5: sur -> menga, Programado, driverThree, vehicleThree, 4 offered, 2 available (2 confirmed passengers: studentThree, studentFour)
        await UpsertTripAsync(context, TripFiveId, sur.Id, menga.Id, 1, driverThree.Id, driverThree.FullName, vehicleThree.Id, 4, 2,
            TripKind.Regular, DateTime.UtcNow.AddHours(4), null, null, null);

        // Trip 6: terminal -> icesi, Programado, driverFour, vehicleFour, 4 offered, 4 available (1 pending passenger: studentFive)
        await UpsertTripAsync(context, TripSixId, terminal.Id, icesi.Id, 1, driverFour.Id, driverFour.FullName, vehicleFour.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(6), null, null, null);

        // Trip 7: bulevar -> campus, En curso, driverFive, vehicleFive, 4 offered, 3 available (1 boarded passenger: studentSix)
        await UpsertTripAsync(context, TripSevenId, bulevar.Id, campus.Id, 3, driverFive.Id, driverFive.FullName, vehicleFive.Id, 4, 3,
            TripKind.Regular, DateTime.UtcNow.AddMinutes(-10), DateTime.UtcNow.AddMinutes(-5), null, null);

        // Trip 8: campus -> ciudadJardin, Finalizado, driverOne, vehicleOne, 4 offered, 2 available
        await UpsertTripAsync(context, TripEightId, campus.Id, ciudadJardin.Id, 4, driverOne.Id, driverOne.FullName, vehicleOne.Id, 4, 2,
            TripKind.Regular, DateTime.UtcNow.AddHours(-4), DateTime.UtcNow.AddHours(-3), DateTime.UtcNow.AddHours(-2), null);

        // Trip 9: pance -> norte, Cancelado, driverTwo, vehicleTwo, 4 offered, 4 available
        await UpsertTripAsync(context, TripNineId, pance.Id, norte.Id, 5, driverTwo.Id, driverTwo.FullName, vehicleTwo.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(-6), null, null, DateTime.UtcNow.AddHours(-5));

        // Trip 10: menga -> sur, Programado, driverThree, vehicleThree, 4 offered, 4 available
        await UpsertTripAsync(context, TripTenId, menga.Id, sur.Id, 1, driverThree.Id, driverThree.FullName, vehicleThree.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddDays(1), null, null, null);

        // Trip 11: campus -> terminal, Programado, driverFour, vehicleFour, 4 offered, 4 available
        await UpsertTripAsync(context, TripElevenId, campus.Id, terminal.Id, 1, driverFour.Id, driverFour.FullName, vehicleFour.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddDays(1).AddHours(2), null, null, null);

        // Trip 12: icesi -> bulevar, Listo, driverFive, vehicleFive, 4 offered, 4 available
        await UpsertTripAsync(context, TripTwelveId, icesi.Id, bulevar.Id, 2, driverFive.Id, driverFive.FullName, vehicleFive.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddDays(1).AddHours(4), DateTime.UtcNow.AddDays(1).AddHours(4).AddMinutes(10), null, null);

        // Trip 13: ciudadJardin -> campus, Programado, driverOne, vehicleOne, 4 offered, 4 available
        await UpsertTripAsync(context, TripThirteenId, ciudadJardin.Id, campus.Id, 1, driverOne.Id, driverOne.FullName, vehicleOne.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddDays(2), null, null, null);

        // Trip 14: sanFernando -> sur, En curso, driverTwo, vehicleTwo, 3 offered, 3 available
        await UpsertTripAsync(context, TripFourteenId, sanFernando.Id, sur.Id, 3, driverTwo.Id, driverTwo.FullName, vehicleTwo.Id, 3, 3,
            TripKind.Regular, DateTime.UtcNow.AddMinutes(-30), DateTime.UtcNow.AddMinutes(-20), null, null);

        // Trip 15: oeste -> campus, Finalizado, driverThree, vehicleThree, 4 offered, 0 available
        await UpsertTripAsync(context, TripFifteenId, oeste.Id, campus.Id, 4, driverThree.Id, driverThree.FullName, vehicleThree.Id, 4, 0,
            TripKind.Regular, DateTime.UtcNow.AddDays(-1), DateTime.UtcNow.AddDays(-1).AddHours(1), DateTime.UtcNow.AddDays(-1).AddHours(2), null);

        // Trip 16: campus -> oeste, Programado, driverFour, vehicleFour, 4 offered, 4 available
        await UpsertTripAsync(context, TripSixteenId, campus.Id, oeste.Id, 1, driverFour.Id, driverFour.FullName, vehicleFour.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(7), null, null, null);

        // Trip 17: norte -> pance, Programado, driverFive, vehicleFive, 4 offered, 4 available
        await UpsertTripAsync(context, TripSeventeenId, norte.Id, pance.Id, 1, driverFive.Id, driverFive.FullName, vehicleFive.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(9), null, null, null);

        // Trip 18: campus -> ingenio, Programado, driverOne, vehicleOne, 4 offered, 4 available
        await UpsertTripAsync(context, TripEighteenId, campus.Id, ingenio.Id, 1, driverOne.Id, driverOne.FullName, vehicleOne.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(11), null, null, null);

        // Trip 19: terminal -> sur, Programado, driverTwo, vehicleTwo, 3 offered, 3 available
        await UpsertTripAsync(context, TripNineteenId, terminal.Id, sur.Id, 1, driverTwo.Id, driverTwo.FullName, vehicleTwo.Id, 3, 3,
            TripKind.Regular, DateTime.UtcNow.AddHours(13), null, null, null);

        // Trip 20: icesi -> campus, Programado, driverThree, vehicleThree, 4 offered, 4 available
        await UpsertTripAsync(context, TripTwentyId, icesi.Id, campus.Id, 1, driverThree.Id, driverThree.FullName, vehicleThree.Id, 4, 4,
            TripKind.Regular, DateTime.UtcNow.AddHours(15), null, null, null);

        // 6. Reservations
        // Res 1 (Trip 1 - studentOne): Confirmed (status = 2)
        await UpsertReservationAsync(context, ResOneId, TripOneId, studentOne.Id, 1, 2, "1234");
        
        // Res 2 (Trip 2 - studentTwo): Confirmed (status = 2)
        await UpsertReservationAsync(context, ResTwoId, TripTwoId, studentTwo.Id, 1, 2, "5678");
        
        // Res 3 (Trip 5 - studentThree): Confirmed (status = 2)
        await UpsertReservationAsync(context, ResThreeId, TripFiveId, studentThree.Id, 1, 2, "9012");
        
        // Res 4 (Trip 5 - studentFour): Confirmed (status = 2)
        await UpsertReservationAsync(context, ResFourId, TripFiveId, studentFour.Id, 1, 2, "3456");
        
        // Res 5 (Trip 6 - studentFive): Pending (status = 1)
        await UpsertReservationAsync(context, ResFiveId, TripSixId, studentFive.Id, 1, 1, "7890");
        
        // Res 6 (Trip 7 - studentSix): Boarded (status = 3)
        await UpsertReservationAsync(context, ResSixId, TripSevenId, studentSix.Id, 1, 3, "2468");

        // 7. Trip Chats
        // Chat for Trip 1 (Carlos Rojas)
        await UpsertTripChatAsync(context, ChatOneId, TripOneId, DateTime.UtcNow.AddHours(-1));
        
        // Chat for Trip 2 (Mariana Torres)
        await UpsertTripChatAsync(context, ChatTwoId, TripTwoId, DateTime.UtcNow.AddHours(-2));

        // Chat for Trip 3 (Andrés Castro)
        await UpsertTripChatAsync(context, ChatThreeId, TripThreeId, DateTime.UtcNow.AddMinutes(-30));

        // Chat for Trip 5 (Diana Morales)
        await UpsertTripChatAsync(context, ChatFiveId, TripFiveId, DateTime.UtcNow.AddHours(-3));

        // Chat for Trip 7 (Mateo Restrepo - In Progress)
        await UpsertTripChatAsync(context, ChatSevenId, TripSevenId, DateTime.UtcNow.AddMinutes(-40));

        // Chat for Trip 8 (Carlos Rojas - Finished)
        await UpsertTripChatAsync(context, ChatEightId, TripEightId, DateTime.UtcNow.AddHours(-5));

        // 8. Trip Chat Messages
        // Chat 1 (Carlos & Valentina)
        await UpsertTripChatMessageAsync(context, MsgOneId, ChatOneId, driverOne.Id, "Hola Valentina, ¿a qué hora sales de clase hoy?", DateTime.UtcNow.AddMinutes(-50));
        await UpsertTripChatMessageAsync(context, MsgTwoId, ChatOneId, studentOne.Id, "Hola Carlos, salgo a las 2 PM puntualmente. Nos vemos en el paradero de Meléndez.", DateTime.UtcNow.AddMinutes(-45));
        await UpsertTripChatMessageAsync(context, MsgThreeId, ChatOneId, driverOne.Id, "Perfecto, allí estaré puntual.", DateTime.UtcNow.AddMinutes(-40));

        // Chat 2 (Mariana & Santiago)
        await UpsertTripChatMessageAsync(context, MsgFourId, ChatTwoId, driverTwo.Id, "Hola Santiago, te espero en la entrada de la terminal.", DateTime.UtcNow.AddMinutes(-30));
        await UpsertTripChatMessageAsync(context, MsgFiveId, ChatTwoId, studentTwo.Id, "Listo Mariana, ya voy saliendo.", DateTime.UtcNow.AddMinutes(-25));

        // Chat 7 (Mateo & Gabriela - En curso)
        await UpsertTripChatMessageAsync(context, MsgSixId, ChatSevenId, driverFive.Id, "Hola Gabriela, ya estoy en el punto de encuentro.", DateTime.UtcNow.AddMinutes(-15));
        await UpsertTripChatMessageAsync(context, MsgSevenId, ChatSevenId, studentSix.Id, "Hola Mateo, ya te vi. Me acerco al carro.", DateTime.UtcNow.AddMinutes(-12));
        await UpsertTripChatMessageAsync(context, MsgEightId, ChatSevenId, driverFive.Id, "Listo, sube y validamos el código de abordaje.", DateTime.UtcNow.AddMinutes(-10));

        // 9. Trip Chat Message Reads
        await UpsertTripChatMessageReadAsync(context, MsgOneId, studentOne.Id, DateTime.UtcNow.AddMinutes(-48));
        await UpsertTripChatMessageReadAsync(context, MsgTwoId, driverOne.Id, DateTime.UtcNow.AddMinutes(-42));
        await UpsertTripChatMessageReadAsync(context, MsgFourId, studentTwo.Id, DateTime.UtcNow.AddMinutes(-28));
        await UpsertTripChatMessageReadAsync(context, MsgSixId, studentSix.Id, DateTime.UtcNow.AddMinutes(-14));

        // 10. Trip Ratings (For Trip 8 - Finished)
        // Passenger to Driver
        await UpsertTripRatingAsync(context, RatingOneId, TripEightId, studentOne.Id, driverOne.Id, RatingRole.PassengerToDriver, 5, 
            "Excelente viaje, el conductor fue muy amable y el vehículo estaba impecable.", "Amable,Puntual,Limpio", DateTime.UtcNow.AddHours(-1));
        // Driver to Passenger
        await UpsertTripRatingAsync(context, RatingTwoId, TripEightId, driverOne.Id, studentOne.Id, RatingRole.DriverToPassenger, 5, 
            "Pasajero muy puntual y educado. Totalmente recomendado.", "Educado,Puntual", DateTime.UtcNow.AddHours(-1));

        // For Trip 15 - Finished
        // Passenger to Driver
        await UpsertTripRatingAsync(context, RatingThreeId, TripFifteenId, studentThree.Id, driverThree.Id, RatingRole.PassengerToDriver, 4, 
            "Buen conductor, pero la ruta tuvo bastante tráfico.", "Puntual,Respetuoso", DateTime.UtcNow.AddDays(-1));
        // Driver to Passenger
        await UpsertTripRatingAsync(context, RatingFourId, TripFifteenId, driverThree.Id, studentThree.Id, RatingRole.DriverToPassenger, 5, 
            "Excelente pasajero.", "Silencioso", DateTime.UtcNow.AddDays(-1));

        // 11. Support Tickets
        // Ticket 1: Student One requesting help for a trip issue (Trip 8)
        await UpsertSupportTicketAsync(context, TicketOneId, studentOne.Id, TripEightId, null, SupportTicketCategory.Trip,
            "Objeto olvidado en el vehículo", "Dejé mi paraguas de color negro en el asiento trasero del Kia Rio Blanco. Agradezco si me ayudan a contactar al conductor.",
            SupportTicketStatus.Open, DateTime.UtcNow.AddHours(-3));

        // Ticket 2: Driver One requesting help for account
        await UpsertSupportTicketAsync(context, TicketTwoId, driverOne.Id, null, null, SupportTicketCategory.Account,
            "Problemas al actualizar foto de perfil", "Intento subir una nueva foto de perfil pero me sale error de red. Ya verifiqué mi formato y tamaño de imagen.",
            SupportTicketStatus.InReview, DateTime.UtcNow.AddDays(-1));

        // Ticket 3: Student Five requesting help for a reservation issue
        await UpsertSupportTicketAsync(context, TicketThreeId, studentFive.Id, null, ResFiveId, SupportTicketCategory.Reservation,
            "Duda con cobro de reserva", "Deseo saber si el cobro estimado de la reserva es fijo o si puede variar de acuerdo a la ruta final que tome el conductor.",
            SupportTicketStatus.Resolved, DateTime.UtcNow.AddDays(-2));

        // Seed default theme if not present
        if (!await context.AppSettings.AnyAsync(s => s.Key == "theme"))
        {
            context.AppSettings.Add(new AppSetting 
            { 
                Key = "theme", 
                Value = "{\"primaryLight\":\"#e08c75\",\"secondaryLight\":\"#6b8f8d\",\"textLight\":\"#1f1d1a\",\"primaryDark\":\"#e27b53\",\"secondaryDark\":\"#85aba9\",\"textDark\":\"#e0e0e0\"}" 
            });
        }

        await context.SaveChangesAsync();
    }

    private static async Task<User> UpsertUserAsync(
        CarPoolingContext context,
        Guid id,
        string fullName,
        string email,
        string passwordHash,
        string roleName,
        string? phoneNumber = null)
    {
        var user = await context.Users
            .Include(u => u.UserRoles)
            .FirstOrDefaultAsync(item => item.Email == email);

        if (user is null)
        {
            user = new User { Id = id };
            context.Users.Add(user);
        }

        user.FullName = fullName;
        user.Email = email;
        user.PasswordHash = passwordHash;
        user.PhoneNumber = phoneNumber;

        var role = await context.Roles.FirstOrDefaultAsync(r => r.Name == roleName);
        if (role != null)
        {
            context.UserRoles.RemoveRange(user.UserRoles);
            user.UserRoles.Add(new UserRole { UserId = user.Id, RoleId = role.Id });
        }

        return user;
    }

    private static async Task UpsertDriverProfileAsync(
        CarPoolingContext context,
        Guid id,
        Guid userId,
        string licenseNumber,
        bool isVerified)
    {
        var profile = await context.DriverProfiles.FirstOrDefaultAsync(item => item.UserId == userId);
        if (profile is null)
        {
            profile = new DriverProfile { Id = id, UserId = userId };
            context.DriverProfiles.Add(profile);
        }

        profile.UserId = userId;
        profile.LicenseNumber = licenseNumber;
        profile.IsVerified = isVerified;
        profile.VerifiedAt = isVerified ? DateTime.UtcNow : null;
    }

    private static async Task<Vehicle> UpsertVehicleAsync(
        CarPoolingContext context,
        Guid id,
        Guid ownerUserId,
        string licensePlate,
        string brand,
        string model,
        string color,
        int year,
        int totalSeats,
        bool isActive,
        bool isVerified)
    {
        var vehicle = await context.Vehicles.FirstOrDefaultAsync(item => item.Id == id);
        if (vehicle is null)
        {
            vehicle = new Vehicle { Id = id };
            context.Vehicles.Add(vehicle);
        }

        vehicle.OwnerUserId = ownerUserId;
        vehicle.LicensePlate = licensePlate;
        vehicle.Brand = brand;
        vehicle.Model = model;
        vehicle.Color = color;
        vehicle.VehicleYear = year;
        vehicle.TotalSeats = totalSeats;
        vehicle.IsActive = isActive;
        vehicle.IsVerified = isVerified;

        return vehicle;
    }

    private static async Task<Location> UpsertLocationAsync(
        CarPoolingContext context,
        Guid id,
        double latitude,
        double longitude,
        string addressLabel)
    {
        var location = await context.Locations.FirstOrDefaultAsync(item => item.Id == id);
        if (location is null)
        {
            location = new Location { Id = id };
            context.Locations.Add(location);
        }

        location.Latitude = latitude;
        location.Longitude = longitude;
        location.AddressLabel = addressLabel;

        return location;
    }

    private static async Task UpsertTripAsync(
        CarPoolingContext context,
        Guid id,
        Guid originLocationId,
        Guid destinationLocationId,
        int statusId,
        Guid? driverUserId,
        string driverName,
        Guid? vehicleId,
        int offeredSeats,
        int availableSeats,
        TripKind kind,
        DateTime createdAt,
        DateTime? startedAt,
        DateTime? finishedAt,
        DateTime? cancelledAt)
    {
        var trip = await context.Trips.FirstOrDefaultAsync(item => item.Id == id);
        if (trip is null)
        {
            trip = new Trip { Id = id };
            context.Trips.Add(trip);
        }

        trip.Kind = kind;
        trip.OriginLocationId = originLocationId;
        trip.DestinationLocationId = destinationLocationId;
        trip.StatusId = statusId;
        trip.DriverUserId = driverUserId;
        trip.DriverName = driverName;
        trip.VehicleId = vehicleId;
        trip.OfferedSeats = offeredSeats;
        trip.AvailableSeats = availableSeats;
        trip.CreatedAt = createdAt;
        trip.StartedAt = startedAt;
        trip.FinishedAt = finishedAt;
        trip.CancelledAt = cancelledAt;
    }

    private static async Task UpsertReservationAsync(
        CarPoolingContext context,
        Guid id,
        Guid tripId,
        Guid passengerUserId,
        int seatsReserved,
        int statusId,
        string boardingCode)
    {
        var reservation = await context.Reservations.FirstOrDefaultAsync(item => item.Id == id);
        if (reservation is null)
        {
            reservation = new Reservation { Id = id };
            context.Reservations.Add(reservation);
        }

        reservation.TripId = tripId;
        reservation.PassengerUserId = passengerUserId;
        reservation.SeatsReserved = seatsReserved;
        reservation.StatusId = statusId;
        reservation.BoardingCode = boardingCode;
    }

    private static string HashPassword(string password)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(bytes);
    }

    private static async Task UpsertTripChatAsync(
        CarPoolingContext context,
        Guid chatId,
        Guid tripId,
        DateTime createdAt)
    {
        var chat = await context.TripChats.FirstOrDefaultAsync(item => item.Id == chatId);
        if (chat is null)
        {
            chat = new TripChat { Id = chatId };
            context.TripChats.Add(chat);
        }
        chat.TripId = tripId;
        chat.CreatedAt = createdAt;
    }

    private static async Task UpsertTripChatMessageAsync(
        CarPoolingContext context,
        Guid messageId,
        Guid chatId,
        Guid senderUserId,
        string messageText,
        DateTime createdAt)
    {
        var msg = await context.TripChatMessages.FirstOrDefaultAsync(item => item.Id == messageId);
        if (msg is null)
        {
            msg = new TripChatMessage { Id = messageId };
            context.TripChatMessages.Add(msg);
        }
        msg.ChatId = chatId;
        msg.SenderUserId = senderUserId;
        msg.MessageText = messageText;
        msg.CreatedAt = createdAt;
    }

    private static async Task UpsertTripChatMessageReadAsync(
        CarPoolingContext context,
        Guid messageId,
        Guid userId,
        DateTime readAt)
    {
        var read = await context.TripChatMessageReads.FirstOrDefaultAsync(item => item.MessageId == messageId && item.UserId == userId);
        if (read is null)
        {
            read = new TripChatMessageRead { MessageId = messageId, UserId = userId };
            context.TripChatMessageReads.Add(read);
        }
        read.ReadAt = readAt;
    }

    private static async Task UpsertTripRatingAsync(
        CarPoolingContext context,
        Guid ratingId,
        Guid tripId,
        Guid evaluatorUserId,
        Guid evaluatedUserId,
        RatingRole ratingRole,
        int score,
        string? comment,
        string? tags,
        DateTime createdAt)
    {
        var rating = await context.TripRatings.FirstOrDefaultAsync(item => item.Id == ratingId);
        if (rating is null)
        {
            rating = new TripRating { Id = ratingId };
            context.TripRatings.Add(rating);
        }
        rating.TripId = tripId;
        rating.EvaluatorUserId = evaluatorUserId;
        rating.EvaluatedUserId = evaluatedUserId;
        rating.RatingRole = ratingRole;
        rating.Score = score;
        rating.Comment = comment;
        rating.Tags = tags;
        rating.CreatedAt = createdAt;
    }

    private static async Task UpsertSupportTicketAsync(
        CarPoolingContext context,
        Guid ticketId,
        Guid userId,
        Guid? tripId,
        Guid? reservationId,
        SupportTicketCategory category,
        string subject,
        string description,
        SupportTicketStatus status,
        DateTime createdAt)
    {
        var ticket = await context.SupportTickets.FirstOrDefaultAsync(item => item.Id == ticketId);
        if (ticket is null)
        {
            ticket = new SupportTicket { Id = ticketId };
            context.SupportTickets.Add(ticket);
        }
        ticket.UserId = userId;
        ticket.TripId = tripId;
        ticket.ReservationId = reservationId;
        ticket.Category = category;
        ticket.Subject = subject;
        ticket.Description = description;
        ticket.Status = status;
        ticket.CreatedAt = createdAt;
    }
}