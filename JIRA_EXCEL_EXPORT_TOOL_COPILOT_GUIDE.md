# Jira Excel Export Tool - Copilot Geliştirme Rehberi

Bu dosya, projeyi GitHub Copilot ile baştan sona yazdırmak için hazırlanmıştır. Amaç, Jira üzerinde manuel JQL filtresi çalıştırıp çıkan sonuçları Excel'e elle listeleme sürecini otomatikleştirmektir.

Bu projede ilk sürümde grafik/dashboard olmayacak. Sadece kullanıcıdan dinamik filtreler alınacak, Jira REST API ile issue listesi çekilecek ve Excel dosyası indirilecek.

---

## 1. Projenin Amacı

Kullanıcı şu anda Jira'da aşağıdaki gibi bir JQL filtresi çalıştırıyor:

```sql
issuetype in (Story, Task)
AND created >= 2025-05-01
AND project in ("Fast Track Board_MÇ_Açık Sistemler Müdürlüğü")
AND status changed to (Done,Closed) during (2026-04-01, 2026-04-27)
```

Sonrasında çıkan sonuçları manuel olarak Excel'e aktarıyor.

Bu uygulamanın amacı:

1. Kullanıcıdan filtre bilgilerini basit bir arayüz ile almak.
2. Bu filtrelerden otomatik JQL üretmek.
3. Jira REST API'ye Personal Access Token ile bağlanmak.
4. Jira'dan issue listesini çekmek.
5. Gelen verileri Excel formatına dönüştürmek.
6. Kullanıcıya `.xlsx` dosyası olarak indirtmek.

---

## 2. İlk Sürümde Olacak Özellikler

MVP kapsamı:

- Basit web arayüzü.
- Jira bağlantı ayarları.
- Dinamik filtre formu.
- JQL otomatik üretimi.
- Jira REST API üzerinden issue çekme.
- Excel dosyası oluşturma.
- Excel dosyasını indirme.
- Temel hata mesajları.
- Token bilgisini koda gömmeme.

İlk sürümde olmayacaklar:

- Grafikler.
- Dashboard.
- Kullanıcı yönetimi.
- Login ekranı.
- Database.
- Jira'ya issue yazma/güncelleme.
- Çok gelişmiş yetkilendirme.

---

## 3. Teknoloji Seçimi

Bu proje Java geliştiricisi tarafından geliştirileceği için Spring Boot tercih edilecektir.

Kullanılacak teknolojiler:

- Java 17
- Spring Boot 3.x
- Spring Web
- Thymeleaf
- Apache POI
- Lombok
- Maven
- Jira REST API
- HTML/CSS/JavaScript

Neden Spring Boot?

- Java ekosistemine uygun.
- Kurumsal kullanıma uygun.
- REST çağrısı, Excel üretimi ve basit arayüz için yeterli.
- Daha sonra dashboard, grafik ve kullanıcı yönetimi eklenebilir.

---

## 4. Proje İsmi

Proje adı:

```text
jira-excel-exporter
```

Package adı:

```text
com.company.jiraexcel
```

İstenirse şirket içi kullanıma göre şu isimler de olabilir:

```text
jira-audit-exporter
jira-report-tool
jira-quality-exporter
```

---

## 5. Hedef Kullanıcı Akışı

Kullanıcı uygulamayı açar.

Ekranda şu form alanlarını görür:

```text
Jira Excel Export Tool

Project:
[ Fast Track Board_MÇ_Açık Sistemler Müdürlüğü ]

Issue Types:
[ Story ] [ Task ]

Created Start Date:
[ 2025-05-01 ]

Statuses:
[ Done ] [ Closed ]

Status Changed Start Date:
[ 2026-04-01 ]

Status Changed End Date:
[ 2026-04-27 ]

[ Excel Oluştur ]
```

Kullanıcı butona basar.

Uygulama arka planda JQL üretir:

```sql
issuetype in (Story, Task)
AND created >= "2025-05-01"
AND project in ("Fast Track Board_MÇ_Açık Sistemler Müdürlüğü")
AND status changed to (Done, Closed) during ("2026-04-01", "2026-04-27")
```

Sonra Jira'dan kayıtları çeker ve Excel dosyasını indirir.

