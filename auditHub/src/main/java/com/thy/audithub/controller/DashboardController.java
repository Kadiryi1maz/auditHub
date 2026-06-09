package com.thy.audithub.controller;

import com.thy.audithub.dto.DashboardEntry;
import com.thy.audithub.dto.IssueRowDto;
import com.thy.audithub.dto.MudurlukStats;
import com.thy.audithub.service.DashboardService;
import com.thy.audithub.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ExcelService excelService;

    private static final List<String> MONTHS = List.of(
            "2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06",
            "2026-07", "2026-08", "2026-09", "2026-10", "2026-11", "2026-12");

    private static final List<String> MONTH_LABELS = List.of(
            "Oca", "Şub", "Mar", "Nis", "May", "Haz",
            "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara");

    private static final List<String> MUDURLUKLER = List.of(
            "Açık Sistem Çöz. Md.",
            "Rezervasyon & Biletleme Md.",
            "Gelir Yönetimi ve Ücret Çözümleri Md.",
            "Doğrudan Satış Çözümleri Md.",
            "Dijital Yolcu Çöz Md.",
            "DCS Çözümleri Md.",
            "Alışveriş İçerik Md.",
            "Miles&Smiles Md.",
            "Biletleme ve Ek Hizmetler Md.",
            "Satış Sonrası ve IRROPS Md.");

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String msg,
                            @RequestParam(required = false) String err,
                            Model model) {
        List<DashboardEntry> allEntries = dashboardService.getAll();

        // müdürlük → yearMonth → entry
        Map<String, Map<String, DashboardEntry>> lookup = new HashMap<>();
        for (DashboardEntry e : allEntries) {
            lookup.computeIfAbsent(e.getMudurluk(), k -> new HashMap<>())
                  .put(e.getYearMonth(), e);
        }

        List<MudurlukStats> stats = new ArrayList<>();
        for (String m : MUDURLUKLER) {
            MudurlukStats ms = new MudurlukStats();
            ms.setName(m);

            Map<String, DashboardEntry> monthMap = lookup.getOrDefault(m, Map.of());

            List<DashboardEntry> monthEntries = new ArrayList<>();
            for (String month : MONTHS) {
                monthEntries.add(monthMap.get(month)); // null if no data
            }
            ms.setMonthEntries(monthEntries);

            List<DashboardEntry> withData = monthEntries.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            DashboardEntry avg = withData.isEmpty() ? null : computeAverage(withData);
            ms.setAverage(avg);

            // Yüzdelik hesabı
            if (avg != null && avg.getTotalTasks() > 0) {
                ms.setFormatUygunPercent((avg.getFormatUygun() * 100.0) / avg.getTotalTasks());
                ms.setIcerikUygunPercent((avg.getIcerikUygun() * 100.0) / avg.getTotalTasks());
            } else {
                ms.setFormatUygunPercent(0);
                ms.setIcerikUygunPercent(0);
            }

            stats.add(ms);
        }

        model.addAttribute("stats", stats);
        model.addAttribute("monthLabels", MONTH_LABELS);
        model.addAttribute("mudurlukler", MUDURLUKLER);
        if (msg != null) model.addAttribute("importMsg", msg);
        if (err != null) model.addAttribute("importErr", err);
        return "dashboard";
    }

    @PostMapping("/dashboard/save-cell")
    @ResponseBody
    public Map<String, Object> saveCell(@RequestParam String mudurluk,
                                        @RequestParam String yearMonth,
                                        @RequestParam String field,
                                        @RequestParam String value) {
        try {
            dashboardService.updateCell(mudurluk, yearMonth, field, value);
            return Map.of("success", true, "message", "Hücre başarıyla kaydedildi.");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Hata: " + e.getMessage());
        }
    }

    @PostMapping("/import-dashboard")
    public String importExcel(@RequestParam("file") MultipartFile file,
                              @RequestParam("mudurluk") String mudurluk,
                              @RequestParam("yearMonth") String yearMonth,
                              RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addAttribute("err", "Dosya seçilmedi.");
            return "redirect:/dashboard";
        }
        try {
            List<IssueRowDto> rows = excelService.parseRowsFromExcel(file.getInputStream());
            dashboardService.saveOrUpdate(mudurluk, yearMonth, rows);
            ra.addAttribute("msg", mudurluk + " / " + yearMonth
                    + " için " + rows.size() + " satır başarıyla içe aktarıldı.");
        } catch (Exception e) {
            ra.addAttribute("err", "Hata: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // -----------------------------------------------------------------------

    private DashboardEntry computeAverage(List<DashboardEntry> entries) {
        int n = entries.size();
        DashboardEntry avg = new DashboardEntry();
        avg.setYearMonth("avg");
        avg.setTotalTasks(round(entries.stream().mapToInt(DashboardEntry::getTotalTasks).sum(), n));
        avg.setFormatUygun(round(entries.stream().mapToInt(DashboardEntry::getFormatUygun).sum(), n));
        avg.setFormatUygunDegil(round(entries.stream().mapToInt(DashboardEntry::getFormatUygunDegil).sum(), n));
        avg.setFormatHesap(round(entries.stream().mapToInt(DashboardEntry::getFormatHesap).sum(), n));
        avg.setIcerikUygun(round(entries.stream().mapToInt(DashboardEntry::getIcerikUygun).sum(), n));
        avg.setIcerikUygunDegil(round(entries.stream().mapToInt(DashboardEntry::getIcerikUygunDegil).sum(), n));
        avg.setIcerikHesap(round(entries.stream().mapToInt(DashboardEntry::getIcerikHesap).sum(), n));
        avg.setBtaCount(round(entries.stream().mapToInt(DashboardEntry::getBtaCount).sum(), n));
        return avg;
    }

    private int round(int sum, int n) {
        if (n == 0) return 0;
        return (int) Math.round((double) sum / n);
    }
}
