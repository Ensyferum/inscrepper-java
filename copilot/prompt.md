# Prompt Completo: Reconstrução do Inscrepper em Java Spring Boot

## Visão Geral do Projeto

Você é um desenvolvedor Java experiente e precisa criar um sistema completo de web scraping para Instagram chamado **InscrepperJava**. Este sistema deve coletar dados de perfis do Instagram de forma automatizada, armazenar informações em banco de dados e fornecer uma interface web para gerenciamento e visualização dos dados coletados.

**REQUISITO CRÍTICO**: Todo o sistema (Backend + Frontend) deve estar em um único projeto Spring Boot, gerando um único arquivo JAR/container Docker para deploy.

---

## Stack Tecnológica Obrigatória

### Backend
- **Java 17+** (LTS)
- **Spring Boot 3.2+**
- **Spring Data JPA** (persistência)
- **Spring Web** (REST API)
- **Spring Boot DevTools** (desenvolvimento)
- **Thymeleaf** (template engine para frontend)
- **H2 Database** (desenvolvimento) / **PostgreSQL** (produção)
- **Selenium WebDriver 4+** (web scraping)
- **WebDriverManager** (gerenciamento automático de drivers)
- **Jackson** (serialização JSON)
- **Lombok** (redução de boilerplate)
- **Spring Scheduler** (agendamento de tarefas)
- **CompletableFuture** (processamento assíncrono)

### Frontend (Integrado ao Spring Boot)
- **Thymeleaf** (server-side rendering)
- **Bootstrap 5** (UI framework)
- **Alpine.js** ou **HTMX** (interatividade client-side leve)
- **Chart.js** (gráficos e visualizações)

### Build e Deploy
- **Maven** (gerenciamento de dependências)
- **Docker** (containerização)
- **Spring Boot Maven Plugin** (build de JAR executável)

---

## Arquitetura do Sistema

### Estrutura de Pacotes

```
com.ensyferum.inscrepper/
├── InscrepperApplication.java
├── config/
│   ├── WebConfig.java
│   ├── SchedulerConfig.java
│   ├── AsyncConfig.java
│   └── SeleniumConfig.java
├── controller/
│   ├── api/
│   │   ├── ProfileApiController.java
│   │   ├── ContentApiController.java
│   │   ├── AdminApiController.java
│   │   └── ReportApiController.java
│   └── web/
│       ├── HomeController.java
│       ├── ProfileController.java
│       ├── ContentController.java
│       └── AdminController.java
├── service/
│   ├── InstagramScraperService.java
│   ├── ProfileService.java
│   ├── ContentService.java
│   ├── DownloadService.java
│   ├── SchedulerService.java
│   └── ReportService.java
├── repository/
│   ├── ProfileRepository.java
│   └── ContentRepository.java
├── model/
│   ├── entity/
│   │   ├── Profile.java
│   │   └── Content.java
│   └── dto/
│       ├── ProfileDTO.java
│       ├── ContentDTO.java
│       ├── ScrapeRequestDTO.java
│       └── ReportRequestDTO.java
├── scraper/
│   ├── InstagramScraper.java
│   ├── ScraperDriver.java
│   └── ScraperUtils.java
├── util/
│   ├── ScraperLogger.java
│   └── FileUtils.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ScraperException.java
    └── ProfileNotFoundException.java
```

### Estrutura de Resources

```
src/main/resources/
├── application.properties
├── application-dev.properties
├── application-prod.properties
├── static/
│   ├── css/
│   │   └── style.css
│   ├── js/
│   │   ├── main.js
│   │   └── admin.js
│   └── images/
└── templates/
    ├── layout/
    │   ├── base.html
    │   └── fragments.html
    ├── home.html
    ├── profiles.html
    ├── content.html
    ├── profile-detail.html
    ├── admin.html
    └── error/
        ├── 404.html
        └── 500.html
```

---

## Modelos de Dados

### Entidade: Profile

