using CarPooling.Models;
using Microsoft.EntityFrameworkCore;

namespace CarPooling.Data;

public class CarPoolingContext(DbContextOptions<CarPoolingContext> options) : DbContext(options)
{
    public DbSet<Trip> Trips => Set<Trip>();
    public DbSet<Reservation> Reservations => Set<Reservation>();
    public DbSet<User> Users => Set<User>();
    public DbSet<DriverProfile> DriverProfiles => Set<DriverProfile>();
    public DbSet<Vehicle> Vehicles => Set<Vehicle>();
    public DbSet<Location> Locations => Set<Location>();
    public DbSet<TripStatusEntity> TripStatuses => Set<TripStatusEntity>();
    public DbSet<ReservationStatusEntity> ReservationStatuses => Set<ReservationStatusEntity>();
    public DbSet<TripChat> TripChats => Set<TripChat>();
    public DbSet<TripChatMessage> TripChatMessages => Set<TripChatMessage>();
    public DbSet<TripChatMessageRead> TripChatMessageReads => Set<TripChatMessageRead>();
    public DbSet<TripRating> TripRatings => Set<TripRating>();
    public DbSet<SupportTicket> SupportTickets => Set<SupportTicket>();
    public DbSet<SupportTicketMessage> SupportTicketMessages => Set<SupportTicketMessage>();
    public DbSet<SupportTicketMessageRead> SupportTicketMessageReads => Set<SupportTicketMessageRead>();
    public DbSet<AppSetting> AppSettings => Set<AppSetting>();
    public DbSet<SafeZone> SafeZones => Set<SafeZone>();
    public DbSet<Role> Roles => Set<Role>();
    public DbSet<Permission> Permissions => Set<Permission>();
    public DbSet<UserRole> UserRoles => Set<UserRole>();
    public DbSet<RolePermission> RolePermissions => Set<RolePermission>();
    public DbSet<PaymentMethod> PaymentMethods => Set<PaymentMethod>();
    public DbSet<UserPaymentMethod> UserPaymentMethods => Set<UserPaymentMethod>();
    public DbSet<Payment> Payments => Set<Payment>();
    public DbSet<PaymentTransaction> PaymentTransactions => Set<PaymentTransaction>();
    public DbSet<PaymentReceipt> PaymentReceipts => Set<PaymentReceipt>();
    public DbSet<Refund> Refunds => Set<Refund>();
    public DbSet<UserDevice> UserDevices => Set<UserDevice>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        ConfigureTrip(modelBuilder);
        ConfigureReservation(modelBuilder);
        ConfigureUser(modelBuilder);
        ConfigureDriverProfile(modelBuilder);
        ConfigureVehicle(modelBuilder);
        ConfigureLocation(modelBuilder);
        ConfigureTripStatus(modelBuilder);
        ConfigureReservationStatus(modelBuilder);
        ConfigureTripChat(modelBuilder);
        ConfigureTripChatMessage(modelBuilder);
        ConfigureTripChatMessageRead(modelBuilder);
        ConfigureTripRating(modelBuilder);
        ConfigureSupportTicket(modelBuilder);
        ConfigureSupportTicketMessage(modelBuilder);
        ConfigureSupportTicketMessageRead(modelBuilder);
        ConfigureAppSetting(modelBuilder);
        ConfigureSafeZone(modelBuilder);
        ConfigureRole(modelBuilder);
        ConfigurePermission(modelBuilder);
        ConfigureUserRole(modelBuilder);
        ConfigureRolePermission(modelBuilder);
        ConfigurePaymentMethod(modelBuilder);
        ConfigureUserPaymentMethod(modelBuilder);
        ConfigurePayment(modelBuilder);
        ConfigurePaymentTransaction(modelBuilder);
        ConfigurePaymentReceipt(modelBuilder);
        ConfigureRefund(modelBuilder);
        ConfigureUserDevice(modelBuilder);

