namespace AuditHub.Models;

public class IssueRowDto
{
    public string IssueType { get; set; } = "";
    public string Key { get; set; } = "";
    public string Summary { get; set; } = "";
    public string Status { get; set; } = "";
    public string Labels { get; set; } = "";
    public string BtaTest { get; set; } = "";
    public string BtaDev { get; set; } = "";
    public string BtaRootCause { get; set; } = "";
    public string Tester { get; set; } = "";
    public string Assignee { get; set; } = "";
    public string Created { get; set; } = "";
    public string Component { get; set; } = "";

    // Kullanıcı tarafından doldurulan sarı kolonlar
    public string Urun { get; set; } = "";
    public string IssueTypeUygunluk { get; set; } = "";
    public string AnalizFormatUygunluk { get; set; } = "";
    public string AnalizIcerikOlgunluk { get; set; } = "";
    public string AnalizNedenUygunDegil { get; set; } = "";
}
