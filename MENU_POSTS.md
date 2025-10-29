# 📸 MENU POSTS - Documentação Completa

## ✨ Nova Funcionalidade Implementada

Menu completo de **Posts** para visualizar, filtrar e analisar todo o conteúdo coletado do Instagram!

---

## 🎯 Features Implementadas

### 1. **Listagem de Posts** (`/posts`)
- **Grid Visual Responsivo:**
  - Cards 3 colunas em desktop, 2 em tablet, 1 em mobile
  - Thumbnail placeholder (preparado para imagens futuras)
  - Badge colorido por tipo (POST/REEL/STORY)
  - Preview de caption (máx 100 caracteres)
  - Métricas inline (likes, comentários, views)
  - Hover effect (elevação suave do card)

- **Estatísticas no Topo:**
  - 📝 Total de Posts
  - ❤️ Total de Likes (com média)
  - 💬 Total de Comentários (com média)
  - 👁️ Total de Views (com média)

- **Filtros Avançados:**
  - **Por Perfil:** Dropdown com todos os perfis disponíveis
  - **Por Tipo:** POST 📷 | REEL 🎬 | STORY ⚡
  - **Ordenação:**
    - Mais recentes (padrão)
    - Mais antigos
    - Mais curtidos
    - Mais comentados
    - Mais visualizados
  - **Botão Limpar:** Remove todos os filtros

- **Paginação Completa:**
  - 12 posts por página
  - Navegação: Primeira | Anterior | Páginas | Próxima | Última
  - Mostra range de páginas (atual ±2)
  - Contador: "Página X de Y | Total: Z posts"
  - Preserva filtros na navegação

### 2. **Visualização Detalhada** (`/posts/{id}`)
- **Seção Principal:**
  - Badge de tipo colorido
  - Nome do perfil (link para perfil)
  - Botão "Ver no Instagram"
  - Preview de imagem (placeholder)
  - Caption completa
  - 4 Métricas Destacadas:
    - ❤️ Likes
    - 💬 Comentários
    - 👁️ Views
    - 📊 Taxa de Engajamento (com cores dinâmicas)
  - Informações técnicas:
    - ID Externo
    - Data de coleta

- **Sidebar:**
  - Card do Perfil:
    - Username e nome completo
    - Link para perfil completo
    - Link para analytics do perfil
  - Posts Relacionados:
    - Até 6 posts do mesmo perfil
    - Preview com caption truncada
    - Métricas inline compactas

- **Ações:**
  - Botão Voltar
  - Botão Deletar (com modal de confirmação)

### 3. **Backend Controller** (`PostsController`)
- **Endpoints Implementados:**
  - `GET /posts` - Lista com filtros e paginação
  - `GET /posts/{id}` - Visualização detalhada
  - `GET /posts/compare` - Comparar posts (preparado para futuro)
  - `POST /posts/{id}/delete` - Deletar post

- **Funcionalidades do Controller:**
  - Filtros dinâmicos (perfil, tipo, ordenação)
  - Cálculo de estatísticas agregadas
  - Paginação Spring Data
  - Taxa de engajamento calculada
  - Posts relacionados por perfil

### 4. **Repository Enhancements** (`ContentRepository`)
Novos métodos adicionados:
```java
Page<Content> findByProfile(Profile profile, Pageable pageable);
Page<Content> findByType(ContentType type, Pageable pageable);
Page<Content> findByProfileAndType(Profile profile, ContentType type, Pageable pageable);
```

---

## 🗂️ Arquitetura

### Controller
```java
@Controller
@RequestMapping("/posts")
public class PostsController {
    // Listagem com filtros
    @GetMapping
    public String listPosts(...);
    
    // Visualização detalhada
    @GetMapping("/{id}")
    public String viewPost(...);
    
    // Comparação de posts
    @GetMapping("/compare")
    public String comparePosts(...);
    
    // Deletar post
    @PostMapping("/{id}/delete")
    public String deletePost(...);
}
```

### Templates
- **`posts/list.html`** - Grid de posts com filtros e paginação
- **`posts/view.html`** - Detalhes completos do post

### Navegação
- Menu principal atualizado com link "Posts"
- Ícone: `bi-grid-3x3-gap`

---

## 📊 Funcionalidades Detalhadas

### Filtros Inteligentes
```java
// Combinações possíveis:
/posts                              // Todos os posts
/posts?profile=oncallpeds           // Posts de um perfil
/posts?type=REEL                    // Apenas reels
/posts?profile=oncallpeds&type=POST // Posts de um perfil
/posts?sort=likes                   // Ordenado por likes
/posts?profile=oncallpeds&sort=views&type=REEL // Combinado
```

### Ordenação Disponível
```java
private Sort getSorting(String sort) {
    return switch (sort) {
        case "likes"    -> Sort.by("likesCount").descending();
        case "comments" -> Sort.by("commentsCount").descending();
        case "views"    -> Sort.by("viewsCount").descending();
        case "oldest"   -> Sort.by("collectedAt").ascending();
        default         -> Sort.by("collectedAt").descending();
    };
}
```

