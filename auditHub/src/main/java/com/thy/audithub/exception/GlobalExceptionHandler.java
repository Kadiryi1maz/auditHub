package com.thy.audithub.exception;

import com.thy.audithub.dto.FilterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Token tanımlı değil.
     */
    @ExceptionHandler(JiraTokenMissingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleTokenMissing(JiraTokenMissingException ex, Model model) {
        log.warn("Token eksik: {}", ex.getMessage());
        return errorPage(model,
                "Jira Personal Access Token bulunamadı. " +
                "Lütfen JIRA_PAT environment variable değerini tanımlayın.");
    }

    /**
     * Jira 401 — token geçersiz veya süresi dolmuş.
     */
    @ExceptionHandler(JiraAuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleAuth(JiraAuthException ex, Model model) {
        log.warn("Jira auth hatası: {}", ex.getMessage());
        return errorPage(model,
                "Jira authentication başarısız. " +
                "Token geçersiz veya süresi dolmuş olabilir.");
    }

    /**
     * Jira 403 — yetersiz yetki.
     */
    @ExceptionHandler(JiraForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(JiraForbiddenException ex, Model model) {
        log.warn("Jira erişim hatası: {}", ex.getMessage());
        return errorPage(model,
                "Bu Jira verilerine erişim yetkiniz olmayabilir.");
    }

    /**
     * Jira 400 — geçersiz JQL.
     */
    @ExceptionHandler(JiraInvalidQueryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidQuery(JiraInvalidQueryException ex, Model model) {
        log.warn("Geçersiz JQL: {}", ex.getMessage());
        return errorPage(model,
                "JQL sorgusu geçersiz. Lütfen filtreleri kontrol edin.");
    }

    /**
     * Jira bağlantı hatası — ağ, VPN veya sunucu erişilemiyor.
     */
    @ExceptionHandler(JiraConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleConnection(JiraConnectionException ex, Model model) {
        log.error("Jira bağlantı hatası: {}", ex.getMessage());
        return errorPage(model,
                "Jira'ya bağlanırken hata oluştu. " +
                "VPN, ağ bağlantısı veya Jira erişimini kontrol edin.");
    }

    /**
     * Beklenmeyen tüm diğer hatalar.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        // Stack trace loglanır ama kullanıcıya iç detay gösterilmez
        log.error("Beklenmeyen hata", ex);
        return errorPage(model,
                "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.");
    }

    // -----------------------------------------------------------------------

    private String errorPage(Model model, String message) {
        model.addAttribute("filterRequest", defaultFilter());
        model.addAttribute("errorMessage", message);
        model.addAttribute("jqlPreview", "");
        return "index";
    }

    private FilterRequest defaultFilter() {
        FilterRequest f = new FilterRequest();
        f.setMudurluk("Açık Sistem Çöz. Md.");
        f.setIssueTypes(java.util.List.of("Story", "Task"));
        f.setStatuses(java.util.List.of("Done", "Closed"));
        f.setCreatedStartDate(java.time.LocalDate.of(2025, 5, 1));
        f.setStatusChangedStartDate(java.time.LocalDate.now().withDayOfMonth(1));
        f.setStatusChangedEndDate(java.time.LocalDate.now().withDayOfMonth(java.time.LocalDate.now().lengthOfMonth()));
        return f;
    }
}
