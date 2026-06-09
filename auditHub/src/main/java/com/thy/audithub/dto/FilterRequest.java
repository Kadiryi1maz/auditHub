package com.thy.audithub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class FilterRequest {

    @NotBlank(message = "Müdürlük seçimi zorunludur.")
    private String mudurluk;

    @NotEmpty(message = "En az bir issue type seçilmelidir.")
    private List<String> issueTypes;

    @NotNull(message = "Oluşturulma başlangıç tarihi boş olamaz.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdStartDate;

    @NotEmpty(message = "En az bir status seçilmelidir.")
    private List<String> statuses;

    @NotNull(message = "Durum değişim başlangıç tarihi boş olamaz.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate statusChangedStartDate;

    @NotNull(message = "Durum değişim bitiş tarihi boş olamaz.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate statusChangedEndDate;

    public boolean isDateRangeValid() {
        if (statusChangedStartDate == null || statusChangedEndDate == null) {
            return true;
        }
        return !statusChangedEndDate.isBefore(statusChangedStartDate);
    }

    public boolean hasCustomJql() {
        if (customJql == null || customJql.isBlank()) return false;
        if (customJql.contains("Tüm alanları doldurunca")) return false;
        return true;
    }

    private String customJql;
}
