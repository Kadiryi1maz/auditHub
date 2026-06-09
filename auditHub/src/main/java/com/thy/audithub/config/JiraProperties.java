package com.thy.audithub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

    @NotBlank(message = "Jira base URL tanımlı olmalıdır.")
    private String baseUrl;

    private String personalAccessToken;

    private String apiSearchPath = "/rest/api/2/search";

    private int maxResults = 100;

    public boolean isTokenConfigured() {
        return personalAccessToken != null && !personalAccessToken.isBlank();
    }
}
