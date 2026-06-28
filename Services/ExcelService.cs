using AuditHub.Config;
using AuditHub.Models;
using AuditHub.Models.Jira;
using ClosedXML.Excel;
using Microsoft.Extensions.Options;

namespace AuditHub.Services;

public class ExcelService(IOptions<JiraSettings> opts, ILogger<ExcelService> logger)
{
    private readonly string _jiraBaseUrl = opts.Value.BaseUrl;
    private const int YellowColStart = 11; // 0-based index, col 12 in 1-based (ClosedXML uses 1-based)

    private static readonly string[] Headers =
    [
        "Issue Type", "Key", "Summary", "Status", "Labels",
        "Back To Analysis Count for Test", "Back To Analysis Count for Development",
        "Back To Analysis Root Cause", "Tester", "Assignee", "Created",
        "Ürün", "Issue Type Uygunluk Durumu", "Analiz Formatına Uygunluk Durumu",
        "Analiz İçeriği Olgunluk Durumu", "Analiz neden uygun değil?"
    ];

    private static readonly string[] DashHeaders =
    [
        "Ay", "Toplam Task",
        "Format Uygun", "Format Uygun Değil", "Format Hesap", "Format %",
        "İçerik Uygun", "İçerik Uygun Değil", "İçerik Hesap", "İçerik %",
        "BTA Sayısı", "Son Güncelleme"
    ];

    // -----------------------------------------------------------------------
    // ConvertToRows
    // -----------------------------------------------------------------------

    public List<IssueRowDto> ConvertToRows(List<JiraIssueDto> issues)
    {
        return issues.Select(issue =>
        {
            var f = issue.Fields;
            var btaTest = GetBackToAnalysisWithDescription(issue, fromTest: true);
            return new IssueRowDto
            {
                IssueType    = f?.IssueType?.Name ?? "",
                Key          = issue.Key ?? "",
                Summary      = f?.Summary ?? "",
                Status       = f?.Status?.Name ?? "",
                Labels       = f?.Labels is { Count: > 0 } ? string.Join(", ", f.Labels) : "",
                BtaTest      = btaTest,
                BtaDev       = "",
                BtaRootCause = !string.IsNullOrEmpty(btaTest) ? "1" : "",
                Tester       = "",
                Assignee     = f?.Assignee?.DisplayName ?? "",
                Created      = FormatJiraDate(f?.Created),
                Component    = f?.Components is { Count: > 0 }
                                 ? string.Join(", ", f.Components.Select(c => c.Name))
                                 : ""
            };
        }).ToList();
    }

    // -----------------------------------------------------------------------
    // GenerateExcelFromRows  (sonuçlar sayfasından export)
    // -----------------------------------------------------------------------

    public byte[] GenerateExcelFromRows(List<IssueRowDto> rows)
    {
        using var wb = new XLWorkbook();
        var ws = wb.AddWorksheet("Jira Export");

        // Başlık satırı
        for (int i = 0; i < Headers.Length; i++)
        {
            var cell = ws.Cell(1, i + 1);
            cell.Value = Headers[i];
            ApplyHeaderStyle(cell, i >= YellowColStart ? "#B8860B" : "#00235D");
        }

        // Veri satırları
        for (int r = 0; r < rows.Count; r++)
            FillRow(ws, r + 2, rows[r]);

        ws.Columns().AdjustToContents();
        // %15 genişlik payı
        for (int i = 1; i <= Headers.Length; i++)
            ws.Column(i).Width = ws.Column(i).Width * 1.15;

        logger.LogInformation("Excel (rows) oluşturuldu. Satır sayısı: {Count}", rows.Count);
        return ToBytes(wb);
    }

    public byte[] GenerateExcel(List<JiraIssueDto> issues) =>
        GenerateExcelFromRows(ConvertToRows(issues));