---

## 6. Jira Bilgileri

Jira base URL:

```text
https://jira.thy.com
```

Örnek issue URL:

```text
https://jira.thy.com/browse/FTBASM-1785
```

Bu Jira kurulumu Atlassian Cloud değil, Jira Server/Data Center gibi görünmektedir.

Authentication yöntemi:

```text
Personal Access Token
```

Token oluşturma ekranı Jira profilinde `Personal Access Tokens` bölümündedir.

Önemli güvenlik kuralı:

Token asla source code içine yazılmayacak.
Token GitHub'a commit edilmeyecek.
Token `.env`, environment variable veya local `application-local.properties` üzerinden alınacak.

---

## 7. Proje Dosya Yapısı

Copilot aşağıdaki yapıya göre projeyi oluşturmalıdır:

```text
jira-excel-exporter
├── pom.xml
├── README.md
├── .gitignore
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── company
│   │   │           └── jiraexcel
│   │   │               ├── JiraExcelExporterApplication.java
│   │   │               ├── config
│   │   │               │   └── JiraProperties.java
│   │   │               ├── controller
│   │   │               │   └── ReportController.java
│   │   │               ├── dto
│   │   │               │   ├── FilterRequest.java
│   │   │               │   ├── JiraIssueDto.java
│   │   │               │   ├── JiraSearchResponse.java
│   │   │               │   └── JiraFieldDto.java
│   │   │               ├── service
│   │   │               │   ├── JqlBuilderService.java
│   │   │               │   ├── JiraService.java
│   │   │               │   └── ExcelService.java
│   │   │               └── exception
│   │   │                   └── GlobalExceptionHandler.java
│   │   └── resources
│   │       ├── application.properties
│   │       ├── templates
│   │       │   └── index.html
│   │       └── static
│   │           ├── css
│   │           │   └── style.css
│   │           └── js
│   │               └── app.js
│   └── test
│       └── java
│           └── com
│               └── company
│                   └── jiraexcel
│                       └── JqlBuilderServiceTest.java
```

---

## 8. Maven Bağımlılıkları

`pom.xml` içinde şu bağımlılıklar olmalıdır:

- spring-boot-starter-web
- spring-boot-starter-thymeleaf
- spring-boot-configuration-processor
- lombok
- apache poi-ooxml
- spring-boot-starter-validation
- spring-boot-starter-test

Copilot'tan istenen:

```text
Create a Spring Boot 3 Maven project with Java 17. Add Spring Web, Thymeleaf, Validation, Lombok and Apache POI dependencies.
```

---

## 9. application.properties İçeriği

`application.properties` içinde şu yapı olmalıdır:

```properties
server.port=8080

jira.base-url=${JIRA_BASE_URL:https://jira.thy.com}
jira.personal-access-token=${JIRA_PAT:}
jira.api-search-path=/rest/api/2/search
jira.max-results=100
```

Açıklama:

- `JIRA_BASE_URL` environment variable yoksa default `https://jira.thy.com` kullanılabilir.
- `JIRA_PAT` environment variable olarak verilecek.
- Token boşsa uygulama hata vermeli ve kullanıcıya anlaşılır mesaj göstermeli.

---

## 10. .gitignore İçeriği

`.gitignore` içinde en az şunlar olmalıdır:

```gitignore
target/
.idea/
*.iml
.env
application-local.properties
.DS_Store
```

Token içeren hiçbir dosya Git'e eklenmemelidir.

---

## 11. DTO Tasarımları

### 11.1 FilterRequest

Kullanıcı formundan gelen değerleri taşır.

Alanlar:

```java
private String project;
private List<String> issueTypes;
private LocalDate createdStartDate;
private List<String> statuses;
private LocalDate statusChangedStartDate;
private LocalDate statusChangedEndDate;
```

Validasyon:

- Project boş olamaz.
- En az bir issue type seçilmelidir.
- Created start date boş olamaz.
- En az bir status seçilmelidir.
- Status changed start date boş olamaz.
- Status changed end date boş olamaz.
- End date, start date'ten önce olamaz.

---

### 11.2 JiraSearchResponse

Jira API response modelidir.

Alanlar:

```java
private Integer startAt;
private Integer maxResults;
private Integer total;
private List<JiraIssueDto> issues;
```

---

### 11.3 JiraIssueDto

Jira issue bilgisini temsil eder.

Alanlar:

```java
private String id;
private String key;
private JiraFieldDto fields;
```

---

### 11.4 JiraFieldDto

Jira issue fields alanını temsil eder.

İlk sürümde şu alanlar yeterlidir:

```java
private String summary;
private JiraNameDto issuetype;
private JiraNameDto status;
private JiraUserDto assignee;
private JiraUserDto reporter;
private String created;
private String updated;
private List<String> labels;
```

Yardımcı DTO'lar:

```java
JiraNameDto:
- name

JiraUserDto:
- displayName
- emailAddress
- name
```

Not:

Jira Server/Data Center alan isimleri Jira Cloud'dan farklı olabilir. Bu nedenle null-safe mapping yapılmalıdır.

---

## 12. JQL Builder Kuralları

`JqlBuilderService` kullanıcı filtrelerinden güvenli ve okunabilir JQL üretmelidir.

Örnek input:

```json
{
  "project": "Fast Track Board_MÇ_Açık Sistemler Müdürlüğü",
  "issueTypes": ["Story", "Task"],
  "createdStartDate": "2025-05-01",
  "statuses": ["Done", "Closed"],
  "statusChangedStartDate": "2026-04-01",
  "statusChangedEndDate": "2026-04-27"
}
```

Beklenen JQL:

```sql
issuetype in (Story, Task)
AND created >= "2025-05-01"
AND project in ("Fast Track Board_MÇ_Açık Sistemler Müdürlüğü")
AND status changed to (Done, Closed) during ("2026-04-01", "2026-04-27")
```

JQL üretim kuralları:

- Project her zaman çift tırnak içine alınmalı.
- Date değerleri `yyyy-MM-dd` formatında çift tırnak içinde yazılmalı.
- Issue type listesi virgül ile ayrılmalı.
- Status listesi virgül ile ayrılmalı.
- Boş/null değerler validasyonda engellenmeli.
- JQL string içinde gereksiz ekstra boşluk olmamalı.

---

## 13. Jira REST API Çağrısı

`JiraService` Jira'ya bağlanmalıdır.

Endpoint:

```text
GET https://jira.thy.com/rest/api/2/search
```

Query parametreleri:

```text
jql=<generated-jql>
startAt=0
maxResults=100
fields=summary,issuetype,status,assignee,reporter,created,updated,labels
```

Header:

```http
Authorization: Bearer <PERSONAL_ACCESS_TOKEN>
Accept: application/json
```

Önemli:

Jira Server/Data Center Personal Access Token genelde Bearer token ile çalışır.

Eğer Bearer çalışmazsa alternatif olarak Basic Auth veya session cookie gerekebilir. İlk sürümde Bearer PAT kullanılacaktır.

---

## 14. Pagination Mantığı

Jira API tek çağrıda tüm sonuçları döndürmeyebilir.

Bu yüzden `JiraService` pagination desteklemelidir.

Mantık:

1. `startAt=0`, `maxResults=100` ile ilk istek atılır.
2. Response içinden `total` okunur.
3. `startAt + maxResults < total` olduğu sürece yeni istek atılır.
4. Tüm issue'lar tek listeye eklenir.

Pseudo flow:

```text
allIssues = []
startAt = 0
maxResults = 100

do:
  response = callJira(jql, startAt, maxResults)
  allIssues.addAll(response.issues)
  startAt = startAt + maxResults
while startAt < response.total
```

---

## 15. Excel Kolonları

İlk sürümde Excel şu kolonlarla oluşturulmalıdır:

```text
Key
Issue Type
Summary
Status
Assignee
Reporter
Created
Updated
Labels
Jira URL
```

Sonra manuel Excel formatına göre şu kolonlar eklenebilir:

```text
Ürün
Issue Type Uygunluk
Analiz Format Uygunluk
Analiz İçeriği Olgunluk
Analiz neden uygun değil?
Back To Analysis
Tester
```

İlk MVP için Jira'dan direkt çekilebilen kolonlar yeterlidir.

---

