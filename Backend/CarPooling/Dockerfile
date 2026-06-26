# Use the official .NET 9.0 SDK image to build the app
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copy the project files and restore dependencies
COPY ["CarPooling/CarPooling.csproj", "CarPooling/"]
RUN dotnet restore "CarPooling/CarPooling.csproj"

# Copy the remaining source code and build the app
COPY . .
WORKDIR "/src/CarPooling"
RUN dotnet publish "CarPooling.csproj" -c Release -o /app/publish

# Use the official .NET 9.0 ASP.NET runtime image to run the app
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS final
WORKDIR /app
COPY --from=build /app/publish .

# Expose the port your application will run on
EXPOSE 8080
ENV ASPNETCORE_URLS=http://+:8080

ENTRYPOINT ["dotnet", "CarPooling.dll"]
