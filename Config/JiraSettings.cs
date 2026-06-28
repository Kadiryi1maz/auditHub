namespace AuditHub.Config;

public class JiraSettings
{
    public string BaseUrl { get; set; } = "";
    public string PersonalAccessToken { get; set; } = "";
    public string ApiSearchPath { get; set; } = "/rest/api/2/search";
    public int MaxResults { get; set; } = 100;

    public bool IsTokenConfigured => !string.IsNullOrWhiteSpace(PersonalAccessToken);
}
