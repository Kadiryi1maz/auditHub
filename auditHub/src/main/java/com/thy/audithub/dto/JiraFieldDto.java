package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraFieldDto {

    private String summary;
    private JiraNameDto issuetype;
    private JiraNameDto status;
    private JiraUserDto assignee;
    private JiraUserDto reporter;
    private String created;
    private String updated;
    private List<String> labels;
    private List<JiraNameDto> components;
}