### Cálculo de Estatísticas
```java
private Map<String, Object> calculateStats(List<Content> contents) {
    long totalLikes = contents.stream()
        .mapToLong(c -> c.getLikesCount() != null ? c.getLikesCount() : 0)
        .sum();
    
    stats.put("totalLikes", totalLikes);
    stats.put("avgLikes", totalLikes / (double) contents.size());
    // ... similar para comments e views
}
```

### Taxa de Engajamento
```java
private double calculateEngagementRate(Content content) {
    if (content.getViewsCount() == null || content.getViewsCount() == 0) {
        return 0.0;
    }
    long likes = content.getLikesCount() != null ? content.getLikesCount() : 0;
    return (likes * 100.0) / content.getViewsCount();
}
```

**Classificação por cores:**
- 🟢 Verde: ≥ 5% (excelente)
- 🟡 Amarelo: 2-5% (bom)
- ⚫ Cinza: < 2% (regular)

---

## 🎨 Design e UX

### Cores de Badges
```java
POST  -> badge bg-primary (azul)   📷
REEL  -> badge bg-danger (vermelho) 🎬
STORY -> badge bg-warning (amarelo) ⚡
```

### Responsividade
```html
<!-- Desktop: 3 colunas -->
<div class="col-lg-4">

<!-- Tablet: 2 colunas -->
<div class="col-md-6">

<!-- Mobile: 1 coluna -->
<div class="col-md-6 col-lg-4">
```

### Efeitos Visuais
```css
.hover-lift {
    transition: transform 0.2s, box-shadow 0.2s;
}
.hover-lift:hover {
    transform: translateY(-5px);
    box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
}
```

---

## 🚀 Como Usar

### 1. Acessar Lista de Posts
```
http://localhost:8080/posts
```

### 2. Filtrar Posts
- **Por Perfil:** Selecione no dropdown "Perfil"
- **Por Tipo:** Selecione POST, REEL ou STORY
- **Ordenar:** Escolha critério de ordenação
- **Limpar:** Clique em "Limpar" para remover filtros

### 3. Visualizar Detalhes
- Clique em "Ver Detalhes" em qualquer card
- Ou acesse diretamente: `/posts/{uuid}`

### 4. Ver no Instagram
- Clique em "Ver no Instagram" para abrir o post original
- Botão disponível tanto na listagem quanto nos detalhes

### 5. Deletar Post
- Na página de detalhes, clique em "Deletar"
- Confirme no modal de exclusão
- Redirecionamento automático para `/posts`

---

## 📡 Exemplos de URLs

### Filtros Combinados
```bash
# Posts do perfil "oncallpeds"
http://localhost:8080/posts?profile=oncallpeds

# Apenas Reels
http://localhost:8080/posts?type=REEL

# Reels do oncallpeds ordenados por likes
http://localhost:8080/posts?profile=oncallpeds&type=REEL&sort=likes

# Posts mais antigos
http://localhost:8080/posts?sort=oldest

# Página 2 com filtros
http://localhost:8080/posts?page=1&profile=oncallpeds&type=POST
```

### Visualização Detalhada
```bash
# Ver post específico
http://localhost:8080/posts/550e8400-e29b-41d4-a716-446655440000
```

---

## 🔧 Customizações Possíveis

### Adicionar Busca por Texto
```java
@GetMapping
public String listPosts(..., @RequestParam(required = false) String search) {
    if (search != null && !search.isEmpty()) {
        contentPage = contentRepository.findByCaptionContaining(search, pageable);
    }
}
```

### Adicionar Filtro por Data
```java
@GetMapping
public String listPosts(..., 
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
    // Filtrar por range de datas
}
```

### Exportar Posts Filtrados
```java
@GetMapping("/export")
public ResponseEntity<byte[]> exportPosts(...) {
    // Gerar CSV/Excel com posts filtrados
}
```

### Adicionar Favoritos
```java
// No Content.java
private boolean favorite;

@PostMapping("/{id}/favorite")
public String toggleFavorite(@PathVariable UUID id) {
    // Marcar/desmarcar como favorito
}
```

---

## 📊 Casos de Uso

### 1. **Análise de Conteúdo**
- Filtrar posts por tipo
- Ordenar por engajamento
- Identificar posts de alto desempenho

### 2. **Auditoria de Dados**
- Verificar posts coletados
- Validar métricas capturadas
- Deletar posts duplicados ou inválidos

### 3. **Comparação de Performance**
- Ver posts do mesmo perfil
- Comparar diferentes tipos (POST vs REEL)
- Analisar tendências temporais

### 4. **Pesquisa e Descoberta**
- Buscar posts específicos
- Explorar conteúdo por perfil
- Navegar posts relacionados

