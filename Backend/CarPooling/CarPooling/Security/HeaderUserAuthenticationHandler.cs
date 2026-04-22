using System.Security.Claims;
using System.Text.Encodings.Web;
using CarPooling.Data;
using Microsoft.AspNetCore.Authentication;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace CarPooling.Security;

public sealed class HeaderUserAuthenticationHandler(
    IOptionsMonitor<AuthenticationSchemeOptions> options,
    ILoggerFactory logger,
    UrlEncoder encoder,
    CarPoolingContext context) : AuthenticationHandler<AuthenticationSchemeOptions>(options, logger, encoder)
{
    public const string SchemeName = "HeaderUser";

    private readonly CarPoolingContext _context = context;

    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        if (!Request.Headers.TryGetValue("X-User-Id", out var userIdHeader))
        {
            return AuthenticateResult.Fail("Falta el encabezado X-User-Id.");
        }

        if (!Guid.TryParse(userIdHeader.ToString(), out var userId))
        {
            return AuthenticateResult.Fail("X-User-Id invalido.");
        }

        var user = await _context.Users
            .AsNoTracking()
            .Where(u => u.Id == userId)
            .Select(u => new { u.Id, u.Role })
            .FirstOrDefaultAsync();

        if (user is null)
        {
            return AuthenticateResult.Fail("Usuario no encontrado.");
        }

        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new(ClaimTypes.Role, user.Role.ToString())
        };

        var identity = new ClaimsIdentity(claims, SchemeName);
        var principal = new ClaimsPrincipal(identity);
        var ticket = new AuthenticationTicket(principal, SchemeName);

        return AuthenticateResult.Success(ticket);
    }
}
