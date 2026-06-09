package com.thy.audithub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUserDto {

    private String displayName;
    private String emailAddress;
    private String name;
}
