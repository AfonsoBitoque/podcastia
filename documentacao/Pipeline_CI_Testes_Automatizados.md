# 🛠️ Infraestrutura de Build e Qualidade

Este projeto utiliza uma pipeline de Integração Contínua (CI) e ferramentas de análise estática para garantir a
estabilidade e segurança do código.

---

## 1. Integração Contínua (CI) - GitHub Actions
A pipeline está configurada para validar automaticamente cada alteração no código.

* **Ficheiro de Configuração:** `.github/workflows/maven-ci.yml`
* **Gatilhos:** `push` ou `pull_request` nos ramos `main`, `master`, ou `POD-*`.
* **Fluxo de Execução:**
    1.  Provisionamento de uma máquina virtual **Ubuntu**.
    2.  Instalação e configuração do **Java JDK 24** (ou versão compatível).
    3.  Execução do comando de validação completa: `./mvnw verify`.
* **Objetivo:** Impedir que código com erros de compilação, testes falhados ou violações de estilo seja fundido na base
principal. Um "X" vermelho no GitHub indicará falhas que devem ser corrigidas antes do merge.

---

## 2. Fundação de Testes Automatizados
A aplicação possui uma base sólida para testes, garantindo que novas funcionalidades não quebrem o que já existe.

* **Frameworks Utilizados:**
    * **JUnit 5:** Para testes unitários e de integração.
    * **Spring Security Test:** Para validar fluxos de autenticação e autorização.
    * **Mockito:** Para simulação (mocking) de dependências.
* **Teste de Sanidade:** O ficheiro `ServidorApplicationTests.java` realiza o *Context Load*, garantindo que o Spring
Boot consegue iniciar corretamente com as configurações atuais.
* **Como executar localmente:**
    ```powershell
    ./mvnw test
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