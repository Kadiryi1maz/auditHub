using System.Net;
using System.Text.Json;
using AuditHub.Config;
using AuditHub.Exceptions;
using AuditHub.Models.Jira;
using Microsoft.Extensions.Options;

namespace AuditHub.Services;

public class JiraService(HttpClient httpClient, IOptions<JiraSettings> opts, ILogger<JiraService> logger)
{
    private const string Fields = "summary,issuetype,status,assignee,reporter,created,updated,labels,components";
    private readonly JiraSettings _settings = opts.Value;
    private readonly JsonSerializerOptions _jsonOpts = new() { PropertyNameCaseInsensitive = true };

    public async Task<List<JiraIssueDto>> FetchIssuesAsync(string jql, string token)
    {
        if (string.IsNullOrWhiteSpace(token))
            throw new JiraTokenMissingException(
                "Jira Personal Access Token bulunamadı. Lütfen ekrandaki Jira Token alanını doldurun.");

        var allIssues = new List<JiraIssueDto>();
        int startAt = 0;
        int maxResults = _settings.MaxResults;

        logger.LogInformation("Jira sorgusu başlatılıyor. JQL: {Jql}", jql);

        do
        {
            var response = await CallJiraAsync(jql, startAt, maxResults, token);
            if (response.Issues?.Count > 0)
                allIssues.AddRange(response.Issues);

            logger.LogInformation("Çekilen: {Current}/{Total}", allIssues.Count, response.Total);
            startAt += maxResults;

            if (startAt >= response.Total) break;
        } while (true);

        logger.LogInformation("Toplam {Count} issue çekildi.", allIssues.Count);
        return allIssues;
    }

    private async Task<JiraSearchResponse> CallJiraAsync(string jql, int startAt, int maxResults, string token)
    {
        var query = $"{_settings.ApiSearchPath}?jql={Uri.EscapeDataString(jql)}" +
                    $"&startAt={startAt}&maxResults={maxResults}" +
                    $"&fields={Fields}&expand=changelog";

        using var request = new HttpRequestMessage(HttpMethod.Get, _settings.BaseUrl + query);
        request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        request.Headers.Accept.ParseAdd("application/json");

        HttpResponseMessage response;
        try
        {
            response = await httpClient.SendAsync(request);
        }
        catch (Exception ex)
        {
            logger.LogError("Jira bağlantı hatası: {Msg}", ex.Message);
            throw new JiraConnectionException(
                "Jira'ya bağlanırken hata oluştu. VPN, ağ bağlantısı veya Jira erişimini kontrol edin.");
        }

        if (response.StatusCode == HttpStatusCode.Unauthorized)
            throw new JiraAuthException(
                "Jira authentication başarısız. Token geçersiz veya süresi dolmuş olabilir.");

        if (response.StatusCode == HttpStatusCode.Forbidden)
            throw new JiraForbiddenException("Bu Jira verilerine erişim yetkiniz olmayabilir.");

        if (response.StatusCode == HttpStatusCode.BadRequest)
        {
            var body = await response.Content.ReadAsStringAsync();
            logger.LogError("Jira 400 yanıtı: {Body}", body);
            if (body.Contains("anonymous users"))
                throw new JiraAuthException(
                    "Jira isteği anonim olarak işlendi. Token'ınızın geçerli olduğunu doğrulayın. Yanıt: " + body);
            throw new JiraInvalidQueryException("Jira yanıtı: " + body);
        }

        response.EnsureSuccessStatusCode();

        var json = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<JiraSearchResponse>(json, _jsonOpts)
               ?? new JiraSearchResponse();
    }
}
