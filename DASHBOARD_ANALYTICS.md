# 📊 DASHBOARD DE ANALYTICS - Documentação Completa

## ✨ Nova Funcionalidade Implementada

Dashboard completo de **Analytics** para visualização de métricas de engajamento dos perfis do Instagram!

---

## 🎯 Features Implementadas

### 1. **Página de Listagem de Perfis** (`/analytics`)
- Lista todos os perfis disponíveis
- Cards visuais para cada perfil
- Link direto para analytics de cada perfil

### 2. **Dashboard do Perfil** (`/analytics/profile/{username}`)
- **Cards de Estatísticas:**
  - 📝 Total de Posts
  - ❤️ Total de Likes (com média)
  - 💬 Total de Comentários (com média)
  - 👁️ Total de Views (com média)

- **Gráficos Interativos (Chart.js):**
  - Gráfico de Barras: Engajamento por Post (Likes, Comentários, Views)
  - Gráfico de Pizza: Distribuição de Métricas Totais

- **Tabela Detalhada:**
  - ID do Post
  - Tipo (POST/REEL)
  - Caption (truncada)
  - Métricas completas
  - Taxa de Engajamento
  - Link para ver no Instagram

### 3. **API REST** (`/api/analytics`)
- `GET /api/analytics/profile/{username}/stats` - Estatísticas gerais
- `GET /api/analytics/profile/{username}/posts` - Lista de posts com métricas
- `GET /api/analytics/profile/{username}/chart-data` - Dados para gráficos

---

## 🗂️ Arquitetura

### Controllers

#### `AnalyticsController.java`
```java
@Controller
@RequestMapping("/analytics")
```

**Endpoints:**
- `GET /analytics` - Lista perfis
- `GET /analytics/profile/{username}` - Dashboard do perfil

**Responsabilidades:**
- Servir templates HTML
- Calcular estatísticas agregadas
- Preparar dados para visualização

#### `AnalyticsApiController.java`
```java
@RestController
@RequestMapping("/api/analytics")
```

**Endpoints:**
- `GET /api/analytics/profile/{username}/stats`
- `GET /api/analytics/profile/{username}/posts`
- `GET /api/analytics/profile/{username}/chart-data`

**Responsabilidades:**
- Fornecer dados JSON para AJAX
- Preparar dados para Chart.js
- Calcular métricas derivadas

### Templates

#### `analytics.html`
Template para listagem de perfis com:
- Cards responsivos
- Efeito hover
- Links para dashboard individual

#### `profile-analytics.html`
Dashboard completo com:
- 4 cards de estatísticas coloridos
- 2 gráficos interativos (Chart.js)
- Tabela responsiva com ordenação
- Badges coloridos por taxa de engajamento

---

## 📊 Métricas Calculadas

### Estatísticas Totais
```java
long totalLikes = soma de todos os likes
long totalComments = soma de todos os comentários
long totalViews = soma de todas as visualizações
```

### Médias
```java
double avgLikes = totalLikes / número de posts
double avgComments = totalComments / número de posts
double avgViews = totalViews / número de posts
```

### Taxa de Engajamento (por post)
```java
double engagementRate = (likes * 100.0) / views
```

**Classificação por cores:**
- 🟢 Verde: ≥ 5%
- 🟡 Amarelo: 2-5%
- ⚫ Cinza: < 2%

---

## 🎨 Design e UX

### Cores dos Cards
- **Primário (Azul):** Total de Posts
- **Vermelho:** Likes ❤️
- **Verde:** Comentários 💬
- **Ciano:** Views 👁️

### Gráficos
- **Barras:** Comparação de métricas por post
- **Pizza/Donut:** Distribuição proporcional das métricas

### Responsividade
- Mobile-first design
- Bootstrap 5 grid system
- Tabela responsiva com scroll horizontal

---

## 🚀 Como Usar

### 1. Acessar Lista de Perfis
```
http://localhost:8080/analytics
```

### 2. Selecionar Perfil
Clicar em "Ver Analytics" no card do perfil desejado

### 3. Visualizar Dashboard
Dashboard carrega automaticamente com:
- Estatísticas atualizadas
- Gráficos interativos
- Tabela completa de posts

### 4. Interagir com Dados
- **Hover nos gráficos:** Ver valores exatos
- **Click na tabela:** Ver caption completa
- **Click no link:** Abrir post no Instagram

---

## 📡 Exemplos de API

### Obter Estatísticas
```bash
curl http://localhost:8080/api/analytics/profile/oncallpeds/stats
```

**Response:**
```json
{
  "username": "oncallpeds",
  "totalPosts": 6,
  "totalLikes": 1287,
  "totalComments": 114,
  "totalViews": 28453,
  "avgLikes": 214.5,
  "avgComments": 19.0,
  "avgViews": 4742.17,
  "topPosts": [...]
}
```

### Obter Posts
```bash
curl http://localhost:8080/api/analytics/profile/oncallpeds/posts
```

**Response:**
```json
[
  {
    "externalId": "DQXGY8VgNpN",
    "type": "REEL",
    "url": "https://instagram.com/...",
    "caption": "Teaching kids...",
    "likesCount": 245,
    "commentsCount": 18,
    "viewsCount": 5432,
    "engagementRate": 4.51,
    "collectedAt": "2025-10-28T23:35:00Z"
  },
  ...
]
```

