namespace CarPooling.Security;

public static class AppPermissions
{
    // Category: Metrics
    public const string ViewMetrics = "metrics:view";

    // Category: Users
    public const string ReadUsers = "users:read";
    public const string WriteUsers = "users:write";
    public const string DeleteUsers = "users:delete";

    // Category: Trips
    public const string ReadTrips = "trips:read";
    public const string WriteTrips = "trips:write";
    public const string DeleteTrips = "trips:delete";

    // Category: Reservations
    public const string ReadReservations = "reservations:read";
    public const string WriteReservations = "reservations:write";
    public const string DeleteReservations = "reservations:delete";

    // Category: Support
    public const string ReadSupport = "support:read";
    public const string WriteSupport = "support:write";

    // Category: Roles & Permissions (Superadmin exclusive)
    public const string ManageRoles = "roles:manage";

    public static readonly IReadOnlyList<(string Id, string Name, string GroupName)> AllPermissions = new List<(string Id, string Name, string GroupName)>
    {
        (ViewMetrics, "Visualizar Métricas y Dashboard", "Métricas"),
        
        (ReadUsers, "Ver Usuarios", "Usuarios"),
        (WriteUsers, "Modificar Usuarios", "Usuarios"),
        (DeleteUsers, "Eliminar Usuarios", "Usuarios"),
        
        (ReadTrips, "Ver Viajes", "Viajes"),
        (WriteTrips, "Modificar Viajes", "Viajes"),
        (DeleteTrips, "Eliminar Viajes", "Viajes"),
        
        (ReadReservations, "Ver Reservas", "Reservas"),
        (WriteReservations, "Modificar Reservas", "Reservas"),
        (DeleteReservations, "Eliminar Reservas", "Reservas"),
        
        (ReadSupport, "Ver Reportes de Soporte", "Soporte"),
        (WriteSupport, "Responder Reportes de Soporte", "Soporte"),
        
        (ManageRoles, "Gestionar Roles y Permisos", "Seguridad")
    };
}
