package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import com.thy.audithub.dto.JiraChangelogDto;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueDto {

    private String id;
    private String key;
    private JiraFieldDto fields;
    private JiraChangelogDto changelog;
}
