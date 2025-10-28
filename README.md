# 📷 Inscrepper Java

Uma aplicação moderna em **Spring Boot** para monitoramento de perfis do Instagram, com interface web responsiva e API REST integradas.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.2-purple)
![H2](https://img.shields.io/badge/Database-H2-blue)

## ✨ Características

- 🎨 **Interface Moderna**: Design minimalista com Bootstrap 5
- 📱 **Responsivo**: Layout adaptável para mobile e desktop  
- 🔄 **Monitoramento**: Acompanhamento automático de perfis do Instagram
- 🛠️ **Admin Panel**: Painel administrativo intuitivo
- 🚀 **API REST**: Endpoints para integração externa
- 🐳 **Docker Ready**: Containerização completa
- ☕ **Java 21 LTS**: Última versão LTS do Java

## 📋 Requisitos

- **Java 21+** (JDK 21 LTS)
- **Maven 3.9+**
- **Browser moderno** (Chrome, Firefox, Safari, Edge)

## 🚀 Execução Rápida

### Desenvolvimento Local
```bash
# Clone o repositório
git clone <repository-url>
cd inscrepper-java

# Execute a aplicação
mvn spring-boot:run
```

### Acessos
- 🏠 **Home**: http://localhost:8080/
- ⚙️ **Admin**: http://localhost:8080/admin
- 📊 **API Health**: http://localhost:8080/health  
- 🗄️ **H2 Console**: http://localhost:8080/h2-console

## 🛠️ Build e Deploy

### JAR Standalone
```bash
mvn clean package -DskipTests
java -jar target/inscrepper-java-1.0.0.jar
```

### Docker
```bash
# 1. Build da aplicação
mvn clean package -DskipTests

# 2. Build da imagem Docker
docker build -t inscrepper-java:latest .

# 3. Execução do container
docker run -d -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  --name inscrepper \
  inscrepper-java:latest
```

## 📁 Estrutura do Projeto

```
📦 inscrepper-java
├── 📂 src/main/
│   ├── 📂 java/com/ensyferum/inscrepper/
│   │   ├── 📄 InscrepperApplication.java     # Aplicação principal
│   │   ├── 📂 api/                           # Controllers REST
│   │   ├── 📂 model/                         # Entidades JPA
│   │   ├── 📂 repository/                    # Repositórios
│   │   ├── 📂 service/                       # Serviços de negócio
│   │   └── 📂 web/                           # Controllers Web
│   └── 📂 resources/
│       ├── 📂 static/                        # Assets estáticos
│       │   ├── 📂 css/                       # Estilos customizados
│       │   └── 📂 js/                        # JavaScript
│       └── 📂 templates/                     # Templates Thymeleaf
├── 📄 Dockerfile                            # Configuração Docker
├── 📄 pom.xml                              # Dependências Maven
└── 📄 README.md                            # Este arquivo
```

## 🎨 Tecnologias Utilizadas

### Backend
- **Spring Boot 3.2.0** - Framework principal
- **Spring Web** - APIs REST e MVC
- **Spring Data JPA** - Persistência de dados
- **Thymeleaf** - Engine de templates
- **H2 Database** - Banco de dados embarcado
- **Lombok** - Redução de boilerplate
- **Selenium** - Automação web (scraping)

### Frontend
- **Bootstrap 5.3.2** - Framework CSS
- **Bootstrap Icons** - Ícones vetoriais
- **JavaScript ES6+** - Interatividade
- **Responsive Design** - Layout adaptativo

## 🔧 Configuração

### Banco de Dados
Por padrão, usa H2 embarcado. Para outros bancos, edite `application.properties`:

```properties
# PostgreSQL exemplo
spring.datasource.url=jdbc:postgresql://localhost:5432/inscrepper
spring.datasource.username=inscrepper
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### Selenium
A aplicação inclui WebDriverManager para automação:
- Chrome/Chromium (padrão)
- Firefox
- Edge

## 🚧 Desenvolvimento

### Executar em modo desenvolvimento
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Live Reload
O Spring Boot DevTools está incluído para recarregamento automático durante desenvolvimento.

### Debug
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## 🌐 API Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/health` | Status da aplicação |
| `GET` | `/api/profiles` | Listar perfis |
| `POST` | `/api/profiles` | Criar perfil |
| `DELETE` | `/api/profiles/{id}` | Excluir perfil |

## 🐳 Docker Compose

```yaml
version: '3.8'
services:
  inscrepper:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

## 📊 Monitoramento

- **Health Check**: `/health` - Status da aplicação
- **Metrics**: Integração com Spring Actuator (configurável)
- **Logs**: Estruturados em JSON (configurável)

## 🤝 Contribuição

1. Fork o projeto
2. Crie sua feature branch (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -am 'Add: nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está sob licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 📞 Suporte

- 🐛 **Issues**: [GitHub Issues](issues)
- 📧 **Email**: suporte@inscrepper.com
- 📖 **Docs**: [Documentação completa](docs)

---

**Desenvolvido com ❤️ usando Spring Boot e Java 21**
