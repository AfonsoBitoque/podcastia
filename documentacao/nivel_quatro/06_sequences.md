# C4 Nível 4 — Diagramas de Sequência (Fluxos Principais)

> Gerado a partir da leitura directa do código-fonte.  
> Representa os fluxos end-to-end mais importantes do sistema Podcastia.

---

## 1. Login e Emissão de JWT

```mermaid
sequenceDiagram
    actor Utilizador
    participant FE as Frontend SPA
    participant AC as AuthController
    participant UR as UserRepository
    participant PE as PasswordEncoder
    participant JU as JwtUtil

    Utilizador->>FE: Submete formulário de login
    FE->>AC: POST /api/auth/login {email, password}
    AC->>UR: findByEmail(email)
    UR-->>AC: Optional[User]
    alt Utilizador não encontrado
        AC-->>FE: 401 Unauthorized
    else Utilizador encontrado
        AC->>PE: matches(password, user.password)
        PE-->>AC: boolean
        alt Password incorrecta
            AC-->>FE: 401 Unauthorized
        else Password correcta
            AC->>AC: Verifica user.status != BANNED/SUSPENDED
            AC->>JU: generateToken(user)
            JU-->>AC: jwtToken (24h, HS256)
            AC-->>FE: 200 OK {token, userId, username, tag, userType}
            FE->>FE: Guarda token em localStorage
        end
    end
```

---

## 2. Registo de Utilizador

```mermaid
sequenceDiagram
    actor Utilizador
    participant FE as Frontend SPA
    participant UC as UserController
    participant UR as UserRepository
    participant PE as PasswordEncoder
    participant RA as RegistrationApiController

    Utilizador->>FE: Preenche form de registo
    FE->>RA: GET /api/register/generate-tag?username=X
    RA->>UR: existsByUsernameAndTag(X, tag)
    RA-->>FE: {tag: "0042"} (1ª tag disponível)
    FE->>UC: POST /users {username, tag, email, password, ...}
    UC->>UR: existsByEmail(email)
    UC->>UR: existsByUsernameAndTag(username, tag)
    alt Email ou (username+tag) já existe
        UC-->>FE: 409 Conflict
    else Dados únicos
        UC->>PE: encode(password)
        PE-->>UC: hashedPassword
        UC->>UR: save(newUser)
        UR-->>UC: User persisted
        UC-->>FE: 201 Created {userId, username, tag}
    end
```

---

## 3. Geração de Podcast com IA

```mermaid
sequenceDiagram
    actor Utilizador
    participant FE as Frontend SPA
    participant JWT as JwtAuthFilter
    participant PGC as PodcastGenerationController
    participant PGS as PodcastGenerationService
    participant GEM as Google Gemini API
    participant TTS as edge-tts (Python)
    participant FS as Sistema de Ficheiros
    participant PR as PodcastRepository

    Utilizador->>FE: "Gerar podcast sobre X"
    FE->>JWT: POST /api/podcasts/generate {tema, tags}\nBearer token
    JWT->>JWT: Valida token, popula SecurityContext
    JWT->>PGC: passa pedido autenticado
    PGC->>PGS: generatePodcast(user, tema, tags)
    PGS->>GEM: POST /v1beta/models/gemini-pro:generateContent\n{prompt com tema e instruções}
    GEM-->>PGS: script narrativo (texto ~500-1500 palavras)
    PGS->>PGS: cleanScript(script) — remove artefactos
    PGS->>PGS: generateFileName(titulo)
    PGS->>TTS: ProcessBuilder: python edge-tts\n--voice pt-PT-RaquelNeural\n--text script\n--write-media output.mp3
    TTS-->>FS: Grava ficheiro .mp3
    TTS-->>PGS: processo terminado (exit code 0)
    PGS->>PGS: estimateDuration(wordCount / 150 wpm)
    PGS->>PR: save(podcast)
    PR-->>PGS: Podcast persisted (com ID)
    PGS-->>PGC: Podcast entity
    PGC-->>FE: 200 OK {id, titulo, duracao, audioUrl}
```

---

## 4. Ciclo de Mensagem de Chat (WebSocket)

```mermaid
sequenceDiagram
    actor Remetente
    actor Destinatario
    participant FE_R as Frontend (Remetente)
    participant FE_D as Frontend (Destinatário)
    participant WS as WebSocket STOMP
    participant CWC as ChatWebSocketController
    participant CMS as ChatMessageServiceImpl
    participant CMR as ChatMessageRepository
    participant NS as NotificationService

    Remetente->>FE_R: Escreve mensagem e envia
    FE_R->>WS: SEND /app/chat.send {recipientId, content}
    WS->>CWC: sendMessage(request, principal)
    CWC->>CMS: sendMessage(senderId, request)
    CMS->>CMS: validateRateLimit(senderId)\n[20/min, 100/h]
    CMS->>CMS: validateContent(content)\n[max 2000 chars]
    CMS->>CMS: validateLinks(senderId, content)\n[max 3 links/h]
    CMS->>CMR: save(chatMessage)
    CMR-->>CMS: ChatMessage persisted
    alt Destinatário online (activeSessions contém id)
        CMS->>WS: messagingTemplate.convertAndSendToUser\n(/user/{destinatarioId}/queue/messages)
        WS->>FE_D: mensagem entregue em tempo real
        FE_D->>WS: SEND /app/chat.ack {messageId, type: DELIVERED}
        WS->>CWC: acknowledgeMessage(request, principal)
        CWC->>CMS: acknowledgeMessage(userId, messageId, DELIVERED)
        CMS->>CMR: update status → DELIVERED, deliveredAt
    else Destinatário offline
        CMS->>CMS: pushQueue.offer(message)\n[processado quando conectar]
        CMS->>NS: sendNotification(destinatarioId, ...)
    end
    CMS-->>CWC: ChatMessageDTO
    CWC-->>FE_R: confirmação de envio
```

