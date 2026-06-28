namespace AuditHub.Models;

public class ExportRequest
{
    public List<IssueRowDto> Rows { get; set; } = [];
    public string Mudurluk { get; set; } = "";
    public string YearMonth { get; set; } = "";
    public bool AutoExportToDashboard { get; set; }
}