    private void FillRow(IXLWorksheet ws, int rowNum, IssueRowDto dto)
    {
        void D(int col, string val)
        {
            var cell = ws.Cell(rowNum, col);
            cell.Value = val;
            ApplyDataStyle(cell, col > YellowColStart ? "#FFFDE7" : null);
        }

        D(1, dto.IssueType);

        // Key + hyperlink
        var keyCell = ws.Cell(rowNum, 2);
        keyCell.Value = dto.Key;
        if (!string.IsNullOrEmpty(dto.Key))
            keyCell.SetHyperlink(new XLHyperlink($"{_jiraBaseUrl}/browse/{dto.Key}"));
        ApplyDataStyle(keyCell, null, underline: !string.IsNullOrEmpty(dto.Key));

        D(3, dto.Summary);
        D(4, dto.Status);
        D(5, dto.Labels);
        D(6, dto.BtaTest);
        D(7, dto.BtaDev);
        D(8, dto.BtaRootCause);
        D(9, dto.Tester);
        D(10, dto.Assignee);
        D(11, dto.Created);
        D(12, dto.Urun);
        D(13, dto.IssueTypeUygunluk);
        D(14, dto.AnalizFormatUygunluk);
        D(15, dto.AnalizIcerikOlgunluk);
        D(16, dto.AnalizNedenUygunDegil);
    }

    // -----------------------------------------------------------------------
    // ParseRowsFromExcel  (Excel yükle → dashboard)
    // -----------------------------------------------------------------------

    public List<IssueRowDto> ParseRowsFromExcel(Stream stream)
    {
        var rows = new List<IssueRowDto>();
        using var wb = new XLWorkbook(stream);
        var ws = wb.Worksheet(1);
        var lastRow = ws.LastRowUsed()?.RowNumber() ?? 1;

        for (int r = 2; r <= lastRow; r++)
        {
            var key = CellStr(ws, r, 2);
            if (string.IsNullOrWhiteSpace(key)) continue;

            rows.Add(new IssueRowDto
            {
                IssueType             = CellStr(ws, r, 1),
                Key                   = key,
                Summary               = CellStr(ws, r, 3),
                Status                = CellStr(ws, r, 4),
                Labels                = CellStr(ws, r, 5),
                BtaTest               = CellStr(ws, r, 6),
                BtaDev                = CellStr(ws, r, 7),
                BtaRootCause          = CellStr(ws, r, 8),
                Tester                = CellStr(ws, r, 9),
                Assignee              = CellStr(ws, r, 10),
                Created               = CellStr(ws, r, 11),
                Urun                  = CellStr(ws, r, 12),
                IssueTypeUygunluk     = CellStr(ws, r, 13),
                AnalizFormatUygunluk  = CellStr(ws, r, 14),
                AnalizIcerikOlgunluk  = CellStr(ws, r, 15),
                AnalizNedenUygunDegil = CellStr(ws, r, 16),
            });
        }

        logger.LogInformation("Excel parse edildi: {Count} satır", rows.Count);
        return rows;
    }

    // -----------------------------------------------------------------------
    // GenerateDashboardExcel
    // -----------------------------------------------------------------------

