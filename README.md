# Projeto CI/CD - DevOps Engineer (GitLab + AWS ECS Fargate)

Este repositório contém a solução do teste prático para a vaga de DevOps Engineer. O objetivo do projeto é demonstrar a construção de um pipeline automatizado, seguro e repetível utilizando GitLab CI/CD para realizar o deploy de uma aplicação Spring Boot no AWS ECS Fargate.

---

## 🏗 Arquitetura da Solução

A arquitetura foi desenhada para ser serverless, escalável e segura. 

**Fluxo macro do Pipeline:**
1. **Developer** faz o push para o repositório GitLab.
2. **GitLab CI** executa o build (Maven), testes unitários e análises de segurança (SAST e Container Scanning).
3. Após o quality gate, o CI constrói a imagem Docker (multi-stage) e envia para o **Amazon ECR**.
4. O pipeline executa o **Terraform (IaC)** para planejar e aplicar a infraestrutura.
5. O deploy é feito no **AWS ECS Fargate**, inicialmente em ambiente de Homologação (`hml`) e, mediante aprovação manual, em Produção (`prd`).

**Diagrama simplificado:**
```text
[GitLab CI/CD] 
   ├──> (Build & Tests) -> JUnit Reports
   ├──> (Security) -> GitLab SAST & Trivy
   ├──> (Push) -> Amazon ECR
   └──> (Deploy via ECS/Terraform) 
          └──> AWS ECS Fargate (hml / prd)
                 └──> Application Load Balancer (ALB)
```

## 🛠 Decisões Técnicas e Trade-offs
Estratégia de Deploy (Rolling Update): Optou-se pelo rolling update nativo do ECS.

**Trade-off**: É mais simples de implementar via CLI/Terraform e não requer os componentes extras de um CodeDeploy (Blue/Green). A desvantagem é que um rollback manual ou automático é ligeiramente mais lento do que apenas virar o peso do tráfego em um Target Group.

## Ferramentas de Segurança:

Utilizamos o SAST do GitLab para análise estática por sua integração nativa.

Para container scanning, foi escolhido o Trivy, configurado para falhar o pipeline (exit-code 1) apenas em vulnerabilidades CRITICAL.

**Trade-off**: Isso evita que a esteira seja bloqueada por falsos positivos ou vulnerabilidades LOW/MEDIUM sem correção imediata disponível, equilibrando segurança e agilidade de entrega.

**Infraestrutura como Código (IaC):** O provisionamento foi feito com Terraform.

O estado é armazenado de forma remota e segura (S3 + DynamoDB para state lock).

A separação de ambientes (hml e prd) foi feita utilizando terraform.workspace, permitindo o reaproveitamento do código e garantindo a paridade entre os ambientes.

Gestão de Segredos: Configurações externas (ex: APP_FEATURE_FLAG) são passadas via variáveis de ambiente injetadas pela Task Definition (através do AWS Systems Manager Parameter Store na infraestrutura real). Nunca hardcoded no repositório.

## 🚀 Como Rodar e Testar Localmente
A aplicação é um microserviço Spring Boot com dois endpoints REST em memória (Clientes e Produtos) e o Actuator habilitado.

Pré-requisitos: Java 17, Maven e Docker.

1. Rodando a aplicação via Maven:
```text
Bash
mvn clean install
mvn spring-boot:run
```

2. Testando os endpoints localmente:

**Health Check (Actuator):** curl http://localhost:8080/actuator/health

**Build Info (Actuator):** curl http://localhost:8080/actuator/info

**Listar Produtos:** curl http://localhost:8080/api/produtos

**Listar Clientes:** curl http://localhost:8080/api/clientes

3. Rodando via Docker:
```text
Bash
docker build -t devops-app:local .
docker run -p 8080:8080 -e APP_FEATURE_FLAG=true devops-app:local
```

## ⚠️ Limitações Conhecidas e Próximos Passos (Se houvesse mais tempo)
Banco de Dados Real: A aplicação atualmente utiliza persistência em memória para facilitar os testes unitários da pipeline. Em um cenário real, integraria a aplicação a um Amazon RDS (PostgreSQL) ou DynamoDB, gerenciando as credenciais no AWS Secrets Manager.

Deploy Blue/Green: Implementaria a estratégia Blue/Green com AWS CodeDeploy. Isso permitiria validações isoladas em produção antes da virada do tráfego e rollbacks instantâneos.

Observabilidade Avançada: O health check atual garante que o container subiu, mas, no futuro, adicionaria instrumentação com o AWS X-Ray, Datadog ou Prometheus/Grafana para métricas customizadas e tracing distribuído.

Autenticação OIDC: Para o CI/CD na AWS, substituiria o uso de chaves IAM por OpenID Connect (OIDC) entre o GitLab e a AWS, eliminando a necessidade de rotacionar credenciais de longo prazo.
