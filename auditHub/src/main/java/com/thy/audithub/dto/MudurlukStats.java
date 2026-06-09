package com.thy.audithub.dto;

import lombok.Data;
import java.util.List;

@Data
public class MudurlukStats {
    private String              name;
    private List<DashboardEntry> monthEntries; // 12 eleman, veri yoksa null
    private DashboardEntry      average;       // hesaplanmış ortalama, hiç veri yoksa null
    private double              formatUygunPercent;    // ortalamada Format Uygunluk yüzdesi
    private double              icerikUygunPercent;    // ortalamada İçerik Olgunluk yüzdesi
}
