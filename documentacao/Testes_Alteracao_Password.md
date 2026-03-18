# Relatório de Implementação: Alteração de Password e Testes de Integração

## 📑 Resumo da Tarefa
Implementação do fluxo de alteração de password de utilizador, garantindo a validação da identidade através da password atual, encriptação segura com **BCrypt** e persistência via **Hibernate**. A tarefa incluiu a criação de uma suite de testes de integração para cobrir cenários de sucesso e falha.

---

## 🛠️ Alterações e Componentes Criados

### 1. DTO: `ChangePasswordRequest`
Criado para mapear e validar o *payload* de entrada.
*   **Campos:** `currentPassword` (password antiga) e `newPassword` (nova password).
*   **Objetivo:** Garantir que os dados chegam ao servidor de forma estruturada antes do processamento.

### 2. Endpoint no `UserController`
Adicionado o endpoint `PUT /users/{userId}/password` com a seguinte lógica:
*   **Validação:** Utiliza o `PasswordEncoder` para comparar a password enviada com o hash guardado na base de dados.
*   **Segurança:** Caso a password atual esteja incorreta, o sistema interrompe a operação e retorna `401 Unauthorized`.
*   **Persistência:** Após validação, gera um novo hash BCrypt para a `newPassword` e atualiza a entidade via Spring Data JPA.

---

## 🧪 Suite de Testes Automatizados
Os testes foram implementados na classe `ChangePasswordIntegrationTest` para garantir a resiliência do sistema.

### Cenários de Sucesso
*   **`shouldReturn200AndChangePassword...`**:
    *   Confirma que, ao fornecer a password antiga correta, o sistema retorna `200 OK`.
    *   Verifica via Hibernate que a password na BD foi alterada (o novo hash é funcionalmente diferente do antigo).
*   **`shouldAllowLoginWithNewPassword...`**:
    *   Fluxo completo: altera a password e tenta fazer login no endpoint `/api/auth/login`.
    *   Garante que a nova password é aceite e que a antiga passa a ser rejeitada (`401 Unauthorized`).

### Cenários de Erro e Segurança
*   **`shouldReturn401WhenCurrentPasswordIsIncorrect`**:
    *   Simula uma tentativa de alteração com a password atual errada.
    *   Valida o retorno `401 Unauthorized`.
    *   Garante que a password na base de dados **não sofreu qualquer alteração**, mantendo a integridade da conta.

---

## 📊 Matriz de Validação Técnica

| Requisito | Validação Realizada | Resultado |
| :--- | :--- | :---: |
| **Identidade** | Verificação de `currentPassword` vs Hash BD | ✅ Passou |
| **Segurança** | Encriptação da nova password com BCrypt | ✅ Passou |
| **Integridade** | Rejeição de credenciais antigas após troca | ✅ Passou |
| **Persistência** | Atualização via Hibernate/Spring Data | ✅ Passou |
| **Robustez** | Retorno 401 para passwords atuais incorretas | ✅ Passou |

---
> **Nota:** Esta implementação segue as melhores práticas de segurança (OWASP), nunca expondo passwords em texto limpo e exigindo prova de conhecimento da password anterior antes de qualquer modificação.