using AuditHub.Models;

namespace AuditHub.Services;

public class JqlBuilderService
{
    public static readonly Dictionary<string, List<string>> MudurlukProjects = new()
    {
        ["Açık Sistem Çöz. Md."]                          = ["FTBASM", "MSS", "MES"],
        ["Rezervasyon & Biletleme Md."]                   = ["TRP", "TKT", "IROR", "IRPR", "TKTPR", "TKTOR"],
        ["Gelir Yönetimi ve Ücret Çöz. Md."]              = ["PRC", "RMOP", "PTS", "UVYFT", "Cygnus"],
        ["Doğrudan Satış Çöz. Md."]                       = ["NDCIN", "NDCUI", "QR", "KB", "BWS"],
        ["Dijital Yolcu Çöz Md."]                         = ["QCG", "KIOSK", "DYMU", "OTHL", "SBD", "TOUR"],
        ["DCS Çöz. Md."]                                  = ["TKP4089", "DCWB", "TDCS"],
        ["Alışveriş İçerik Md."]                          = ["DIJITAL"],
        ["Miles&Smiles Md."]                              = ["DIJITAL"],
        ["Biletleme ve Ek Hizmetler Md."]                 = ["DIJITAL"],
        ["Satış Sonrası ve IRROPS Md."]                   = ["DIJITAL"],
        ["Bağlantı ve Uçak İçi Dijital Çözümler Md."]    = ["ONBP", "BUIDCM", "TKP14896", "TKP18320"],
        ["Ajet Dijital Çöz Md."]                          = ["AJETPSSD", "AKU"],
        ["B2b Çöz Md."]                                   = ["CHA", "AEK", "TKP24958", "TKP17702"],
        ["Ödeme Çöz. Md."]                                = ["TKPAY3"],
        ["Miles and Smiles Çöz. Md."]                     = ["LAS", "SHOPMILES"],
        ["Müşteri İlişkileri ve Pazarlama Çöz.Md"]        = ["IVR", "WAF", "TKP14515", "CPM"],
    };

    public string Build(FilterRequest filter)
    {
        var jql = new System.Text.StringBuilder();

        // issuetype in ("Story", "Task")
        var issueTypes = string.Join(", ", filter.IssueTypes.Select(t => $"\"{t.Replace("\"", "\\\"")}\""));
        jql.Append($"issuetype in ({issueTypes})");

        // AND created >= "2025-05-01"
        jql.Append($" AND created >= \"{filter.CreatedStartDate:yyyy-MM-dd}\"");

        // AND project in ("FTBASM", "MSS", "MES")
        var projects = MudurlukProjects.TryGetValue(filter.Mudurluk, out var list)
            ? list
            : [filter.Mudurluk];
        var projectClause = string.Join(", ", projects.Select(p => $"\"{p}\""));
        jql.Append($" AND project in ({projectClause})");

        // AND status changed to ("Done", "Closed") during ("start", "end")
        var statuses = string.Join(", ", filter.Statuses.Select(s => $"\"{s.Replace("\"", "\\\"")}\""));
        jql.Append($" AND status changed to ({statuses})");
        jql.Append($" during (\"{filter.StatusChangedStartDate:yyyy-MM-dd}\", \"{filter.StatusChangedEndDate:yyyy-MM-dd}\")");

        return jql.ToString();
    }
}
