package com.thy.audithub.service;

import com.thy.audithub.config.JiraProperties;
import com.thy.audithub.dto.JiraIssueDto;
import com.thy.audithub.dto.JiraSearchResponse;
import com.thy.audithub.exception.JiraAuthException;
import com.thy.audithub.exception.JiraConnectionException;
import com.thy.audithub.exception.JiraForbiddenException;
import com.thy.audithub.exception.JiraInvalidQueryException;
import com.thy.audithub.exception.JiraTokenMissingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JiraService {

    private static final String FIELDS =
            "summary,issuetype,status,assignee,reporter,created,updated,labels,components";

    private final JiraProperties jiraProperties;
    private final RestClient restClient;

    public JiraService(JiraProperties jiraProperties) {
        this.jiraProperties = jiraProperties;
        this.restClient = RestClient.builder()
                .baseUrl(jiraProperties.getBaseUrl())
                .build();
    }

    /**
     * Verilen JQL ile tüm issue'ları pagination yaparak çeker.
     */
    public List<JiraIssueDto> fetchIssues(String jql) {
        if (!jiraProperties.isTokenConfigured()) {
            throw new JiraTokenMissingException(
                    "Jira Personal Access Token bulunamadı. " +
                    "Lütfen JIRA_PAT environment variable değerini tanımlayın.");
        }

        List<JiraIssueDto> allIssues = new ArrayList<>();
        int startAt = 0;
        int maxResults = jiraProperties.getMaxResults();

        log.info("Jira sorgusu başlatılıyor. JQL: {}", jql);

        do {
            JiraSearchResponse response = callJira(jql, startAt, maxResults);

            if (response.getIssues() != null) {
                allIssues.addAll(response.getIssues());
            }

            int total = response.getTotal() != null ? response.getTotal() : 0;
            log.info("Çekilen: {}/{}", allIssues.size(), total);

            startAt += maxResults;

            if (response.getTotal() == null || startAt >= response.getTotal()) {
                break;
            }

        } while (true);

        log.info("Toplam {} issue çekildi.", allIssues.size());
        return allIssues;
    }

    private JiraSearchResponse callJira(String jql, int startAt, int maxResults) {
        String uri = UriComponentsBuilder.fromPath(jiraProperties.getApiSearchPath())
                .queryParam("jql", jql)
                .queryParam("startAt", startAt)
                .queryParam("maxResults", maxResults)
                .queryParam("fields", FIELDS)
                .queryParam("expand", "changelog")
                .build()
                .toUriString();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + jiraProperties.getPersonalAccessToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.UNAUTHORIZED,
                            (req, res) -> {
                                throw new JiraAuthException(
                                        "Jira authentication başarısız. " +
                                        "Token geçersiz veya süresi dolmuş olabilir.");
                            })
                    .onStatus(status -> status == HttpStatus.FORBIDDEN,
                            (req, res) -> {
                                throw new JiraForbiddenException(
                                        "Bu Jira verilerine erişim yetkiniz olmayabilir.");
                            })
                    .onStatus(status -> status == HttpStatus.BAD_REQUEST,
                            (req, res) -> {
                                String body = new String(res.getBody().readAllBytes(),
                                        java.nio.charset.StandardCharsets.UTF_8);
                                log.error("Jira 400 yanıtı — URI: {} — Body: {}", uri, body);
                                throw new JiraInvalidQueryException(
                                        "JQL sorgusu geçersiz. Jira yanıtı: " + body);
                            })
                    .body(JiraSearchResponse.class);

        } catch (JiraAuthException | JiraForbiddenException | JiraInvalidQueryException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Jira HTTP hatası: {}", e.getStatusCode());
            throw new JiraConnectionException(
                    "Jira'ya bağlanırken hata oluştu. " +
                    "VPN, ağ bağlantısı veya Jira erişimini kontrol edin.");
        } catch (ResourceAccessException e) {
            log.error("Jira bağlantı hatası: {}", e.getMessage());
            throw new JiraConnectionException(
                    "Jira'ya bağlanırken hata oluştu. " +
                    "VPN, ağ bağlantısı veya Jira erişimini kontrol edin.");
        }
    }
}
