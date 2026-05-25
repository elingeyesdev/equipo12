namespace CarPooling.Dtos;

public class LocationDto
{
    public Guid Id { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string? AddressLabel { get; set; }
}