## 16. ExcelService Kuralları

`ExcelService` Apache POI kullanarak `.xlsx` dosyası üretmelidir.

Excel dosyası özellikleri:

- Sheet adı: `Jira Export`
- İlk satır header olmalı.
- Header satırı bold olmalı.
- Header arka planı açık gri veya koyu mavi olabilir.
- Kolon genişlikleri otomatik ayarlanmalı.
- Tarih kolonları okunabilir formatta olmalı.
- Labels virgül ile birleştirilerek yazılmalı.
- Jira URL kolonu şu formatta olmalı:

```text
https://jira.thy.com/browse/FTBASM-1785
```

- Issue yoksa yine header içeren boş Excel oluşturulmalı.
- Response `byte[]` olarak controller'a dönmelidir.

---

## 17. Controller Tasarımı

`ReportController` iki endpoint içermelidir.

### 17.1 Ana Sayfa

```http
GET /
```

Döner:

```text
index.html
```

Model içine default filtre değerleri koyulabilir:

```text
project = Fast Track Board_MÇ_Açık Sistemler Müdürlüğü
issueTypes = Story, Task
createdStartDate = 2025-05-01
statuses = Done, Closed
statusChangedStartDate = 2026-04-01
statusChangedEndDate = 2026-04-27
```

---

### 17.2 Excel Export

```http
POST /export
```

Form'dan `FilterRequest` alır.

Akış:

1. Request validate edilir.
2. JQL oluşturulur.
3. Jira'dan issue listesi çekilir.
4. Excel oluşturulur.
5. Dosya response olarak indirilir.

Response header:

```http
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="jira-export.xlsx"
```

Dosya adı tarih içerebilir:

```text
jira-export-2026-04-01_2026-04-27.xlsx
```

---

## 18. UI Tasarımı

`index.html` sade ve kullanışlı olmalıdır.

Sayfa başlığı:

```text
Jira Excel Export Tool
```

Form alanları:

```text
Project
Issue Types
Created Start Date
Statuses
Status Changed Start Date
Status Changed End Date
Excel Oluştur button
```

Issue type checkbox'ları:

```text
Story
Task
Bug
Sub-task
```

Default seçili:

```text
Story
Task
```

Status checkbox'ları:

```text
Done
Closed
Resolved
```

Default seçili:

```text
Done
Closed
```

Buton:

```text
Excel Oluştur
```

Opsiyonel:

- Ekranda üretilen JQL preview gösterilebilir.
- Kullanıcı filtreleri değiştirdikçe JQL preview güncellenebilir.

---

## 19. CSS Tasarım Beklentisi

`style.css` modern ve sade olmalıdır.

Tasarım:

- Ortalanmış container.
- Beyaz kart görünümü.
- Açık gri arka plan.
- Inputlar geniş ve okunabilir.
- Buton mavi veya kırmızı olabilir.
- Mobil uyumlu basit yapı.

Örnek görünüm:

```text
------------------------------------------------
| Jira Excel Export Tool                       |
|                                              |
| Project                                      |
| [ Fast Track Board_MÇ_Açık Sistemler... ]    |
|                                              |
| Issue Types                                  |
| [x] Story  [x] Task  [ ] Bug                 |
|                                              |
| Created Start Date                           |
| [2025-05-01]                                 |
|                                              |
| Statuses                                     |
| [x] Done  [x] Closed                         |
|                                              |
| Status Changed Date Range                    |
| [2026-04-01] [2026-04-27]                    |
|                                              |
| [ Excel Oluştur ]                            |
------------------------------------------------
```

---

## 20. JavaScript Beklentisi

`app.js` ilk sürümde zorunlu değildir.

Eklenirse sadece şunları yapabilir:

- Tarih aralığı kontrolü.
- JQL preview güncelleme.
- Export butonuna basınca loading durumu gösterme.

Örnek loading mesajı:

```text
Excel oluşturuluyor, lütfen bekleyin...
```

---

## 21. Hata Yönetimi

Aşağıdaki hatalar kullanıcıya anlaşılır gösterilmelidir:

### Token boşsa

```text
Jira Personal Access Token bulunamadı. Lütfen JIRA_PAT environment variable değerini tanımlayın.
```

