package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraChangelogItemDto {

    private String field;

    private String fromString;

    // Jira API "toString" adlı alanı - Java'da toString() rezerve olduğu için alias kullanıyoruz
    @JsonProperty("toString")
    private String toStringValue;
}
