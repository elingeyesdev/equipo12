using System.Security.Cryptography;
using System.Text;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;
using CarPooling.Security;

namespace CarPooling.Data;

public static class DevelopmentDataSeeder
{
    // Admin User
    private static readonly Guid AdminId = Guid.Parse("11111111-1111-1111-1111-111111111111");

    // Drivers
    private static readonly Guid DriverOneId = Guid.Parse("22222222-2222-2222-2222-222222222222"); // Juan Pérez
    private static readonly Guid DriverTwoId = Guid.Parse("33333333-3333-3333-3333-333333333333"); // María Gómez

    // Passengers
    private static readonly Guid PassengerOneId = Guid.Parse("44444444-4444-4444-4444-444444444444"); // Carlos Rojas
    private static readonly Guid PassengerTwoId = Guid.Parse("55555555-5555-5555-5555-555555555555"); // Ana Torres
    private static readonly Guid PassengerThreeId = Guid.Parse("66666666-6666-6666-6666-666666666666"); // Luis Castro

    // Analyst
    private static readonly Guid AnalystId = Guid.Parse("77777777-7777-7777-7777-777777777777"); // Sofía Morales

    // Driver Profiles
    private static readonly Guid DriverOneProfileId = Guid.Parse("88888888-8888-8888-8888-888888888888");
    private static readonly Guid DriverTwoProfileId = Guid.Parse("88888888-8888-8888-8888-888888888889");

    // Vehicles
    private static readonly Guid VehicleOneId = Guid.Parse("99999999-9999-9999-9999-999999999991");
    private static readonly Guid VehicleTwoId = Guid.Parse("99999999-9999-9999-9999-999999999992");

    // Safe Zones & Locations (Santa Cruz, Bolivia)
    private static readonly Guid LocUnivalleId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static readonly Guid LocPlazaId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static readonly Guid LocEquipetrolId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static readonly Guid LocBuschId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4");
    private static readonly Guid LocBimodalId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5");

    // Trips
    private static readonly Guid TripOneId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"); // Scheduled
    private static readonly Guid TripTwoId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2"); // Ready
    private static readonly Guid TripThreeId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3"); // In Progress
    private static readonly Guid TripFourId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb4"); // Finished
    private static readonly Guid TripFiveId = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb5"); // Cancelled

    // Reservations
    private static readonly Guid ResOneId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc1");
    private static readonly Guid ResTwoId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc2");
    private static readonly Guid ResThreeId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc3");
    private static readonly Guid ResFourId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc4");
    private static readonly Guid ResFiveId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc5");
    private static readonly Guid ResSixId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc6");
    private static readonly Guid ResSevenId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc7");
    private static readonly Guid ResEightId = Guid.Parse("cccccccc-cccc-cccc-cccc-ccccccccccc8");

    // Payments
    private static readonly Guid PayOneId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd1");
    private static readonly Guid PayTwoId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd2");
    private static readonly Guid PayThreeId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd3");
    private static readonly Guid PayFourId = Guid.Parse("dddddddd-dddd-dddd-dddd-ddddddddddd4");

    // Chats & Messages
    private static readonly Guid ChatOneId = Guid.Parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
    private static readonly Guid MsgOneId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff1");
    private static readonly Guid MsgTwoId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff2");
    private static readonly Guid MsgThreeId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff3");
    private static readonly Guid MsgFourId = Guid.Parse("ffffffff-ffff-ffff-ffff-fffffffffff4");

    // Ratings
    private static readonly Guid RatingOneId = Guid.Parse("11112222-3333-4444-5555-666677778881");
    private static readonly Guid RatingTwoId = Guid.Parse("11112222-3333-4444-5555-666677778882");
    private static readonly Guid RatingThreeId = Guid.Parse("11112222-3333-4444-5555-666677778883");
    private static readonly Guid RatingFourId = Guid.Parse("11112222-3333-4444-5555-666677778884");

