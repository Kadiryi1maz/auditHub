using System.Collections.Concurrent;
using System.Text.Json;
using AuditHub.Models;

namespace AuditHub.Services;

public class DashboardService(ILogger<DashboardService> logger)
{
    private readonly ConcurrentDictionary<string, DashboardEntry> _store = new();
    private const string DataFile = "data/dashboard.json";
    private readonly JsonSerializerOptions _jsonOpts = new() { WriteIndented = true, PropertyNameCaseInsensitive = true };

    public DashboardService(ILogger<DashboardService> logger, IConfiguration _) : this(logger)
    {
        Load();
    }

    private void Load()
    {
        if (!File.Exists(DataFile)) return;
        try
        {
            var json = File.ReadAllText(DataFile);
            var entries = JsonSerializer.Deserialize<List<DashboardEntry>>(json, _jsonOpts) ?? [];
            foreach (var e in entries)
                _store[Key(e.Mudurluk, e.YearMonth)] = e;
            logger.LogInformation("Dashboard: {Count} kayıt yüklendi", entries.Count);
        }
        catch (Exception ex)
        {
            logger.LogWarning("Dashboard verisi yüklenemedi: {Msg}", ex.Message);
        }
    }

    public void SaveOrUpdate(string mudurluk, string yearMonth, List<IssueRowDto> rows)
    {
        int fU = 0, fUD = 0, fH = 0, iU = 0, iUD = 0, iH = 0, bta = 0;

        foreach (var row in rows)
        {
            switch (row.AnalizFormatUygunluk)
            {
                case "Uygun":        fU++;  break;
                case "Uygun Değil":  fUD++; break;
                case "Hesaplanamaz": fH++;  break;
            }
            switch (row.AnalizIcerikOlgunluk)
            {
                case "Uygun":        iU++;  break;
                case "Uygun Değil":  iUD++; break;
                case "Hesaplanamaz": iH++;  break;
            }
            bta += ToInt(row.BtaTest) + ToInt(row.BtaDev);
        }

        var entry = new DashboardEntry
        {
            Mudurluk = mudurluk,
            YearMonth = yearMonth,
            TotalTasks = rows.Count,
            FormatUygun = fU, FormatUygunDegil = fUD, FormatHesap = fH,
            IcerikUygun = iU, IcerikUygunDegil = iUD, IcerikHesap = iH,
            BtaCount = bta,
            LastUpdated = DateTime.Now.ToString("dd.MM.yyyy HH:mm")
        };

        _store[Key(mudurluk, yearMonth)] = entry;
        Persist();
        logger.LogInformation("Dashboard güncellendi: {M}/{Y}", mudurluk, yearMonth);
    }

    public List<DashboardEntry> GetAll() => [.. _store.Values];

    public void UpdateCell(string mudurluk, string yearMonth, string field, string value)
    {
        if (!int.TryParse(value, out var intValue))
            throw new ArgumentException("Sayı dönüşümü başarısız: " + value);

        var entry = _store.GetOrAdd(Key(mudurluk, yearMonth), _ => new DashboardEntry
        {
            Mudurluk = mudurluk,
            YearMonth = yearMonth
        });

        switch (field)
        {
            case "totalTasks":       entry.TotalTasks = intValue;       break;
            case "formatUygun":      entry.FormatUygun = intValue;      break;
            case "formatUygunDegil": entry.FormatUygunDegil = intValue; break;
            case "formatHesap":      entry.FormatHesap = intValue;      break;
            case "icerikUygun":      entry.IcerikUygun = intValue;      break;
            case "icerikUygunDegil": entry.IcerikUygunDegil = intValue; break;
            case "icerikHesap":      entry.IcerikHesap = intValue;      break;
            case "btaCount":         entry.BtaCount = intValue;         break;
            default: throw new ArgumentException("Bilinmeyen alan: " + field);
        }

        entry.LastUpdated = DateTime.Now.ToString("dd.MM.yyyy HH:mm");
        Persist();
    }

    private static string Key(string m, string ym) => $"{m}|{ym}";

    private static int ToInt(string? s)
    {
        return int.TryParse(s?.Trim(), out var v) ? v : 0;
    }

    private void Persist()
    {
        try
        {
            Directory.CreateDirectory("data");
            File.WriteAllText(DataFile, JsonSerializer.Serialize(_store.Values.ToList(), _jsonOpts));
        }
        catch (Exception ex)
        {
            logger.LogError("Dashboard kaydedilemedi: {Msg}", ex.Message);
        }
    }
}