```java
@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username; // formato: @username
    
    @Column(name = "profile_id")
    private String profileId; // ID gerado: profile_username
    
    @Column(name = "last_scan")
    private LocalDateTime lastScan;
    
    @Column(name = "total_content")
    private Integer totalContent = 0;
    
    @Column(name = "posts_found")
    private Integer postsFound = 0;
    
    @Column(name = "posts_processed")
    private Integer postsProcessed = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Content> contents = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (profileId == null) {
            profileId = "profile_" + username.replace("@", "").toLowerCase();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Entidade: Content

```java
@Entity
@Table(name = "contents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_id", unique = true)
    private String contentId; // ID do post no Instagram
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;
    
    @Column(name = "original_url")
    private String originalUrl;
    
    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType contentType; // POST, REEL, STORY
    
    @Column(name = "owner_username")
    private String ownerUsername;
    
    @Column(name = "is_reposted")
    private Boolean isReposted = false;
    
    @Column(name = "description", length = 5000)
    private String description;
    
    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;
    
    @Column(name = "local_cover_path")
    private String localCoverPath;
    
    // Metadata
    @Column(name = "caption", length = 5000)
    private String caption;
    
    @Column(name = "likes")
    private Integer likes = 0;
    
    @Column(name = "comments")
    private Integer comments = 0;
    
    @Column(name = "views")
    private Integer views = 0;
    
    @Column(name = "post_date")
    private LocalDateTime postDate;
    
    @ElementCollection
    @CollectionTable(name = "content_hashtags", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "hashtag")
    private List<String> hashtags = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "content_mentions", joinColumns = @JoinColumn(name = "content_id"))
    @Column(name = "mention")
    private List<String> mentions = new ArrayList<>();
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ContentStatus status = ContentStatus.PENDING; // PENDING, PROCESSED, ERROR
    
    @Column(name = "processing_error", length = 1000)
    private String processingError;
    
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}

enum ContentType {
    POST, REEL, STORY
}

enum ContentStatus {
    PENDING, PROCESSED, ERROR
}
```

---

## Funcionalidades Principais

### 1. Sistema de Scraping

#### InstagramScraperService.java
```java
@Service
@Slf4j
public class InstagramScraperService {
    
    @Autowired
    private ScraperDriver scraperDriver;
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private ContentService contentService;
    
    @Autowired
    private DownloadService downloadService;
    
    @Value("${scraper.max-posts:50}")
    private int maxPosts;
    
    @Value("${scraper.parallel-workers:3}")
    private int parallelWorkers;
    
    /**
     * Coleta posts de um perfil específico
     */
    public List<Content> scrapeProfile(String username, Integer customMaxPosts) {
        int postsLimit = customMaxPosts != null ? customMaxPosts : maxPosts;
        
        log.info("Iniciando scrape do perfil: {}, limite: {} posts", username, postsLimit);
        
        // Normalizar username
        username = username.replace("@", "");
        
        // Criar/atualizar perfil
        Profile profile = profileService.findOrCreate(username);
        
        // Inicializar driver
        WebDriver driver = scraperDriver.initDriver();
        
        try {
            // Navegar para perfil
            driver.get("https://www.instagram.com/" + username);
            Thread.sleep(3000); // Aguardar carregamento
            
            // Aguardar posts aparecerem
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("a[href*='/p/'], a[href*='/reel/']")
            ));
            
            // Coletar links de posts
            List<WebElement> postElements = driver.findElements(
                By.cssSelector("a[href*='/p/'], a[href*='/reel/']")
            );
            
            List<Content> collectedContents = new ArrayList<>();
            int count = 0;
            
            for (WebElement element : postElements) {
                if (count >= postsLimit) break;
                
                try {
                    String postUrl = element.getAttribute("href");
                    
                    // Verificar se já existe
                    if (contentService.existsByOriginalUrl(postUrl)) {
                        log.debug("Post já existe: {}", postUrl);
                        continue;
                    }
                    
                    // Extrair dados do post
                    Content content = extractPostData(element, profile, postUrl);
                    
                    // Salvar no banco
                    Content saved = contentService.save(content);
                    collectedContents.add(saved);
                    
                    count++;
                    log.info("Post coletado {}/{}: {}", count, postsLimit, postUrl);
                    
                } catch (Exception e) {
                    log.error("Erro ao processar post", e);
                }
            }
            
            // Atualizar estatísticas do perfil
            profileService.updateScanStatistics(profile.getId(), collectedContents.size());
            