    public byte[] GenerateDashboardExcel(string mudurluk, List<DashboardEntry> entries,
                                          List<string> months, List<string> monthLabels)
    {
        using var wb = new XLWorkbook();
        var ws = wb.AddWorksheet("Dashboard");

        // Başlık satırı (birleştirilmiş)
        var titleCell = ws.Cell(1, 1);
        titleCell.Value = $"{mudurluk} — Dashboard Özeti";
        titleCell.Style.Fill.BackgroundColor = XLColor.FromHtml("#00235D");
        titleCell.Style.Font.FontColor = XLColor.White;
        titleCell.Style.Font.Bold = true;
        titleCell.Style.Font.FontSize = 13;
        titleCell.Style.Alignment.Horizontal = XLAlignmentHorizontalValues.Center;
        titleCell.Style.Alignment.Vertical = XLAlignmentVerticalValues.Center;
        ws.Range(1, 1, 1, DashHeaders.Length).Merge();
        ws.Row(1).Height = 28;

        // Kolon başlıkları
        for (int i = 0; i < DashHeaders.Length; i++)
            ApplyHeaderStyle(ws.Cell(2, i + 1), "#00235D");
        for (int i = 0; i < DashHeaders.Length; i++)
        {
            ws.Cell(2, i + 1).Value = DashHeaders[i];
        }
        ws.Row(2).Height = 20;

        // Lookup
        var lookup = entries.ToDictionary(e => e.YearMonth);

        int sumTotal = 0, sumFU = 0, sumFUD = 0, sumFH = 0;
        int sumIU = 0, sumIUD = 0, sumIH = 0, sumBta = 0;
        int monthsWithData = 0;

        for (int i = 0; i < months.Count; i++)
        {
            int row = i + 3;
            ws.Row(row).Height = 18;
            ws.Cell(row, 1).Value = monthLabels[i];
            ApplyDataStyle(ws.Cell(row, 1), null);

            if (lookup.TryGetValue(months[i], out var e) && e.TotalTasks > 0)
            {
                monthsWithData++;
                sumTotal += e.TotalTasks; sumFU += e.FormatUygun; sumFUD += e.FormatUygunDegil;
                sumFH += e.FormatHesap; sumIU += e.IcerikUygun; sumIUD += e.IcerikUygunDegil;
                sumIH += e.IcerikHesap; sumBta += e.BtaCount;

                void N(int col, int val) { ws.Cell(row, col).Value = val; ApplyDataStyle(ws.Cell(row, col), null, center: true); }
                N(2, e.TotalTasks); N(3, e.FormatUygun); N(4, e.FormatUygunDegil); N(5, e.FormatHesap);

                double fPct = e.TotalTasks > 0 ? e.FormatUygun * 100.0 / e.TotalTasks : 0;
                ApplyPctCell(ws.Cell(row, 6), fPct);

                N(7, e.IcerikUygun); N(8, e.IcerikUygunDegil); N(9, e.IcerikHesap);

                double iPct = e.TotalTasks > 0 ? e.IcerikUygun * 100.0 / e.TotalTasks : 0;
                ApplyPctCell(ws.Cell(row, 10), iPct);

                N(11, e.BtaCount);
                ws.Cell(row, 12).Value = e.LastUpdated;
                ApplyDataStyle(ws.Cell(row, 12), null);
            }
            else
            {
                for (int c = 2; c <= DashHeaders.Length; c++)
                {
                    ws.Cell(row, c).Value = "-";
                    ApplyDataStyle(ws.Cell(row, c), null, center: true);
                }
            }
        }

        // Toplam satırı
        int totRow = months.Count + 3;
        ws.Row(totRow).Height = 20;
        void Tot(int col, object val)
        {
            ws.Cell(totRow, col).Value = XLCellValue.FromObject(val);
            ws.Cell(totRow, col).Style.Fill.BackgroundColor = XLColor.FromHtml("#D3D3D3");
            ws.Cell(totRow, col).Style.Font.Bold = true;
            ws.Cell(totRow, col).Style.Alignment.Horizontal = XLAlignmentHorizontalValues.Center;
            ApplyBorder(ws.Cell(totRow, col));
        }
        Tot(1, "TOPLAM / ORT."); Tot(2, sumTotal); Tot(3, sumFU); Tot(4, sumFUD); Tot(5, sumFH);

        double totF = sumTotal > 0 ? sumFU * 100.0 / sumTotal : 0;
        ApplyPctCell(ws.Cell(totRow, 6), totF);

        Tot(7, sumIU); Tot(8, sumIUD); Tot(9, sumIH);

        double totI = sumTotal > 0 ? sumIU * 100.0 / sumTotal : 0;
        ApplyPctCell(ws.Cell(totRow, 10), totI);

        Tot(11, sumBta); Tot(12, $"{monthsWithData} ay");

        // Sütun genişlikleri
        ws.Column(1).Width = 12;
        for (int c = 2; c <= 11; c++) ws.Column(c).Width = 14;
        ws.Column(12).Width = 20;

        return ToBytes(wb);
    }

    // -----------------------------------------------------------------------
    // BTA helper
    // -----------------------------------------------------------------------

