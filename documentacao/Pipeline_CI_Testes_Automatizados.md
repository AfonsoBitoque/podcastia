# 🛠️ Infraestrutura de Build e Qualidade

Este projeto utiliza duas pipelines de Integração Contínua (CI) e ferramentas de análise estática para garantir a
estabilidade e segurança do código.

---

## 1. Integração Contínua (CI) - GitHub Actions

### 1.1 Backend — `maven-ci.yml`
* **Ficheiro de Configuração:** `.github/workflows/maven-ci.yml`
* **Gatilhos:** `push` em **qualquer ramo** (`**`); `pull_request` para `main` ou `master`.
* **Fluxo de Execução:**
    1. Provisionamento de uma máquina virtual **Ubuntu**.
    2. Instalação e configuração do **Java JDK 17** (distribuição Temurin, com cache Maven).
    3. Execução do comando de validação completa: `mvn -B verify` (dentro de `./servidor`).
* **Objetivo:** Impedir que código com erros de compilação, testes falhados ou violações de estilo seja fundido na base
principal. Um "X" vermelho no GitHub indicará falhas que devem ser corrigidas antes do merge.

### 1.2 Frontend — `frontend-ci.yml`
* **Ficheiro de Configuração:** `.github/workflows/frontend-ci.yml`
* **Gatilhos:** `push` ou `pull_request` para `main`/`master` com alterações em `frontend/**`.
* **Fluxo de Execução:**
    1. Provisionamento de uma máquina virtual **Ubuntu**.
    2. Instalação do **Node.js 20.x** com cache npm.
    3. `npm ci` — instalação de dependências determinística.
    4. `npm run lint` — verificação ESLint.
    5. `npm run test:ci` — testes unitários/componente (Vitest, modo CI).
    6. `npx playwright install --with-deps` + `npm run test:e2e` — testes E2E.
    7. Upload do relatório Playwright como artefacto (retido 30 dias).

---

## 2. Suite de Testes Automatizados (Backend)
A aplicação possui uma base sólida de testes de integração, garantindo que novas funcionalidades não quebrem o que já existe.

* **Frameworks Utilizados:**
    * **JUnit 5:** Para testes unitários e de integração.
    * **Spring Security Test / MockMvc:** Para validar fluxos de autenticação e autorização.
    * **Mockito:** Para simulação (mocking) de dependências em testes unitários.
* **Base de dados de testes:** H2 em memória, isolada por contexto de teste.

### Testes disponíveis

| Ficheiro | Tipo | O que cobre |
| :--- | :--- | :--- |
| `ServidorApplicationTests.java` | Context Load | Arranque correto do contexto Spring Boot |
| `UserRegistrationTest.java` | Integração | Criação de utilizador, conflitos, validações |
| `ChangePasswordIntegrationTest.java` | Integração | Alteração de password com BCrypt |
| `AuthIntegrationTest.java` | Integração | Login (email + username#tag), JWT, endpoints protegidos |
| `UserRelationshipServiceTest.java` | Unitário | Pedidos de amizade, cooldown, bloqueios (Mockito) |
| `UserRelationIntegrationTest.java` | Integração | Ciclo completo de amizades via API REST |
| `FeedIntegrationTest.java` | Integração | Filtros do feed (categoria, shorts, favoritos) |
| `PlaylistIntegrationTest.java` | Integração | CRUD de playlists e episódios |
| `ChatMessageServiceTest.java` | Unitário | Envio, rate limiting, links, permissões |
| `ChatReactionServiceTest.java` | Unitário | Adicionar/remover/atualizar reações |
| `RssServiceTest.java` | Integração | Consumo de feed RSS, deduplicação, fallbacks |
| `OpenApiIntegrationTest.java` | Integração | Swagger UI e `/v3/api-docs` acessíveis sem autenticação |

* **Como executar localmente:**
    ```bash
    ./mvnw test
    ```
* **Executar um teste específico:**
    ```bash
    ./mvnw test -Dtest=UserRegistrationTest
    ```

---

## 3. Verificação de Qualidade (Quality Gates)
Para além dos testes funcionais, o projeto integra ferramentas automáticas de auditoria:

### 🖋️ Estilo de Código (Checkstyle)
Utilizamos o `maven-checkstyle-plugin` configurado com as regras do **Google (`google_checks.xml`)**.
* **O que valida:** Javadoc, indentação, padrões de nomes de variáveis e organização de imports.
* **Resultado:** Garante que o código escrito por diferentes programadores mantenha a mesma estética e legibilidade.

### 🛡️ Segurança (OWASP Dependency Check)
O plugin `dependency-check-maven` analisa todas as bibliotecas declaradas no `pom.xml`.
* **O que faz:** Cruza as dependências do projeto com bases de dados de vulnerabilidades conhecidas (CVEs).
* **Importância:** Alerta proativamente se alguma biblioteca precisar de atualização devido a falhas de segurança críticas.

---

## 🚀 Comandos Úteis de Build

| Objetivo | Comando |
| :--- | :--- |
| **Validar tudo** (estilo + testes + build) | `./mvnw verify` |
| **Executar apenas testes** | `./mvnw test` |
| **Verificar vulnerabilidades** | `./mvnw dependency-check:check` |
| **Limpar a pasta target** | `./mvnw clean` |