            log.info("Scrape concluído. Total coletado: {}", collectedContents.size());
            return collectedContents;
            
        } catch (Exception e) {
            log.error("Erro durante scrape do perfil: {}", username, e);
            throw new ScraperException("Falha ao fazer scrape do perfil: " + username, e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    /**
     * Extrai dados de um post
     */
    private Content extractPostData(WebElement element, Profile profile, String postUrl) {
        Content content = new Content();
        content.setProfile(profile);
        content.setOriginalUrl(postUrl);
        
        // Extrair ID do post da URL
        String contentId = extractPostId(postUrl);
        content.setContentId(contentId);
        
        // Determinar tipo
        ContentType type = postUrl.contains("/reel/") ? ContentType.REEL : ContentType.POST;
        content.setContentType(type);
        
        // Tentar extrair imagem
        try {
            WebElement img = element.findElement(By.cssSelector("img"));
            String imageUrl = img.getAttribute("src");
            String alt = img.getAttribute("alt");
            
            content.setCoverImageUrl(imageUrl);
            content.setDescription(alt);
            
        } catch (NoSuchElementException e) {
            log.warn("Imagem não encontrada para post: {}", postUrl);
        }
        
        content.setStatus(ContentStatus.PENDING);
        content.setCollectedAt(LocalDateTime.now());
        
        return content;
    }
    
    /**
     * Faz scrape de todos os perfis ativos em paralelo
     */
    @Async
    public CompletableFuture<Map<String, Integer>> scrapeAllProfiles() {
        List<Profile> activeProfiles = profileService.findAllActive();
        
        log.info("Iniciando scrape paralelo de {} perfis", activeProfiles.size());
        
        Map<String, Integer> results = new ConcurrentHashMap<>();
        
        List<CompletableFuture<Void>> futures = activeProfiles.stream()
            .map(profile -> CompletableFuture.runAsync(() -> {
                try {
                    List<Content> contents = scrapeProfile(profile.getUsername(), null);
                    results.put(profile.getUsername(), contents.size());
                } catch (Exception e) {
                    log.error("Erro ao fazer scrape de: {}", profile.getUsername(), e);
                    results.put(profile.getUsername(), -1);
                }
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("Scrape paralelo concluído. Resultados: {}", results);
        return CompletableFuture.completedFuture(results);
    }
}
```

### 2. API REST

#### ProfileApiController.java
```java
@RestController
@RequestMapping("/api/profiles")
@Slf4j
public class ProfileApiController {
    
    @Autowired
    private ProfileService profileService;
    
    @GetMapping
    public ResponseEntity<List<ProfileDTO>> getAllProfiles() {
        List<Profile> profiles = profileService.findAll();
        List<ProfileDTO> dtos = profiles.stream()
            .map(ProfileDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{username}")
    public ResponseEntity<ProfileDTO> getProfile(@PathVariable String username) {
        Profile profile = profileService.findByUsername(username)
            .orElseThrow(() -> new ProfileNotFoundException(username));
        return ResponseEntity.ok(ProfileDTO.fromEntity(profile));
    }
    
    @PostMapping
    public ResponseEntity<ProfileDTO> createProfile(@RequestBody @Valid ProfileDTO dto) {
        Profile profile = profileService.create(dto.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProfileDTO.fromEntity(profile));
    }
    
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String username) {
        profileService.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{username}/content")
    public ResponseEntity<List<ContentDTO>> getProfileContent(@PathVariable String username) {
        List<Content> contents = contentService.findByProfileUsername(username);
        List<ContentDTO> dtos = contents.stream()
            .map(ContentDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
```

#### AdminApiController.java
```java
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminApiController {
    
    @Autowired
    private InstagramScraperService scraperService;
    
    @Autowired
    private ProfileService profileService;
    
    @PostMapping("/scrape/all")
    public ResponseEntity<Map<String, Object>> scrapeAllProfiles(
            @RequestParam(required = false) Integer maxPosts,
            @RequestParam(required = false) Integer workers) {
        
        log.info("Iniciando scrape manual de todos os perfis");
        
        CompletableFuture<Map<String, Integer>> future = scraperService.scrapeAllProfiles();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "Scrape iniciado em background");
        
        return ResponseEntity.accepted().body(response);
    }
    
    @PostMapping("/scrape/profile/{username}")
    public ResponseEntity<Map<String, Object>> scrapeProfile(
            @PathVariable String username,
            @RequestParam(required = false) Integer maxPosts) {
        
        log.info("Iniciando scrape manual do perfil: {}", username);
        
        List<Content> contents = scraperService.scrapeProfile(username, maxPosts);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "completed");
        response.put("username", username);
        response.put("postsCollected", contents.size());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/database/reset")
    public ResponseEntity<Map<String, String>> resetDatabase() {
        contentService.deleteAll();
        profileService.deleteAll();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Database resetado com sucesso");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxPosts", scraperService.getMaxPosts());
        config.put("parallelWorkers", scraperService.getParallelWorkers());
        config.put("autoScrapeEnabled", schedulerService.isAutoScrapeEnabled());
        
        return ResponseEntity.ok(config);
    }
}
```

### 3. Frontend com Thymeleaf

#### admin.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Admin - Inscrepper</title>
    <th:block th:replace="~{layout/fragments :: head}"></th:block>
</head>
<body>
    <div th:replace="~{layout/fragments :: navbar}"></div>
    
    <div class="container mt-4">
        <h1>Painel Administrativo</h1>
        
        <!-- Controles de Scraping -->
        <div class="row mt-4">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5>Controles de Scraping</h5>
                    </div>
                    <div class="card-body">
                        <button class="btn btn-success" onclick="scrapeAll()">
                            <i class="bi bi-arrow-clockwise"></i> Scrape Todos os Perfis
                        </button>
                        <button class="btn btn-danger" onclick="resetDatabase()">
                            <i class="bi bi-trash"></i> Zerar Database
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- Gerenciamento de Perfis -->
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header d-flex justify-content-between">
                        <h5>Perfis Cadastrados</h5>
                        <button class="btn btn-sm btn-primary" onclick="showAddProfileModal()">
                            <i class="bi bi-plus"></i> Novo Perfil
                        </button>
                    </div>
                    <div class="card-body">
                        <div id="profilesList"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Relatórios -->
        <div class="row mt-4">
            <div class="col-md-12">
                <div class="card">
                    <div class="card-header">
                        <h5>Gerar Relatórios</h5>
                    </div>
                    <div class="card-body">
                        <form id="reportForm">
                            <div class="row">
                                <div class="col-md-4">
                                    <label>Perfil</label>
                                    <select class="form-select" id="reportProfile">
                                        <option value="">Todos</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label>Data Inicial</label>
                                    <input type="date" class="form-control" id="reportStartDate">
                                </div>
                                <div class="col-md-3">
                                    <label>Data Final</label>
                                    <input type="date" class="form-control" id="reportEndDate">
                                </div>
                                <div class="col-md-2">
                                    <label>&nbsp;</label>
                                    <button type="button" class="btn btn-primary w-100" onclick="generateReport('xlsx')">
                                        Excel
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <th:block th:replace="~{layout/fragments :: scripts}"></th:block>
    <script th:src="@{/js/admin.js}"></script>
</body>
</html>
```

#### admin.js
```javascript
// Funções do painel admin
async function loadProfiles() {
    const response = await fetch('/api/profiles');
    const profiles = await response.json();
    
    const list = document.getElementById('profilesList');
    list.innerHTML = '';
    
    profiles.forEach(profile => {
        const item = document.createElement('div');
        item.className = 'list-group-item d-flex justify-content-between align-items-center';
        item.innerHTML = `
            <span>${profile.username}</span>
            <div class="btn-group btn-group-sm">
                <button class="btn btn-outline-primary" onclick="scrapeProfile('${profile.username}')">
                    <i class="bi bi-arrow-clockwise"></i>
                </button>
                <button class="btn btn-outline-danger" onclick="deleteProfile('${profile.username}')">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
        list.appendChild(item);
    });
}

async function scrapeAll() {
    if (!confirm('Iniciar scrape de todos os perfis?')) return;
    
    try {
        const response = await fetch('/api/admin/scrape/all', { method: 'POST' });
        const data = await response.json();
        alert('Scrape iniciado em background!');
    } catch (error) {
        alert('Erro ao iniciar scrape: ' + error.message);
    }
}

async function scrapeProfile(username) {
    try {
        const response = await fetch(`/api/admin/scrape/profile/${username}`, { method: 'POST' });
        const data = await response.json();
        alert(`Scrape concluído! ${data.postsCollected} posts coletados.`);
        loadProfiles();
    } catch (error) {
        alert('Erro ao fazer scrape: ' + error.message);
    }
}

async function deleteProfile(username) {
    if (!confirm(`Remover perfil ${username}?`)) return;
    
    try {
        await fetch(`/api/profiles/${username}`, { method: 'DELETE' });
        alert('Perfil removido com sucesso!');
        loadProfiles();
    } catch (error) {
        alert('Erro ao remover perfil: ' + error.message);
    }
}

async function resetDatabase() {
    if (!confirm('ATENÇÃO: Isso apagará TODOS os dados! Confirma?')) return;
    
    try {
        await fetch('/api/admin/database/reset', { method: 'POST' });
        alert('Database zerado com sucesso!');
        location.reload();
    } catch (error) {
        alert('Erro ao resetar database: ' + error.message);
    }
}

function generateReport(format) {
    const profile = document.getElementById('reportProfile').value;
    const startDate = document.getElementById('reportStartDate').value;
    const endDate = document.getElementById('reportEndDate').value;
    
    let url = `/api/reports/export?format=${format}`;
    if (profile) url += `&username=${profile}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    
    window.open(url, '_blank');
}

// Carregar ao iniciar
document.addEventListener('DOMContentLoaded', loadProfiles);
```

### 4. Agendamento Automático

#### SchedulerService.java
```java
@Service
@Slf4j
public class SchedulerService {
    
    @Autowired
    private InstagramScraperService scraperService;
    
    @Value("${scraper.auto-scrape-enabled:false}")
    private boolean autoScrapeEnabled;
    
    @Value("${scraper.schedule-cron:0 0 */6 * * *}") // A cada 6 horas por padrão
    private String scheduleCron;
    
    @Scheduled(cron = "${scraper.schedule-cron:0 0 */6 * * *}")
    public void scheduledScrape() {
        if (!autoScrapeEnabled) {
            log.debug("Auto-scrape desabilitado, pulando execução agendada");
            return;
        }
        
        log.info("Iniciando scrape agendado de todos os perfis");
        
        try {
            scraperService.scrapeAllProfiles().get();
            log.info("Scrape agendado concluído com sucesso");
        } catch (Exception e) {
            log.error("Erro durante scrape agendado", e);
        }
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (autoScrapeEnabled) {
            log.info("Auto-scrape habilitado. Iniciando scrape inicial...");
            scraperService.scrapeAllProfiles();
        }
    }
}
```

### 5. Geração de Relatórios

#### ReportService.java
```java
@Service
@Slf4j
public class ReportService {
    
    @Autowired
    private ContentService contentService;
    
    public byte[] generateExcelReport(String username, LocalDate startDate, LocalDate endDate) {
        List<Content> contents = contentService.findFiltered(username, startDate, endDate);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Conteúdos");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Perfil");
            headerRow.createCell(2).setCellValue("Tipo");
            headerRow.createCell(3).setCellValue("URL");
            headerRow.createCell(4).setCellValue("Descrição");
            headerRow.createCell(5).setCellValue("Likes");
            headerRow.createCell(6).setCellValue("Comentários");
            headerRow.createCell(7).setCellValue("Data Coleta");
            
            // Data
            int rowNum = 1;
            for (Content content : contents) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(content.getContentId());
                row.createCell(1).setCellValue(content.getProfile().getUsername());
                row.createCell(2).setCellValue(content.getContentType().name());
                row.createCell(3).setCellValue(content.getOriginalUrl());
                row.createCell(4).setCellValue(content.getDescription());
                row.createCell(5).setCellValue(content.getLikes());
                row.createCell(6).setCellValue(content.getComments());
                row.createCell(7).setCellValue(content.getCollectedAt().toString());
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Erro ao gerar relatório Excel", e);
            throw new RuntimeException("Falha ao gerar relatório", e);
        }
    }
}
```

---

## Configurações

### application.properties
```properties
# Application
spring.application.name=inscrepper-java
server.port=8080

# Database - H2 (desenvolvimento)
spring.datasource.url=jdbc:h2:file:./data/inscrepper
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Scraper Settings
scraper.max-posts=50
scraper.parallel-workers=3
scraper.auto-scrape-enabled=true
scraper.schedule-cron=0 0 */6 * * *
scraper.media-path=./data/media
scraper.headless=true

# Async
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100

# Logging
logging.level.com.ensyferum.inscrepper=DEBUG
logging.level.org.springframework=INFO
logging.file.name=./logs/inscrepper.log
```

### pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.ensyferum</groupId>
    <artifactId>inscrepper-java</artifactId>
    <version>1.0.0</version>
    <name>Inscrepper Java</name>
    <description>Instagram Scraper em Java com Spring Boot</description>
    
    <properties>
        <java.version>17</java.version>
        <selenium.version>4.15.0</selenium.version>
        <webdrivermanager.version>5.6.2</webdrivermanager.version>
        <poi.version>5.2.5</poi.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Selenium -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.github.bonigarcia</groupId>
            <artifactId>webdrivermanager</artifactId>
            <version>${webdrivermanager.version}</version>
        </dependency>
        
        <!-- Apache POI para Excel -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar Chrome e dependências para Selenium
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    ttf-freefont

ENV CHROME_BIN=/usr/bin/chromium-browser
ENV CHROME_DRIVER=/usr/bin/chromedriver

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Requisitos de Implementação

### Prioridade ALTA
1. ✅ Estrutura básica do projeto Spring Boot com Maven
2. ✅ Modelos JPA (Profile, Content) com relacionamentos
3. ✅ Repositories e Services básicos
4. ✅ InstagramScraperService com Selenium
5. ✅ Controllers REST API completos
6. ✅ Frontend Thymeleaf integrado (todas as páginas)
7. ✅ Sistema de agendamento com Spring Scheduler
8. ✅ Download assíncrono de imagens
9. ✅ Geração de relatórios Excel
10. ✅ Configuração Docker para deploy único

### Prioridade MÉDIA
1. ✅ Tratamento de exceções global
2. ✅ Validações de entrada
3. ✅ Logs estruturados
4. ✅ Testes unitários básicos
5. ✅ Documentação Swagger/OpenAPI

### Prioridade BAIXA
1. ⚠️ Autenticação/Autorização
2. ⚠️ Cache com Redis
3. ⚠️ Métricas com Actuator
4. ⚠️ CI/CD pipeline

---

## Diferenciais do Projeto Python Original

### Funcionalidades Existentes
- ✅ Scraping de perfis do Instagram
- ✅ Coleta de posts e reels
- ✅ Download de thumbnails
- ✅ Armazenamento de metadados (likes, comentários, views)
- ✅ API REST completa
- ✅ Interface web administrativa
- ✅ Agendamento automático de scrapes
- ✅ Scraping paralelo de múltiplos perfis
- ✅ Geração de relatórios (Excel)
- ✅ Detecção de posts repostados
- ✅ Sistema de logging customizado

### Melhorias a Implementar em Java
- ✅ Banco de dados relacional (PostgreSQL/H2) ao invés de JSON
- ✅ ORM com JPA/Hibernate
- ✅ Validações robustas com Bean Validation
- ✅ Transações gerenciadas pelo Spring
- ✅ Thread-safe por padrão
- ✅ Melhor gestão de recursos (conexões, drivers)
- ✅ Configurações externalizadas com Spring Profiles
- ✅ Deploy único (JAR ou Container)

---

## Instruções de Execução

### Desenvolvimento
```bash
# Clonar repositório
git clone <repo-url>
cd inscrepper-java

# Compilar
./mvnw clean install

# Executar
./mvnw spring-boot:run

# Acessar
http://localhost:8080
```

### Produção (Docker)
```bash
# Build
docker build -t inscrepper-java:latest .

# Run
docker run -d \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  -e SPRING_PROFILES_ACTIVE=prod \
  inscrepper-java:latest
```

---

## Resumo

Este prompt fornece TODOS os detalhes técnicos necessários para reconstruir o projeto Inscrepper em Java com Spring Boot, incluindo:

1. ✅ **Arquitetura completa** com separação de camadas
2. ✅ **Modelos de dados** com JPA annotations
3. ✅ **Lógica de scraping** com Selenium
4. ✅ **API REST** completa com todos os endpoints
5. ✅ **Frontend integrado** com Thymeleaf
6. ✅ **Sistema de agendamento** automático
7. ✅ **Geração de relatórios** em Excel
8. ✅ **Configurações** completas (properties, pom.xml, Dockerfile)
9. ✅ **Deploy único** em container Docker

O projeto resultante terá TODAS as funcionalidades do projeto Python original, porém com as vantagens de um ecossistema empresarial robusto (Spring Boot) e deploy simplificado em um único artefato.
