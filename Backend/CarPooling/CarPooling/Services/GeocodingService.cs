using System.Text.Json;
using Microsoft.Extensions.Configuration;

namespace CarPooling.Services;

public class GeocodingService(HttpClient httpClient, IConfiguration configuration)
{
    private readonly HttpClient _httpClient = httpClient;
    private readonly string _accessToken = configuration["Mapbox:AccessToken"]
        ?? throw new InvalidOperationException("Mapbox:AccessToken no configurado.");

    public async Task<string?> ReverseGeocodeAsync(double latitude, double longitude)
    {
        var url = $"https://api.mapbox.com/geocoding/v5/mapbox.places/{longitude},{latitude}.json" +
                  $"?access_token={_accessToken}&language=es&limit=1&types=address,place,poi";

        try
        {
            var response = await _httpClient.GetStringAsync(url);
            using var doc = JsonDocument.Parse(response);
            var features = doc.RootElement.GetProperty("features");
            if (features.GetArrayLength() > 0)
            {
                var placeName = features[0].GetProperty("place_name").GetString();
                return placeName;
            }
        }
        catch
        {
            // Si falla geocoding, devolver formato coordenadas como fallback
        }

        return $"{latitude:F5}, {longitude:F5}";
    }
}