---

## ✅ Checklist de Implementação

- [x] PostsController criado
- [x] ContentRepository atualizado (métodos de paginação)
- [x] Template posts/list.html criado
- [x] Template posts/view.html criado
- [x] Filtros por perfil implementados
- [x] Filtros por tipo implementados
- [x] Ordenação múltipla implementada
- [x] Paginação completa
- [x] Estatísticas agregadas
- [x] Taxa de engajamento calculada
- [x] Posts relacionados
- [x] Modal de confirmação de exclusão
- [x] Menu de navegação atualizado
- [x] Design responsivo
- [x] Efeitos hover
- [x] Badges coloridos por tipo
- [x] Links para perfil e analytics
- [x] Breadcrumb de navegação

---

## 🎯 Próximos Passos

### Features Futuras

1. **Busca Avançada**
   - Busca full-text em captions
   - Filtro por hashtags
   - Busca por mentions

2. **Comparação de Posts**
   - Selecionar múltiplos posts
   - Gráfico comparativo
   - Análise side-by-side

3. **Bulk Actions**
   - Deletar múltiplos posts
   - Exportar seleção
   - Marcar como favoritos

4. **Visualização de Mídia**
   - Download de thumbnails
   - Preview de imagens
   - Player de vídeo inline

5. **Filtros Avançados**
   - Range de métricas (ex: 1000-5000 likes)
   - Filtro por data de coleta
   - Filtro por taxa de engajamento

6. **Tags e Categorias**
   - Adicionar tags personalizadas
   - Categorizar posts
   - Filtrar por tags

7. **Comentários e Notas**
   - Adicionar notas aos posts
   - Comentários internos
   - Histórico de observações

---

## 🔗 Navegação do Sistema

### Menu Principal
- **Home** → `/`
- **Perfis** → `/profiles`
- **Posts** → `/posts` ⭐ NOVO
- **Analytics** → `/analytics`
- **Admin** → `/admin`
- **Console** → `/h2-console`

### Fluxos de Navegação

**Descoberta de Posts:**
```
Home → Posts → Filtrar → Ver Detalhes → Ver no Instagram
```

**Análise de Perfil:**
```
Posts → Filtrar por Perfil → Ver Detalhes → Ver Perfil Completo
```

**Gerenciamento:**
```
Posts → Ver Detalhes → Deletar → Confirmação → Posts
```

**Analytics:**
```
Posts → Ver Detalhes → Ver Analytics → Dashboard Completo
```

---

## 📸 Componentes da Interface

### Lista de Posts
- **Header:**
  - Título com ícone
  - Subtítulo explicativo
- **Stats Cards:**
  - 4 cards coloridos com ícones
  - Totais e médias
- **Filtros:**
  - 3 selects (Perfil, Tipo, Ordenação)
  - Botão limpar
  - Auto-submit em mudança
- **Grid:**
  - Cards responsivos
  - Badges de tipo
  - Métricas inline
  - Botões de ação
- **Paginação:**
  - Navegação completa
  - Contador de itens

### Visualização Detalhada
- **Main Content:**
  - Breadcrumb
  - Header com tipo e perfil
  - Preview de imagem
  - Caption completa
  - 4 cards de métricas
  - Informações técnicas
  - Botões de ação
- **Sidebar:**
  - Card do perfil
  - Lista de relacionados
- **Modal:**
  - Confirmação de exclusão

---

## 🎉 Conclusão

**Menu Posts 100% Funcional!**

✅ **Listagem Completa** - Grid responsivo com 12 posts/página  
✅ **Filtros Avançados** - Perfil, Tipo, Ordenação  
✅ **Paginação Inteligente** - Com preservação de filtros  
✅ **Visualização Detalhada** - Todas as informações e métricas  
✅ **Posts Relacionados** - Navegação contextual  
✅ **Estatísticas** - Totais e médias em tempo real  
✅ **Taxa de Engajamento** - Com indicação visual por cores  
✅ **Design Profissional** - Bootstrap 5 + efeitos modernos  
✅ **Mobile-First** - Totalmente responsivo  

**O Inscrepper agora tem navegação completa de posts!** 📸🚀

---

## 📚 Documentação Técnica

### Models Utilizados
- `Content` - Entidade principal de post
- `Profile` - Perfil associado
- `ContentType` - Enum (POST, REEL, STORY)

### Repositories
- `ContentRepository` - CRUD e queries customizadas
- `ProfileRepository` - Busca de perfis

### Services
- Nenhum service específico (controller direto)

### Templates
- `layout/base.html` - Layout base (atualizado)
- `posts/list.html` - Lista de posts
- `posts/view.html` - Detalhes do post

### Dependências
- Spring Boot Web
- Spring Data JPA
- Thymeleaf
- Bootstrap 5.3.2
- Bootstrap Icons 1.11.1

---

**Sistema completo de navegação de posts pronto para produção!** 🎊