### Jira 401 dönerse

```text
Jira authentication başarısız. Token geçersiz veya süresi dolmuş olabilir.
```

### Jira 403 dönerse

```text
Bu Jira verilerine erişim yetkiniz olmayabilir.
```

### Jira 400 dönerse

```text
JQL sorgusu geçersiz. Lütfen filtreleri kontrol edin.
```

### Jira bağlantı hatası

```text
Jira'ya bağlanırken hata oluştu. VPN, ağ bağlantısı veya Jira erişimini kontrol edin.
```

### Kayıt yoksa

Excel oluşturulabilir ama kullanıcıya şu bilgi gösterilebilir:

```text
Seçilen filtrelere uygun issue bulunamadı.
```

---

## 22. Güvenlik Kuralları

Copilot bu kurallara uymalıdır:

1. Token source code içine yazılmayacak.
2. Token loglanmayacak.
3. Token frontend'e gönderilmeyecek.
4. Token Git'e commit edilmeyecek.
5. Hata mesajlarında token gösterilmeyecek.
6. Jira response loglanacaksa hassas alanlar maskelenmeli.
7. `.env` dosyası `.gitignore` içinde olmalı.

---

## 23. Local Çalıştırma

Terminalden token setleme örneği:

### macOS/Linux

```bash
export JIRA_BASE_URL="https://jira.thy.com"
export JIRA_PAT="BURAYA_TOKEN_GELECEK"
mvn spring-boot:run
```

### Windows PowerShell

```powershell
$env:JIRA_BASE_URL="https://jira.thy.com"
$env:JIRA_PAT="BURAYA_TOKEN_GELECEK"
mvn spring-boot:run
```

Uygulama açılır:

```text
http://localhost:8080
```

---

## 24. Test Senaryoları

### 24.1 JQL Builder Testi

Input:

```text
project = Fast Track Board_MÇ_Açık Sistemler Müdürlüğü
issueTypes = Story, Task
createdStartDate = 2025-05-01
statuses = Done, Closed
statusChangedStartDate = 2026-04-01
statusChangedEndDate = 2026-04-27
```

Beklenen output:

```sql
issuetype in (Story, Task) AND created >= "2025-05-01" AND project in ("Fast Track Board_MÇ_Açık Sistemler Müdürlüğü") AND status changed to (Done, Closed) during ("2026-04-01", "2026-04-27")
```

### 24.2 Date Validation Testi

Status changed end date, start date'ten önce ise validation hatası dönmeli.

### 24.3 Empty Issue List Testi

Issue listesi boş olsa bile Excel header ile oluşturulmalı.

### 24.4 Excel Column Testi

Oluşan Excel'de şu kolonlar bulunmalı:

```text
Key
Issue Type
Summary
Status
Assignee
Reporter
Created
Updated
Labels
Jira URL
```

---

## 25. Copilot İçin Adım Adım Prompt Planı

Aşağıdaki promptları sırayla Copilot'a ver.

### Prompt 1 - Proje Oluşturma

```text
Create a Spring Boot 3 Maven project named jira-excel-exporter using Java 17. Add dependencies for Spring Web, Thymeleaf, Validation, Lombok, Apache POI and Spring Boot Test. Use package com.company.jiraexcel. Create the folder structure described in JIRA_EXCEL_EXPORT_TOOL_COPILOT_GUIDE.md.
```

---

### Prompt 2 - Configuration

```text
Create JiraProperties class under config package. It should read jira.base-url, jira.personal-access-token, jira.api-search-path and jira.max-results from application.properties using @ConfigurationProperties. Also configure application.properties according to the guide. Do not hardcode any token.
```

---

### Prompt 3 - DTO Classes

```text
Create all DTO classes described in the guide: FilterRequest, JiraSearchResponse, JiraIssueDto, JiraFieldDto, JiraNameDto and JiraUserDto. Add Lombok annotations. Add validation annotations to FilterRequest. Use LocalDate for date fields.
```

---

### Prompt 4 - JQL Builder

```text
Create JqlBuilderService. It should generate a valid Jira JQL string from FilterRequest. Project and date values must be wrapped with double quotes. Issue types and statuses must be comma separated. Follow the exact JQL format described in the guide.
```

