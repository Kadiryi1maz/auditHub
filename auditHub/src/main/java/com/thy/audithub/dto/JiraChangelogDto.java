package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraChangelogDto {

    private List<JiraChangelogHistoryDto> histories;
}