---

## 5. Ciclo de Pedido de Amizade

```mermaid
sequenceDiagram
    actor UserA
    actor UserB
    participant FE_A as Frontend (A)
    participant FE_B as Frontend (B)
    participant URC as UserRelationController
    participant URS as UserRelationshipServiceImpl
    participant URR as UserRelationRepository
    participant NS as NotificationService

    UserA->>FE_A: Clica "Adicionar Amigo" no perfil de B
    FE_A->>URC: POST /api/relations/friend-request/{B_id}\nBearer token(A)
    URC->>URS: sendFriendRequest(A_id, B_id)
    URS->>URR: findRelationship(A_id, B_id)
    alt Relação já existe (PEDIDO/AMIGO/BLOQUEADO)
        URS-->>URC: BusinessException
        URC-->>FE_A: 400 Bad Request
    else Cooldown activo (< 7 dias desde rejeição/cancelamento)
        URS-->>URC: BusinessException "cooldown"
        URC-->>FE_A: 400 Bad Request
    else Sem relação anterior
        URS->>URR: save(UserRelation{A→B, PEDIDO})
        URS->>NS: sendFriendRequestNotification(A_id, B_id)
        URS-->>URC: OK
        URC-->>FE_A: 200 OK
    end

    UserB->>FE_B: Vê notificação, clica "Aceitar"
    FE_B->>URC: POST /api/relations/accept/{A_id}\nBearer token(B)
    URC->>URS: acceptFriendRequest(A_id, B_id)
    URS->>URR: findRelationship(A_id, B_id)
    URR-->>URS: UserRelation{A→B, PEDIDO}
    URS->>URR: update type → AMIGO
    URS->>URR: save(UserRelation{B→A, AMIGO})
    URS->>NS: sendFriendAcceptedNotification(A_id, B_id)
    URS-->>URC: OK
    URC-->>FE_B: 200 OK
```

---

## 6. Geração Automática de Playlist Diária (Scheduler)

```mermaid
sequenceDiagram
    participant SCH as DailyPlaylistScheduler\n@Scheduled 00:00 Europe/Lisbon
    participant DPS as DailyPlaylistService
    participant UR as UserRepository
    participant PR as PodcastRepository
    participant DPR as DailyPlaylistRepository
    participant DPIR as DailyPlaylistItemRepository

    SCH->>DPS: regenerateAllDailyPlaylists()
    DPS->>UR: findAll() — todos os utilizadores ACTIVE
    loop Para cada utilizador
        DPS->>DPR: findByUserAndPlaylistDate(user, today)
        alt Playlist do dia já existe
            DPS->>DPIR: deleteByDailyPlaylist(existing)
            DPS->>DPR: delete(existing)
        end
        DPS->>PR: findAllByPublicoTrueAndAvailableTrue()
        DPS->>DPS: selectPodcastsForUser(user)\n[score por tag: DESPORTO/POLITICA/FINANCAS/GERAL]
        DPS->>DPS: sortByAfinity(podcasts, user)\n[top 10 por score DESC]
        DPS->>DPR: save(DailyPlaylist{user, today, title, ...})
        loop Para cada podcast selecionado
            DPS->>DPIR: save(DailyPlaylistItem{playlist, podcast, position})
        end
    end
    DPS-->>SCH: completo
```

---

## 7. Consumo de RSS (Scheduler)

```mermaid
sequenceDiagram
    participant SCH as RssService\n@Scheduled cada 2h
    participant RSS as RssService
    participant RSR as RssSourceRepository
    participant AR as ArticleRepository
    participant ROME as ROME Library

    SCH->>RSS: fetchAllFeeds()
    RSS->>RSR: findByAtiva(true)
    RSR-->>RSS: List[RssSource]
    loop Para cada RssSource activa
        RSS->>ROME: parseRome(source.url)
        ROME-->>RSS: SyndFeed {entries}
        loop Para cada SyndEntry
            RSS->>AR: existsByUrl(entry.url)
            alt Artigo já existe
                RSS->>RSS: skip (sem duplicados)
            else Artigo novo
                RSS->>AR: save(Article{titulo, url, descricao, fonte, publicadoEm})
            end
        end
    end
    RSS-->>SCH: completo
```

---

## 8. Autenticação WebSocket + Chat em Tempo Real

```mermaid
sequenceDiagram
    actor Utilizador
    participant FE as Frontend SPA
    participant INT as JwtWebSocketHandshakeInterceptor
    participant HAN as JwtWebSocketHandshakeHandler
    participant CMS as ChatMessageServiceImpl
    participant BR as STOMP Broker

    Utilizador->>FE: Navega para o chat
    FE->>INT: GET /ws?token=eyJhbGci...\n(Upgrade: websocket)
    INT->>INT: JwtUtil.extractEmail(token)
    INT->>INT: JwtUtil.isTokenValid(token, email)
    alt Token inválido
        INT-->>FE: 403 Forbidden — Handshake recusado
    else Token válido
        INT->>INT: Guarda userId em session attributes
        INT->>HAN: determineUser(request, ..., attributes)
        HAN-->>INT: Principal{userId}
        INT-->>FE: 101 Switching Protocols
        FE->>BR: SUBSCRIBE /user/queue/messages
        BR->>CMS: handleConnect(principal, sessionId)
        CMS->>CMS: activeSessions.put(userId, new AtomicInteger(1))
        Note over FE,BR: Ligação STOMP estabelecida
        Note over FE,BR: Mensagens chegam via /user/{userId}/queue/messages
    end
```
