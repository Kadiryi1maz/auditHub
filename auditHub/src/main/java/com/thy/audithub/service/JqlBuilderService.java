package com.thy.audithub.service;

import com.thy.audithub.dto.FilterRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JqlBuilderService {

    private static final DateTimeFormatter JIRA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Md. adı → Jira proje anahtarları eşlemesi.
     * Yeni müdürlükler buraya eklenir.
     */
    public static final Map<String, List<String>> MUDURLUK_PROJECTS = Map.ofEntries(
            Map.entry("Açık Sistem Çöz. Md.",                  List.of("FTBASM", "MSS", "MES")),
            Map.entry("Rezervasyon & Biletleme Md.",            List.of("TRP", "TKT", "IROR", "IRPR", "TKTPR", "TKTOR")),
            Map.entry("Gelir Yönetimi ve Ücret Çöz. Md.",      List.of("PRC", "RMOP", "PTS", "UVYFT", "Cygnus")),
            Map.entry("Doğrudan Satış Çöz. Md.",               List.of("NDCIN", "NDCUI", "QR", "KB", "BWS")),
            Map.entry("Dijital Yolcu Çöz Md.",                 List.of("QCG", "KIOSK", "DYMU", "OTHL", "SBD", "TOUR")),
            Map.entry("DCS Çöz. Md.",                          List.of("TKP4089", "DCWB", "TDCS")),
            Map.entry("Alışveriş İçerik Md.",                  List.of("DIJITAL")),
            Map.entry("Miles&Smiles Md.",                      List.of("DIJITAL")),
            Map.entry("Biletleme ve Ek Hizmetler Md.",         List.of("DIJITAL")),
            Map.entry("Satış Sonrası ve IRROPS Md.",           List.of("DIJITAL")),
            Map.entry("Bağlantı ve Uçak İçi Dijital Çözümler Md.", List.of("ONBP", "BUIDCM", "TKP14896", "TKP18320")),
            Map.entry("Ajet Dijital Çöz Md.",                  List.of("AJETPSSD", "AKU")),
            Map.entry("B2b Çöz Md.",                           List.of("CHA", "AEK", "TKP24958", "TKP17702")),
            Map.entry("Ödeme Çöz. Md.",                        List.of("TKPAY3")),
            Map.entry("Miles and Smiles Çöz. Md.",             List.of("LAS", "SHOPMILES")),
            Map.entry("Müşteri İlişkileri ve Pazarlama Çöz.Md", List.of("IVR", "WAF", "TKP14515", "CPM"))
    );

    /**
     * Md. adı → Board keys eşlemesi (otomatik filtreleme için).
     */
    public static final Map<String, List<String>> MUDURLUK_BOARDS = Map.ofEntries(
            Map.entry("Rezervasyon & Biletleme Md.",            List.of("IROR", "IRPR", "TKTPR", "TKTOR")),
            Map.entry("Gelir Yönetimi ve Ücret Çöz. Md.",      List.of("Cygnus")),
            Map.entry("Bağlantı ve Uçak İçi Dijital Çözümler Md.", List.of("ONBP", "BUIDCM", "TKP14896", "TKP18320")),
            Map.entry("Ajet Dijital Çöz Md.",                  List.of("AJETPSSD", "AKU"))
    );

    /**
     * FilterRequest'ten geçerli bir Jira JQL sorgusu üretir.
     */
    public String build(FilterRequest filter) {
        StringBuilder jql = new StringBuilder();

        // issuetype in ("Story", "Task") — tırnaklı, boşluk veya özel karakterler için güvenli
        String issueTypes = filter.getIssueTypes().stream()
                .map(it -> "\"" + it.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(", "));
        jql.append("issuetype in (").append(issueTypes).append(")");

        // AND created >= "2025-05-01"
        jql.append(" AND created >= \"")
           .append(formatDate(filter.getCreatedStartDate()))
           .append("\"");

        // AND project in ("FTBASM", "MSS", "MES")
        List<String> projects = MUDURLUK_PROJECTS.getOrDefault(
                filter.getMudurluk(), List.of(filter.getMudurluk()));
        String projectClause = projects.stream()
                .map(p -> "\"" + p + "\"")
                .collect(Collectors.joining(", "));
        jql.append(" AND project in (").append(projectClause).append(")");

        // AND status changed to (Done, Closed) during ("2026-04-01", "2026-04-27")
        // status changed to ("Done", "Closed") — tırnaklı olarak gönder
        String statuses = filter.getStatuses().stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(", "));
        jql.append(" AND status changed to (").append(statuses).append(")")
           .append(" during (\"").append(formatDate(filter.getStatusChangedStartDate())).append("\"")
           .append(", \"").append(formatDate(filter.getStatusChangedEndDate())).append("\")");

        // NOTE: The Jira board field is not reliably supported by standard JQL.

        return jql.toString();
    }

    private String formatDate(LocalDate date) {
        return date.format(JIRA_DATE_FORMATTER);
    }
}
