# Projeto CI/CD - DevOps Engineer (GitLab + AWS ECS Fargate)

Este repositório contém a solução do teste prático para a vaga de DevOps Engineer. O objetivo do projeto é demonstrar a construção de um pipeline automatizado, seguro e repetível utilizando GitLab CI/CD para realizar o deploy de uma aplicação Spring Boot no AWS ECS Fargate.

---

## 🏗 Arquitetura da Solução

A arquitetura foi desenhada para ser serverless, escalável e segura. 

**Fluxo macro do Pipeline:**
1. **Developer** faz o push para o repositório GitLab.
2. **GitLab CI** executa o build (Maven), testes unitários com publicação de relatórios JUnit e análises de segurança (SAST e Trivy).
3. Após o quality gate de segurança e testes, o **Terraform (IaC)** planeja e aplica a infraestrutura (ECR, ECS Fargate, ALB, Target Groups, Security Groups e Roles).
4. O pipeline constrói a imagem Docker (multi-stage) e envia com versionamento rastreável (`$CI_COMMIT_SHA` e `latest`) para o **Amazon ECR**.
5. O deploy é realizado no **AWS ECS Fargate** atrás de um **Application Load Balancer (ALB)**, inicialmente em ambiente de Homologação (`hml`) de forma automática e, mediante aprovação manual, em Produção (`prd`).

**Diagrama simplificado:**
```text
[GitLab CI/CD] 
   ├──> (Build & Tests) -> JUnit Reports XML
   ├──> (Security) -> GitLab SAST & Trivy (FS/Dependency Scan)
   ├──> (IaC - Terraform) -> ALB, ECR, ECS Fargate (hml / prd)
   ├──> (Docker Build & Push) -> Amazon ECR
   └──> (Deploy ECS) 
          └──> Application Load Balancer (ALB) -> AWS ECS Fargate
```

---

## 🛠 Decisões Técnicas e Trade-offs

* **Estratégia de Deploy (Rolling Update):** Optou-se pelo rolling update nativo do ECS com `aws ecs update-service --force-new-deployment`.
  * **Trade-off:** É mais simples de implementar via CLI/Terraform e não requer os componentes extras de um CodeDeploy (Blue/Green). A desvantagem é que um rollback manual ou automático é ligeiramente mais lento do que apenas virar o peso do tráfego em um Target Group.

* **Ferramentas de Segurança:**
  * **SAST:** Utilizamos o template oficial de SAST do GitLab para análise estática de vulnerabilidades no código-fonte.
  * **Dependency / Container Scanning:** Foi escolhido o **Trivy**, configurado para falhar o pipeline (`--exit-code 1`) apenas em vulnerabilidades de severidade `CRITICAL`.
  * **Trade-off:** Evita que a esteira seja bloqueada por falsos positivos ou vulnerabilidades de menor impacto sem correção imediata disponível, equilibrando segurança corporativa e agilidade de entrega.

* **Infraestrutura como Código (IaC - Terraform):**
  * O estado é armazenado de forma remota no **Amazon S3** com locking nativo (`use_lockfile = true`).
  * Separação de ambientes (`hml` e `prd`) utilizando `terraform.workspace`, garantindo paridade total entre ambientes e reaproveitamento de código.
  * Inclusão de **Application Load Balancer (ALB)** gerenciado via Terraform com health checks no Actuator (`/actuator/health`) e Security Groups restritivos (ECS aceita requisições apenas originadas do ALB).

* **Gestão de Configuração e Segredos:** Configurações externas (ex: `app.feature-flag`, `APP_MESSAGE`) são injetadas fora da imagem através de variáveis de ambiente/Task Definition no ECS (ou AWS SSM Parameter Store em ambiente produtivo), nunca hardcoded no código ou Dockerfile.

---

## 📖 Documentação da API (Swagger / OpenAPI)

A aplicação conta com documentação interativa completa gerada via **Springdoc OpenAPI (Swagger 3)**.

### URLs de Acesso:
* **Swagger UI (Interface Web Interativa):**
  * Local: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
  * Nuvem (AWS ALB): `http://<ALB_DNS_NAME>/swagger-ui/index.html`
* **OpenAPI Spec (JSON):**
  * Local: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
  * Nuvem (AWS ALB): `http://<ALB_DNS_NAME>/v3/api-docs`

---

## 🔌 Endpoints da Aplicação

### 1. Negócio (CRUD em Memória com Validação e Regras de Borda)

| Método | Endpoint | Descrição | Regras / Validações |
|---|---|---|---|
| `GET` | `/api/produtos` | Lista todos os produtos | Retorna lista com status 200 |
| `GET` | `/api/produtos/{id}` | Busca produto por ID | Retorna 200 ou 404 Not Found |
| `POST` | `/api/produtos` | Cadastra novo produto | 400 Bad Request se nome for vazio ou preço < 0 |
| `PUT` | `/api/produtos/{id}` | Atualiza produto existente | 400 Bad Request em dados inválidos, 404 se não existir |
| `DELETE` | `/api/produtos/{id}` | Remove produto | 204 No Content se removido, 404 se não existir |
| `GET` | `/api/produtos/config` | Exibe configurações externas | Demonstra leitura de variáveis de ambiente (`@Value`) |
| `GET` | `/api/clientes` | Lista todos os clientes | Retorna lista de clientes cadastrados |
| `GET` | `/api/clientes/{id}` | Busca cliente por ID | Retorna 200 ou 404 Not Found |
| `POST` | `/api/clientes` | Cadastra novo cliente | Retorna 200 com objeto criado |
| `PUT` | `/api/clientes/{id}` | Atualiza cliente | Retorna 200 ou 404 Not Found |
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

## ⚠️ Limitações Conhecidas e Próximos Passos (Se houvesse mais tempo)

* **Banco de Dados Real:** A aplicação atualmente utiliza persistência em memória para facilitar os testes unitários da pipeline. Em um cenário real, integraria a aplicação a um Amazon RDS (PostgreSQL) ou Aurora Serverless, gerenciando as credenciais no AWS Secrets Manager.
* **Deploy Blue/Green:** Implementaria a estratégia Blue/Green com AWS CodeDeploy e dois Target Groups no ALB. Isso permitiria validações isoladas em produção antes da virada do tráfego e rollbacks instantâneos.
* **Observabilidade Avançada:** O health check atual garante que o container subiu, mas, no futuro, adicionaria instrumentação com AWS X-Ray, Datadog ou Prometheus/Grafana e OpenTelemetry para tracing distribuído e métricas de APM.
* **Autenticação OIDC:** Para o CI/CD na AWS, substituiria o uso de chaves IAM fixas por OpenID Connect (OIDC) entre o GitLab e a AWS IAM Role, eliminando a necessidade de credenciais de longo prazo.
