# Documentação Técnica: POD-66 (US-2-1)
## Implementação de Fluxo de Registo e Validação de Utilizadores

Esta tarefa focou-se na implementação da lógica de criação de conta, garantindo a segurança das credenciais e a
integridade dos dados através de validações rigorosas e testes de integração.

---

### 🛠️ Alterações de Infraestrutura e Dependências

* **`pom.xml`**: Adicionada a dependência `spring-boot-starter-validation`.
    * *Objetivo:* Permitir a validação declarativa (Bean Validation) nos modelos de dados.

### 🔐 Segurança (SecurityConfig.java)
Implementada a camada de segurança inicial para proteger os dados sensíveis:
* **BCrypt**: Configurado `BCryptPasswordEncoder` como o standard para hashing de passwords.
* **Permissões**:
    * Acesso público ao endpoint de registo (`/users`).
    * Acesso livre à consola **H2** para facilitar o desenvolvimento e debug.

### 🏗️ Camada de Modelo (User.java)
Adicionadas restrições de integridade diretamente na entidade:
- `@NotBlank`: Aplicado a todos os campos obrigatórios.
- `@Email`: Garante o formato correto do endereço de correio eletrónico.
- `@Size(min = 4, max = 4)`: Restrição específica para o campo `tag`.

### 🎮 Controlador (UserController.java)
O controlador foi atualizado para gerir a lógica de negócio de registo:
1.  **Injeção de Dependência**: Adicionado `PasswordEncoder`.
2.  **Validação Ativa**: Uso de `@Valid` para intercetar dados malformados antes de chegarem à lógica de persistência.
3.  **Segurança**: Implementada a encriptação da password no momento da criação do registo.

---

### 🧪 Suite de Testes (UserRegistrationTest.java)
Foi implementada uma suite de testes de integração completa para cobrir os seguintes cenários:

| Cenário | Resultado Esperado | Descrição |
| :--- | :--- | :--- |
| **Sucesso** | `201 Created` | Registo válido com password encriptada na BD. |
| **Email Duplicado** | `409 Conflict` | Impede registos com emails já existentes. |
| **Username + Tag** | `409 Conflict` | Garante a unicidade da combinação Username#Tag. |
| **Email Inválido** | `400 Bad Request` | Valida o formato do email via `@Email`. |
| **Campos Vazios** | `400 Bad Request` | Bloqueia submissões sem campos obrigatórios. |

---
> **Nota de Implementação:** As validações de conflito (409) garantem que a experiência do utilizador seja consistente com as regras de negócio de unicidade do sistema.