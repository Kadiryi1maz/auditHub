package com.thy.audithub.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExportRequest {
    private List<IssueRowDto> rows = new ArrayList<>();
    private String mudurluk  = "";
    private String yearMonth = ""; // "2026-06"
    private boolean autoExportToDashboard = false;
}
