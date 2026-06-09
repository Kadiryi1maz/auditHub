package com.thy.audithub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.audithub.dto.DashboardEntry;
import com.thy.audithub.dto.IssueRowDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ObjectMapper objectMapper;

    // key = mudurluk + "|" + yearMonth  (örn. "Açık Sistem Çöz. Md.|2026-04")
    private final Map<String, DashboardEntry> store = new ConcurrentHashMap<>();

    private static final String DATA_FILE = "data/dashboard.json";

    @PostConstruct
    public void load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try {
            List<DashboardEntry> entries = objectMapper.readValue(
                    file, new TypeReference<List<DashboardEntry>>() {});
            entries.forEach(e -> store.put(key(e.getMudurluk(), e.getYearMonth()), e));
            log.info("Dashboard: {} kayıt yüklendi", entries.size());
        } catch (IOException e) {
            log.warn("Dashboard verisi yüklenemedi: {}", e.getMessage());
        }
    }

    public void saveOrUpdate(String mudurluk, String yearMonth, List<IssueRowDto> rows) {
        DashboardEntry entry = new DashboardEntry();
        entry.setMudurluk(mudurluk);
        entry.setYearMonth(yearMonth);
        entry.setTotalTasks(rows.size());

        int fU = 0, fUD = 0, fH = 0;
        int iU = 0, iUD = 0, iH = 0;
        int bta = 0;

        for (IssueRowDto row : rows) {
            switch (safe(row.getAnalizFormatUygunluk())) {
                case "Uygun"        -> fU++;
                case "Uygun Değil"  -> fUD++;
                case "Hesaplanamaz" -> fH++;
                default -> { /* boş bırakıldı */ }
            }
            switch (safe(row.getAnalizIcerikOlgunluk())) {
                case "Uygun"        -> iU++;
                case "Uygun Değil"  -> iUD++;
                case "Hesaplanamaz" -> iH++;
                default -> { /* boş bırakıldı */ }
            }
            bta += toInt(row.getBtaTest()) + toInt(row.getBtaDev());
        }

        entry.setFormatUygun(fU);
        entry.setFormatUygunDegil(fUD);
        entry.setFormatHesap(fH);
        entry.setIcerikUygun(iU);
        entry.setIcerikUygunDegil(iUD);
        entry.setIcerikHesap(iH);
        entry.setBtaCount(bta);
        entry.setLastUpdated(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        store.put(key(mudurluk, yearMonth), entry);
        persist();
        log.info("Dashboard güncellendi: {} / {}", mudurluk, yearMonth);
    }

    public List<DashboardEntry> getAll() {
        return new ArrayList<>(store.values());
    }

    public void updateCell(String mudurluk, String yearMonth, String field, String value) {
        DashboardEntry entry = store.getOrDefault(key(mudurluk, yearMonth), new DashboardEntry());
        entry.setMudurluk(mudurluk);
        entry.setYearMonth(yearMonth);

        try {
            int intValue = Integer.parseInt(value);
            switch (field) {
                case "totalTasks" -> entry.setTotalTasks(intValue);
                case "formatUygun" -> entry.setFormatUygun(intValue);
                case "formatUygunDegil" -> entry.setFormatUygunDegil(intValue);
                case "formatHesap" -> entry.setFormatHesap(intValue);
                case "icerikUygun" -> entry.setIcerikUygun(intValue);
                case "icerikUygunDegil" -> entry.setIcerikUygunDegil(intValue);
                case "icerikHesap" -> entry.setIcerikHesap(intValue);
                case "btaCount" -> entry.setBtaCount(intValue);
                default -> throw new IllegalArgumentException("Bilinmeyen alan: " + field);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Sayı dönüşümü başarısız: " + value);
        }

        entry.setLastUpdated(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        store.put(key(mudurluk, yearMonth), entry);
        persist();
        log.info("Dashboard hücresi güncellendi: {}/{} - {} = {}", mudurluk, yearMonth, field, value);
    }

    // -----------------------------------------------------------------------

    private String key(String m, String ym) {
        return m + "|" + ym;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s == null ? "0" : s.trim()); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void persist() {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DATA_FILE), new ArrayList<>(store.values()));
        } catch (IOException e) {
            log.error("Dashboard kaydedilemedi: {}", e.getMessage());
        }
    }
}
