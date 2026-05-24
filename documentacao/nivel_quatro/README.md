# C4 Nível 4 — Índice

Diagramas de código gerados a partir da leitura directa do código-fonte do projecto Podcastia.  
Todos os diagramas utilizam **Mermaid JS** (`classDiagram`, `sequenceDiagram`, `flowchart`, `stateDiagram`).

| Ficheiro | Conteúdo |
|---|---|
| [`01_domain_model.md`](./01_domain_model.md) | Todas as entidades JPA, enumerações e relações (modelo de domínio completo) |
| [`02_security_layer.md`](./02_security_layer.md) | JWT, `JwtAuthenticationFilter`, `SecurityConfig`, `WebSocketConfig`, fluxos de autenticação |
| [`03_service_layer.md`](./03_service_layer.md) | Interfaces de serviço, implementações, dependências e regras de negócio chave |
| [`04_repository_layer.md`](./04_repository_layer.md) | Repositórios Spring Data JPA, queries JPQL personalizadas e mapa serviço→repositório |
| [`05_controller_layer.md`](./05_controller_layer.md) | Controllers REST e WebSocket, endpoints, parâmetros e dependências |
| [`06_sequences.md`](./06_sequences.md) | Diagramas de sequência dos 8 fluxos principais (login, registo, geração IA, chat, amizades, scheduler, RSS, WS) |

## Estrutura de Pacotes

```
com.jep.servidor
├── config/          → SecurityConfig, JwtUtil, JwtAuthFilter, WebSocketConfig, OpenApiConfig, ...
├── controller/      → 17 controllers REST + 1 WebSocket
├── service/         → interfaces + implementações (13 serviços)
├── repository/      → 13 repositórios Spring Data JPA
├── model/           → 16 entidades JPA + enumerações
├── dto/             → Data Transfer Objects (request/response)
└── exceptions/      → BusinessException, ChatMessageException, FriendshipNotFoundException
```
