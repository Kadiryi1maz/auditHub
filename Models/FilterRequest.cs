using System.ComponentModel.DataAnnotations;

namespace AuditHub.Models;

public class FilterRequest
{
    [Required(ErrorMessage = "Jira token gereklidir.")]
    public string JiraToken { get; set; } = "";

    [Required(ErrorMessage = "Müdürlük seçimi gereklidir.")]
    public string Mudurluk { get; set; } = "";

    public List<string> IssueTypes { get; set; } = [];

    [Required(ErrorMessage = "Created başlangıç tarihi gereklidir.")]
    public DateOnly CreatedStartDate { get; set; }

    public List<string> Statuses { get; set; } = [];

    [Required(ErrorMessage = "Status tarihi gereklidir.")]
    public DateOnly StatusChangedStartDate { get; set; }

    [Required(ErrorMessage = "Status bitiş tarihi gereklidir.")]
    public DateOnly StatusChangedEndDate { get; set; }

    public string? CustomJql { get; set; }

    public bool IsDateRangeValid() => StatusChangedEndDate >= StatusChangedStartDate;

    public bool HasCustomJql() => !string.IsNullOrWhiteSpace(CustomJql);
}
