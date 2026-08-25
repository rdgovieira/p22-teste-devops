# Projeto CI/CD - DevOps Engineer (GitLab + AWS ECS Fargate)

Este repositório contém a solução completa do teste prático para a vaga de DevOps Engineer. O objetivo do projeto é demonstrar a construção de um pipeline automatizado, seguro e repetível utilizando GitLab CI/CD para realizar o deploy de uma aplicação Spring Boot no AWS ECS Fargate com infraestrutura como código (Terraform).

---

## 🏗 Arquitetura da Solução

A arquitetura foi desenhada para ser serverless, escalável e segura, cobrindo todos os requisitos e diferenciais do desafio.

**Fluxo macro do Pipeline:**
1. **Developer** faz o push para o repositório GitLab.
2. **Build & Tests (`build-test`):** Compilação Maven, execução dos testes unitários com publicação de relatórios JUnit XML no GitLab.
3. **Segurança (`security`):**
   * **SAST:** Análise estática de código com o template oficial do GitLab.
   * **Dependency Scanning:** Varredura de dependências com **Trivy FS** (falhando em vulnerabilidades `CRITICAL`).
4. **Build & Container Scanning (`build-push`):**
   * Construção da imagem Docker multi-stage (base `eclipse-temurin:17-jre-alpine`).
   * **Container Scanning:** Varredura da imagem Docker construída com **Trivy Image** antes do push.
   * Publicação da imagem no **Amazon ECR** com tag imutável amarrada ao commit (`$CI_COMMIT_SHA`).
5. **Infraestrutura como Código (`infrastructure`):**
   * Execução do **Terraform** (`plan` e `apply` com aprovação manual em `prd`).
   * Provisionamento de ALB com suporte a **HTTP (80) e HTTPS (443 via ACM)**, Target Groups, CloudWatch Log Group (`/ecs/app-task-${workspace}`), ECS Cluster, Task Definition e Service Fargate.
6. **Deploy no ECS (`deploy`):**
   * Deploy automatizado em `hml` e protegido por aprovação manual em `prd` (`when: manual`).
   * Rastreabilidade total commit → Task Definition → imagem no ECR.

**Diagrama simplificado:**
```text
[GitLab CI/CD] 
   ├──> (Build & Tests) -> JUnit Reports XML
   ├──> (Security) -> GitLab SAST & Trivy Dependency Scanning
   ├──> (Docker Build & Trivy Container Scan) -> Amazon ECR (Immutable $CI_COMMIT_SHA)
   ├──> (IaC - Terraform) -> ALB (HTTP/HTTPS + ACM), CloudWatch Logs, ECS Fargate
   └──> (Deploy ECS) 
          └──> Application Load Balancer (80/443) -> AWS ECS Fargate (hml / prd)
```

---

## 🛠 Decisões Técnicas e Trade-offs

* **Rastreabilidade e Imutabilidade de Imagens:**
  * As imagens no ECR são configuradas como `IMMUTABLE`, eliminando o uso de tags mutáveis como `latest` em deploy de produção.
  * O Terraform recebe explicitamente `-var="image_tag=$CI_COMMIT_SHA"`, garantindo que cada commit gere uma nova revisão da Task Definition perfeitamente rastreável para auditoria e rollbacks seguros.

* **Segurança e Criptografia em Trânsito:**
  * **HTTPS no ALB:** O Load Balancer foi configurado com listener HTTPS (porta 443) associado a um certificado gerenciado pelo AWS Certificate Manager (ACM), garantindo que todo o tráfego da API trafegue criptografado.
  * **Isolamento de Rede:** Os containers Fargate não aceitam conexões públicas diretas; o Security Group do ECS permite tráfego na porta 8080 exclusivamente a partir do Security Group do ALB.
  * **Trivy Dual-Scan:** Executamos análise de dependências no código (`trivy fs`) e análise de vulnerabilidades no artefato final da imagem (`trivy image`), aplicando o critério de corte de falhar o pipeline apenas em vulnerabilidades `CRITICAL`.

* **Observabilidade e Logs:**
  * A Task Definition foi instrumentada com `logConfiguration` direcionando os logs da aplicação para o **AWS CloudWatch Logs** (`/ecs/app-task-${workspace}`), permitindo troubleshooting em tempo real de containers em produção.
  * O endpoint `/actuator/health` possui `show-details=never` para evitar a exposição pública inadvertida de detalhes de infraestrutura.

* **Concorrência e Validação de Dados na Aplicação:**
  * Para evitar problemas de concorrência em ambiente multi-thread do Spring Boot (singleton beans), os controllers utilizam estruturas thread-safe: `CopyOnWriteArrayList` para coleções e `AtomicLong` para geração de IDs.
  * Padronização de validação com **Bean Validation (Jakarta Validation)** utilizando `@Valid`, `@NotBlank`, `@NotNull`, `@PositiveOrZero` e `@Email` nos modelos e controllers.

