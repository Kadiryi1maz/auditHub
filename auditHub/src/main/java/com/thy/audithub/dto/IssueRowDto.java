package com.thy.audithub.dto;

import lombok.Data;

@Data
public class IssueRowDto {

    // Jira'dan gelen veriler (hidden inputs olarak taşınır)
    private String issueType           = "";
    private String key                 = "";
    private String summary             = "";
    private String status              = "";
    private String labels              = "";
    private String btaTest             = "";
    private String btaDev              = "";
    private String btaRootCause        = "";
    private String tester              = "";
    private String assignee            = "";
    private String created             = "";
    private String component           = "";

    // Sarı kolonlar — kullanıcı tarafından doldurulur
    private String urun                    = "";
    private String issueTypeUygunluk       = "";
    private String analizFormatUygunluk    = "";
    private String analizIcerikOlgunluk    = "";
    private String analizNedenUygunDegil   = "";
}
