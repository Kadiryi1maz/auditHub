using System.Text.Json.Serialization;

namespace AuditHub.Models.Jira;

public class JiraSearchResponse
{
    [JsonPropertyName("startAt")]
    public int StartAt { get; set; }

    [JsonPropertyName("maxResults")]
    public int MaxResults { get; set; }

    [JsonPropertyName("total")]
    public int Total { get; set; }

    [JsonPropertyName("issues")]
    public List<JiraIssueDto> Issues { get; set; } = [];
}

public class JiraIssueDto
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";

    [JsonPropertyName("key")]
    public string Key { get; set; } = "";

    [JsonPropertyName("fields")]
    public JiraFieldDto Fields { get; set; } = new();

    [JsonPropertyName("changelog")]
    public JiraChangelogDto? Changelog { get; set; }
}

public class JiraFieldDto
{
    [JsonPropertyName("summary")]
    public string Summary { get; set; } = "";

    [JsonPropertyName("issuetype")]
    public JiraNameDto? IssueType { get; set; }

    [JsonPropertyName("status")]
    public JiraNameDto? Status { get; set; }

    [JsonPropertyName("assignee")]
    public JiraUserDto? Assignee { get; set; }

    [JsonPropertyName("reporter")]
    public JiraUserDto? Reporter { get; set; }

    [JsonPropertyName("created")]
    public string? Created { get; set; }

    [JsonPropertyName("updated")]
    public string? Updated { get; set; }

    [JsonPropertyName("labels")]
    public List<string> Labels { get; set; } = [];

    [JsonPropertyName("components")]
    public List<JiraNameDto> Components { get; set; } = [];
}

public class JiraNameDto
{
    [JsonPropertyName("name")]
    public string Name { get; set; } = "";
}

public class JiraUserDto
{
    [JsonPropertyName("displayName")]
    public string? DisplayName { get; set; }

    [JsonPropertyName("emailAddress")]
    public string? EmailAddress { get; set; }

    [JsonPropertyName("name")]
    public string? Name { get; set; }
}

public class JiraChangelogDto
{
    [JsonPropertyName("histories")]
    public List<JiraChangelogHistoryDto> Histories { get; set; } = [];
}

public class JiraChangelogHistoryDto
{
    [JsonPropertyName("items")]
    public List<JiraChangelogItemDto> Items { get; set; } = [];
}

public class JiraChangelogItemDto
{
    [JsonPropertyName("field")]
    public string Field { get; set; } = "";

    [JsonPropertyName("fromString")]
    public string? FromString { get; set; }

    [JsonPropertyName("toString")]
    public string? ToStringValue { get; set; }
}
