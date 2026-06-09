package com.thy.audithub.service;

import com.thy.audithub.dto.FilterRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JqlBuilderService {

    /**
     * Müdürlük adı → Jira proje anahtarları eşlemesi.
     * Yeni müdürlükler buraya eklenir.
     */
    public static final Map<String, List<String>> MUDURLUK_PROJECTS = Map.ofEntries(
            Map.entry("Açık Sistem Çöz. Md.",                  List.of("FTBASM", "MSS", "MES")),
            Map.entry("Rezervasyon & Biletleme Md.",            List.of("TRP", "TKT")),
            Map.entry("Gelir Yönetimi ve Ücret Çözümleri Md.", List.of("PRC", "RMOP", "PTS", "UVYFT")),
            Map.entry("Doğrudan Satış Çözümleri Md.",          List.of("NDCIN", "NDCUI", "QR", "KB")),
            Map.entry("Dijital Yolcu Çöz Md.",                 List.of("QCG", "KIOSK", "DYMU", "OTHL", "SBD", "TOUR")),
            Map.entry("DCS Çözümleri Md.",                     List.of("TKP4089", "DCWB", "TDCS")),
            Map.entry("Alışveriş İçerik Md.",                  List.of("DIJITAL")),
            Map.entry("Miles&Smiles Md.",                      List.of("DIJITAL")),
            Map.entry("Biletleme ve Ek Hizmetler Md.",         List.of("DIJITAL")),
            Map.entry("Satış Sonrası ve IRROPS Md.",           List.of("DIJITAL")),
            Map.entry("BAGLANTI&UCAK ICI DIJITAL COZUMLER MD.", List.of("ONBP", "BUIDCM", "TKP14896", "TKP18320")),
            Map.entry("Ajet Dijital Çöz Md.",                  List.of("AJETPSSD", "AKU"))
    );

    /**
     * Müdürlük adı → Board keys eşlemesi (otomatik filtreleme için).
     */
    public static final Map<String, List<String>> MUDURLUK_BOARDS = Map.ofEntries(
            Map.entry("Rezervasyon & Biletleme Md.",            List.of("IROR", "IRPR", "TKTPR", "TKTOR")),
            Map.entry("Gelir Yönetimi ve Ücret Çözümleri Md.", List.of("Cygnus")),
            Map.entry("BAGLANTI&UCAK ICI DIJITAL COZUMLER MD.", List.of("ONBP", "BUIDCM", "TKP14896", "TKP18320")),
            Map.entry("Ajet Dijital Çöz Md.",                  List.of("AJETPSSD", "AKU"))
    );

    /**
     * Müdürlük adı → Jira component filtresi eşlemesi.
     * Sadece component bazlı ayrışan müdürlükler için tanımlanır.
     */
    public static final Map<String, String> MUDURLUK_COMPONENT = Map.of(
            "Alışveriş İçerik Md.",         "Alisveris & Icerik",
            "Miles&Smiles Md.",             "Miles & Smiles",
            "Biletleme ve Ek Hizmetler Md.", "Profil & Bilet",
            "Satış Sonrası ve IRROPS Md.",  "Satis Sonrasi & Irrops"
    );

    /**
     * FilterRequest'ten geçerli bir Jira JQL sorgusu üretir.
     */
    public String build(FilterRequest filter) {
        StringBuilder jql = new StringBuilder();

        // issuetype in (Story, Task)
        String issueTypes = filter.getIssueTypes().stream()
                .collect(Collectors.joining(", "));
        jql.append("issuetype in (").append(issueTypes).append(")");

        // AND created >= "2025-05-01"
        jql.append(" AND created >= \"").append(filter.getCreatedStartDate()).append("\"");

        // AND project in ("FTBASM", "MSS", "MES")
        List<String> projects = MUDURLUK_PROJECTS.getOrDefault(
                filter.getMudurluk(), List.of(filter.getMudurluk()));
        String projectClause = projects.stream()
                .map(p -> "\"" + p + "\"")
                .collect(Collectors.joining(", "));
        jql.append(" AND project in (").append(projectClause).append(")");

        // AND component = "..." (sadece component bazlı müdürlükler için)
        String component = MUDURLUK_COMPONENT.get(filter.getMudurluk());
        if (component != null) {
            jql.append(" AND component = \"").append(component).append("\"");
        }

        // AND status changed to (Done, Closed) during ("2026-04-01", "2026-04-27")
        String statuses = filter.getStatuses().stream()
                .collect(Collectors.joining(", "));
        jql.append(" AND status changed to (").append(statuses).append(")")
           .append(" during (\"").append(filter.getStatusChangedStartDate()).append("\"")
           .append(", \"").append(filter.getStatusChangedEndDate()).append("\")");

        // AND board in (...) — otomatik müdürlüğe göre belirlenir
        List<String> boards = MUDURLUK_BOARDS.getOrDefault(filter.getMudurluk(), List.of());
        if (!boards.isEmpty()) {
            String boardClause = boards.stream()
                    .map(b -> "\"" + b + "\"")
                    .collect(Collectors.joining(", "));
            jql.append(" AND board in (").append(boardClause).append(")");
        }

        return jql.toString();
    }
}
