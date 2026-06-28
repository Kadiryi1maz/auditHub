namespace AuditHub.Models;

public class DashboardEntry
{
    public string Mudurluk { get; set; } = "";
    public string YearMonth { get; set; } = "";
    public int TotalTasks { get; set; }
    public int FormatUygun { get; set; }
    public int FormatUygunDegil { get; set; }
    public int FormatHesap { get; set; }
    public int IcerikUygun { get; set; }
    public int IcerikUygunDegil { get; set; }
    public int IcerikHesap { get; set; }
    public int BtaCount { get; set; }
    public string LastUpdated { get; set; } = "";
}