* **Infraestrutura como Código (IaC - Terraform):**
  * O estado é armazenado de forma remota no **Amazon S3** com locking nativo (`use_lockfile = true`).
  * Separação de ambientes (`hml` e `prd`) utilizando `terraform.workspace`.

---

## 📖 Documentação da API (Swagger / OpenAPI)

A aplicação conta com documentação interativa completa gerada via **Springdoc OpenAPI (Swagger 3)**.

### URLs de Acesso:
* **Swagger UI (Interface Web Interativa):**
  * Local: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
  * Nuvem (AWS ALB HTTP): `http://<ALB_DNS_NAME>/swagger-ui/index.html`
  * Nuvem (AWS ALB HTTPS): `https://<ALB_DNS_NAME>/swagger-ui/index.html`
* **OpenAPI Spec (JSON):**
  * Local: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
  * Nuvem (AWS ALB): `https://<ALB_DNS_NAME>/v3/api-docs`

---

## 🔌 Endpoints da Aplicação

### 1. Negócio (CRUD em Memória com Bean Validation e Concorrência Thread-Safe)

| Método | Endpoint | Descrição | Regras / Validações |
|---|---|---|---|
| `GET` | `/api/produtos` | Lista todos os produtos | Retorna lista com status 200 |
| `GET` | `/api/produtos/{id}` | Busca produto por ID | Retorna 200 ou 404 Not Found |
| `POST` | `/api/produtos` | Cadastra novo produto | 400 Bad Request se nome for vazio ou preço < 0 (`@Valid`) |
| `PUT` | `/api/produtos/{id}` | Atualiza produto existente | 400 Bad Request em dados inválidos, 404 se não existir |
| `DELETE` | `/api/produtos/{id}` | Remove produto | 204 No Content se removido, 404 se não existir |
| `GET` | `/api/produtos/config` | Exibe configurações externas | Demonstra leitura de variáveis de ambiente (`@Value`) |
| `GET` | `/api/clientes` | Lista todos os clientes | Retorna lista de clientes cadastrados |
| `GET` | `/api/clientes/{id}` | Busca cliente por ID | Retorna 200 ou 404 Not Found |
| `POST` | `/api/clientes` | Cadastra novo cliente | 400 Bad Request se nome ou email inválido (`@Valid`) |
| `PUT` | `/api/clientes/{id}` | Atualiza cliente | 400 Bad Request em email/nome inválido, 404 se não existir |
| `DELETE` | `/api/clientes/{id}` | Remove cliente | Retorna 204 No Content ou 404 Not Found |

### 2. Observabilidade e Monitoramento (Spring Boot Actuator)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Health check da aplicação (utilizado pelo Target Group do ALB e ECS) |
| `GET` | `/actuator/info` | Informações de build, versão e metadados da aplicação gerados no Maven |

---

## 🚀 Como Rodar e Testar Localmente

### Pré-requisitos
* Java 17 (JDK)
* Maven 3.9+
* Docker

### 1. Rodando a aplicação via Maven:
```bash
mvn clean install
mvn spring-boot:run
```

### 2. Testando os endpoints via cURL:

* **Documentação Swagger:** Acesse `http://localhost:8080/swagger-ui/index.html` no navegador.
* **Health Check:**
  ```bash
  curl http://localhost:8080/actuator/health
  ```
* **Build Info:**
  ```bash
  curl http://localhost:8080/actuator/info
  ```
* **Configuração Externa:**
  ```bash
  curl http://localhost:8080/api/produtos/config
  ```
* **Listar Produtos:**
  ```bash
  curl http://localhost:8080/api/produtos
  ```
* **Criar Produto:**
  ```bash
  curl -X POST http://localhost:8080/api/produtos \
    -H "Content-Type: application/json" \
    -d '{"nome": "Notebook Dell Inspiron", "preco": 4500.00}'
  ```

### 3. Rodando via Docker (Multi-stage build):
```bash
docker build -t devops-app:local .
docker run -p 8080:8080 -e APP_FEATURE_FLAG=true -e APP_MESSAGE="Ambiente Docker Local" devops-app:local
```

---

## ⚠️ Limitações Conhecidas e Próximos Passos

* **Banco de Dados Real:** A aplicação atualmente utiliza persistência thread-safe em memória para exercitar a pipeline de forma desacoplada. Em ambiente de produção persistente, integraria a um Amazon RDS (PostgreSQL) com credenciais no AWS Secrets Manager.
* **Deploy Blue/Green:** Implementaria a estratégia Blue/Green com AWS CodeDeploy e múltiplos Target Groups no ALB para rotação gradual e instantânea de tráfego.
* **Autenticação OIDC:** Para o CI/CD na AWS, substituiria o uso de chaves IAM por OpenID Connect (OIDC) entre o GitLab e a AWS IAM Role, eliminando credenciais de longo prazo no CI.
