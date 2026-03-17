## 🛡️ Visão Geral
Esta suite de testes foi desenhada para validar o ciclo completo de autenticação e manutenção de sessão, garantindo que a integração entre **Spring Security**, **Hibernate** e **Bcrypt** está blindada contra falhas de acesso e vulnerabilidades comuns.

---

## 🎯 Objetivos dos Testes
*   **Validação de Credenciais:** Confirmar que o login apenas ocorre com dados 100% corretos.
*   **Segurança de Password:** Garantir que o sistema nunca lida com passwords em texto limpo na base de dados (uso de `PasswordEncoder`).
*   **Persistência de Sessão:** Validar a geração e o funcionamento de tokens (JWT) ou cookies.
*   **Ofuscação de Erros:** Assegurar que o sistema não dá pistas sobre a existência de e-mails para evitar ataques de enumeração.

---

## 🛠️ Detalhes da Implementação

### 1. Sucesso no Login e Retorno de Token
O teste `shouldLoginSuccessfullyWithEmail` valida o fluxo principal.
*   **Verificação:** Retorno de `200 OK`.
*   **Token:** Validamos a presença do campo `token` no JSON de resposta, essencial para a persistência da sessão no frontend.

### 2. Integração com PasswordEncoder (BCrypt)
Validado de forma transparente durante o ciclo de vida do teste:
*   No `setUp()`, o utilizador é persistido via Hibernate com a password já encriptada (`passwordEncoder.encode("password123")`).
*   No pedido de login, enviamos `"password123"`. O sucesso do teste prova que o Spring Security está a comparar corretamente o hash na BD com o input do utilizador.

### 3. Tratamento de Erros e Segurança (401 Unauthorized)
Implementámos testes rigorosos para falhas de autenticação:
*   **Password Errada:** O teste `shouldFailLoginWithIncorrectPassword` garante o retorno de `401`.
*   **Email Não Registado:** No teste `shouldFailLoginWhenUserNotFound`, o sistema retorna a mesma mensagem de erro genérica ("Credenciais inválidas"), impedindo que um atacante saiba se o e-mail existe no sistema.

### 4. Acesso a Endpoints Protegidos
Testámos a validade prática do token com o cenário `shouldAccessProtectedEndpointWithValidToken`:
1.  O utilizador faz login e extrai o token.
2.  Faz um pedido `GET` ao endpoint `/podcasts`.
3.  Envia o header `Authorization: Bearer <token>`.
4.  **Resultado esperado:** `200 OK`.

### 5. Rejeição de Tokens Inválidos
Para garantir que a "porta" fecha quando deve, adicionámos:
*   **Tokens Malformados:** Rejeição imediata de strings de token aleatórias.
*   **Tokens Expirados:** Teste manual onde criamos um JWT com data de expiração no passado, garantindo que o Spring Security bloqueia o acesso (`403 Forbidden`).

---

## 📊 Resumo de Cobertura

| Cenário de Teste | Validação | Status Esperado |
| :--- | :--- | :--- |
| Login com sucesso | Credenciais válidas + Token | `200 OK` |
| Password incorreta | Segurança de credenciais | `401 Unauthorized` |
| Email não existente | Proteção contra enumeração | `401 Unauthorized` |
| Acesso a Recurso | Token JWT válido no Header | `200 OK` |
| Token Expirado | Validade temporal do acesso | `403 Forbidden` |

---
> **Nota:** Esta suite utiliza um contexto de base de dados em memória (H2 ou similar) para garantir que os testes são isolados e rápidos.