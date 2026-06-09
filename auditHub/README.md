# auditHub — Jira Excel Export Tool

Jira'dan issue'ları JQL filtresiyle çekip `.xlsx` dosyası olarak indiren kurumsal web uygulaması.

---

## Ne İşe Yarar?

Manuel JQL çalıştırıp sonuçları Excel'e kopyalama işlemini otomatikleştirir.

Kullanıcı tarayıcıda formu doldurur → uygulama JQL üretir → Jira REST API'den issue'ları çeker → Excel dosyasını indirir.

---

## Teknoloji

| | |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.x |
| Thymeleaf | UI şablonu |
| Apache POI | Excel üretimi |
| Lombok | Boilerplate azaltma |
| Maven | Build |

---

## Kurulum

### Gereksinimler

- Java 17+
- Maven 3.8+
- Jira'ya ağ erişimi (VPN gerekebilir)
- Jira Personal Access Token

### Token Oluşturma

1. Jira'ya giriş yap.
2. Sağ üst profil menüsü → **Profile** → **Personal Access Tokens**.
3. **Create token** → isim ver → oluştur.
4. Token'ı kopyala, bir daha gösterilmez.

> **Güvenlik uyarısı:** Token'ı asla kaynak koda veya Git'e ekleme.

---

## Çalıştırma

### 1. Environment Variable'ları Set Et

**Windows PowerShell:**
```powershell
$env:JIRA_BASE_URL = "https://jira.thy.com"
$env:JIRA_PAT      = "buraya-token-yapistir"
```

**macOS / Linux:**
```bash
export JIRA_BASE_URL="https://jira.thy.com"
export JIRA_PAT="buraya-token-yapistir"
```

### 2. Projeyi Derle ve Çalıştır

```bash
cd auditHub
mvn clean spring-boot:run
```

### 3. Tarayıcıda Aç

```
http://localhost:8080
```

---

## Kullanım

1. **Project** alanına Jira proje adını gir.
2. **Issue Types** kutucuklarından istediğin tipleri seç (Story, Task, Bug, Sub-task).
3. **Created Start Date** ile issue oluşturma başlangıç tarihini seç.
4. **Statuses** kutucuklarından hedef durumları seç (Done, Closed, Resolved).
5. **Status Changed Date Range** ile durum değişim aralığını gir.
6. **JQL Preview** alanında oluşan sorguyu kontrol et.
7. **Excel Oluştur** butonuna bas.
8. `jira-export-<başlangıç>_<bitiş>.xlsx` dosyası otomatik indirilir.

---

## Excel İçeriği

İndirilen dosyada şu kolonlar bulunur:

| Kolon | Açıklama |
|---|---|
| Key | Issue anahtarı (örn. FTBASM-1785) |
| Issue Type | Story, Task, Bug vb. |
| Summary | Issue başlığı |
| Status | Done, Closed vb. |
| Assignee | Atanan kişi |
| Reporter | Bildiren kişi |
| Created | Oluşturulma tarihi |
| Updated | Son güncellenme tarihi |
| Labels | Etiketler (virgülle ayrılmış) |
| Jira URL | Issue'ya doğrudan bağlantı |

---

## Hata Mesajları

| Mesaj | Çözüm |
|---|---|
| JIRA_PAT environment variable bulunamadı | `$env:JIRA_PAT` set et |
| Authentication başarısız | Token'ı kontrol et veya yenile |
| Erişim yetkiniz olmayabilir | Jira proje yetkini kontrol et |
| JQL sorgusu geçersiz | Proje adını ve tarih formatını kontrol et |
| Jira'ya bağlanırken hata | VPN bağlantısını ve ağ erişimini kontrol et |

---

## Proje Yapısı

```
auditHub/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/thy/audithub/
    │   │   ├── AuditHubApplication.java
    │   │   ├── config/
    │   │   │   └── JiraProperties.java
    │   │   ├── controller/
    │   │   │   └── ReportController.java
    │   │   ├── dto/
    │   │   │   ├── FilterRequest.java
    │   │   │   ├── JiraFieldDto.java
    │   │   │   ├── JiraIssueDto.java
    │   │   │   ├── JiraNameDto.java
    │   │   │   ├── JiraSearchResponse.java
    │   │   │   └── JiraUserDto.java
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── JiraAuthException.java
    │   │   │   ├── JiraConnectionException.java
    │   │   │   ├── JiraForbiddenException.java
    │   │   │   ├── JiraInvalidQueryException.java
    │   │   │   └── JiraTokenMissingException.java
    │   │   └── service/
    │   │       ├── ExcelService.java
    │   │       ├── JiraService.java
    │   │       └── JqlBuilderService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/
    │       │   └── index.html
    │       └── static/
    │           ├── css/style.css
    │           └── js/app.js
    └── test/
        └── java/com/thy/audithub/
            └── JqlBuilderServiceTest.java
```

---

## Testleri Çalıştırma

```bash
mvn test
```

---

## Güvenlik

- Token kaynak koda **asla** yazılmaz.
- `.env` ve `application-local.properties` `.gitignore` ile Git dışında tutulur.
- Token loglanmaz, frontend'e gönderilmez, hata mesajlarında gösterilmez.