    // Support Tickets & Messages
    private static readonly Guid TicketOneId = Guid.Parse("22223333-4444-5555-6666-777788889991");
    private static readonly Guid TicketOneMsgId = Guid.Parse("33334444-5555-6666-7777-888899990001");
    private static readonly Guid TicketTwoId = Guid.Parse("22223333-4444-5555-6666-777788889992");
    private static readonly Guid TicketTwoMsgOneId = Guid.Parse("33334444-5555-6666-7777-888899990002");
    private static readonly Guid TicketTwoMsgTwoId = Guid.Parse("33334444-5555-6666-7777-888899990003");
    private static readonly Guid TicketTwoMsgThreeId = Guid.Parse("33334444-5555-6666-7777-888899990004");

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

        // 0. Seed Permissions lookup table
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

        // Seed Roles lookup table
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

        // 1. CLEAR DYNAMIC AND TRANSACTION TABLES
        context.Refunds.RemoveRange(context.Refunds);
        context.PaymentTransactions.RemoveRange(context.PaymentTransactions);
        context.PaymentReceipts.RemoveRange(context.PaymentReceipts);
        context.Payments.RemoveRange(context.Payments);
        context.SupportTicketMessageReads.RemoveRange(context.SupportTicketMessageReads);
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
        context.DriverProfiles.RemoveRange(context.DriverProfiles);
        context.UserRoles.RemoveRange(context.UserRoles);
        context.Users.RemoveRange(context.Users);
        context.SafeZones.RemoveRange(context.SafeZones);
        context.Locations.RemoveRange(context.Locations);

        await context.SaveChangesAsync();

        // 2. SEED USERS & ROLES ASSIGNMENT
        var passwordHash = HashPassword("123456");

        var roleSuperAdmin = await context.Roles.FirstAsync(r => r.Name == "SuperAdmin");
        var roleDriver = await context.Roles.FirstAsync(r => r.Name == "Driver");
        var roleStudent = await context.Roles.FirstAsync(r => r.Name == "Student");
        var roleAnalyst = await context.Roles.FirstAsync(r => r.Name == "Analyst");

        // Create Users
        var adminUser = new User { Id = AdminId, FullName = "Administrador Univalle", Email = "admin@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 71011111" };
        var driverOne = new User { Id = DriverOneId, FullName = "Juan Pérez", Email = "conductor1@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 71020304" };
        var driverTwo = new User { Id = DriverTwoId, FullName = "María Gómez", Email = "conductor2@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 72030405" };
        var passengerOne = new User { Id = PassengerOneId, FullName = "Carlos Rojas", Email = "estudiante1@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 73040506" };
        var passengerTwo = new User { Id = PassengerTwoId, FullName = "Ana Torres", Email = "estudiante2@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 74050607" };
        var passengerThree = new User { Id = PassengerThreeId, FullName = "Luis Castro", Email = "estudiante3@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 75060708" };
        var analystUser = new User { Id = AnalystId, FullName = "Sofía Morales", Email = "analista@univalle.edu", PasswordHash = passwordHash, PhoneNumber = "+591 76070809" };

        context.Users.AddRange(adminUser, driverOne, driverTwo, passengerOne, passengerTwo, passengerThree, analystUser);
        await context.SaveChangesAsync();

        // Assign Roles
        context.UserRoles.AddRange(
            new UserRole { UserId = AdminId, RoleId = roleSuperAdmin.Id },
            new UserRole { UserId = DriverOneId, RoleId = roleDriver.Id },
            new UserRole { UserId = DriverTwoId, RoleId = roleDriver.Id },
            new UserRole { UserId = PassengerOneId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerTwoId, RoleId = roleStudent.Id },
            new UserRole { UserId = PassengerThreeId, RoleId = roleStudent.Id },
            new UserRole { UserId = AnalystId, RoleId = roleAnalyst.Id }
        );
        await context.SaveChangesAsync();

        // 3. SEED DRIVER PROFILES
        var profileOne = new DriverProfile
        {
            Id = DriverOneProfileId,
            UserId = DriverOneId,
            LicenseNumber = "LIC-54321",
            IsVerified = true,
            VerifiedAt = DateTime.UtcNow.AddDays(-10)
        };
        var profileTwo = new DriverProfile
        {
            Id = DriverTwoProfileId,
            UserId = DriverTwoId,
            LicenseNumber = "LIC-09876",
            IsVerified = true,
            VerifiedAt = DateTime.UtcNow.AddDays(-5)
        };