---

### Prompt 5 - Jira Service

```text
Create JiraService. It should call Jira REST API /rest/api/2/search using RestClient or WebClient. Use Bearer token authentication with jira.personal-access-token. Support pagination using startAt, maxResults and total. Request only these fields: summary, issuetype, status, assignee, reporter, created, updated, labels. Return a List<JiraIssueDto>.
```

---

### Prompt 6 - Excel Service

```text
Create ExcelService using Apache POI. It should create an XLSX workbook with a sheet named Jira Export. Add headers: Key, Issue Type, Summary, Status, Assignee, Reporter, Created, Updated, Labels, Jira URL. Make header row bold, apply readable column widths, and write Jira issue data null-safely. Return the workbook as byte[]. Jira URL must be baseUrl + /browse/ + issueKey.
```

---

### Prompt 7 - Controller

```text
Create ReportController with GET / and POST /export. GET / should return index.html with default FilterRequest values. POST /export should validate FilterRequest, build JQL, fetch Jira issues, generate Excel and return it as downloadable file with correct Content-Type and Content-Disposition headers.
```

---

### Prompt 8 - UI

```text
Create index.html using Thymeleaf. Build a clean form for project, issueTypes checkboxes, createdStartDate, statuses checkboxes, statusChangedStartDate and statusChangedEndDate. Add a submit button named Excel Oluştur. Use default values from the model. Link style.css and app.js.
```

---

### Prompt 9 - CSS

```text
Create style.css with a modern, simple, responsive design. Use a centered card layout, readable inputs, checkbox groups, clear labels and a primary button. Keep it suitable for an internal corporate tool.
```

---

### Prompt 10 - Error Handling

```text
Create GlobalExceptionHandler. Handle validation errors, Jira authentication errors, Jira forbidden errors, bad JQL errors and generic connection errors. Show user-friendly Turkish error messages on the index page. Never expose the personal access token.
```

---

### Prompt 11 - Tests

```text
Create unit tests for JqlBuilderService. Test default filter generation, multiple issue types, multiple statuses and invalid date range validation if applicable.
```

---

### Prompt 12 - README

```text
Create README.md explaining what this tool does, how to configure JIRA_BASE_URL and JIRA_PAT, how to run the project locally, how to use the form, and how to download the Excel report. Add a security warning that the token must never be committed.
```

---

## 26. İlk Çalıştırma Kontrol Listesi

Projeyi oluşturduktan sonra şu adımları kontrol et:

1. `mvn clean install` başarılı mı?
2. `JIRA_PAT` environment variable set edildi mi?
3. `mvn spring-boot:run` çalışıyor mu?
4. `http://localhost:8080` açılıyor mu?
5. Form default değerlerle geliyor mu?
6. Excel Oluştur butonuna basınca dosya iniyor mu?
7. Dosya açılıyor mu?
8. Excel içinde Jira kayıtları var mı?
9. Türkçe karakterlerde bozulma var mı?
10. Token loglarda görünmüyor mu?

---

## 27. Gelecek Versiyon Fikirleri

İlk sürüm tamamlandıktan sonra eklenebilecek özellikler:

- Excel yükleme ve karşılaştırma.
- Ürün bazlı otomatik mapping.
- Müdürlük bazlı filtre.
- Dashboard/grafik.
- Analiz kalite alanları.
- Manuel değerlendirme kolonları.
- Kullanıcı bazlı kaydedilmiş filtreler.
- Scheduled export.
- Mail ile rapor gönderme.
- Jira custom field desteği.
- AuditX benzeri görsel dashboard.

---

## 28. Önemli Notlar

Bu proje ilk aşamada şirket içi kullanıma yönelik basit bir export aracıdır.

Kod yazılırken hedef:

- Basitlik
- Güvenlik
- Okunabilirlik
- Genişletilebilirlik

olmalıdır.

Copilot, gereksiz karmaşık mimari kurmamalıdır. Database, login, dashboard ve grafik gibi konular ilk sürüme dahil edilmemelidir.

İlk hedef:

```text
Filtre gir → Jira'dan issue çek → Excel indir
```

Bu akış stabil çalıştıktan sonra proje geliştirilebilir.

