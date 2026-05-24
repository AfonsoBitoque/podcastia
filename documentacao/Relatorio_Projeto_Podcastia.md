# Relatório de Projeto — Podcastia
## Plataforma de Podcasts Gerados por Inteligência Artificial

---

| Campo | Detalhe |
|---|---|
| **Projeto** | Podcastia |
| **Versão** | 1.0.0-SNAPSHOT |
| **Data do Relatório** | Maio de 2026 |
| **Autores** | Afonso Bitoque, Maria Neto, Duarte Cunha, José Tico |
| **Repositório** | https://github.com/AfonsoBitoque/podcastia |
| **Licença** | Ver ficheiro LICENSE |

---

## Índice

1. [Introdução](#1-introdução)
2. [Objetivos do Projeto](#2-objetivos-do-projeto)
3. [Requisitos do Sistema](#3-requisitos-do-sistema)
4. [Arquitetura do Sistema](#4-arquitetura-do-sistema)
5. [Tecnologias Utilizadas](#5-tecnologias-utilizadas)
6. [Modelo de Dados](#6-modelo-de-dados)
7. [Funcionalidades Implementadas](#7-funcionalidades-implementadas)
8. [API REST — Documentação de Endpoints](#8-api-rest--documentação-de-endpoints)
9. [Segurança](#9-segurança)
10. [Testes](#10-testes)
11. [Pipeline de Integração Contínua](#11-pipeline-de-integração-contínua)
12. [Análise de Qualidade e Problemas Conhecidos](#12-análise-de-qualidade-e-problemas-conhecidos)
13. [Guia de Instalação e Execução](#13-guia-de-instalação-e-execução)
14. [Decisões Arquiteturais](#14-decisões-arquiteturais)
15. [Trabalho Futuro](#15-trabalho-futuro)
16. [Conclusão](#16-conclusão)

---

## 1. Introdução

O **Podcastia** é uma plataforma web de podcasts gerados automaticamente por Inteligência Artificial, desenvolvida no âmbito de um projeto universitário. A plataforma permite a qualquer utilizador registado criar podcasts sobre qualquer tema, bastando indicar o assunto desejado: o sistema gera automaticamente um guião narrativo recorrendo à API da Google Gemini e sintetiza o áudio em língua portuguesa utilizando a ferramenta `edge-tts`.

Para além da geração de conteúdo, o Podcastia oferece um conjunto rico de funcionalidades sociais, incluindo sistema de amizades, chat privado em tempo real, playlists personalizadas, feed recomendado baseado em preferências e um painel de administração completo.

O projeto foi desenvolvido seguindo uma arquitetura cliente-servidor, com um backend em Java Spring Boot e um frontend em React, comunicando via API REST e WebSocket STOMP para funcionalidades em tempo real.

---

## 2. Objetivos do Projeto

### 2.1 Objetivos Principais

- Desenvolver uma plataforma web funcional de criação e partilha de podcasts gerados por IA
- Implementar um sistema de autenticação seguro baseado em JWT
- Criar um motor de recomendação de conteúdo baseado nas preferências do utilizador
- Disponibilizar funcionalidades sociais (amizades, chat, partilha de playlists)
- Fornecer um painel de administração com analytics e ferramentas de moderação

### 2.2 Objetivos Secundários

- Integrar consumo automático de feeds RSS de fontes externas
- Implementar playlists diárias geradas automaticamente por um agendador
- Assegurar qualidade através de testes automatizados e pipelines de CI/CD
- Documentar a arquitetura utilizando o modelo C4 em Mermaid.js

---

## 3. Requisitos do Sistema

### 3.1 Requisitos Funcionais

| ID | Requisito | Estado |
|---|---|---|
| RF01 | Registo e autenticação de utilizadores (email ou username#tag) | ✅ Implementado |
| RF02 | Geração de podcasts por IA a partir de um tema | ✅ Implementado |
| RF03 | Streaming e download de ficheiros de áudio MP3 | ✅ Implementado |
| RF04 | Feed personalizado com base em preferências do utilizador | ✅ Implementado |
| RF05 | Sistema de amizades (pedido, aceitar, rejeitar, bloquear) | ✅ Implementado |
| RF06 | Chat privado em tempo real entre utilizadores | ✅ Implementado |
| RF07 | Criação e gestão de playlists personalizadas | ✅ Implementado |
| RF08 | Playlist diária gerada automaticamente | ✅ Implementado |
| RF09 | Sistema de favoritos para podcasts | ✅ Implementado |
| RF10 | Pesquisa unificada de utilizadores e podcasts | ✅ Implementado |
| RF11 | Perfil de utilizador com imagem e onboarding | ✅ Implementado |
| RF12 | Painel de administração com analytics e moderação | ✅ Implementado |
| RF13 | Consumo automático de feeds RSS externos | ✅ Implementado |
| RF14 | Exportação de dados (CSV e PDF) pelo administrador | ✅ Implementado |
| RF15 | Reações a mensagens no chat (emojis) | ✅ Implementado |
| RF16 | Download de playlist em formato ZIP | ✅ Implementado |
| RF17 | Registo de progresso de escuta de podcasts | ✅ Implementado |
| RF18 | Moderação de conteúdo (ocultar, destacar, marcar explícito) | ✅ Implementado |

### 3.2 Requisitos Não Funcionais

| ID | Requisito | Solução Aplicada |
|---|---|---|
| RNF01 | Autenticação stateless e segura | JWT HS512, validade 24h, BCrypt para passwords |
| RNF02 | Comunicação em tempo real para chat | WebSocket STOMP com autenticação JWT no handshake |
| RNF03 | Filtros de feed dinâmicos e eficientes | JPA Specification (queries dinâmicas sem SQL concatenado) |
| RNF04 | Proteção contra abuso no chat | Rate limiting (20 msg/min, 100 msg/h), blacklist de links |
| RNF05 | Imagens de perfil com tamanho controlado | Validação ≤ 5MB, redimensionamento automático para 400×400px |
| RNF06 | Cache de recomendações | Cache in-memory `ConcurrentHashMap` (24h por utilizador) |
| RNF07 | Código com qualidade controlada | Checkstyle (Google style), OWASP Dependency Check |
| RNF08 | Persistência de dados | H2 (dev/testes), migrável para PostgreSQL/MySQL via JPA |

---

## 4. Arquitetura do Sistema

### 4.1 Visão Geral (C4 Nível 1 — System Context)

O Podcastia interage com quatro sistemas externos:

- **Google Gemini API** — geração do guião narrativo do podcast
- **edge-tts (Python)** — síntese de texto em áudio MP3 com voz portuguesa (`pt-PT-RaquelNeural`)
- **Fontes RSS Externas** — feeds de notícias/artigos consumidos a cada 2 horas (Observador, Público Desporto, TechCrunch, BBC News)
- **Serviço de Email** — envio de notificações e relatórios (atualmente em modo stub para desenvolvimento)

### 4.2 Containers (C4 Nível 2)

O sistema é composto por quatro containers principais:

| Container | Tecnologia | Responsabilidade |
|---|---|---|
| **Frontend SPA** | React 19 + Vite 7 | Interface web do utilizador; comunica com backend via REST e WebSocket STOMP |
| **Backend API** | Spring Boot 3.4.3 + Java 17 | Toda a lógica de negócio, autenticação JWT, agendadores e integrações externas |
| **Base de Dados** | H2 (dev) / SQL | Persistência de utilizadores, podcasts, playlists, mensagens, relações, logs |
| **Sistema de Ficheiros** | Disco local | Armazenamento de ficheiros MP3 gerados em `generated-podcasts/` e imagens de perfil |

### 4.3 Componentes do Backend (C4 Nível 3)

O backend está organizado em camadas bem definidas:

#### Camada de Segurança
- `SecurityConfig` — configuração do Spring Security, CORS e endpoints públicos
- `JwtAuthenticationFilter` — valida o token Bearer em cada pedido
- `JwtUtil` — geração e validação de tokens JWT HS512 com validade de 24h
- `JwtWebSocketHandshakeInterceptor` — autenticação de ligações WebSocket

#### Camada de Controllers (17 controllers)
Responsável por receber pedidos HTTP/WebSocket e delegar para os serviços:

| Controller | Base Path | Responsabilidade |
|---|---|---|
| `AuthController` | `/api/auth` | Login por email ou username#tag |
| `AuthUserController` | `/api/users` | Perfil próprio, onboarding |
| `UserController` | `/users` | CRUD de utilizadores, password, perfil público |
| `UserRelationController` | `/api/relations` | Sistema de amizades e bloqueios |
| `PodcastController` | `/podcasts` | Feed, progresso de escuta, homepage |
| `PodcastGenerationController` | `/api/podcasts` | Geração IA, streaming, download de áudio |
| `PodcastFavoriteController` | `/api/favorites` | Toggle, verificar e listar favoritos |
| `FeedController` | `/api/home` | Feed filtrado (categoria, shorts, favoritos) |
| `PlaylistController` | `/api/playlists` | CRUD playlists, episódios, download ZIP |
| `DailyPlaylistController` | `/api/daily-playlists` | Playlists diárias automáticas |
| `ChatController` | `/api/chats` | Histórico de mensagens, reações |
| `ChatWebSocketController` | STOMP `/app/chat.*` | Chat em tempo real |
| `SearchController` | `/api/search` | Pesquisa unificada |
| `TopicController` | `/api/topics` | Gestão de tópicos de interesse |
| `ProfileImageController` | `/api/profile` | Upload e gestão de imagens de perfil |
| `AdminController` | `/api/admin` | Analytics, moderação, relatórios (USER_ADMIN) |
| `RegistrationApiController` | `/api/register` | Geração e verificação de tags únicas |

#### Camada de Serviços (13 serviços)
Contém toda a lógica de negócio:

- `PodcastGenerationService` — pipeline Gemini API → edge-tts → persistência MP3
- `RecommendationService` — feed personalizado por pontos de tag, cache 24h
- `FeedService` — filtros dinâmicos com JPA Specification
- `DailyPlaylistService` — geração automática de playlists diárias
- `PlaylistService` — CRUD de playlists com reordenação atómica
- `UserRelationshipServiceImpl` — ciclo de amizades com cooldown de 7 dias
- `ChatMessageServiceImpl` — rate limiting, link blacklist, push queue WebSocket
- `AdminService` — analytics, export CSV/PDF, logs de auditoria
- `RssService` — consumo de feeds RSS com a biblioteca ROME
- `SearchService` — pesquisa paginada e agregada
- `ProfileImageService` — upload, resize e validação de imagens
- `NotificationService` — stub de notificações (expansível)
- `EmailService` — stub de email (expansível)

#### Camada de Repositórios (13 repositórios)
Todos baseados em Spring Data JPA com queries JPQL personalizadas para operações complexas:

- `UserRepository` — autenticação, busca por email/username, contagem de amizades
- `PodcastRepository` — JpaSpecificationExecutor para filtros dinâmicos do feed
- `ChatMessageRepository` — paginação cursor-based por `Instant`
- `UserRelationRepository` — queries direcionais para relações remetente/destinatário
- `PlaylistRepository` — playlists públicas de amigos via JPQL
- E mais 8 repositórios para as restantes entidades

#### Agendadores
- `DailyPlaylistScheduler` — regenera playlists diárias às 00:00 (fuso `Europe/Lisbon`)
- `RssService @Scheduled` — consome feeds RSS a cada 2 horas

### 4.4 Pipeline de Geração de Podcasts

O processo de geração de um podcast segue o fluxo:

```
Utilizador → POST /api/podcasts/generate {tema, tags}
    → PodcastGenerationService.generatePodcast()
        → Google Gemini API (gera guião narrativo em PT)
        → cleanScript() (limpa formatação markdown)
        → edge-tts ProcessBuilder (pt-PT-RaquelNeural → .mp3)
        → estimateDuration(wordCount / 150 palavras/min)
        → PodcastRepository.save()
    → Resposta: {podcastId, audioUrl, duracao, titulo}
```

---

## 5. Tecnologias Utilizadas

### 5.1 Backend

| Tecnologia | Versão | Utilização |
|---|---|---|
| Java | 17 | Linguagem principal do backend |
| Spring Boot | 3.4.3 | Framework principal (web, security, data, websocket) |
| Spring Security | 6.x | Autenticação e autorização |
| Spring Data JPA | 3.x | Persistência e queries |
| Hibernate | 6.x | ORM (mapeamento objeto-relacional) |
| H2 Database | — | Base de dados em desenvolvimento e testes |
| JJWT | 0.11.5 | Geração e validação de tokens JWT |
| ROME Library | 2.1.0 | Parsing de feeds RSS/Atom |
| SpringDoc OpenAPI | 2.8.5 | Documentação automática da API (Swagger UI) |
| Python edge-tts | 7.2.8 | Síntese de texto em áudio MP3 (voz PT) |
| Google Gemini API | — | Geração de guiões narrativos com LLM |
| Maven | 3.x | Gestão de dependências e build |
| JUnit 5 | — | Testes unitários e de integração |
| Mockito | — | Mocking em testes unitários |
| Checkstyle | 3.3.1 | Estilo de código (Google style) |
| OWASP Dependency Check | — | Auditoria de segurança das dependências |

### 5.2 Frontend

| Tecnologia | Versão | Utilização |
|---|---|---|
| React | 19.2 | Framework de UI |
| Vite | 7.3 | Bundler e servidor de desenvolvimento |
| React Router | 7.13 | Routing client-side (SPA) |
| react-hot-toast | 2.4 | Notificações visuais |
| WebSocket STOMP | — | Chat em tempo real |
| Service Worker | — | Suporte offline (PWA) |
| ESLint | 9.x | Linting do código JavaScript |
| Prettier | 3.3 | Formatação de código |
| Vitest | 4.x | Testes unitários de componentes |
| Playwright | 1.45 | Testes End-to-End |

### 5.3 DevOps / Qualidade

| Tecnologia | Utilização |
|---|---|
| GitHub Actions | CI/CD automático (backend + frontend) |
| Git | Controlo de versão |
| GitHub | Repositório remoto e PR workflow |

---

## 6. Modelo de Dados

O sistema possui **16 entidades/enums** persistidos via JPA/Hibernate numa base de dados relacional.

### 6.1 Entidades Principais

#### `User`
Representa um utilizador registado na plataforma.

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long (PK) | Identificador único |
| `username` | String | Nome de utilizador |
| `tag` | String | Tag numérica de 4 dígitos (ex: `0000`) |
| `email` | String (único) | Email de acesso |
| `password` | String | Hash BCrypt da password |
| `userType` | Enum | `USERNORMAL` ou `USERADMIN` |
| `status` | Enum | `ACTIVE`, `INACTIVE`, `BANNED` |
| `bio` | String | Biografia opcional |
| `pontosDesporto/Política/Finanças/Geral` | int | Pontuação por categoria (motor de recomendação) |
| `playbackSpeed` | double | Velocidade de reprodução preferida |
| `hasCompletedOnboarding` | boolean | Flag de primeiro acesso |
| `topics` | Set\<String\> | Tópicos de interesse selecionados no onboarding |

#### `Podcast`
Representa um episódio de podcast.

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long (PK) | Identificador único |
| `titulo` | String | Título do podcast |
| `user` | User (FK) | Criador/host do podcast |
| `conteudoPath` | String | Caminho relativo do ficheiro MP3 |
| `coverImagePath` | String | Caminho da imagem de capa |
| `duracao` | int | Duração em minutos |
| `tags` | List\<PodcastTag\> | Categorias do podcast |
| `publico` | boolean | Visibilidade pública |
| `available` | boolean | Disponibilidade (soft-delete) |
| `hidden` | boolean | Ocultado por admin |
| `featured` | boolean | Destacado pelo admin |
| `explicitContent` | boolean | Conteúdo explícito |

#### `UserRelation`
Modelo direcional de relação entre utilizadores.

| Campo | Tipo | Descrição |
|---|---|---|
| `user` | User (FK) | Utilizador que iniciou a relação (remetente) |
| `friend` | User (FK) | Utilizador alvo (destinatário) |
| `type` | Enum | `PEDIDO_PENDENTE`, `AMIGO`, `BLOQUEADO`, `PEDIDO_REJEITADO`, `CANCELADO` |

> **Nota:** Uma amizade aceite cria dois registos (bidirecional). O cooldown de reenvio de pedido após rejeição é de 7 dias.

#### `ChatMessage`
Mensagem privada entre dois utilizadores.

| Campo | Tipo | Descrição |
|---|---|---|
| `sender` | User (FK) | Remetente |
| `recipient` | User (FK) | Destinatário |
| `content` | String | Conteúdo da mensagem |
| `status` | Enum | `SENT`, `DELIVERED`, `READ` |
| `metadata` | ChatMessageMetadata | Anexos opcionais |
| `reactions` | List | Reações emoji dos utilizadores |

#### `Playlist` e `PlaylistItem`
Playlist de podcasts criada pelo utilizador. Cada item tem uma posição ordenada.

#### `DailyPlaylist` e `DailyPlaylistItem`
Playlist diária gerada automaticamente com uma pontuação de relevância por item. Única por utilizador/dia.

#### `PodcastFavorite`
Relação utilizador ↔ podcast marcado como favorito.

#### `PodcastProgress`
Regista o progresso de escuta (posição em segundos) de um utilizador num podcast.

#### `RssSource`
Fonte RSS parceira configurada no sistema (ativa/inativa).

#### `AdminActionLog`
Log de auditoria de todas as ações realizadas por administradores (com timestamp, utilizador, ação e resultado).

### 6.2 Enumerações

| Enum | Valores |
|---|---|
| `PodcastTag` | `DESPORTO`, `POLITICA`, `FINANCAS`, `GERAL` |
| `User.UserType` | `USERNORMAL`, `USERADMIN` |
| `User.UserStatus` | `ACTIVE`, `INACTIVE`, `BANNED` |
| `RelationType` | `PEDIDO_PENDENTE`, `AMIGO`, `BLOQUEADO`, `PEDIDO_REJEITADO`, `CANCELADO` |
| `MessageStatus` | `SENT`, `DELIVERED`, `READ` |

---

## 7. Funcionalidades Implementadas

### 7.1 Autenticação e Registo

O sistema suporta dois modos de login:
- **Por email** — `{"identifier": "utilizador@email.com", "password": "..."}`
- **Por username#tag** — `{"identifier": "nomeutilizador#0000", "password": "..."}`

O registo atribui automaticamente uma tag numérica única (0000–9999) ao utilizador, permitindo que múltiplas pessoas tenham o mesmo username.

O **onboarding** pós-registo recolhe os tópicos de interesse do utilizador, que alimentam o motor de recomendação.

### 7.2 Geração de Podcasts por IA

Um utilizador autenticado pode gerar um podcast enviando apenas:
- `tema` — assunto do podcast (ex: "Curiosidades sobre o espaço sideral")
- `tags` — lista de categorias (`GERAL`, `DESPORTO`, `POLITICA`, `FINANCAS`)

O sistema faz o seguinte automaticamente:
1. Chama a Google Gemini API para gerar um guião narrativo em português
2. Passa o texto ao `edge-tts` com a voz `pt-PT-RaquelNeural`
3. Guarda o ficheiro `.mp3` em `generated-podcasts/`
4. Persiste os metadados na base de dados
5. Devolve a URL de áudio imediatamente utilizável

### 7.3 Feed Personalizado e Recomendação

O feed (`/api/home`) suporta múltiplos filtros combinados:

| Parâmetro | Descrição |
|---|---|
| `type` | `recommended` (por pontos de tag), `recent`, `following` |
| `category` | `DESPORTO`, `POLITICA`, `FINANCAS`, `GERAL` |
| `is_favorite` | Apenas favoritos |
| `max_duration` | Duração máxima em minutos |
| `hide_played` | Ocultar podcasts já ouvidos |
| `shorts` | Apenas podcasts curtos (≤ 5 min) |

O `RecommendationService` mantém uma pontuação por categoria para cada utilizador. Ouvir um podcast de uma categoria incrementa os pontos dessa categoria, influenciando as recomendações futuras.

### 7.4 Sistema Social

**Amizades:**
- Envio de pedido, aceitação, rejeição, cancelamento e bloqueio
- Cooldown de 7 dias após rejeição para reenviar pedido
- Amizade aceite cria dois registos direcionais

**Chat Privado:**
- Mensagens em tempo real via WebSocket STOMP
- Histórico com paginação cursor-based (por `Instant`)
- Acknowledgment de entrega e leitura
- Reações com emojis
- Rate limiting: 20 mensagens/minuto, 100 mensagens/hora
- Blacklist de links com cooldown progressivo

### 7.5 Playlists

**Playlists Personalizadas:**
- CRUD completo (criar, listar, editar, eliminar)
- Adicionar, remover e reordenar episódios
- Visibilidade pública/privada
- Feed de playlists públicas de amigos
- Download da playlist completa em ZIP de MP3s

**Playlist Diária Automática:**
- Gerada automaticamente à meia-noite (Europe/Lisbon)
- Seleciona até 20 podcasts com duração mínima total de 30 minutos
- Ordenados por pontuação de relevância (calculada com base nos pontos de tag do utilizador)
- Um único registo por utilizador/dia (atualizado se já existir)

### 7.6 Administração

O painel de administração (apenas `USER_ADMIN`) disponibiliza:

- **Analytics:** DAU, MAU, novos registos, top podcasts, saúde do sistema, uso semanal/mensal
- **Gestão de Podcasts:** Ocultar, destacar, marcar conteúdo explícito, eliminar com dupla confirmação
- **Gestão de Utilizadores:** Listar, reset de password, eliminar com dupla confirmação
- **Logs de Auditoria:** Histórico paginado de todas as ações administrativas
- **Exportação:** Relatórios em CSV e PDF, geração assíncrona de relatórios por job

### 7.7 Consumo de RSS

O sistema consome automaticamente feeds RSS externos a cada 2 horas (usando a biblioteca ROME). Os artigos são deduplicados por URL e armazenados como entidades `Article`.

Fontes pré-configuradas: Observador, Público Desporto, TechCrunch, BBC News World.

---

## 8. API REST — Documentação de Endpoints

A documentação completa da API está disponível em formato **OpenAPI/Swagger** no endpoint `/swagger-ui.html` (quando o servidor está em execução) ou em `/v3/api-docs` para formato JSON.

### Resumo dos Grupos de Endpoints

| Grupo | Base Path | Autenticação |
|---|---|---|
| Autenticação | `/api/auth/login` | Pública |
| Registo | `/api/register` | Pública |
| Perfil do utilizador | `/api/users/me` | JWT obrigatório |
| Feed principal | `/api/home` | JWT obrigatório |
| Podcasts (geração) | `/api/podcasts` | JWT obrigatório |
| Podcasts (feed) | `/podcasts` | Pública (feed) / JWT (progresso) |
| Favoritos | `/api/favorites` | JWT obrigatório |
| Playlists | `/api/playlists` | JWT obrigatório |
| Playlists Diárias | `/api/daily-playlists` | JWT obrigatório |
| Relações sociais | `/api/relations` | JWT obrigatório |
| Chat REST | `/api/chats` | JWT obrigatório |
| Chat WebSocket | `ws://host/ws-chat` (STOMP) | JWT no handshake |
| Pesquisa | `/api/search` | JWT obrigatório |
| Tópicos | `/api/topics` | JWT obrigatório |
| Imagem de perfil | `/api/profile` | JWT obrigatório |
| Administração | `/api/admin` | JWT + `USER_ADMIN` |
| Utilizadores (CRUD) | `/users` | Misto |

### Exemplos de Chamadas

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"utilizador@email.com","password":"password123"}'
```

**Gerar Podcast:**
```bash
curl -X POST http://localhost:8080/api/podcasts/generate \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"tema":"Curiosidades sobre o espaço sideral","tags":["GERAL"]}'
```

**Feed Filtrado:**
```bash
curl "http://localhost:8080/api/home?type=recommended&category=DESPORTO&max_duration=30" \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 9. Segurança

### 9.1 Autenticação JWT

- **Algoritmo:** HMAC-SHA512 (HS512)
- **Validade:** 24 horas
- **Claims:** `sub` (email), `id` (userId), `type` (UserType)
- **Transmissão:** Header `Authorization: Bearer <token>`
- **Validação:** `JwtAuthenticationFilter` intercepta todos os pedidos e popula o `SecurityContext`

### 9.2 Passwords

- **Algoritmo:** BCrypt com fator de custo padrão do Spring Security
- **Sem armazenamento em plain text** em nenhum ponto do sistema

### 9.3 Controlo de Acesso

- **Endpoints de admin:** Protegidos com `@PreAuthorize("hasRole('USER_ADMIN')")` — verificação declarativa antes da execução da lógica de negócio
- **Operações sobre recursos:** Verificações programáticas de propriedade (ex: só o dono de uma playlist pode editá-la ou eliminá-la)
- **WebSocket:** Autenticação JWT no handshake HTTP via `JwtWebSocketHandshakeInterceptor`

### 9.4 CORS

Configurado no `SecurityConfig` para aceitar pedidos de:
- `http://localhost:5173` (frontend desenvolvimento)
- `http://localhost:5174` (frontend desenvolvimento alternativo)

### 9.5 Proteções Adicionais

- **Rate limiting no chat:** 20 mensagens/minuto, 100 mensagens/hora por utilizador
- **Blacklist de links no chat:** Com cooldown progressivo para utilizadores que tentam enviar links excessivamente
- **Upload de imagens:** Validação de tipo (JPG/PNG) e tamanho (≤ 5MB), redimensionamento automático para 400×400px

### 9.6 Vulnerabilidades Conhecidas e Mitigações

| Vulnerabilidade | Severidade | Estado |
|---|---|---|
| Chave JWT hardcoded em `JwtUtil` | Alta | Deve ser movida para variável de ambiente em produção |
| Logging de dados sensíveis no frontend | Alta | `console.log` com tokens deve ser removido antes de produção |
| Stack traces expostos nas respostas de erro | Média | Sanitizar mensagens de erro para não expor detalhes internos |

---

## 10. Testes

### 10.1 Estratégia de Testes

O projeto segue uma pirâmide de testes com cobertura nas camadas de serviço e integração:

| Tipo | Framework | Âmbito |
|---|---|---|
| Testes unitários | JUnit 5 + Mockito | Serviços de negócio (lógica isolada) |
| Testes de integração | JUnit 5 + MockMvc + H2 | Fluxos completos via API REST |
| Testes de contexto | Spring Boot Test | Arranque correto do contexto Spring |
| Testes de componente | Vitest + Testing Library | Componentes React isolados |
| Testes E2E | Playwright | Fluxos de utilizador completos no browser |

### 10.2 Suite de Testes Backend

| Ficheiro de Teste | Tipo | O Que Cobre |
|---|---|---|
| `ServidorApplicationTests` | Context Load | Arranque correto do contexto Spring Boot |
| `UserRegistrationTest` | Integração | Criação de utilizador, conflitos, validações de campos |
| `ChangePasswordIntegrationTest` | Integração | Alteração de password com verificação BCrypt |
| `AuthIntegrationTest` | Integração | Login por email e username#tag, JWT, endpoints protegidos |
| `UserRelationshipServiceTest` | Unitário | Pedidos de amizade, cooldown 7 dias, bloqueios (Mockito) |
| `UserRelationIntegrationTest` | Integração | Ciclo completo de amizades via API REST |
| `FeedIntegrationTest` | Integração | Filtros do feed (categoria, shorts, favoritos, duração) |
| `PlaylistIntegrationTest` | Integração | CRUD de playlists, episódios, visibilidade, permissões |
| `ChatMessageServiceTest` | Unitário | Envio de mensagens, rate limiting, links, permissões |
| `ChatReactionServiceTest` | Unitário | Adicionar, remover e atualizar reações a mensagens |
| `RssServiceTest` | Integração | Consumo de feed RSS, deduplicação de artigos, fallbacks |
| `OpenApiIntegrationTest` | Integração | Swagger UI e `/v3/api-docs` acessíveis sem autenticação |

**Total: 12 ficheiros de teste** cobrindo os fluxos críticos do sistema.

### 10.3 Execução dos Testes

```bash
# Todos os testes
cd servidor && ./mvnw test

# Teste específico
./mvnw test -Dtest=UserRegistrationTest

# Validação completa (build + testes + checkstyle)
./mvnw verify
```

### 10.4 Cobertura

- **Camada de serviço:** Coberta (testes unitários com Mockito)
- **Camada de integração (API):** Coberta (autenticação, feed, playlists, chat, relações, RSS)
- **Geração de podcasts:** Sem testes automatizados (depende de APIs externas pagas)
- **Painel de admin:** Sem testes automatizados (prioridade futura)

---

## 11. Pipeline de Integração Contínua

### 11.1 Backend CI (`maven-ci.yml`)

**Gatilhos:** `push` em qualquer branch; `pull_request` para `main`/`master`

**Fluxo:**
1. Provisionamento de Ubuntu
2. Java JDK 17 (distribuição Temurin, com cache Maven)
3. `mvn -B verify` — compila, testa, valida estilo e dependências

### 11.2 Frontend CI (`frontend-ci.yml`)

**Gatilhos:** `push`/`pull_request` para `main`/`master` com alterações em `frontend/**`

**Fluxo:**
1. Provisionamento de Ubuntu
2. Node.js 20.x com cache npm
3. `npm ci` — instalação determinística
4. `npm run lint` — ESLint
5. `npm run test:ci` — Vitest (modo CI)
6. `npx playwright install && npm run test:e2e` — testes E2E
7. Upload do relatório Playwright como artefacto (30 dias de retenção)

### 11.3 Quality Gates

| Verificação | Ferramenta | Quando |
|---|---|---|
| Estilo de código Java | Checkstyle (Google) | `mvn verify` |
| Vulnerabilidades em dependências | OWASP Dependency Check | `mvn verify` |
| Linting JavaScript | ESLint | CI frontend + local |
| Formatação | Prettier | Local (`npm run format`) |

---

## 12. Análise de Qualidade e Problemas Conhecidos

### 12.1 Métricas do Código

| Métrica | Valor |
|---|---|
| Classes Java (backend) | ~91 |
| Controllers | 17 |
| Serviços | 13 |
| Entidades/Modelos | 16 |
| Repositórios | 13 |
| Ficheiros de teste | 12 |
| Ficheiros JSX (frontend) | ~35 |
| Hooks customizados React | 3 |

### 12.2 Bugs Resolvidos

| Bug | Severidade | Resolução |
|---|---|---|
| Loop de redirecionamento pós-onboarding | Crítica | Evento `auth-change` para re-render reativo |
| Endpoint `GET /api/podcasts` inexistente | Crítica | Adicionado `getAllPublicPodcasts()` com filtro `hidden=false` |
| Recursos estáticos com 403 | Crítica | Criado `StaticResourceConfig` com handlers `/images/**` |
| `UnsupportedOperationException` no onboarding | Crítica | Conversão para `ArrayList` mutável antes de persistir |
| `window.confirm()` bloqueado no browser | Média | Substituído por modal React interno |

### 12.3 Dívida Técnica

| Issue | Impacto | Prioridade |
|---|---|---|
| Chave JWT hardcoded em `JwtUtil` | Risco de segurança em produção | Alta |
| `PodcastFavoriteController.removeFavorite` usa `findAll()` | Ineficiência — N+1 em produção | Média |
| Estado duplicado no `localStorage` (`user` + `topicsOnboardingComplete`) | Inconsistências potenciais | Média |
| Sem lazy loading de componentes React | Bundle size inicial excessivo | Baixa |
| `AuthUserController` mistura autenticação e onboarding | Viola SRP | Baixa |
| Sem Context API para estado de autenticação | Re-renders desnecessários | Baixa |

### 12.4 Limitações Conhecidas

- **Base de dados H2** — adequada para desenvolvimento; para produção recomenda-se migrar para PostgreSQL ou MySQL alterando apenas o `application.properties` e o driver
- **edge-tts** — requer Python 3 instalado no sistema onde o servidor corre
- **Chave Gemini** — funcionalidade de geração de podcasts requer uma chave de API válida (configurada em `env.properties` ou variável de ambiente `GEMINI_API_KEY`)
- **Email e Notificações** — atualmente implementados como stubs (output no console); requerem integração com serviço real para produção
- **Cache de recomendações** — implementada em memória (`ConcurrentHashMap`); para produção com múltiplas instâncias, necessita de Redis

---

## 13. Guia de Instalação e Execução

### 13.1 Pré-requisitos

| Requisito | Versão | Observação |
|---|---|---|
| Java JDK | 17+ | Necessário para o backend |
| Maven | 3.8+ | Ou usar o wrapper `./mvnw` incluído |
| Node.js | 20+ | Necessário para o frontend |
| Python | 3.x | Necessário para o TTS |
| pip | — | Para instalar o edge-tts |

### 13.2 Instalação

```bash
# 1. Clonar o repositório
git clone https://github.com/AfonsoBitoque/podcastia.git
cd podcastia

# 2. Instalar dependência Python para TTS
python3 -m pip install edge-tts --break-system-packages

# 3. Instalar dependências do frontend
cd frontend && npm install && cd ..

# 4. Configurar chave Gemini (necessário para geração de podcasts)
echo "GEMINI_API_KEY=SUA_CHAVE_AQUI" > servidor/env.properties
```

### 13.3 Execução

**Backend:**
```bash
cd servidor
./mvnw spring-boot:run
# Disponível em: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

**Frontend:**
```bash
cd frontend
npm run dev
# Disponível em: http://localhost:5173
```

### 13.4 Credenciais Padrão (DataSeeder)

| Utilizador | Email | Password | Papel |
|---|---|---|---|
| admin | admin@podcastia.com | admin | USER_ADMIN |

### 13.5 Gerar Podcasts de Teste

```bash
# Obter token JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin@podcastia.com","password":"admin"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Executar script de seed (gera 20 podcasts, ~10-20 min)
echo "" | bash scripts/seed_initial_podcasts.sh "$TOKEN"
```

---

## 14. Decisões Arquiteturais

| Decisão | Escolha | Justificação |
|---|---|---|
| **Autenticação** | JWT stateless (24h, HS512) | Sem estado no servidor; escalável horizontalmente; simples de implementar em SPA |
| **Chat em tempo real** | WebSocket STOMP | Bidirecional; integra nativamente com Spring Messaging; suporte a subscrições por tópico |
| **ORM** | JPA/Hibernate + H2 | Desenvolvimento rápido com DDL automático; migração para PostgreSQL/MySQL com mínima alteração de configuração |
| **Filtros de feed** | JPA Specification | Queries dinâmicas seguras sem concatenação SQL; composição de critérios em runtime |
| **Geração de áudio** | edge-tts via ProcessBuilder | TTS gratuito, voz portuguesa nativa de alta qualidade; sem custo por utilização |
| **Geração de script** | Google Gemini API | LLM com qualidade comprovada em português europeu; integração simples via REST |
| **Cache de feed** | `ConcurrentHashMap` in-memory (24h) | Simples e suficiente para um único servidor; substituível por Redis numa arquitetura distribuída |
| **Playlists diárias** | `@Scheduled` cron à meia-noite | Operação leve não interativa; sem necessidade de job queue externo |
| **Consumo RSS** | ROME Library + `@Scheduled` 2h | Abstrai diferenças entre RSS 1.0/2.0/Atom; maturidade e fiabilidade da biblioteca |
| **Segurança admin** | `@PreAuthorize("hasRole('USER_ADMIN')")` | Declarativo; verificado pelo AOP do Spring antes de chegar à lógica de negócio |
| **Paginação chat** | Cursor-based (Instant) | Estável em feeds com inserções frequentes; evita o problema do "shifting" do offset-based |
| **Tags de utilizador** | `username#0000-9999` | Permite nomes duplicados sem ambiguidade (inspirado no modelo Discord) |

---

## 15. Trabalho Futuro

### 15.1 Curto Prazo

- [ ] Remover `console.log` de dados sensíveis antes de deploy em produção
- [ ] Mover chave JWT para variável de ambiente
- [ ] Adicionar endpoint de health check que verifica Python/edge-tts
- [ ] Implementar error boundaries no React
- [ ] Optimizar `PodcastFavoriteController.removeFavorite` (substituir `findAll()` por query direta)

### 15.2 Médio Prazo

- [ ] Implementar `NotificationService` e `EmailService` reais (SMTP/push)
- [ ] Migrar base de dados para PostgreSQL para produção
- [ ] Adicionar Context API para estado de autenticação no frontend
- [ ] Implementar lazy loading de páginas React (`React.lazy()`)
- [ ] Criar `OnboardingController` separado (SRP)
- [ ] Adicionar sistema de logging estruturado (SLF4J + Logback + JSON)

### 15.3 Longo Prazo

- [ ] Substituir cache in-memory por Redis (arquitetura distribuída)
- [ ] Expandir testes E2E com Playwright
- [ ] Sistema de comentários em podcasts
- [ ] Notificações push em tempo real (WebSocket para notificações)
- [ ] Suporte a múltiplas vozes TTS e idiomas
- [ ] Modo offline melhorado (Service Worker + cache estratégico)
- [ ] Inclusão de podcasts de amigos nas playlists diárias
- [ ] Recomendações baseadas em histórico de reprodução completo
- [ ] Integração com plataformas externas (Spotify, Apple Podcasts)
- [ ] Deploy em cloud (Docker + CI/CD com deploy automático)

---

## 16. Conclusão

O projeto **Podcastia** foi desenvolvido com sucesso como uma plataforma completa e funcional de podcasts gerados por Inteligência Artificial. O sistema implementa uma arquitetura cliente-servidor bem estruturada, com clara separação de responsabilidades entre as camadas de segurança, controllers, serviços e repositórios.

As principais conquistas do projeto incluem:

- **Motor de geração de conteúdo** funcional integrando dois sistemas externos (Google Gemini API e edge-tts), capaz de produzir podcasts narrados em português europeu sobre qualquer tema
- **Sistema social completo** com amizades, chat em tempo real via WebSocket STOMP, playlists partilháveis e feed personalizado por preferências
- **Qualidade de código** assegurada por pipelines de CI automáticas, suite de 12 testes automatizados, Checkstyle (Google style) e auditoria OWASP das dependências
- **Documentação técnica** abrangente com modelo C4 em 4 níveis (usando Mermaid.js), documentação de endpoints OpenAPI/Swagger, análise crítica de bugs e este relatório

O projeto demonstra domínio dos conceitos e tecnologias de desenvolvimento de software moderno, incluindo REST API design, autenticação stateless, comunicação em tempo real, sistemas de recomendação, integração com APIs externas e qualidade de código.

As limitações identificadas — como a chave JWT hardcoded, a base de dados H2 para desenvolvimento e o estado dos serviços stub — são compreendidas e documentadas, com um plano claro de trabalho futuro para as abordar antes de um eventual deploy em produção.

---

*Relatório elaborado por: Afonso Bitoque, Maria Neto, Duarte Cunha, José Tico*
*Data: Maio de 2026*
*Universidade — Projeto de Desenvolvimento de Software*
