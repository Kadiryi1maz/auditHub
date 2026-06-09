package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchResponse {

    private Integer startAt;
    private Integer maxResults;
    private Integer total;
    private List<JiraIssueDto> issues;
}
