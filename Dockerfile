# .NET 8.0 Runtime image
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 5000

# .NET 8.0 SDK image for building
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY ["AuditHub.csproj", "./"]
RUN dotnet restore "AuditHub.csproj"
COPY . .
RUN dotnet build "AuditHub.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "AuditHub.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENV ASPNETCORE_URLS=http://+:5000
ENTRYPOINT ["dotnet", "AuditHub.dll"]
