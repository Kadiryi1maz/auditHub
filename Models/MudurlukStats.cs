namespace AuditHub.Models;

public class MudurlukStats
{
    public string Name { get; set; } = "";
    public List<DashboardEntry?> MonthEntries { get; set; } = [];
    public DashboardEntry? Average { get; set; }
    public double FormatUygunPercent { get; set; }
    public double IcerikUygunPercent { get; set; }
    public int DataCount { get; set; }
}
