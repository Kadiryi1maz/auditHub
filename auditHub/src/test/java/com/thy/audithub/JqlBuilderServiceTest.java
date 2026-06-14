package com.thy.audithub;

import com.thy.audithub.dto.FilterRequest;
import com.thy.audithub.service.JqlBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JqlBuilderServiceTest {

    private JqlBuilderService jqlBuilderService;

    @BeforeEach
    void setUp() {
        jqlBuilderService = new JqlBuilderService();
    }

    // -----------------------------------------------------------------------
    // Test 1 — Rehberdeki örnek input ile beklenen JQL tam eşleşmeli
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Standart filtre ile beklenen JQL üretilmeli")
    void shouldBuildExpectedJql_forStandardFilter() {
        FilterRequest filter = new FilterRequest();
        filter.setMudurluk("Açık Sistem Çöz. Md.");
        filter.setIssueTypes(List.of("Story", "Task"));
        filter.setCreatedStartDate(LocalDate.of(2025, 5, 1));
        filter.setStatuses(List.of("Done", "Closed"));
        filter.setStatusChangedStartDate(LocalDate.of(2026, 4, 1));
        filter.setStatusChangedEndDate(LocalDate.of(2026, 4, 27));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).isEqualTo(
                "issuetype in (\"Story\", \"Task\")"
                + " AND created >= \"2025-05-01\""
                + " AND project in (\"FTBASM\", \"MSS\", \"MES\")"
                + " AND status changed to (\"Done\", \"Closed\")"
                + " during (\"2026-04-01\", \"2026-04-27\")"
        );
    }

    // -----------------------------------------------------------------------
    // Test 2 — Tek issue type ile JQL
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Tek issue type seçilince parantez içinde virgülsüz yazılmalı")
    void shouldBuildJql_withSingleIssueType() {
        FilterRequest filter = buildBaseFilter();
        filter.setIssueTypes(List.of("Bug"));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("issuetype in (\"Bug\")");
    }

    // -----------------------------------------------------------------------
    // Test 3 — Üç issue type ile virgülle ayrılmalı
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Birden fazla issue type virgülle ayrılmalı")
    void shouldBuildJql_withMultipleIssueTypes() {
        FilterRequest filter = buildBaseFilter();
        filter.setIssueTypes(List.of("Story", "Task", "Bug"));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("issuetype in (\"Story\", \"Task\", \"Bug\")");
    }

    // -----------------------------------------------------------------------
    // Test 4 — Tek status ile JQL
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Tek status seçilince doğru JQL üretilmeli")
    void shouldBuildJql_withSingleStatus() {
        FilterRequest filter = buildBaseFilter();
        filter.setStatuses(List.of("Done"));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("status changed to (\"Done\")");
    }

    // -----------------------------------------------------------------------
    // Test 5 — Üç status virgülle ayrılmalı
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Birden fazla status virgülle ayrılmalı")
    void shouldBuildJql_withMultipleStatuses() {
        FilterRequest filter = buildBaseFilter();
        filter.setStatuses(List.of("Done", "Closed", "Resolved"));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("status changed to (\"Done\", \"Closed\", \"Resolved\")");
    }

    // -----------------------------------------------------------------------
    // Test 6 — Proje adı çift tırnak içinde olmalı
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Md. seçilince doğru projeler JQL'e eklenmeli")
    void shouldMapMudurluk_toProjects() {
        FilterRequest filter = buildBaseFilter();

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("project in (\"FTBASM\", \"MSS\", \"MES\")");
    }

    @Test
    @DisplayName("Gelir Yönetimi için doğru proje keyleri kullanılmalı, component filtresi olmamalı")
    void shouldMapGelirYonetimi_toCorrectProjects() {
        FilterRequest filter = buildBaseFilter();
        filter.setMudurluk("Gelir Yönetimi ve Ücret Çöz. Md.");

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("project in (\"PRC\", \"RMOP\", \"PTS\", \"UVYFT\", \"Cygnus\")");
        assertThat(jql).doesNotContain("component in");
    }

    @Test
    @DisplayName("Board filtresi standart Jira JQL'de desteklenmiyorsa eklenmemeli")
    void shouldNotIncludeBoardClause_whenBoardFilterMappingExists() {
        FilterRequest filter = buildBaseFilter();
        filter.setMudurluk("Rezervasyon & Biletleme Md.");

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).doesNotContain("board in");
    }

    // -----------------------------------------------------------------------
    // Test 7 — Tarihler yyyy-MM-dd formatında çift tırnak içinde olmalı
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Tarihler yyyy-MM-dd formatında çift tırnak içinde olmalı")
    void shouldFormatDates_inDoubleQuotes() {
        FilterRequest filter = buildBaseFilter();
        filter.setCreatedStartDate(LocalDate.of(2024, 1, 15));
        filter.setStatusChangedStartDate(LocalDate.of(2024, 6, 1));
        filter.setStatusChangedEndDate(LocalDate.of(2024, 6, 30));

        String jql = jqlBuilderService.build(filter);

        assertThat(jql).contains("created >= \"2024-01-15\"");
        assertThat(jql).contains("during (\"2024-06-01\", \"2024-06-30\")");
    }

    // -----------------------------------------------------------------------
    // Test 8 — FilterRequest tarih aralığı validasyonu
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Bitiş tarihi başlangıçtan önce ise isDateRangeValid false dönmeli")
    void shouldReturnFalse_whenEndDateBeforeStartDate() {
        FilterRequest filter = buildBaseFilter();
        filter.setStatusChangedStartDate(LocalDate.of(2026, 4, 27));
        filter.setStatusChangedEndDate(LocalDate.of(2026, 4, 1));   // bitiş < başlangıç

        assertThat(filter.isDateRangeValid()).isFalse();
    }

    @Test
    @DisplayName("Başlangıç ve bitiş aynı gün ise isDateRangeValid true dönmeli")
    void shouldReturnTrue_whenStartDateEqualsEndDate() {
        FilterRequest filter = buildBaseFilter();
        filter.setStatusChangedStartDate(LocalDate.of(2026, 4, 1));
        filter.setStatusChangedEndDate(LocalDate.of(2026, 4, 1));

        assertThat(filter.isDateRangeValid()).isTrue();
    }

    @Test
    @DisplayName("Bitiş tarihi başlangıçtan sonra ise isDateRangeValid true dönmeli")
    void shouldReturnTrue_whenEndDateAfterStartDate() {
        FilterRequest filter = buildBaseFilter();
        filter.setStatusChangedStartDate(LocalDate.of(2026, 4, 1));
        filter.setStatusChangedEndDate(LocalDate.of(2026, 4, 27));

        assertThat(filter.isDateRangeValid()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Yardımcı metot
    // -----------------------------------------------------------------------
    private FilterRequest buildBaseFilter() {
        FilterRequest f = new FilterRequest();
        f.setMudurluk("Açık Sistem Çöz. Md.");
        f.setIssueTypes(List.of("Story", "Task"));
        f.setCreatedStartDate(LocalDate.of(2025, 5, 1));
        f.setStatuses(List.of("Done", "Closed"));
        f.setStatusChangedStartDate(LocalDate.of(2026, 4, 1));
        f.setStatusChangedEndDate(LocalDate.of(2026, 4, 27));
        return f;
    }
}
