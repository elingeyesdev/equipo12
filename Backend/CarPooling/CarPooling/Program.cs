using CarPooling.Data;
using CarPooling.Security;
using CarPooling.Services;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

const string AdminPanelCorsPolicy = "AdminPanelCorsPolicy";

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services
    .AddAuthentication(HeaderUserAuthenticationHandler.SchemeName)
    .AddScheme<Microsoft.AspNetCore.Authentication.AuthenticationSchemeOptions, HeaderUserAuthenticationHandler>(
        HeaderUserAuthenticationHandler.SchemeName,
        _ => { });

builder.Services.AddAuthorization();
builder.Services.AddSignalR();

builder.Services.AddDbContext<CarPoolingContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

// Services
builder.Services.AddScoped<TripService>();
builder.Services.AddScoped<ChatService>();
builder.Services.AddScoped<ReservationService>();
builder.Services.AddScoped<VehicleService>();
builder.Services.AddScoped<DriverService>();
builder.Services.AddScoped<GeocodingService>();
builder.Services.AddScoped<RatingService>();
builder.Services.AddScoped<SupportTicketService>();
builder.Services.AddHttpClient<GeocodingService>();

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

app.Run();