### Obter Dados para Gráficos
```bash
curl http://localhost:8080/api/analytics/profile/oncallpeds/chart-data
```

**Response:**
```json
{
  "labels": ["DQXGY8VgNpN", "DP62NJKgJk6", ...],
  "likes": [245, 189, ...],
  "comments": [18, 12, ...],
  "views": [5432, 3211, ...]
}
```

---

## 🔧 Customizações Possíveis

### Adicionar Novas Métricas
```java
// No AnalyticsController
double engagementRate = totalViews > 0 ? 
    (totalLikes * 100.0 / totalViews) : 0;
model.addAttribute("engagementRate", engagementRate);
```

### Adicionar Novos Gráficos
```javascript
// No profile-analytics.html
function createLineChart(data) {
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Tendência de Likes',
                data: data.likes,
                borderColor: 'rgb(220, 53, 69)',
                tension: 0.1
            }]
        }
    });
}
```

### Filtrar por Período
```java
// Adicionar parâmetro de data
@GetMapping("/profile/{username}")
public String profileAnalytics(
    @PathVariable String username,
    @RequestParam(required = false) LocalDate from,
    @RequestParam(required = false) LocalDate to,
    Model model
) {
    // Filtrar contents por data...
}
```

---

## 📈 Análises Disponíveis

### 1. **Performance Geral**
- Quantos posts foram publicados
- Total de engajamento (likes + comentários)
- Média de engajamento por post

### 2. **Análise de Conteúdo**
- Quais posts têm melhor performance
- Taxa de engajamento por tipo (POST vs REEL)
- Comparação entre posts

### 3. **Tendências**
- Crescimento de likes ao longo dos posts
- Variação de comentários
- Padrões de visualizações

### 4. **Benchmarking**
- Comparar com média do perfil
- Identificar posts acima/abaixo da média
- Detectar outliers

---

## 🎯 Casos de Uso

### Marketing
- Identificar conteúdo de alto engajamento
- Otimizar estratégia de posts
- Analisar ROI de campanhas

### Pesquisa
- Coletar dados de múltiplos perfis
- Comparar performance entre perfis
- Analisar tendências temporais

### Monitoramento
- Acompanhar performance em tempo real
- Detectar mudanças de engajamento
- Alertar sobre posts virais

---

## ✅ Checklist de Implementação

- [x] AnalyticsController criado
- [x] AnalyticsApiController criado
- [x] Template analytics.html criado
- [x] Template profile-analytics.html criado
- [x] Integração com Chart.js
- [x] Cards de estatísticas
- [x] Gráfico de barras
- [x] Gráfico de pizza
- [x] Tabela de posts
- [x] Taxa de engajamento calculada
- [x] API REST completa
- [x] Responsividade mobile
- [x] Links de navegação
- [x] Botão no menu principal

---

## 🚀 Próximos Passos

### Features Futuras

1. **Filtros e Buscas**
   - Filtrar por período
   - Filtrar por tipo (POST/REEL)
   - Buscar por caption

2. **Exportação**
   - Exportar para CSV
   - Exportar para Excel
   - Gerar PDF com relatório

3. **Comparações**
   - Comparar múltiplos perfis
   - Comparar períodos
   - Benchmark da indústria

4. **Alertas**
   - Notificar posts de alto engajamento
   - Alertar queda de performance
   - Email com resumo semanal

5. **Gráficos Avançados**
   - Linha temporal
   - Heatmap de engajamento
   - Scatter plot (likes vs views)

6. **Machine Learning**
   - Prever engajamento futuro
   - Recomendar melhor horário para postar
   - Sugerir tipo de conteúdo

---

## 🎉 Conclusão

**Dashboard de Analytics 100% Funcional!**

✅ **Visualização Completa** - 4 cards + 2 gráficos + tabela  
✅ **API REST** - 3 endpoints JSON  
✅ **Responsivo** - Mobile-first design  
✅ **Interativo** - Chart.js com tooltips  
✅ **Profissional** - Design moderno com Bootstrap 5  

**O Inscrepper agora é uma plataforma completa de Instagram Analytics!** 📊🚀

---

## 📸 Screenshots

### Dashboard Principal
- Cards coloridos com métricas totais
- Gráficos side-by-side
- Tabela responsiva com badges

### API JSON
- Endpoints RESTful
- Dados estruturados
- Fácil integração

### Mobile View
- Layout adaptativo
- Cards empilhados
- Tabela com scroll horizontal

---

## 🔗 Links Úteis

- **Home:** `http://localhost:8080/`
- **Analytics:** `http://localhost:8080/analytics`
- **Profile Analytics:** `http://localhost:8080/analytics/profile/{username}`
- **API Stats:** `http://localhost:8080/api/analytics/profile/{username}/stats`
- **API Posts:** `http://localhost:8080/api/analytics/profile/{username}/posts`
- **API Chart Data:** `http://localhost:8080/api/analytics/profile/{username}/chart-data`

**Sistema completo de analytics pronto para produção!** 🎊
