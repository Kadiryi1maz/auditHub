package com.thy.audithub.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DashboardEntry {
    private String mudurluk          = "";
    private String yearMonth         = ""; // "2026-01"
    private int    totalTasks        = 0;
    private int    formatUygun       = 0;
    private int    formatUygunDegil  = 0;
    private int    formatHesap       = 0;
    private int    icerikUygun       = 0;
    private int    icerikUygunDegil  = 0;
    private int    icerikHesap       = 0;
    private int    btaCount          = 0;
    private String lastUpdated       = "";
}
