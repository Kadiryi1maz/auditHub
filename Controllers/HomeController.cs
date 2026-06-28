using AuditHub.Exceptions;
using AuditHub.Models;
using AuditHub.Services;
using Microsoft.AspNetCore.Mvc;

namespace AuditHub.Controllers;

public class HomeController(JqlBuilderService jqlBuilder, JiraService jiraService,
                             ExcelService excelService, DashboardService dashboardService,
                             ILogger<HomeController> logger) : Controller
{
    [HttpGet("/")]
    public IActionResult Index()
    {
        var filter = BuildDefaultFilter();
        ViewBag.JqlPreview = jqlBuilder.Build(filter);
        return View(filter);
    }

    [HttpPost("/preview")]
    public async Task<IActionResult> Preview(FilterRequest filter)
    {
        if (!filter.IsDateRangeValid())
            ModelState.AddModelError("StatusChangedEndDate", "Bitiş tarihi başlangıçtan önce olamaz.");

        if (filter.IssueTypes.Count == 0)
            ModelState.AddModelError("IssueTypes", "En az bir Issue Type seçin.");

        if (filter.Statuses.Count == 0)
            ModelState.AddModelError("Statuses", "En az bir Status seçin.");

        if (!ModelState.IsValid)
        {
            ViewBag.JqlPreview = "";
            return View("Index", filter);
        }

        var jql = filter.HasCustomJql()
            ? filter.CustomJql!.Trim()
            : jqlBuilder.Build(filter);

        logger.LogInformation("Üretilen JQL: {Jql}", jql);

        try
        {
            var issues = await jiraService.FetchIssuesAsync(jql, filter.JiraToken);
            var rows = excelService.ConvertToRows(issues);

            ViewBag.Jql = jql;
            ViewBag.IssueCount = rows.Count;
            ViewBag.Filename = BuildFilename(filter);
            ViewBag.Mudurluk = filter.Mudurluk;
            ViewBag.YearMonth = filter.StatusChangedStartDate.ToString("yyyy-MM");

            return View("Results", rows);
        }
        catch (JiraTokenMissingException ex) { return ErrorView(ex.Message); }
        catch (JiraAuthException ex)         { return ErrorView(ex.Message); }
        catch (JiraForbiddenException ex)    { return ErrorView(ex.Message); }
        catch (JiraInvalidQueryException ex) { return ErrorView(ex.Message); }
        catch (JiraConnectionException ex)   { return ErrorView(ex.Message); }
        catch (Exception ex)
        {
            logger.LogError(ex, "Beklenmeyen hata");
            return ErrorView("Beklenmeyen bir hata oluştu: " + ex.Message);
        }
    }

    [HttpPost("/export-excel")]
    public IActionResult ExportExcel(ExportRequest request,
                                     [FromQuery] string filename = "export.xlsx")
    {
        if (request.AutoExportToDashboard
            && !string.IsNullOrWhiteSpace(request.Mudurluk)
            && !string.IsNullOrWhiteSpace(request.YearMonth))
        {
            dashboardService.SaveOrUpdate(request.Mudurluk, request.YearMonth, request.Rows);
        }

        var bytes = excelService.GenerateExcelFromRows(request.Rows);
        var contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return File(bytes, contentType, filename);
    }

    [HttpGet("/error")]
    public IActionResult Error() => View();

    // -----------------------------------------------------------------------

    private IActionResult ErrorView(string message)
    {
        ViewBag.ErrorMessage = message;
        return View("Index", BuildDefaultFilter());
    }

    private static FilterRequest BuildDefaultFilter() => new()
    {
        Mudurluk = "Açık Sistem Çöz. Md.",
        IssueTypes = ["Story", "Task"],
        CreatedStartDate = new DateOnly(2025, 5, 1),
        Statuses = ["Done", "Closed"],
        StatusChangedStartDate = new DateOnly(DateTime.Today.Year, DateTime.Today.Month, 1),
        StatusChangedEndDate = new DateOnly(DateTime.Today.Year, DateTime.Today.Month,
                                            DateTime.DaysInMonth(DateTime.Today.Year, DateTime.Today.Month))
    };

    private static string BuildFilename(FilterRequest f)
    {
        var project = SanitizeName(f.Mudurluk);
        return $"{project}-{f.StatusChangedStartDate:yyyy-MM-dd}_{f.StatusChangedEndDate:yyyy-MM-dd}.xlsx";
    }

    private static string SanitizeName(string? name)
    {
        if (string.IsNullOrWhiteSpace(name)) return "export";
        var s = name
            .Replace('ı', 'i').Replace('İ', 'I')
            .Replace('ş', 's').Replace('Ş', 'S')
            .Replace('ğ', 'g').Replace('Ğ', 'G')
            .Replace('ü', 'u').Replace('Ü', 'U')
            .Replace('ö', 'o').Replace('Ö', 'O')
            .Replace('ç', 'c').Replace('Ç', 'C');
        s = System.Text.RegularExpressions.Regex.Replace(s, @"[\\/:*?""<>|\s]", "");
        return s.Length > 40 ? s[..40] : (s.Length == 0 ? "export" : s);
    }
}
