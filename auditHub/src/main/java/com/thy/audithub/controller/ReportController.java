package com.thy.audithub.controller;

import com.thy.audithub.dto.ExportRequest;
import com.thy.audithub.dto.FilterRequest;
import com.thy.audithub.dto.IssueRowDto;
import com.thy.audithub.dto.JiraIssueDto;
import com.thy.audithub.service.DashboardService;
import com.thy.audithub.service.ExcelService;
import com.thy.audithub.service.JiraService;
import com.thy.audithub.service.JqlBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ReportController {

    private final JqlBuilderService jqlBuilderService;
    private final JiraService jiraService;
    private final ExcelService excelService;
    private final DashboardService dashboardService;

    /**
     * GET / — Ana sayfa, formu default değerlerle gösterir.
     */
    @GetMapping("/")
    public String index(Model model) {
        FilterRequest defaultFilter = buildDefaultFilter();
        model.addAttribute("filterRequest", defaultFilter);
        model.addAttribute("jqlPreview", jqlBuilderService.build(defaultFilter));
        return "index";
    }

    /**
     * POST /preview — Jira'dan issue çeker, sonuçları ekranda gösterir.
     */
    @PostMapping("/preview")
    public Object preview(@Valid @ModelAttribute FilterRequest filterRequest,
                          BindingResult bindingResult,
                          Model model) {

        if (!filterRequest.isDateRangeValid()) {
            bindingResult.rejectValue("statusChangedEndDate", "date.range",
                    "Bitiş tarihi, başlangıç tarihinden önce olamaz.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("jqlPreview", "");
            return "index";
        }

        String jql = filterRequest.hasCustomJql()
                ? filterRequest.getCustomJql().trim()
                : jqlBuilderService.build(filterRequest);
        log.info("Üretilen JQL: {}", jql);

        List<JiraIssueDto> issues = jiraService.fetchIssues(jql, filterRequest.getJiraToken());
        List<IssueRowDto> rows = excelService.convertToRows(issues);

        model.addAttribute("rows", rows);
        model.addAttribute("issueCount", rows.size());
        model.addAttribute("jql", jql);
        model.addAttribute("exportRequest", new ExportRequest());

        // Dosya adı için proje ve tarihler
        String filename = buildFilename(filterRequest);
        model.addAttribute("filename", filename);

        // Dashboard için müdürlük ve ay bilgisi
        String yearMonth = filterRequest.getStatusChangedStartDate() != null
                ? filterRequest.getStatusChangedStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        model.addAttribute("mudurluk", filterRequest.getMudurluk());
        model.addAttribute("yearMonth", yearMonth);

        return "results";
    }

    /**
     * POST /export-excel — results sayfasından gelen verilerle Excel indirir.
     */
    @PostMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute ExportRequest exportRequest,
                                              @org.springframework.web.bind.annotation.RequestParam(defaultValue = "export.xlsx") String filename) {

        List<IssueRowDto> rows = exportRequest.getRows();

        // Dashboard'a kaydet (sadece autoExportToDashboard=true ise)
        if (exportRequest.isAutoExportToDashboard()) {
            String mudurluk  = exportRequest.getMudurluk();
            String yearMonth = exportRequest.getYearMonth();
            if (mudurluk != null && !mudurluk.isBlank() && yearMonth != null && !yearMonth.isBlank()) {
                dashboardService.saveOrUpdate(mudurluk, yearMonth, rows);
            }
        }

        byte[] excelBytes = excelService.generateExcelFromRows(rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                        .build());
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    /**
     * POST /export — Eski direkt Excel indirme (geriye dönük uyumluluk).
     */
    @PostMapping("/export")
    public Object export(@Valid @ModelAttribute FilterRequest filterRequest,
                         BindingResult bindingResult,
                         Model model) {

        if (!filterRequest.isDateRangeValid()) {
            bindingResult.rejectValue("statusChangedEndDate", "date.range",
                    "Bitiş tarihi, başlangıç tarihinden önce olamaz.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("jqlPreview", "");
            return "index";
        }

        String jql = filterRequest.hasCustomJql()
                ? filterRequest.getCustomJql().trim()
                : jqlBuilderService.build(filterRequest);
        log.info("Üretilen JQL: {}", jql);

        model.addAttribute("jqlPreview", jql);

        List<JiraIssueDto> issues = jiraService.fetchIssues(jql, filterRequest.getJiraToken());

        byte[] excelBytes = excelService.generateExcel(issues);
        String filename = buildFilename(filterRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                        .build());
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    // -----------------------------------------------------------------------

    private FilterRequest buildDefaultFilter() {
        FilterRequest f = new FilterRequest();
        f.setMudurluk("Açık Sistem Çöz. Md.");
        f.setIssueTypes(List.of("Story", "Task"));
        f.setCreatedStartDate(LocalDate.of(2025, 5, 1));
        f.setStatuses(List.of("Done", "Closed"));
        f.setStatusChangedStartDate(LocalDate.now().withDayOfMonth(1));
        f.setStatusChangedEndDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        return f;
    }

    private String buildFilename(FilterRequest filter) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String project = sanitizeForFilename(filter.getMudurluk());
        return project
                + "-"
                + filter.getStatusChangedStartDate().format(fmt)
                + "_"
                + filter.getStatusChangedEndDate().format(fmt)
                + ".xlsx";
    }

    private String sanitizeForFilename(String name) {
        if (name == null || name.isBlank()) return "export";
        String sanitized = name
                .replace('ı', 'i').replace('İ', 'I')
                .replace('ş', 's').replace('Ş', 'S')
                .replace('ğ', 'g').replace('Ğ', 'G')
                .replace('ü', 'u').replace('Ü', 'U')
                .replace('ö', 'o').replace('Ö', 'O')
                .replace('ç', 'c').replace('Ç', 'C');
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|\\s]", "");
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }
        return sanitized.isEmpty() ? "export" : sanitized;
    }
}