        context.DriverProfiles.AddRange(profileOne, profileTwo);
        await context.SaveChangesAsync();

        // 4. SEED VEHICLES
        var vehicleOne = new Vehicle
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
            IsVerified = true
        };
        var vehicleTwo = new Vehicle
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
            IsVerified = true
        };

        context.Vehicles.AddRange(vehicleOne, vehicleTwo);
        await context.SaveChangesAsync();

        // 5. SEED SAFE ZONES AND LOCATIONS (Santa Cruz, Bolivia)
        // Coordenadas basadas en Santa Cruz de la Sierra:
        // Campus Univalle: -17.74797, -63.16611
        // Plaza 24 de Septiembre (Centro): -17.78330, -63.18210
        // Equipetrol: -17.76560, -63.19320
        // Av. Busch y 2do Anillo: -17.77120, -63.18540
        // Terminal Bimodal: -17.79410, -63.16640

        var locUnivalle = new Location { Id = LocUnivalleId, Latitude = -17.74797, Longitude = -63.16611, AddressLabel = "Campus Univalle Santa Cruz, Av. Banzer Km 8" };
        var locPlaza = new Location { Id = LocPlazaId, Latitude = -17.78330, Longitude = -63.18210, AddressLabel = "Plaza 24 de Septiembre, Centro" };
        var locEquipetrol = new Location { Id = LocEquipetrolId, Latitude = -17.76560, Longitude = -63.19320, AddressLabel = "Av. San Martín, Barrio Equipetrol" };
        var locBusch = new Location { Id = LocBuschId, Latitude = -17.77120, Longitude = -63.18540, AddressLabel = "Avenida Busch y Segundo Anillo" };
        var locBimodal = new Location { Id = LocBimodalId, Latitude = -17.79410, Longitude = -63.16640, AddressLabel = "Terminal Bimodal, Santa Cruz" };

        context.Locations.AddRange(locUnivalle, locPlaza, locEquipetrol, locBusch, locBimodal);
        await context.SaveChangesAsync();

        var zoneUnivalle = new SafeZone { Id = Guid.NewGuid(), Name = "Campus Univalle Banzer", Description = "Puerta de ingreso principal del Campus Santa Cruz", Latitude = -17.74797, Longitude = -63.16611, AddressLabel = "Campus Univalle Santa Cruz, Av. Banzer Km 8", Purpose = SafeZonePurpose.Both, IsActive = true, DisplayOrder = 1, CampusArea = "Entrada Principal" };
        var zonePlaza = new SafeZone { Id = Guid.NewGuid(), Name = "Plaza 24 de Septiembre", Description = "Parada de encuentro en la esquina Libertad y Ayacucho", Latitude = -17.78330, Longitude = -63.18210, AddressLabel = "Calle Libertad esq. Ayacucho, Centro Histórico", Purpose = SafeZonePurpose.Both, IsActive = true, DisplayOrder = 2, CampusArea = "Centro" };
        var zoneEquipetrol = new SafeZone { Id = Guid.NewGuid(), Name = "Zona Equipetrol", Description = "Encuentro frente al centro comercial de la Av. San Martín", Latitude = -17.76560, Longitude = -63.19320, AddressLabel = "Av. San Martín y 3er Anillo, Equipetrol", Purpose = SafeZonePurpose.Both, IsActive = true, DisplayOrder = 3, CampusArea = "Equipetrol" };
        var zoneBimodal = new SafeZone { Id = Guid.NewGuid(), Name = "Terminal Bimodal", Description = "Punto de abordaje seguro frente a la boletería principal", Latitude = -17.79410, Longitude = -63.16640, AddressLabel = "Av. Interbimodal, Terminal de Buses", Purpose = SafeZonePurpose.Both, IsActive = true, DisplayOrder = 4, CampusArea = "Bimodal" };

        context.SafeZones.AddRange(zoneUnivalle, zonePlaza, zoneEquipetrol, zoneBimodal);
        await context.SaveChangesAsync();

        // 6. SEED TRIPS
        // Trip 1: Scheduled. Plaza -> Univalle. Driver One. 4 seats offered, 3 available.
        var tripOne = new Trip
        {
            Id = TripOneId,
            OriginLocationId = LocPlazaId,
            DestinationLocationId = LocUnivalleId,
            StatusId = 1, // Scheduled
            DriverUserId = DriverOneId,
            DriverName = "Juan Pérez",
            VehicleId = VehicleOneId,
            OfferedSeats = 4,
            AvailableSeats = 3,
            FareAmount = 10.00m,
            Kind = TripKind.Regular,
            CreatedAt = DateTime.UtcNow.AddHours(-1)
        };

        // Trip 2: Ready. Equipetrol -> Univalle. Driver Two. 4 seats offered, 2 available.
        var tripTwo = new Trip
        {
            Id = TripTwoId,
            OriginLocationId = LocEquipetrolId,
            DestinationLocationId = LocUnivalleId,
            StatusId = 2, // Ready
            DriverUserId = DriverTwoId,
            DriverName = "María Gómez",
            VehicleId = VehicleTwoId,
            OfferedSeats = 4,
            AvailableSeats = 2,
            FareAmount = 8.00m,
            Kind = TripKind.Regular,
            CreatedAt = DateTime.UtcNow.AddHours(-2)
        };

        // Trip 3: In Progress. Univalle -> Av. Busch. Driver One. 4 seats offered, 2 available.
        var tripThree = new Trip
        {
            Id = TripThreeId,
            OriginLocationId = LocUnivalleId,
            DestinationLocationId = LocBuschId,
            StatusId = 3, // In Progress
            DriverUserId = DriverOneId,
            DriverName = "Juan Pérez",
            VehicleId = VehicleOneId,
            OfferedSeats = 4,
            AvailableSeats = 2,
            FareAmount = 10.00m,
            Kind = TripKind.Regular,
            CreatedAt = DateTime.UtcNow.AddMinutes(-45),
            StartedAt = DateTime.UtcNow.AddMinutes(-15)
        };

        // Trip 4: Finished. Terminal Bimodal -> Univalle. Driver Two. 4 seats offered, 2 available.
        var tripFour = new Trip
        {
            Id = TripFourId,
            OriginLocationId = LocBimodalId,
            DestinationLocationId = LocUnivalleId,
            StatusId = 4, // Finished
            DriverUserId = DriverTwoId,
            DriverName = "María Gómez",
            VehicleId = VehicleTwoId,
            OfferedSeats = 4,
            AvailableSeats = 2,
            FareAmount = 12.00m,
            Kind = TripKind.Regular,
            CreatedAt = DateTime.UtcNow.AddHours(-4),
            StartedAt = DateTime.UtcNow.AddHours(-3),
            FinishedAt = DateTime.UtcNow.AddHours(-2)
        };

        // Trip 5: Cancelled. Univalle -> Plaza. Driver One. 4 seats offered, 4 available.
        var tripFive = new Trip
        {
            Id = TripFiveId,
            OriginLocationId = LocUnivalleId,
            DestinationLocationId = LocPlazaId,
            StatusId = 5, // Cancelled
            DriverUserId = DriverOneId,
            DriverName = "Juan Pérez",
            VehicleId = VehicleOneId,
            OfferedSeats = 4,
            AvailableSeats = 4,
            FareAmount = 10.00m,
            Kind = TripKind.Regular,
            CreatedAt = DateTime.UtcNow.AddHours(-6),
            CancelledAt = DateTime.UtcNow.AddHours(-5)
        };

        context.Trips.AddRange(tripOne, tripTwo, tripThree, tripFour, tripFive);
        await context.SaveChangesAsync();

        // 7. SEED RESERVATIONS
        // Trip 1
        var resOne = new Reservation { Id = ResOneId, TripId = TripOneId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 2, BoardingCode = "1111", CreatedAt = DateTime.UtcNow.AddMinutes(-30) }; // Ana: Confirmed

        // Trip 2
        var resTwo = new Reservation { Id = ResTwoId, TripId = TripTwoId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 2, BoardingCode = "2222", CreatedAt = DateTime.UtcNow.AddMinutes(-90) }; // Carlos: Confirmed
        var resThree = new Reservation { Id = ResThreeId, TripId = TripTwoId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 1, BoardingCode = "3333", CreatedAt = DateTime.UtcNow.AddMinutes(-40) }; // Luis: Pending

        // Trip 3
        var resFour = new Reservation { Id = ResFourId, TripId = TripThreeId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 3, BoardingCode = "4444", CreatedAt = DateTime.UtcNow.AddMinutes(-35) }; // Ana: Boarded
        var resFive = new Reservation { Id = ResFiveId, TripId = TripThreeId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "5555", CreatedAt = DateTime.UtcNow.AddMinutes(-30) }; // Carlos: Boarded

        // Trip 4
        var resSix = new Reservation { Id = ResSixId, TripId = TripFourId, PassengerUserId = PassengerOneId, SeatsReserved = 1, StatusId = 3, BoardingCode = "6666", CreatedAt = DateTime.UtcNow.AddHours(-3).AddMinutes(-30) }; // Carlos: Boarded (Completed)
        var resSeven = new Reservation { Id = ResSevenId, TripId = TripFourId, PassengerUserId = PassengerThreeId, SeatsReserved = 1, StatusId = 3, BoardingCode = "7777", CreatedAt = DateTime.UtcNow.AddHours(-3).AddMinutes(-10) }; // Luis: Boarded (Completed)
        var resEight = new Reservation { Id = ResEightId, TripId = TripFourId, PassengerUserId = PassengerTwoId, SeatsReserved = 1, StatusId = 4, BoardingCode = "8888", CreatedAt = DateTime.UtcNow.AddHours(-3).AddMinutes(-5) }; // Ana: Cancelled

        context.Reservations.AddRange(resOne, resTwo, resThree, resFour, resFive, resSix, resSeven, resEight);
        await context.SaveChangesAsync();

        // 8. SEED DRIVER QR CONFIGURATION (UserPaymentMethod)
        var driverQrMethod = new UserPaymentMethod
        {
            Id = Guid.NewGuid(),
            UserId = DriverTwoId, // María Gómez
            PaymentMethodId = 3,  // BankQr
            Alias = "Mi QR BNB Personal",
            QrImageUrl = "https://i.imgur.com/7b1Xw2r.png",
            BankName = "Banco Nacional de Bolivia",
            AccountHolderName = "María Gómez Vaca",
            IsDefault = true,
            IsActive = true
        };
        context.UserPaymentMethods.Add(driverQrMethod);
        await context.SaveChangesAsync();

        // 9. SEED PAYMENTS & TRANSACTIONS
        // Payment 1: Ana (Card SIM) for Trip 3 (Active, Boarded)
        var payOne = new Payment
        {
            Id = PayOneId,
            ReservationId = ResFourId,
            PassengerUserId = PassengerTwoId,
            PaymentMethodId = 2, // Card Simulated
            Amount = 10.00m,
            Currency = "BOB",
            Status = PaymentStatus.Approved,
            Description = "Pago de pasaje simulado por tarjeta de débito",
            PaidAt = DateTime.UtcNow.AddMinutes(-15)
        };

        // Payment 2: Carlos (Cash) for Trip 3 (Active, Boarded) - Pending confirmation
        var payTwo = new Payment
        {
            Id = PayTwoId,
            ReservationId = ResFiveId,
            PassengerUserId = PassengerOneId,
            PaymentMethodId = 1, // Cash
            Amount = 10.00m,
            Currency = "BOB",
            Status = PaymentStatus.Pending,
            Description = "Pago en efectivo - Pendiente de confirmación del conductor"
        };

        // Payment 3: Carlos (BankQr) for Trip 4 (Finished)
        var payThree = new Payment
        {
            Id = PayThreeId,
            ReservationId = ResSixId,
            PassengerUserId = PassengerOneId,
            PaymentMethodId = 3, // QR bancario
            UserPaymentMethodId = driverQrMethod.Id,
            Amount = 12.00m,
            Currency = "BOB",
            Status = PaymentStatus.Approved,
            Description = "Pago verificado por QR",
            ConfirmedByUserId = DriverTwoId,
            ConfirmedAt = DateTime.UtcNow.AddHours(-2).AddMinutes(-30),
            ConfirmationNotes = "QR verificado en cuenta BNB",
            PaidAt = DateTime.UtcNow.AddHours(-2).AddMinutes(-40)
        };

        // Payment 4: Luis (Cash) for Trip 4 (Finished)
        var payFour = new Payment
        {
            Id = PayFourId,
            ReservationId = ResSevenId,
            PassengerUserId = PassengerThreeId,
            PaymentMethodId = 1, // Cash
            Amount = 12.00m,
            Currency = "BOB",
            Status = PaymentStatus.Approved,
            Description = "Pago en efectivo entregado a conductora",
            ConfirmedByUserId = DriverTwoId,
            ConfirmedAt = DateTime.UtcNow.AddHours(-2),
            PaidAt = DateTime.UtcNow.AddHours(-2)
        };

        context.Payments.AddRange(payOne, payTwo, payThree, payFour);
        await context.SaveChangesAsync();

        // Seed Payment Transactions
        var txOne = new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = PayOneId,
            TransactionType = PaymentTransactionType.Payment,
            Status = PaymentTransactionStatus.Success,
            Amount = 10.00m,
            Provider = "SIMULATOR_GATEWAY",
            ProviderTransactionId = "TX-CARD-987654",
            AuthorizationCode = "AUTH-8877",
            ResponseCode = "00",
            ResponseMessage = "Aprobado exitosamente",
            ProcessedAt = DateTime.UtcNow.AddMinutes(-15)
        };
        var txThree = new PaymentTransaction
        {
            Id = Guid.NewGuid(),
            PaymentId = PayThreeId,
            TransactionType = PaymentTransactionType.Payment,
            Status = PaymentTransactionStatus.Success,
            Amount = 12.00m,
            Provider = "QR_BNB_INTEGRATION",
            ProviderTransactionId = "TX-QR-112233",
            ResponseMessage = "Verificación manual de QR exitosa",
            ProcessedAt = DateTime.UtcNow.AddHours(-2).AddMinutes(-30)
        };

        context.PaymentTransactions.AddRange(txOne, txThree);
        await context.SaveChangesAsync();

        // Seed Payment Receipts
        context.PaymentReceipts.AddRange(
            new PaymentReceipt
            {
                Id = Guid.NewGuid(),
                PaymentId = PayThreeId,
                ReceiptNumber = "R-2026-00001",
                QrCodeValue = "RECEIPT-VAL-00001",
                IssuedAt = DateTime.UtcNow.AddHours(-2).AddMinutes(-30)
            },
            new PaymentReceipt
            {
                Id = Guid.NewGuid(),
                PaymentId = PayFourId,
                ReceiptNumber = "R-2026-00002",
                QrCodeValue = "RECEIPT-VAL-00002",
                IssuedAt = DateTime.UtcNow.AddHours(-2)
            }
        );
        await context.SaveChangesAsync();

        // 10. SEED TRIP CHATS & MESSAGES
        var tripChat = new TripChat
        {
            Id = ChatOneId,
            TripId = TripThreeId,
            CreatedAt = DateTime.UtcNow.AddMinutes(-40)
        };
        context.TripChats.Add(tripChat);
        await context.SaveChangesAsync();

        var msg1 = new TripChatMessage { Id = MsgOneId, ChatId = ChatOneId, SenderUserId = DriverOneId, MessageText = "Hola a todos. Ya estoy parqueado en la salida del Campus, frente al portón principal.", CreatedAt = DateTime.UtcNow.AddMinutes(-15) };
        var msg2 = new TripChatMessage { Id = MsgTwoId, ChatId = ChatOneId, SenderUserId = PassengerTwoId, MessageText = "Hola Juan, voy saliendo de mi clase del edificio de Ingeniería. Llego en 3 minutos.", CreatedAt = DateTime.UtcNow.AddMinutes(-12) };
        var msg3 = new TripChatMessage { Id = MsgThreeId, ChatId = ChatOneId, SenderUserId = PassengerOneId, MessageText = "Listo, yo también ya estoy bajando por las escaleras principales. Voy en camino.", CreatedAt = DateTime.UtcNow.AddMinutes(-10) };
        var msg4 = new TripChatMessage { Id = MsgFourId, ChatId = ChatOneId, SenderUserId = DriverOneId, MessageText = "Excelente, no se preocupen, aquí los espero en el Corolla Blanco.", CreatedAt = DateTime.UtcNow.AddMinutes(-8) };

        context.TripChatMessages.AddRange(msg1, msg2, msg3, msg4);
        await context.SaveChangesAsync();

        // Mark messages as read
        context.TripChatMessageReads.AddRange(
            new TripChatMessageRead { MessageId = MsgOneId, UserId = PassengerTwoId, ReadAt = DateTime.UtcNow.AddMinutes(-14) },
            new TripChatMessageRead { MessageId = MsgOneId, UserId = PassengerOneId, ReadAt = DateTime.UtcNow.AddMinutes(-13) },
            new TripChatMessageRead { MessageId = MsgTwoId, UserId = DriverOneId, ReadAt = DateTime.UtcNow.AddMinutes(-11) },
            new TripChatMessageRead { MessageId = MsgThreeId, UserId = DriverOneId, ReadAt = DateTime.UtcNow.AddMinutes(-9) }
        );
        await context.SaveChangesAsync();

        // 11. SEED RATINGS (Trip 4)
        // Passenger 1 to Driver
        var rating1 = new TripRating
        {
            Id = RatingOneId,
            TripId = TripFourId,
            EvaluatorUserId = PassengerOneId,
            EvaluatedUserId = DriverTwoId,
            RatingRole = RatingRole.PassengerToDriver,
            Score = 5,
            Comment = "La conductora fue muy puntual y el auto estaba sumamente limpio y cómodo.",
            Tags = "Puntual,Limpio,Amable",
            CreatedAt = DateTime.UtcNow.AddHours(-1).AddMinutes(-50)
        };
        // Passenger 3 to Driver
        var rating2 = new TripRating
        {
            Id = RatingThreeId,
            TripId = TripFourId,
            EvaluatorUserId = PassengerThreeId,
            EvaluatedUserId = DriverTwoId,
            RatingRole = RatingRole.PassengerToDriver,
            Score = 4,
            Comment = "Buen viaje, aunque la ruta elegida tenía bastante tráfico por el 3er anillo.",
            Tags = "Respetuoso,Puntual",
            CreatedAt = DateTime.UtcNow.AddHours(-1).AddMinutes(-45)
        };
        // Driver to Passenger 1
        var rating3 = new TripRating
        {
            Id = RatingTwoId,
            TripId = TripFourId,
            EvaluatorUserId = DriverTwoId,
            EvaluatedUserId = PassengerOneId,
            RatingRole = RatingRole.DriverToPassenger,
            Score = 5,
            Comment = "Pasajero muy educado y amigable. Abordó el vehículo puntualmente.",
            Tags = "Educado,Puntual",
            CreatedAt = DateTime.UtcNow.AddHours(-1).AddMinutes(-30)
        };
        // Driver to Passenger 3
        var rating4 = new TripRating
        {
            Id = RatingFourId,
            TripId = TripFourId,
            EvaluatorUserId = DriverTwoId,
            EvaluatedUserId = PassengerThreeId,
            RatingRole = RatingRole.DriverToPassenger,
            Score = 5,
            Comment = "Pasajero muy respetuoso y callado.",
            Tags = "Respetuoso",
            CreatedAt = DateTime.UtcNow.AddHours(-1).AddMinutes(-28)
        };

        context.TripRatings.AddRange(rating1, rating2, rating3, rating4);
        await context.SaveChangesAsync();

        // 12. SEED SUPPORT TICKETS & MESSAGES
        // Ticket 1: Lost Item
        var ticketOne = new SupportTicket
        {
            Id = TicketOneId,
            UserId = PassengerOneId,
            TripId = TripFourId,
            Category = SupportTicketCategory.Trip,
            Subject = "Celular olvidado en el vehículo",
            Description = "Creo que olvidé mi celular Samsung con funda azul en el asiento trasero del Suzuki Swift gris de María Gómez durante el viaje de ayer por la tarde.",
            Status = SupportTicketStatus.Open,
            CreatedAt = DateTime.UtcNow.AddHours(-3)
        };
        context.SupportTickets.Add(ticketOne);
        await context.SaveChangesAsync();

        var t1Msg = new SupportTicketMessage
        {
            Id = TicketOneMsgId,
            TicketId = TicketOneId,
            SenderUserId = PassengerOneId,
            SenderKind = SupportMessageSenderKind.User,
            MessageText = "Hola, les escribo por aquí porque no he podido comunicarme con la conductora directamente. Ojalá me puedan ayudar.",
            CreatedAt = DateTime.UtcNow.AddHours(-2).AddMinutes(-50)
        };
        context.SupportTicketMessages.Add(t1Msg);
        await context.SaveChangesAsync();

        // Ticket 2: Account issue (Resolved)
        var ticketTwo = new SupportTicket
        {
            Id = TicketTwoId,
            UserId = PassengerTwoId,
            Category = SupportTicketCategory.Account,
            Subject = "Problema con saldo de Billetera Simulada",
            Description = "Realicé una recarga de 50 BOB a mi cuenta mediante tarjeta pero el saldo sigue apareciendo en 0 BOB. Solicito la verificación por favor.",
            Status = SupportTicketStatus.Resolved,
            CreatedAt = DateTime.UtcNow.AddDays(-3),
            FirstAdminReplyAt = DateTime.UtcNow.AddDays(-2),
            ClosedAt = DateTime.UtcNow.AddDays(-1)
        };
        context.SupportTickets.Add(ticketTwo);
        await context.SaveChangesAsync();

        var t2Msg1 = new SupportTicketMessage
        {
            Id = TicketTwoMsgOneId,
            TicketId = TicketTwoId,
            SenderUserId = PassengerTwoId,
            SenderKind = SupportMessageSenderKind.User,
            MessageText = "Ayer hice la recarga pero no se refleja en mi balance. Adjunto captura si es necesario.",
            CreatedAt = DateTime.UtcNow.AddDays(-3).AddHours(1)
        };
        var t2Msg2 = new SupportTicketMessage
        {
            Id = TicketTwoMsgTwoId,
            TicketId = TicketTwoId,
            SenderUserId = AdminId,
            SenderKind = SupportMessageSenderKind.Admin,
            MessageText = "Hola Ana. Hemos verificado la transacción en el simulador y tuvimos una desconexión momentánea. Ya hemos acreditado manualmente los 50 BOB a tu billetera. Por favor verifica tu balance en la app.",
            CreatedAt = DateTime.UtcNow.AddDays(-2)
        };
        var t2Msg3 = new SupportTicketMessage
        {
            Id = TicketTwoMsgThreeId,
            TicketId = TicketTwoId,
            SenderUserId = PassengerTwoId,
            SenderKind = SupportMessageSenderKind.User,
            MessageText = "Excelente, ya verifiqué y aparece el saldo correcto. Muchas gracias por la pronta solución!",
            CreatedAt = DateTime.UtcNow.AddDays(-1)
        };

        context.SupportTicketMessages.AddRange(t2Msg1, t2Msg2, t2Msg3);
        await context.SaveChangesAsync();

        // 13. SEED DEFAULT UNIVALLE THEME SETTING IF NOT PRESENT
        if (!await context.AppSettings.AnyAsync(s => s.Key == "theme"))
        {
            context.AppSettings.Add(new AppSetting 
            { 
                Key = "theme", 
                Value = "{\"primaryLight\":\"#82254B\",\"secondaryLight\":\"#6E1E3F\",\"textLight\":\"#111827\",\"bgLight\":\"#FFFFFF\",\"cardLight\":\"#F5F5F5\",\"borderLight\":\"#9CA8B0\",\"primaryDark\":\"#82254B\",\"secondaryDark\":\"#6E1E3F\",\"textDark\":\"#ffffff\",\"bgDark\":\"#121011\",\"cardDark\":\"#251a1e\",\"borderDark\":\"#6E1E3F\"}" 
            });
            await context.SaveChangesAsync();
        }
    }

    private static string HashPassword(string password)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(bytes);
    }
}