    private static string GetBackToAnalysisWithDescription(JiraIssueDto issue, bool fromTest)
    {
        var histories = issue.Changelog?.Histories;
        if (histories == null) return "";

        bool hasBta = histories
            .SelectMany(h => h.Items)
            .Any(item =>
            {
                if (!"status".Equals(item.Field, StringComparison.OrdinalIgnoreCase)) return false;
                var to   = item.ToStringValue?.ToLower().Trim() ?? "";
                var from = item.FromString?.ToLower().Trim() ?? "";
                bool isBta = to.Contains("back to analysis") || to.Contains("analysis") || from.Contains("back to analysis");
                if (!isBta) return false;
                return fromTest
                    ? from.Contains("test") || from.Contains("qa")
                    : !from.Contains("test") && !from.Contains("qa");
            });

        if (!hasBta) return "";

        return histories
            .SelectMany(h => h.Items)
            .Where(i => "Back to Analysis Description".Equals(i.Field, StringComparison.OrdinalIgnoreCase)
                        && !string.IsNullOrWhiteSpace(i.ToStringValue))
            .Select(i => i.ToStringValue!)
            .LastOrDefault() ?? "";
    }

    // -----------------------------------------------------------------------
    // Style helpers
    // -----------------------------------------------------------------------

    private static void ApplyHeaderStyle(IXLCell cell, string bgHex)
    {
        cell.Style.Fill.BackgroundColor = XLColor.FromHtml(bgHex);
        cell.Style.Font.FontColor = XLColor.White;
        cell.Style.Font.Bold = true;
        cell.Style.Font.FontSize = 11;
        cell.Style.Alignment.Horizontal = XLAlignmentHorizontalValues.Center;
        cell.Style.Alignment.Vertical = XLAlignmentVerticalValues.Center;
        ApplyBorder(cell);
    }

    private static void ApplyDataStyle(IXLCell cell, string? bgHex, bool center = false, bool underline = false)
    {
        if (bgHex != null)
            cell.Style.Fill.BackgroundColor = XLColor.FromHtml(bgHex);
        cell.Style.Font.FontSize = 10;
        if (underline)
        {
            cell.Style.Font.Underline = XLFontUnderlineValues.Single;
            cell.Style.Font.FontColor = XLColor.Blue;
        }
        cell.Style.Alignment.Vertical = XLAlignmentVerticalValues.Center;
        if (center) cell.Style.Alignment.Horizontal = XLAlignmentHorizontalValues.Center;
        cell.Style.Alignment.WrapText = true;
        ApplyBorder(cell);
    }

    private static void ApplyPctCell(IXLCell cell, double pct)
    {
        cell.Value = $"{pct:F1}%";
        var bg = pct >= 80 ? "#C6EFCE" : pct >= 60 ? "#FFEB9C" : "#FFC7CE";
        cell.Style.Fill.BackgroundColor = XLColor.FromHtml(bg);
        cell.Style.Font.Bold = true;
        cell.Style.Alignment.Horizontal = XLAlignmentHorizontalValues.Center;
        ApplyBorder(cell);
    }

    private static void ApplyBorder(IXLCell cell)
    {
        cell.Style.Border.OutsideBorder = XLBorderStyleValues.Thin;
        cell.Style.Border.OutsideBorderColor = XLColor.FromHtml("#CCCCCC");
    }

    // -----------------------------------------------------------------------
    // Utils
    // -----------------------------------------------------------------------

    private static string CellStr(IXLWorksheet ws, int row, int col) =>
        ws.Cell(row, col).GetString().Trim();

    private static string FormatJiraDate(string? jiraDate)
    {
        if (string.IsNullOrWhiteSpace(jiraDate) || jiraDate.Length < 16) return jiraDate ?? "";
        return jiraDate[..16].Replace("T", " ");
    }

    private static byte[] ToBytes(XLWorkbook wb)
    {
        using var ms = new MemoryStream();
        wb.SaveAs(ms);
        return ms.ToArray();
    }
}
