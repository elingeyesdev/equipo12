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
    public DbSet<AppSetting> AppSettings => Set<AppSetting>();
    public DbSet<SafeZone> SafeZones => Set<SafeZone>();
    public DbSet<Role> Roles => Set<Role>();
    public DbSet<Permission> Permissions => Set<Permission>();
    public DbSet<UserRole> UserRoles => Set<UserRole>();
    public DbSet<RolePermission> RolePermissions => Set<RolePermission>();

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
        ConfigureAppSetting(modelBuilder);
        ConfigureSafeZone(modelBuilder);
        ConfigureRole(modelBuilder);
        ConfigurePermission(modelBuilder);
        ConfigureUserRole(modelBuilder);
        ConfigureRolePermission(modelBuilder);

        SeedTripStatuses(modelBuilder);
        SeedReservationStatuses(modelBuilder);
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

            entity.HasIndex(t => t.UserId);
            entity.HasIndex(t => t.TripId);
            entity.HasIndex(t => t.ReservationId);
            entity.HasIndex(t => t.Status);
            entity.HasIndex(t => t.CreatedAt);
            entity.HasIndex(t => new { t.UserId, t.Category, t.TripId, t.Status });
            entity.HasIndex(t => new { t.UserId, t.Category, t.ReservationId, t.Status });
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
}
