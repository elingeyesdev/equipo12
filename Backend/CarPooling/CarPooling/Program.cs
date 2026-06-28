using CarPooling.Data;
using CarPooling.Security;
using CarPooling.Services;
using CarPooling.Models;
using Microsoft.EntityFrameworkCore;
using System.Security.Cryptography;
using System.Text;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

var builder = WebApplication.CreateBuilder(args);

const string AdminPanelCorsPolicy = "AdminPanelCorsPolicy";

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddHttpContextAccessor();

builder.Services
    .AddAuthentication(HeaderUserAuthenticationHandler.SchemeName)
    .AddScheme<Microsoft.AspNetCore.Authentication.AuthenticationSchemeOptions, HeaderUserAuthenticationHandler>(
        HeaderUserAuthenticationHandler.SchemeName,
        _ => { });

builder.Services.AddAuthorization();
builder.Services.AddSignalR();

builder.Services.AddDbContext<CarPoolingContext>(options =>
{
    if (builder.Configuration["UseInMemoryDatabase"] == "true")
    {
        var dbName = builder.Configuration["InMemoryDatabaseName"] ?? "CarPoolingDb";
        options.UseInMemoryDatabase(dbName);
    }
    else
    {
        options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection"));
    }
});


// Services
builder.Services.AddScoped<TripService>();
builder.Services.AddScoped<ChatService>();
builder.Services.AddScoped<ReservationService>();
builder.Services.AddScoped<VehicleService>();
builder.Services.AddScoped<GeocodingService>();
builder.Services.AddScoped<RatingService>();
builder.Services.AddScoped<SupportTicketService>();
builder.Services.AddScoped<SupportTicketMessagingService>();
builder.Services.AddScoped<SafeZoneService>();
builder.Services.AddScoped<PaymentService>();
builder.Services.AddScoped<AuditService>();
builder.Services.AddScoped<INotificationService, FirebaseNotificationService>();
builder.Services.AddHttpClient<GeocodingService>();
builder.Services.AddHostedService<TripSchedulerService>();

// Initialize Firebase
var firebaseJson = builder.Configuration["Firebase:CredentialsJson"];
if (!string.IsNullOrWhiteSpace(firebaseJson))
{
    lock (typeof(Program))
    {
        if (FirebaseApp.DefaultInstance is null)
        {
            FirebaseApp.Create(new AppOptions
            {
                Credential = GoogleCredential.FromJson(firebaseJson)
            });
            Console.WriteLine("Firebase inicializado exitosamente desde variable de entorno.");
        }
    }
}
else
{
    var pathToFirebaseKey = builder.Configuration["Firebase:CredentialsPath"] ?? "firebase-adminsdk.json";
    if (!Path.IsPathRooted(pathToFirebaseKey))
    {
        pathToFirebaseKey = Path.Combine(builder.Environment.ContentRootPath, pathToFirebaseKey);
    }

    if (File.Exists(pathToFirebaseKey))
    {
        lock (typeof(Program))
        {
            if (FirebaseApp.DefaultInstance is null)
            {
                FirebaseApp.Create(new AppOptions
                {
                    Credential = GoogleCredential.FromFile(pathToFirebaseKey)
                });
                Console.WriteLine($"Firebase inicializado con credenciales en: {pathToFirebaseKey}");
            }
        }
    }
    else
    {
        Console.WriteLine("Advertencia: No se encontró el archivo de credenciales de Firebase ni la variable de entorno. Las notificaciones push no funcionarán.");
    }
}

builder.Services.AddCors(options =>
{
    options.AddPolicy(AdminPanelCorsPolicy, policy =>
    {
        policy
            .SetIsOriginAllowed(_ => true)
            .AllowAnyHeader()
            .AllowAnyMethod()
            .AllowCredentials();
    });
});

var app = builder.Build();

await DevelopmentDataSeeder.SeedAsync(app.Services);

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}
else
{
    app.UseHttpsRedirection();
}

app.UseRouting();
app.UseCors(AdminPanelCorsPolicy);
app.UseAuthentication();

app.Use(async (context, next) =>
{
    var origin = context.Request.Headers.Origin.ToString();
    if (!string.IsNullOrWhiteSpace(origin))
    {
        context.Response.Headers["Access-Control-Allow-Origin"] = origin;
        context.Response.Headers["Vary"] = "Origin";
        context.Response.Headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
        context.Response.Headers["Access-Control-Allow-Headers"] = "Content-Type, X-Admin-Override, X-User-Id";
    }

    if (HttpMethods.IsOptions(context.Request.Method))
    {
        context.Response.StatusCode = StatusCodes.Status204NoContent;
        return;
    }

    await next();
});

app.UseAuthorization();

app.MapControllers().RequireCors(AdminPanelCorsPolicy);
app.MapHub<CarPooling.Hubs.TripChatHub>("/hubs/tripChat").RequireCors(AdminPanelCorsPolicy);

app.Map("/", () => "API CarPooling is running! 🚀");

app.Run();

public partial class Program { }