        SeedTripStatuses(modelBuilder);
        SeedReservationStatuses(modelBuilder);
        SeedPaymentMethods(modelBuilder);
    }

    private static void ConfigureTrip(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Trip>(entity =>
        {
            entity.ToTable("Trips");
            entity.HasKey(t => t.Id);

            entity.HasOne(t => t.OriginLocation)
                .WithMany()
                .HasForeignKey(t => t.OriginLocationId)
                .OnDelete(DeleteBehavior.NoAction)
                .IsRequired();

            entity.HasOne(t => t.DestinationLocation)
                .WithMany()
                .HasForeignKey(t => t.DestinationLocationId)
                .OnDelete(DeleteBehavior.NoAction)
                .IsRequired();

            entity.HasOne(t => t.StatusEntity)
                .WithMany(s => s.Trips)
                .HasForeignKey(t => t.StatusId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(t => t.Vehicle)
                .WithMany(v => v.Trips)
                .HasForeignKey(t => t.VehicleId)
                .OnDelete(DeleteBehavior.NoAction);

            entity.HasOne(t => t.DriverUser)
                .WithMany(u => u.Trips)
                .HasForeignKey(t => t.DriverUserId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasMany(t => t.Reservations)
                .WithOne(r => r.Trip)
                .HasForeignKey(r => r.TripId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.Property(t => t.OfferedSeats).IsRequired().HasDefaultValue(4);
            entity.Property(t => t.AvailableSeats).IsRequired().HasDefaultValue(4);
            entity.Property(t => t.FareAmount).HasColumnType("decimal(10,2)").HasDefaultValue(10m).IsRequired();
            entity.Property(t => t.DriverName).HasMaxLength(100).IsRequired();
            entity.Property(t => t.Kind).HasConversion<int>().HasDefaultValue(TripKind.Regular).IsRequired();
            entity.Property(t => t.BookmarkUseCount).HasDefaultValue(0);
            entity.Property(t => t.BookmarkLastUsedAt);
            entity.Property(t => t.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(t => t.UpdatedAt);
            entity.Property(t => t.StartedAt);
            entity.Property(t => t.FinishedAt);
            entity.Property(t => t.CancelledAt);
        });
    }

    private static void ConfigureReservation(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Reservation>(entity =>
        {
            entity.ToTable("Reservations");
            entity.HasKey(r => r.Id);

            entity.HasOne(r => r.PassengerUser)
                .WithMany()
                .HasForeignKey(r => r.PassengerUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(r => r.StatusEntity)
                .WithMany(s => s.Reservations)
                .HasForeignKey(r => r.StatusId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(r => r.SeatsReserved).IsRequired().HasDefaultValue(1);
            entity.Property(r => r.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureUser(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("Users");
            entity.HasKey(u => u.Id);
            entity.HasIndex(u => u.Email).IsUnique();
            entity.Property(u => u.FullName).IsRequired().HasMaxLength(120);
            entity.Property(u => u.Email).IsRequired().HasMaxLength(120);
            entity.Property(u => u.PasswordHash).IsRequired().HasMaxLength(256);
            entity.Property(u => u.PhoneNumber).HasMaxLength(25);
            entity.Property(u => u.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasMany(u => u.UserRoles)
                .WithOne(ur => ur.User)
                .HasForeignKey(ur => ur.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(u => u.DriverProfile)
                .WithOne(p => p.User)
                .HasForeignKey<DriverProfile>(p => p.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasMany(u => u.Vehicles)
                .WithOne(v => v.OwnerUser)
                .HasForeignKey(v => v.OwnerUserId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private static void ConfigureDriverProfile(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<DriverProfile>(entity =>
        {
            entity.ToTable("DriverProfiles");
            entity.HasKey(p => p.Id);
            entity.HasIndex(p => p.UserId).IsUnique();
            entity.Property(p => p.IsVerified).IsRequired();
            entity.Property(p => p.LicenseNumber).HasMaxLength(30);
            entity.Property(p => p.LicenseDocumentUrl).HasMaxLength(300);
            entity.Property(p => p.VerifiedAt);
            entity.Property(p => p.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(p => p.UpdatedAt);
        });
    }

    private static void ConfigureVehicle(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Vehicle>(entity =>
        {
            entity.ToTable("Vehicles");
            entity.HasKey(v => v.Id);
            entity.Property(v => v.LicensePlate).IsRequired().HasMaxLength(20);
            entity.Property(v => v.Brand).IsRequired().HasMaxLength(60);
            entity.Property(v => v.Model).HasMaxLength(60);
            entity.Property(v => v.Color).IsRequired().HasMaxLength(30);
            entity.Property(v => v.VehicleYear);
            entity.Property(v => v.TotalSeats).IsRequired().HasDefaultValue(4);
            entity.Property(v => v.IsActive).IsRequired().HasDefaultValue(true);
            entity.Property(v => v.IsVerified).IsRequired().HasDefaultValue(false);
            entity.Property(v => v.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureLocation(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Location>(entity =>
        {
            entity.ToTable("Locations");
            entity.HasKey(l => l.Id);
            entity.Property(l => l.Latitude).IsRequired();
            entity.Property(l => l.Longitude).IsRequired();
            entity.Property(l => l.AddressLabel).HasMaxLength(200);
            entity.Property(l => l.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureTripStatus(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripStatusEntity>(entity =>
        {
            entity.ToTable("TripStatuses");
            entity.HasKey(s => s.Id);
            entity.Property(s => s.Code).IsRequired().HasMaxLength(30);
            entity.Property(s => s.LabelEs).IsRequired().HasMaxLength(40);
            entity.Property(s => s.IsActiveState).IsRequired();
        });
    }

    private static void ConfigureReservationStatus(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ReservationStatusEntity>(entity =>
        {
            entity.ToTable("ReservationStatuses");
            entity.HasKey(s => s.Id);
            entity.Property(s => s.Code).IsRequired().HasMaxLength(30);
            entity.Property(s => s.LabelEs).IsRequired().HasMaxLength(40);
        });
    }

    private static void SeedTripStatuses(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripStatusEntity>().HasData(
            new TripStatusEntity { Id = 1, Code = "scheduled", LabelEs = "Programado", IsActiveState = true },
            new TripStatusEntity { Id = 2, Code = "ready", LabelEs = "Listo", IsActiveState = true },
            new TripStatusEntity { Id = 3, Code = "in_progress", LabelEs = "En curso", IsActiveState = true },
            new TripStatusEntity { Id = 4, Code = "finished", LabelEs = "Finalizado", IsActiveState = false },
            new TripStatusEntity { Id = 5, Code = "cancelled", LabelEs = "Cancelado", IsActiveState = false }
        );
    }

    private static void SeedReservationStatuses(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ReservationStatusEntity>().HasData(
            new ReservationStatusEntity { Id = 1, Code = "pending", LabelEs = "Pendiente" },
            new ReservationStatusEntity { Id = 2, Code = "confirmed", LabelEs = "Confirmado" },
            new ReservationStatusEntity { Id = 3, Code = "boarded", LabelEs = "Abordado" },
            new ReservationStatusEntity { Id = 4, Code = "cancelled", LabelEs = "Cancelado" }
        );
    }

    private static void ConfigureTripChat(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripChat>(entity =>
        {
            entity.ToTable("TripChats");
            entity.HasKey(c => c.Id);

            entity.HasOne(c => c.Trip)
                .WithOne()
                .HasForeignKey<TripChat>(c => c.TripId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.Property(c => c.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureTripChatMessage(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripChatMessage>(entity =>
        {
            entity.ToTable("TripChatMessages");
            entity.HasKey(m => m.Id);

            entity.HasOne(m => m.Chat)
                .WithMany(c => c.Messages)
                .HasForeignKey(m => m.ChatId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(m => m.SenderUser)
                .WithMany()
                .HasForeignKey(m => m.SenderUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(m => m.MessageText).IsRequired();
            entity.Property(m => m.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasIndex(m => m.ChatId);
            entity.HasIndex(m => m.CreatedAt);
        });
    }

    private static void ConfigureTripChatMessageRead(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripChatMessageRead>(entity =>
        {
            entity.ToTable("TripChatMessageReads");
            entity.HasKey(r => new { r.MessageId, r.UserId });

            entity.HasOne(r => r.Message)
                .WithMany(m => m.Reads)
                .HasForeignKey(r => r.MessageId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(r => r.User)
                .WithMany()
                .HasForeignKey(r => r.UserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(r => r.ReadAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureTripRating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<TripRating>(entity =>
        {
            entity.ToTable("TripRatings");
            entity.HasKey(r => r.Id);

            entity.HasOne(r => r.Trip)
                .WithMany()
                .HasForeignKey(r => r.TripId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(r => r.EvaluatorUser)
                .WithMany()
                .HasForeignKey(r => r.EvaluatorUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(r => r.EvaluatedUser)
                .WithMany()
                .HasForeignKey(r => r.EvaluatedUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(r => r.Score).IsRequired();
            entity.Property(r => r.Comment).HasMaxLength(1000);
            entity.Property(r => r.RatingRole).HasConversion<int>().IsRequired();
            entity.Property(r => r.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            // Unique composite index: 1 rating per evaluator->evaluated per trip
            entity.HasIndex(r => new { r.TripId, r.EvaluatorUserId, r.EvaluatedUserId }).IsUnique();
        });
    }

    private static void ConfigureSupportTicket(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SupportTicket>(entity =>
        {
            entity.ToTable("SupportTickets");
            entity.HasKey(t => t.Id);

            entity.HasOne(t => t.User)
                .WithMany()
                .HasForeignKey(t => t.UserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(t => t.Trip)
                .WithMany()
                .HasForeignKey(t => t.TripId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(t => t.Reservation)
                .WithMany()
                .HasForeignKey(t => t.ReservationId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.Property(t => t.Category).HasConversion<int>().IsRequired();
            entity.Property(t => t.Status).HasConversion<int>().HasDefaultValue(SupportTicketStatus.Open).IsRequired();
            entity.Property(t => t.Subject).IsRequired().HasMaxLength(120);
            entity.Property(t => t.Description).IsRequired().HasMaxLength(2000);
            entity.Property(t => t.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(t => t.UpdatedAt);
            entity.Property(t => t.FirstAdminReplyAt);
            entity.Property(t => t.LastMessageAt);
            entity.Property(t => t.ClosedAt);

            entity.HasOne(t => t.AssignedAdminUser)
                .WithMany()
                .HasForeignKey(t => t.AssignedAdminUserId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasIndex(t => t.UserId);
            entity.HasIndex(t => t.TripId);
            entity.HasIndex(t => t.ReservationId);
            entity.HasIndex(t => t.Status);
            entity.HasIndex(t => t.CreatedAt);
            entity.HasIndex(t => new { t.UserId, t.Category, t.TripId, t.Status });
            entity.HasIndex(t => new { t.UserId, t.Category, t.ReservationId, t.Status });
            entity.HasIndex(t => new { t.UserId, t.FirstAdminReplyAt });
        });
    }

    private static void ConfigureSupportTicketMessage(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SupportTicketMessage>(entity =>
        {
            entity.ToTable("SupportTicketMessages");
            entity.HasKey(m => m.Id);

            entity.HasOne(m => m.Ticket)
                .WithMany(t => t.Messages)
                .HasForeignKey(m => m.TicketId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(m => m.SenderUser)
                .WithMany()
                .HasForeignKey(m => m.SenderUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(m => m.SenderKind).HasConversion<int>().IsRequired();
            entity.Property(m => m.MessageText).IsRequired().HasMaxLength(2000);
            entity.Property(m => m.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasIndex(m => m.TicketId);
            entity.HasIndex(m => new { m.TicketId, m.CreatedAt });
        });
    }

    private static void ConfigureSupportTicketMessageRead(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SupportTicketMessageRead>(entity =>
        {
            entity.ToTable("SupportTicketMessageReads");
            entity.HasKey(r => new { r.MessageId, r.UserId });

            entity.HasOne(r => r.Message)
                .WithMany(m => m.Reads)
                .HasForeignKey(r => r.MessageId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(r => r.User)
                .WithMany()
                .HasForeignKey(r => r.UserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(r => r.ReadAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasIndex(r => r.UserId);
        });
    }

    private static void ConfigureAppSetting(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<AppSetting>(entity =>
        {
            entity.ToTable("AppSettings");
            entity.HasKey(s => s.Key);
            entity.Property(s => s.Key).HasMaxLength(100);
            entity.Property(s => s.Value).IsRequired().HasMaxLength(2000);
        });
    }

    private static void ConfigureSafeZone(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<SafeZone>(entity =>
        {
            entity.ToTable("SafeZones");
            entity.HasKey(z => z.Id);
            entity.Property(z => z.Name).IsRequired().HasMaxLength(120);
            entity.Property(z => z.Description).HasMaxLength(400);
            entity.Property(z => z.Latitude).IsRequired();
            entity.Property(z => z.Longitude).IsRequired();
            entity.Property(z => z.AddressLabel).HasMaxLength(200);
            entity.Property(z => z.Purpose).HasConversion<int>().HasDefaultValue(SafeZonePurpose.Both).IsRequired();
            entity.Property(z => z.IsActive).HasDefaultValue(true).IsRequired();
            entity.Property(z => z.DisplayOrder).HasDefaultValue(0);
            entity.Property(z => z.CampusArea).HasMaxLength(80);
            entity.Property(z => z.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(z => z.UpdatedAt);
            entity.HasIndex(z => z.IsActive);
            entity.HasIndex(z => z.DisplayOrder);
        });
    }
    private static void ConfigureRole(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Role>(entity =>
        {
            entity.ToTable("Roles");
            entity.HasKey(r => r.Id);
            entity.Property(r => r.Name).IsRequired().HasMaxLength(100);
            entity.Property(r => r.Description).HasMaxLength(500);
            entity.Property(r => r.IsSystemRole).HasDefaultValue(false);
        });
    }

    private static void ConfigurePermission(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Permission>(entity =>
        {
            entity.ToTable("Permissions");
            entity.HasKey(p => p.Id);
            entity.Property(p => p.Name).IsRequired().HasMaxLength(120);
            entity.Property(p => p.GroupName).IsRequired().HasMaxLength(120);
        });
    }

    private static void ConfigureUserRole(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserRole>(entity =>
        {
            entity.ToTable("UserRoles");
            entity.HasKey(ur => new { ur.UserId, ur.RoleId });

            entity.HasOne(ur => ur.User)
                .WithMany(u => u.UserRoles)
                .HasForeignKey(ur => ur.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(ur => ur.Role)
                .WithMany(r => r.UserRoles)
                .HasForeignKey(ur => ur.RoleId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureRolePermission(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<RolePermission>(entity =>
        {
            entity.ToTable("RolePermissions");
            entity.HasKey(rp => new { rp.RoleId, rp.PermissionId });

            entity.HasOne(rp => rp.Role)
                .WithMany(r => r.RolePermissions)
                .HasForeignKey(rp => rp.RoleId)
                .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(rp => rp.Permission)
                .WithMany(p => p.RolePermissions)
                .HasForeignKey(rp => rp.PermissionId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigurePaymentMethod(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PaymentMethod>(entity =>
        {
            entity.ToTable("PaymentMethods");
            entity.HasKey(m => m.Id);
            entity.HasIndex(m => m.Code).IsUnique();
            entity.Property(m => m.Code).IsRequired().HasMaxLength(30);
            entity.Property(m => m.Name).IsRequired().HasMaxLength(80);
            entity.Property(m => m.Description).HasMaxLength(300);
            entity.Property(m => m.Type).HasConversion<int>().IsRequired();
            entity.Property(m => m.RequiresManualConfirmation).HasDefaultValue(false).IsRequired();
            entity.Property(m => m.SupportsRefunds).HasDefaultValue(true).IsRequired();
            entity.Property(m => m.IsActive).HasDefaultValue(true).IsRequired();
            entity.Property(m => m.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(m => m.UpdatedAt);
        });
    }

    private static void ConfigureUserPaymentMethod(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserPaymentMethod>(entity =>
        {
            entity.ToTable("UserPaymentMethods");
            entity.HasKey(m => m.Id);

            entity.HasOne(m => m.User)
                .WithMany()
                .HasForeignKey(m => m.UserId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(m => m.PaymentMethod)
                .WithMany(p => p.UserPaymentMethods)
                .HasForeignKey(m => m.PaymentMethodId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.Property(m => m.Alias).HasMaxLength(80);
            entity.Property(m => m.MaskedValue).HasMaxLength(80);
            entity.Property(m => m.ProviderToken).HasMaxLength(120);
            entity.Property(m => m.QrImageUrl).HasMaxLength(300);
            entity.Property(m => m.BankName).HasMaxLength(120);
            entity.Property(m => m.AccountHolderName).HasMaxLength(120);
            entity.Property(m => m.IsDefault).HasDefaultValue(false).IsRequired();
            entity.Property(m => m.IsActive).HasDefaultValue(true).IsRequired();
            entity.Property(m => m.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(m => m.UpdatedAt);

            entity.HasIndex(m => m.UserId);
            entity.HasIndex(m => new { m.UserId, m.PaymentMethodId });
        });
    }

    private static void ConfigurePayment(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Payment>(entity =>
        {
            entity.ToTable("Payments");
            entity.HasKey(p => p.Id);

            entity.HasOne(p => p.Reservation)
                .WithMany()
                .HasForeignKey(p => p.ReservationId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(p => p.PassengerUser)
                .WithMany()
                .HasForeignKey(p => p.PassengerUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(p => p.PaymentMethod)
                .WithMany(m => m.Payments)
                .HasForeignKey(p => p.PaymentMethodId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(p => p.UserPaymentMethod)
                .WithMany()
                .HasForeignKey(p => p.UserPaymentMethodId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.HasOne(p => p.ConfirmedByUser)
                .WithMany()
                .HasForeignKey(p => p.ConfirmedByUserId)
                .OnDelete(DeleteBehavior.NoAction);

            entity.Property(p => p.Amount).HasColumnType("decimal(10,2)").IsRequired();
            entity.Property(p => p.RefundedAmount).HasColumnType("decimal(10,2)").HasDefaultValue(0m).IsRequired();
            entity.Property(p => p.Currency).IsRequired().HasMaxLength(3).HasDefaultValue("BOB");
            entity.Property(p => p.Status).HasConversion<int>().HasDefaultValue(PaymentStatus.Pending).IsRequired();
            entity.Property(p => p.Description).HasMaxLength(300);
            entity.Property(p => p.ExternalReference).HasMaxLength(80);
            entity.Property(p => p.FailureReason).HasMaxLength(300);
            entity.Property(p => p.ConfirmationNotes).HasMaxLength(300);
            entity.Property(p => p.ConfirmationEvidenceUrl).HasMaxLength(300);
            entity.Property(p => p.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(p => p.UpdatedAt);

            entity.HasIndex(p => p.ReservationId);
            entity.HasIndex(p => p.PassengerUserId);
            entity.HasIndex(p => p.Status);
            entity.HasIndex(p => p.CreatedAt);
        });
    }

    private static void ConfigurePaymentTransaction(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PaymentTransaction>(entity =>
        {
            entity.ToTable("PaymentTransactions");
            entity.HasKey(t => t.Id);

            entity.HasOne(t => t.Payment)
                .WithMany(p => p.Transactions)
                .HasForeignKey(t => t.PaymentId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.Property(t => t.TransactionType).HasConversion<int>().HasDefaultValue(PaymentTransactionType.Payment).IsRequired();
            entity.Property(t => t.Status).HasConversion<int>().HasDefaultValue(PaymentTransactionStatus.Pending).IsRequired();
            entity.Property(t => t.Amount).HasColumnType("decimal(10,2)").IsRequired();
            entity.Property(t => t.Provider).HasMaxLength(80);
            entity.Property(t => t.ProviderTransactionId).HasMaxLength(120);
            entity.Property(t => t.AuthorizationCode).HasMaxLength(40);
            entity.Property(t => t.ResponseCode).HasMaxLength(40);
            entity.Property(t => t.ResponseMessage).HasMaxLength(300);
            entity.Property(t => t.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasIndex(t => t.PaymentId);
            entity.HasIndex(t => t.Status);
            entity.HasIndex(t => t.CreatedAt);
        });
    }

    private static void ConfigurePaymentReceipt(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PaymentReceipt>(entity =>
        {
            entity.ToTable("PaymentReceipts");
            entity.HasKey(r => r.Id);
            entity.HasIndex(r => r.PaymentId).IsUnique();
            entity.HasIndex(r => r.ReceiptNumber).IsUnique();

            entity.HasOne(r => r.Payment)
                .WithOne(p => p.Receipt)
                .HasForeignKey<PaymentReceipt>(r => r.PaymentId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.Property(r => r.ReceiptNumber).IsRequired().HasMaxLength(40);
            entity.Property(r => r.ReceiptUrl).HasMaxLength(300);
            entity.Property(r => r.QrCodeValue).HasMaxLength(300);
            entity.Property(r => r.IssuedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(r => r.CreatedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }

    private static void ConfigureRefund(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Refund>(entity =>
        {
            entity.ToTable("Refunds");
            entity.HasKey(r => r.Id);

            entity.HasOne(r => r.Payment)
                .WithMany(p => p.Refunds)
                .HasForeignKey(r => r.PaymentId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.HasOne(r => r.RequestedByUser)
                .WithMany()
                .HasForeignKey(r => r.RequestedByUserId)
                .OnDelete(DeleteBehavior.Restrict)
                .IsRequired();

            entity.HasOne(r => r.ProcessedByUser)
                .WithMany()
                .HasForeignKey(r => r.ProcessedByUserId)
                .OnDelete(DeleteBehavior.SetNull);

            entity.Property(r => r.Amount).HasColumnType("decimal(10,2)").IsRequired();
            entity.Property(r => r.Status).HasConversion<int>().HasDefaultValue(RefundStatus.Requested).IsRequired();
            entity.Property(r => r.Reason).HasMaxLength(500);
            entity.Property(r => r.RejectionReason).HasMaxLength(500);
            entity.Property(r => r.IsWithinCancellationWindow).HasDefaultValue(true).IsRequired();
            entity.Property(r => r.RequestedAt).HasDefaultValueSql("GETUTCDATE()");
            entity.Property(r => r.CreatedAt).HasDefaultValueSql("GETUTCDATE()");

            entity.HasIndex(r => r.PaymentId);
            entity.HasIndex(r => r.RequestedByUserId);
            entity.HasIndex(r => r.Status);
            entity.HasIndex(r => r.RequestedAt);
        });
    }

    private static void SeedPaymentMethods(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PaymentMethod>().HasData(
            new PaymentMethod
            {
                Id = 1,
                Code = "CASH",
                Name = "Efectivo",
                Description = "Pago en efectivo confirmado por el conductor.",
                Type = PaymentMethodType.Cash,
                RequiresManualConfirmation = true,
                SupportsRefunds = false,
                IsActive = true,
                CreatedAt = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc)
            },
            new PaymentMethod
            {
                Id = 2,
                Code = "CARD_SIM",
                Name = "Tarjeta simulada",
                Description = "Pago con tarjeta en ambiente simulado para fines academicos.",
                Type = PaymentMethodType.Simulated,
                RequiresManualConfirmation = false,
                SupportsRefunds = true,
                IsActive = true,
                CreatedAt = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc)
            },
            new PaymentMethod
            {
                Id = 3,
                Code = "QR_BANK",
                Name = "QR bancario",
                Description = "Pago mediante QR bancario del conductor, confirmado manualmente.",
                Type = PaymentMethodType.BankQr,
                RequiresManualConfirmation = true,
                SupportsRefunds = true,
                IsActive = true,
                CreatedAt = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc)
            },
            new PaymentMethod
            {
                Id = 4,
                Code = "WALLET_SIM",
                Name = "Billetera simulada",
                Description = "Saldo interno simulado para fines academicos.",
                Type = PaymentMethodType.Wallet,
                RequiresManualConfirmation = false,
                SupportsRefunds = true,
                IsActive = true,
                CreatedAt = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc)
            }
        );
    }

    private static void ConfigureUserDevice(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserDevice>(entity =>
        {
            entity.ToTable("UserDevices");
            entity.HasKey(d => d.Id);
            entity.HasIndex(d => new { d.UserId, d.FcmToken }).IsUnique();

            entity.HasOne(d => d.User)
                .WithMany()
                .HasForeignKey(d => d.UserId)
                .OnDelete(DeleteBehavior.Cascade)
                .IsRequired();

            entity.Property(d => d.FcmToken).IsRequired().HasMaxLength(500);
            entity.Property(d => d.DeviceName).HasMaxLength(100);
            entity.Property(d => d.LastUsedAt).HasDefaultValueSql("GETUTCDATE()");
        });
    }
}
