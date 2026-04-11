using CarPooling.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

namespace CarPooling.Data;

public class CarPoolingContext(DbContextOptions<CarPoolingContext> options) : DbContext(options)
{
    public DbSet<Trip> Trips => Set<Trip>();
    public DbSet<Reservation> Reservations => Set<Reservation>();
    public DbSet<User> Users => Set<User>();

    private static string StatusToString(TripStatus status)
    {
        return status == TripStatus.Ready
            ? "listo"
            : status == TripStatus.Cancelled
                ? "cancelado"
                : status == TripStatus.InProgress
                    ? "en_curso"
                    : status == TripStatus.Finished
                        ? "finalizado"
                        : "activo";
    }

    private static TripStatus StatusFromString(string value)
    {
        var normalized = value.Trim().ToLower();

        if (normalized == "activo" || normalized == "awaitingdestination" || normalized == "pending")
        {
            return TripStatus.AwaitingDestination;
        }

        if (normalized == "listo" || normalized == "ready")
        {
            return TripStatus.Ready;
        }

        if (normalized == "cancelado" || normalized == "cancelled")
        {
            return TripStatus.Cancelled;
        }

        if (normalized == "en_curso" || normalized == "en curso" || normalized == "inprogress" || normalized == "in_progress" || normalized == "ongoing")
        {
            return TripStatus.InProgress;
        }

        if (normalized == "finalizado" || normalized == "finished" || normalized == "completed" || normalized == "done")
        {
            return TripStatus.Finished;
        }

        return TripStatus.AwaitingDestination;
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Trip>(entity =>
        {
            entity.ToTable("Trips");
            entity.HasKey(t => t.Id);

            entity.Property(t => t.OriginLatitude)
                .IsRequired();

            entity.Property(t => t.OriginLongitude)
                .IsRequired();

            entity.Property(t => t.DestinationLatitude);
            entity.Property(t => t.DestinationLongitude);

            entity.Property(t => t.Status)
                .HasConversion(new ValueConverter<TripStatus, string>(
                    status => StatusToString(status),
                    value => StatusFromString(value)))
                .HasMaxLength(32)
                .IsRequired();

            entity.Property(t => t.CreatedAt)
                .HasDefaultValueSql("GETUTCDATE()");

            entity.Property(t => t.UpdatedAt);
            entity.Property(t => t.CancelledAt)
                .HasDefaultValueSql("NULL");
        });

        modelBuilder.Entity<Reservation>(entity =>
        {
            entity.ToTable("Reservations");
            entity.HasKey(r => r.Id);

            entity.HasOne(r => r.Trip)
                .WithMany()
                .HasForeignKey(r => r.TripId)
                .OnDelete(DeleteBehavior.Restrict);

            entity.Property(r => r.PassengerName)
                .IsRequired()
                .HasMaxLength(100);

            entity.Property(r => r.Status)
                .IsRequired();

            entity.Property(r => r.CreatedAt)
                .HasDefaultValueSql("GETUTCDATE()");
        });

        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("Users");
            entity.HasKey(u => u.Id);

            entity.HasIndex(u => u.Email)
                .IsUnique();

            entity.Property(u => u.FullName)
                .IsRequired()
                .HasMaxLength(120);

            entity.Property(u => u.Email)
                .IsRequired()
                .HasMaxLength(120);

            entity.Property(u => u.PasswordHash)
                .IsRequired()
                .HasMaxLength(256);

            entity.Property(u => u.PhoneNumber)
                .HasMaxLength(25);

            entity.Property(u => u.CreatedAt)
                .HasDefaultValueSql("GETUTCDATE()");
        });
    }
}
