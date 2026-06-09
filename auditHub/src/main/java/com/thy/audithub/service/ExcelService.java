package com.thy.audithub.service;

import com.thy.audithub.config.JiraProperties;
import com.thy.audithub.dto.IssueRowDto;
import com.thy.audithub.dto.JiraFieldDto;
import com.thy.audithub.dto.JiraIssueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final String SHEET_NAME = "Jira Export";
    private static final int YELLOW_COLUMN_START = 11;

    private static final String[] HEADERS = {
            "Issue Type",                             // 0
            "Key",                                    // 1
            "Summary",                                // 2
            "Status",                                 // 3
            "Labels",                                 // 4
            "Back To Analysis Count for Test",        // 5
            "Back To Analysis Count for Development", // 6
            "Back To Analysis Root Cause",            // 7
            "Tester",                                 // 8
            "Assignee",                               // 9
            "Created",                                // 10
            "\u00DCr\u00FCn",                         // 11 - Ürün
            "Issue Type Uygunluk Durumu",             // 12
            "Analiz Format\u0131na Uygunluk Durumu",  // 13
            "Analiz \u0130\u00E7eri\u011Fi Olgunluk Durumu",     // 14
            "Analiz neden uygun de\u011Fil?"          // 15
    };

    private final JiraProperties jiraProperties;

    /**
     * JiraIssueDto listesini IssueRowDto listesine dönüştürür (preview için).
     */
    public List<IssueRowDto> convertToRows(List<JiraIssueDto> issues) {
        return issues.stream().map(issue -> {
            JiraFieldDto fields = issue.getFields();
            IssueRowDto row = new IssueRowDto();
            row.setIssueType(safe(fields != null && fields.getIssuetype() != null
                    ? fields.getIssuetype().getName() : null));
            row.setKey(safe(issue.getKey()));
            row.setSummary(safe(fields != null ? fields.getSummary() : null));
            row.setStatus(safe(fields != null && fields.getStatus() != null
                    ? fields.getStatus().getName() : null));
            String labels = "";
            if (fields != null && fields.getLabels() != null && !fields.getLabels().isEmpty()) {
                labels = String.join(", ", fields.getLabels());
            }
            row.setLabels(labels);
            String btaTest = getBackToAnalysisWithDescription(issue, true);
            row.setBtaTest(btaTest);
            row.setBtaDev("");
            row.setBtaRootCause(!btaTest.isEmpty() ? "1" : "");
            row.setTester("");
            row.setAssignee(safe(fields != null && fields.getAssignee() != null
                    ? fields.getAssignee().getDisplayName() : null));
            row.setCreated(formatJiraDate(fields != null ? fields.getCreated() : null));
            // Bileşen: ilk component adını al
            String component = "";
            if (fields != null && fields.getComponents() != null && !fields.getComponents().isEmpty()) {
                component = fields.getComponents().stream()
                        .map(c -> c.getName() != null ? c.getName() : "")
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            row.setComponent(component);
            return row;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * IssueRowDto listesinden (kullanıcının sarı kolon değerleriyle) Excel üretir.
     */
    public byte[] generateExcelFromRows(List<IssueRowDto> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle       = createHeaderStyle(workbook);
            CellStyle yellowHeaderStyle = createYellowHeaderStyle(workbook);
            CellStyle dataStyle         = createDataStyle(workbook);
            CellStyle yellowDataStyle   = createYellowDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(i >= YELLOW_COLUMN_START ? yellowHeaderStyle : headerStyle);
            }

            int rowIndex = 1;
            for (IssueRowDto row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                fillRowFromDto(excelRow, row, dataStyle, yellowDataStyle, workbook);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, (int) (sheet.getColumnWidth(i) * 1.15));
            }

            workbook.write(out);
            log.info("Excel (rows) olusturuldu. Satir sayisi: {}", rows.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel olusturulurken hata: {}", e.getMessage());
            throw new RuntimeException("Excel dosyasi olusturulamadi.", e);
        }
    }

    private void fillRowFromDto(Row row, IssueRowDto dto, CellStyle dataStyle,
                                CellStyle yellowStyle, XSSFWorkbook workbook) {
        createCell(row, 0, dto.getIssueType(), dataStyle);

        // Key + hyperlink
        Cell keyCell = row.createCell(1);
        keyCell.setCellValue(dto.getKey());
        if (dto.getKey() != null && !dto.getKey().isEmpty()) {
            String url = jiraProperties.getBaseUrl() + "/browse/" + dto.getKey();
            CreationHelper ch = workbook.getCreationHelper();
            Hyperlink link = ch.createHyperlink(HyperlinkType.URL);
            link.setAddress(url);
            keyCell.setHyperlink(link);
            CellStyle linkStyle = workbook.createCellStyle();
            linkStyle.cloneStyleFrom(dataStyle);
            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkFont.setFontHeightInPoints((short) 10);
            linkStyle.setFont(linkFont);
            keyCell.setCellStyle(linkStyle);
        } else {
            keyCell.setCellStyle(dataStyle);
        }

        createCell(row, 2,  dto.getSummary(),    dataStyle);
        createCell(row, 3,  dto.getStatus(),     dataStyle);
        createCell(row, 4,  dto.getLabels(),     dataStyle);
        createCell(row, 5,  dto.getBtaTest(),    dataStyle);
        createCell(row, 6,  dto.getBtaDev(),     dataStyle);
        createCell(row, 7,  dto.getBtaRootCause(), dataStyle);
        createCell(row, 8,  dto.getTester(),     dataStyle);
        createCell(row, 9,  dto.getAssignee(),   dataStyle);
        createCell(row, 10, dto.getCreated(),    dataStyle);
        createCell(row, 11, dto.getUrun(),               yellowStyle);
        createCell(row, 12, dto.getIssueTypeUygunluk(),  yellowStyle);
        createCell(row, 13, dto.getAnalizFormatUygunluk(), yellowStyle);
        createCell(row, 14, dto.getAnalizIcerikOlgunluk(), yellowStyle);
        createCell(row, 15, dto.getAnalizNedenUygunDegil(), yellowStyle);
    }

    public byte[] generateExcel(List<JiraIssueDto> issues) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle       = createHeaderStyle(workbook);
            CellStyle yellowHeaderStyle = createYellowHeaderStyle(workbook);
            CellStyle dataStyle         = createDataStyle(workbook);
            CellStyle yellowDataStyle   = createYellowDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(i >= YELLOW_COLUMN_START ? yellowHeaderStyle : headerStyle);
            }

            int rowIndex = 1;
            for (JiraIssueDto issue : issues) {
                Row row = sheet.createRow(rowIndex++);
                fillRow(row, issue, dataStyle, yellowDataStyle);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, (int) (sheet.getColumnWidth(i) * 1.15));
            }

            workbook.write(out);
            log.info("Excel olusturuldu. Satir sayisi: {}", issues.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel olusturulurken hata: {}", e.getMessage());
            throw new RuntimeException("Excel dosyasi olusturulamadi.", e);
        }
    }

    private void fillRow(Row row, JiraIssueDto issue, CellStyle dataStyle, CellStyle yellowStyle) {
        JiraFieldDto fields = issue.getFields();

        // 0: Issue Type
        createCell(row, 0, safe(fields != null && fields.getIssuetype() != null
                ? fields.getIssuetype().getName() : null), dataStyle);
        // 1: Key - hyperlink
        String key = safe(issue.getKey());
        Cell keyCell = row.createCell(1);
        keyCell.setCellValue(key);
        if (!key.isEmpty()) {
            String url = jiraProperties.getBaseUrl() + "/browse/" + key;
            CreationHelper ch = row.getSheet().getWorkbook().getCreationHelper();
            Hyperlink link = ch.createHyperlink(HyperlinkType.URL);
            link.setAddress(url);
            keyCell.setHyperlink(link);
            CellStyle linkStyle = row.getSheet().getWorkbook().createCellStyle();
            linkStyle.cloneStyleFrom(dataStyle);
            Font linkFont = row.getSheet().getWorkbook().createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkFont.setFontHeightInPoints((short) 10);
            linkStyle.setFont(linkFont);
            keyCell.setCellStyle(linkStyle);
        } else {
            keyCell.setCellStyle(dataStyle);
        }
        // 2: Summary
        createCell(row, 2, safe(fields != null ? fields.getSummary() : null), dataStyle);
        // 3: Status
        createCell(row, 3, safe(fields != null && fields.getStatus() != null
                ? fields.getStatus().getName() : null), dataStyle);
        // 4: Labels
        String labels = "";
        if (fields != null && fields.getLabels() != null && !fields.getLabels().isEmpty()) {
            labels = String.join(", ", fields.getLabels());
        }
        createCell(row, 4, labels, dataStyle);

        // 5: Back To Analysis Count for Test + Description
        String btaTest = getBackToAnalysisWithDescription(issue, true);
        createCell(row, 5, btaTest, dataStyle);

        // 6: Back To Analysis Count for Development
        createCell(row, 6, "", dataStyle);

        // 7: Back To Analysis Root Cause - BTA varsa "1" yaz
        boolean hasBta = !btaTest.isEmpty();
        createCell(row, 7, hasBta ? "1" : "", dataStyle);

        // 8: Tester
        createCell(row, 8, "", dataStyle);
        // 9: Assignee
        createCell(row, 9, safe(fields != null && fields.getAssignee() != null
                ? fields.getAssignee().getDisplayName() : null), dataStyle);
        // 10: Created
        createCell(row, 10, formatJiraDate(fields != null ? fields.getCreated() : null), dataStyle);

        // 11-15: Sari manuel kolonlar
        for (int i = YELLOW_COLUMN_START; i < HEADERS.length; i++) {
            createCell(row, i, "", yellowStyle);
        }
    }

    /**
     * BTA Count for Test: BTA varsa "BTA Yapilmistir\nAciklama: ..." formatinda dondurur.
     */
    private String getBackToAnalysisWithDescription(JiraIssueDto issue, boolean fromTest) {
        if (issue.getChangelog() == null || issue.getChangelog().getHistories() == null) {
            return "";
        }

        long count = issue.getChangelog().getHistories().stream()
                .filter(h -> h.getItems() != null)
                .flatMap(h -> h.getItems().stream())
                .filter(item -> "status".equalsIgnoreCase(item.getField()))
                .filter(item -> {
                    String to   = item.getToStringValue() != null ? item.getToStringValue().toLowerCase().trim() : "";
                    String from = item.getFromString()    != null ? item.getFromString().toLowerCase().trim()    : "";
                    boolean isBta = to.contains("back to analysis") || to.contains("analysis")
                            || from.contains("back to analysis");
                    if (!isBta) return false;
                    if (fromTest) {
                        return from.contains("test") || from.contains("qa");
                    } else {
                        return !from.contains("test") && !from.contains("qa");
                    }
                })
                .count();

        if (count == 0) return "";

        return getChangelogFieldValue(issue, "Back to Analysis Description");
    }

    private String getBackToAnalysis(JiraIssueDto issue, boolean fromTest) {
        if (issue.getChangelog() == null || issue.getChangelog().getHistories() == null) {
            return "";
        }

        long count = issue.getChangelog().getHistories().stream()
                .filter(h -> h.getItems() != null)
                .flatMap(h -> h.getItems().stream())
                .filter(item -> "status".equalsIgnoreCase(item.getField()))
                .filter(item -> {
                    String to   = item.getToStringValue() != null ? item.getToStringValue().toLowerCase().trim() : "";
                    String from = item.getFromString()    != null ? item.getFromString().toLowerCase().trim()    : "";
                    boolean isBta = to.contains("back to analysis") || to.contains("analysis")
                            || from.contains("back to analysis");
                    if (!isBta) return false;
                    if (fromTest) {
                        return from.contains("test") || from.contains("qa");
                    } else {
                        return !from.contains("test") && !from.contains("qa");
                    }
                })
                .count();

        return count > 0 ? "BTA Yapilmistir" : "";
    }

    private String getChangelogFieldValue(JiraIssueDto issue, String fieldName) {
        if (issue.getChangelog() == null || issue.getChangelog().getHistories() == null) {
            return "";
        }
        return issue.getChangelog().getHistories().stream()
                .filter(h -> h.getItems() != null)
                .flatMap(h -> h.getItems().stream())
                .filter(item -> fieldName.equalsIgnoreCase(item.getField()))
                .map(item -> item.getToStringValue() != null ? item.getToStringValue() : "")
                .filter(v -> !v.isBlank())
                .reduce((first, second) -> second)
                .orElse("");
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Dışarıdan yüklenen Excel dosyasındaki satırları okur.
     * Beklenen sütun düzeni (bu tool'un export ettiği format):
     *   0:IssueType 1:Key 2:Summary 3:Status 4:Labels
     *   5:BTA Test  6:BTA Dev  7:BTA Root  8:Tester  9:Assignee  10:Created
     *   11:Ürün  12:IssueTypeUygunluk  13:AnalizFormat  14:AnalizIcerik  15:AnalizNeden
     */
    public List<IssueRowDto> parseRowsFromExcel(InputStream is) throws IOException {
        List<IssueRowDto> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) { // 0. satır header
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String key = cellStr(row, 1);
                if (key == null || key.isBlank()) continue; // boş satır atla

                IssueRowDto dto = new IssueRowDto();
                dto.setIssueType(cellStr(row, 0));
                dto.setKey(key);
                dto.setSummary(cellStr(row, 2));
                dto.setStatus(cellStr(row, 3));
                dto.setLabels(cellStr(row, 4));
                dto.setBtaTest(cellStr(row, 5));
                dto.setBtaDev(cellStr(row, 6));
                dto.setBtaRootCause(cellStr(row, 7));
                dto.setTester(cellStr(row, 8));
                dto.setAssignee(cellStr(row, 9));
                dto.setCreated(cellStr(row, 10));
                dto.setUrun(cellStr(row, 11));
                dto.setIssueTypeUygunluk(cellStr(row, 12));
                dto.setAnalizFormatUygunluk(cellStr(row, 13));
                dto.setAnalizIcerikOlgunluk(cellStr(row, 14));
                dto.setAnalizNedenUygunDegil(cellStr(row, 15));
                rows.add(dto);
            }
        }
        log.info("Excel parse edildi: {} satir", rows.size());
        return rows;
    }

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) { yield String.valueOf((long) cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private String formatJiraDate(String jiraDate) {
        if (jiraDate == null || jiraDate.isBlank()) return "";
        try {
            return jiraDate.substring(0, 16).replace("T", " ");
        } catch (Exception e) {
            return jiraDate;
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        applyBorders(style);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createYellowHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        applyBorders(style);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        applyBorders(style);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createYellowDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        applyBorders(style);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}