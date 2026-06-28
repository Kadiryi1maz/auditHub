using AuditHub.Models;
using AuditHub.Services;
using Microsoft.AspNetCore.Mvc;

namespace AuditHub.Controllers;

public class DashboardController(DashboardService dashboardService, ExcelService excelService,
                                  ILogger<DashboardController> logger) : Controller
{
    public static readonly List<string> Months =
    [
        "2026-01","2026-02","2026-03","2026-04","2026-05","2026-06",
        "2026-07","2026-08","2026-09","2026-10","2026-11","2026-12"
    ];

    public static readonly List<string> MonthLabels =
    [
        "Oca","Şub","Mar","Nis","May","Haz",
        "Tem","Ağu","Eyl","Eki","Kas","Ara"
    ];

    public static readonly List<string> Mudurlukler =
    [
        "Açık Sistem Çöz. Md.",
        "Rezervasyon & Biletleme Md.",
        "Gelir Yönetimi ve Ücret Çöz. Md.",
        "Doğrudan Satış Çöz. Md.",
        "Dijital Yolcu Çöz Md.",
        "DCS Çöz. Md.",
        "Alışveriş İçerik Md.",
        "Miles&Smiles Md.",
        "Biletleme ve Ek Hizmetler Md.",
        "Satış Sonrası ve IRROPS Md.",
        "Bağlantı ve Uçak İçi Dijital Çözümler Md.",
        "Ajet Dijital Çöz Md.",
        "B2b Çöz Md.",
        "Ödeme Çöz. Md.",
        "Miles and Smiles Çöz. Md.",
        "Müşteri İlişkileri ve Pazarlama Çöz.Md"
    ];

    [HttpGet("/dashboard")]
    public IActionResult Index(string? msg, string? err)
    {
        var allEntries = dashboardService.GetAll();
        var lookup = allEntries
            .GroupBy(e => e.Mudurluk)
            .ToDictionary(g => g.Key, g => g.ToDictionary(e => e.YearMonth));

        var stats = Mudurlukler.Select(m =>
        {
            var monthMap = lookup.GetValueOrDefault(m) ?? [];
            var monthEntries = Months.Select(ym => monthMap.GetValueOrDefault(ym)).ToList();
            var withData = monthEntries.Where(e => e != null).Select(e => e!).ToList();

            int sumTotal = withData.Sum(e => e.TotalTasks);
            int sumFU    = withData.Sum(e => e.FormatUygun);
            int sumIU    = withData.Sum(e => e.IcerikUygun);

            return new MudurlukStats
            {
                Name = m,
                MonthEntries = monthEntries,
                Average = withData.Count == 0 ? null : ComputeAverage(withData),
                FormatUygunPercent  = sumTotal > 0 ? sumFU * 100.0 / sumTotal : 0,
                IcerikUygunPercent  = sumTotal > 0 ? sumIU * 100.0 / sumTotal : 0,
                DataCount = withData.Count
            };
        }).ToList();

        ViewBag.Months      = Months;
        ViewBag.MonthLabels = MonthLabels;
        ViewBag.Mudurlukler = Mudurlukler;
        ViewBag.ImportMsg   = msg;
        ViewBag.ImportErr   = err;
        return View(stats);
    }

    [HttpPost("/dashboard/save-cell")]
    public IActionResult SaveCell([FromForm] string mudurluk, [FromForm] string yearMonth,
                                   [FromForm] string field,    [FromForm] string value)
    {
        try
        {
            dashboardService.UpdateCell(mudurluk, yearMonth, field, value);
            return Json(new { success = true, message = "Hücre başarıyla kaydedildi." });
        }
        catch (Exception ex)
        {
            return Json(new { success = false, message = "Hata: " + ex.Message });
        }
    }

    [HttpPost("/import-dashboard")]
    public async Task<IActionResult> ImportExcel(IFormFile file, string mudurluk, string yearMonth)
    {
        if (file == null || file.Length == 0)
            return RedirectToAction("Index", new { err = "Dosya seçilmedi." });

        try
        {
            using var stream = file.OpenReadStream();
            var rows = excelService.ParseRowsFromExcel(stream);
            dashboardService.SaveOrUpdate(mudurluk, yearMonth, rows);
            return RedirectToAction("Index", new
            {
                msg = $"{mudurluk} / {yearMonth} için {rows.Count} satır başarıyla içe aktarıldı."
            });
        }
        catch (Exception ex)
        {
            return RedirectToAction("Index", new { err = "Hata: " + ex.Message });
        }
    }

    [HttpGet("/dashboard/export-excel")]
    public IActionResult ExportExcel(string mudurluk)
    {
        var entries = dashboardService.GetAll()
            .Where(e => e.Mudurluk == mudurluk)
            .OrderBy(e => e.YearMonth)
            .ToList();

        var bytes = excelService.GenerateDashboardExcel(mudurluk, entries, Months, MonthLabels);
        var filename = "dashboard_" + SanitizeName(mudurluk) + ".xlsx";
        var contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return File(bytes, contentType, filename);
    }

    // -----------------------------------------------------------------------

    private static string SanitizeName(string name) =>
        System.Text.RegularExpressions.Regex.Replace(name, @"[^a-zA-Z0-9_\-]", "_");

    private static DashboardEntry ComputeAverage(List<DashboardEntry> entries)
    {
        int n = entries.Count;
        static int Avg(int sum, int n) => n == 0 ? 0 : (int)Math.Round((double)sum / n);
        return new DashboardEntry
        {
            YearMonth        = "avg",
            TotalTasks       = Avg(entries.Sum(e => e.TotalTasks), n),
            FormatUygun      = Avg(entries.Sum(e => e.FormatUygun), n),
            FormatUygunDegil = Avg(entries.Sum(e => e.FormatUygunDegil), n),
            FormatHesap      = Avg(entries.Sum(e => e.FormatHesap), n),
            IcerikUygun      = Avg(entries.Sum(e => e.IcerikUygun), n),
            IcerikUygunDegil = Avg(entries.Sum(e => e.IcerikUygunDegil), n),
            IcerikHesap      = Avg(entries.Sum(e => e.IcerikHesap), n),
            BtaCount         = Avg(entries.Sum(e => e.BtaCount), n),
        };
    }
}
