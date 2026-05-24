# C4 Nível 4 — Camada de Controllers (REST + WebSocket)

> Gerado a partir da leitura directa do código-fonte em `com.jep.servidor.controller`.  
> Representa todos os controllers, os seus endpoints, parâmetros e dependências.

---

## Diagrama de Classes — Controllers

```mermaid
classDiagram
    direction TB

    %% ─── AUTENTICAÇÃO ───────────────────────────────────────────────────
    class AuthController {
        <<@RestController>>
        <<POST /api/auth/login>>
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtUtil jwtUtil
        +login(LoginRequest) ResponseEntity
    }

    class AuthUserController {
        <<@RestController>>
        <<GET /api/users/me>>
        <<POST /api/users/onboarding>>
        -UserRepository userRepository
        +getMe(principal) ResponseEntity
        +completeOnboarding(principal, request) ResponseEntity
    }

    %% ─── UTILIZADORES ───────────────────────────────────────────────────
    class UserController {
        <<@RestController>>
        <<GET /users>>
        <<GET /users/{id}>>
        <<POST /users>>
        <<PUT /users/{id}>>
        <<PUT /users/{id}/password>>
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +getAllUsers() List~User~
        +getUserById(id) ResponseEntity
        +createUser(user) ResponseEntity
        +updateUser(id, user) ResponseEntity
        +changePassword(id, request) ResponseEntity
        +getPublicProfile(id) ResponseEntity
    }

    class RegistrationApiController {
        <<@RestController>>
        <<GET /api/register/check-tag>>
        <<GET /api/register/generate-tag>>
        -UserRepository userRepository
        +checkTagAvailability(username, tag) ResponseEntity
        +generateTag(username) ResponseEntity
    }

    class ProfileImageController {
        <<@RestController>>
        -ProfileImageService profileImageService
        -UserRepository userRepository
        +uploadProfileImage(file, principal) ResponseEntity
        +getProfileImage(filename) ResponseEntity
        +removeProfileImage(principal) ResponseEntity
    }

    class TopicController {
        <<@RestController>>
        <<GET /api/topics>>
        <<PUT /api/topics>>
        -UserRepository userRepository
        -RecommendationService recommendationService
        +getAllTopics() List~PodcastTag~
        +updateTopics(userId, topics) ResponseEntity
    }

    %% ─── RELAÇÕES ───────────────────────────────────────────────────────
    class UserRelationController {
        <<@RestController>>
        <<POST /api/relations/friend-request/{friendId}>>
        <<GET /api/relations/status/{targetUserId}>>
        <<DELETE /api/relations/friend-request/{friendId}/cancel>>
        <<POST /api/relations/accept/{senderId}>>
        <<POST /api/relations/reject/{senderId}>>
        <<POST /api/relations/block/{blockedId}>>
        <<DELETE /api/relations/remove/{friendId}>>
        <<GET /api/relations/friends>>
        <<GET /api/relations/pending>>
        -UserRelationshipService userRelationshipService
        -UserRepository userRepository
        +sendFriendRequest(friendId, principal) ResponseEntity
        +getRelationStatus(targetId, principal) ResponseEntity
        +cancelFriendRequest(friendId, principal) ResponseEntity
        +acceptFriendRequest(senderId, principal) ResponseEntity
        +rejectFriendRequest(senderId, principal) ResponseEntity
        +blockUser(blockedId, principal) ResponseEntity
        +removeFriend(friendId, principal) ResponseEntity
        +getFriends(principal) ResponseEntity
        +getPendingRequests(principal) ResponseEntity
    }

    %% ─── PODCASTS ───────────────────────────────────────────────────────
    class PodcastController {
        <<@RestController>>
        <<GET /podcasts/feed>>
        <<GET /podcasts/{id}/listen>>
        <<PUT /podcasts/{id}/progress>>
        <<GET /podcasts/continue-listening>>
        <<GET /podcasts/homepage>>
        -PodcastRepository podcastRepository
        -UserRepository userRepository
        -RecommendationService recommendationService
        -PodcastProgressRepository podcastProgressRepository
        -UserRelationshipService userRelationshipService
        +getFeed(principal, pageable) ResponseEntity
        +listenPodcast(id, principal) ResponseEntity
        +updateProgress(id, principal, request) ResponseEntity
        +getContinueListening(principal) ResponseEntity
        +getHomepage(principal) ResponseEntity
    }

    class PodcastGenerationController {
        <<@RestController>>
        <<POST /api/podcasts/generate>>
        <<GET /api/podcasts/{id}/audio>>
        <<GET /api/podcasts/{id}/download>>
        <<PUT /api/podcasts/{id}/visibility>>
        <<GET /api/podcasts>>
        <<DELETE /api/podcasts/{id}>>
        -PodcastGenerationService podcastGenerationService
        -PodcastRepository podcastRepository
        -UserRepository userRepository
        +generatePodcast(request, principal) ResponseEntity
        +streamAudio(id, rangeHeader, response)
        +downloadAudio(id, principal) ResponseEntity
        +setVisibility(id, request, principal) ResponseEntity
        +getUserPodcasts(principal) ResponseEntity
        +deletePodcast(id, principal) ResponseEntity
    }

    class FeedController {
        <<@RestController>>
        <<GET /api/home>>
        -FeedService feedService
        -UserRepository userRepository
        +getFeed(principal, type, category, isFavorite, maxDuration, hidePlayed, shorts, pageable) ResponseEntity
    }

    class PodcastFavoriteController {
        <<@RestController>>
        <<GET /api/favorites>>
        <<POST /api/favorites/{podcastId}/toggle>>
        <<GET /api/favorites/{podcastId}/status>>
        -PodcastFavoriteRepository favoriteRepository
        -PodcastRepository podcastRepository
        -UserRepository userRepository
        +getFavorites(principal) ResponseEntity
        +toggleFavorite(podcastId, principal) ResponseEntity
        +checkFavoriteStatus(podcastId, principal) ResponseEntity
    }

    class SearchController {
        <<@RestController>>
        <<GET /api/search>>
        -SearchService searchService
        +search(query, page, size) ResponseEntity
    }

    %% ─── PLAYLISTS ──────────────────────────────────────────────────────
    class PlaylistController {
        <<@RestController>>
        <<GET /api/playlists>>
        <<POST /api/playlists>>
        <<GET /api/playlists/{id}>>
        <<PUT /api/playlists/{id}>>
        <<DELETE /api/playlists/{id}>>
        <<POST /api/playlists/{id}/episodes>>
        <<DELETE /api/playlists/{id}/episodes/{podcastId}>>
        <<PUT /api/playlists/{id}/reorder>>
        <<GET /api/playlists/friends-feed>>
        <<GET /api/playlists/{id}/download>>
        -PlaylistService playlistService
        -UserRepository userRepository
        +getUserPlaylists(principal) ResponseEntity
        +createPlaylist(request, principal) ResponseEntity
        +getPlaylist(id, principal) ResponseEntity
        +updatePlaylist(id, request, principal) ResponseEntity
        +deletePlaylist(id, principal) ResponseEntity
        +addEpisode(id, podcastId, principal) ResponseEntity
        +removeEpisode(id, podcastId, principal) ResponseEntity
        +reorderEpisodes(id, request, principal) ResponseEntity
        +getFriendsFeed(principal) ResponseEntity
        +downloadPlaylist(id, principal) ResponseEntity
    }

    class DailyPlaylistController {
        <<@RestController>>
        <<GET /api/daily-playlists/today>>
        <<GET /api/daily-playlists/latest>>
        <<POST /api/daily-playlists/generate>>
        -DailyPlaylistService dailyPlaylistService
        -UserRepository userRepository
        +getTodayPlaylist(principal) ResponseEntity
        +getLatestPlaylist(principal) ResponseEntity
        +generatePlaylist(principal) ResponseEntity
    }

    %% ─── CHAT ───────────────────────────────────────────────────────────
    class ChatController {
        <<@RestController>>
        <<GET /api/chats/{friendId}>>
        <<GET /api/chats/unread-count>>
        <<POST /api/chats/{messageId}/react>>
        <<DELETE /api/chats/{messageId}>>
        -ChatMessageService chatMessageService
        +getConversation(friendId, cursor, limit, principal) ResponseEntity
        +getUnreadCount(principal) ResponseEntity
        +reactToMessage(messageId, request, principal) ResponseEntity
        +deleteMessage(messageId, principal) ResponseEntity
    }

    class ChatWebSocketController {
        <<@Controller>>
        <<STOMP /app/chat.send>>
        <<STOMP /app/chat.ack>>
        <<STOMP /app/chat.reaction>>
        -ChatMessageService chatMessageService
        +sendMessage(request, principal) ChatMessageDTO
        +acknowledgeMessage(request, principal) ChatMessageDTO
        +reactToMessage(request, principal) ChatReactionUpdateResponse
        +handleConnect(event)
        +handleDisconnect(event)
    }

    %% ─── ADMINISTRAÇÃO ──────────────────────────────────────────────────
    class AdminController {
        <<@RestController>>
        <<@PreAuthorize hasRole USER_ADMIN>>
        <<GET /api/admin/analytics>>
        <<POST /api/admin/users/{id}/ban>>
        <<POST /api/admin/users/{id}/suspend>>
        <<POST /api/admin/users/{id}/activate>>
        <<DELETE /api/admin/podcasts/{id}>>
        <<PUT /api/admin/podcasts/{id}/feature>>
        <<PUT /api/admin/podcasts/{id}/hide>>
        <<GET /api/admin/logs>>
        <<GET /api/admin/export/users>>
        <<GET /api/admin/export/podcasts>>
        <<GET /api/admin/report>>
        -AdminService adminService
        +getAnalytics() ResponseEntity
        +banUser(id, request) ResponseEntity
        +suspendUser(id, request) ResponseEntity
        +activateUser(id) ResponseEntity
        +deletePodcast(id, request) ResponseEntity
        +featurePodcast(id) ResponseEntity
        +hidePodcast(id) ResponseEntity
        +getActionLogs(pageable) ResponseEntity
        +exportUsers() ResponseEntity
        +exportPodcasts() ResponseEntity
        +getReport() ResponseEntity
    }

    %% ─── DEPENDÊNCIAS ───────────────────────────────────────────────────
    AuthController --> UserRelationshipService : não usa
    AuthController --> UserRepository : valida credenciais
    AuthController --> JwtUtil : gera token

    UserController --> UserRepository : CRUD
    AuthUserController --> UserRepository : perfil próprio

    UserRelationController --> UserRelationshipService : delega
    UserRelationController --> UserRepository : resolve userId

    PodcastController --> PodcastRepository : consulta
    PodcastController --> RecommendationService : feed personalizado
    PodcastController --> UserRelationshipService : verifica amizade

    PodcastGenerationController --> PodcastGenerationService : gera podcast
    PodcastGenerationController --> PodcastRepository : visibilidade, delete

    FeedController --> FeedService : feed filtrado

    PlaylistController --> PlaylistService : CRUD playlists

    DailyPlaylistController --> DailyPlaylistService : playlists diárias

    ChatController --> ChatMessageService : chat REST
    ChatWebSocketController --> ChatMessageService : chat STOMP

    AdminController --> AdminService : administração

    TopicController --> RecommendationService : pontuação
    SearchController --> SearchService : pesquisa
```

---

## Tabela de Endpoints por Controller

| Controller | Método | Path | Auth |
|---|---|---|---|
| `AuthController` | POST | `/api/auth/login` | Público |
| `AuthUserController` | GET | `/api/users/me` | JWT |
| `AuthUserController` | POST | `/api/users/onboarding` | JWT |
| `UserController` | GET | `/users` | Público |
| `UserController` | GET | `/users/{id}` | Público |
| `UserController` | POST | `/users` | Público |
| `UserController` | PUT | `/users/{id}` | JWT |
| `UserController` | PUT | `/users/{id}/password` | JWT |
| `RegistrationApiController` | GET | `/api/register/check-tag` | Público |
| `RegistrationApiController` | GET | `/api/register/generate-tag` | Público |
| `ProfileImageController` | POST | `/api/profile/upload` | JWT |
| `ProfileImageController` | GET | `/images/{filename}` | Público |
| `ProfileImageController` | DELETE | `/api/profile/remove` | JWT |
| `TopicController` | GET | `/api/topics` | JWT |
| `TopicController` | PUT | `/api/topics` | JWT |
| `UserRelationController` | POST | `/api/relations/friend-request/{id}` | JWT |
| `UserRelationController` | GET | `/api/relations/status/{id}` | JWT |
| `UserRelationController` | DELETE | `/api/relations/friend-request/{id}/cancel` | JWT |
| `UserRelationController` | POST | `/api/relations/accept/{id}` | JWT |
| `UserRelationController` | POST | `/api/relations/reject/{id}` | JWT |
| `UserRelationController` | POST | `/api/relations/block/{id}` | JWT |
| `UserRelationController` | DELETE | `/api/relations/remove/{id}` | JWT |
| `UserRelationController` | GET | `/api/relations/friends` | JWT |
| `UserRelationController` | GET | `/api/relations/pending` | JWT |
| `PodcastController` | GET | `/podcasts/feed` | JWT |
| `PodcastController` | GET | `/podcasts/{id}/listen` | JWT |
| `PodcastController` | PUT | `/podcasts/{id}/progress` | JWT |
| `PodcastController` | GET | `/podcasts/continue-listening` | JWT |
| `PodcastController` | GET | `/podcasts/homepage` | JWT |
| `PodcastGenerationController` | POST | `/api/podcasts/generate` | JWT |
| `PodcastGenerationController` | GET | `/api/podcasts/{id}/audio` | Público |
| `PodcastGenerationController` | GET | `/api/podcasts/{id}/download` | JWT |
| `PodcastGenerationController` | PUT | `/api/podcasts/{id}/visibility` | JWT |
| `PodcastGenerationController` | GET | `/api/podcasts` | JWT |
| `PodcastGenerationController` | DELETE | `/api/podcasts/{id}` | JWT |
| `FeedController` | GET | `/api/home` | JWT |
| `PodcastFavoriteController` | GET | `/api/favorites` | JWT |
| `PodcastFavoriteController` | POST | `/api/favorites/{id}/toggle` | JWT |
| `PodcastFavoriteController` | GET | `/api/favorites/{id}/status` | JWT |
| `SearchController` | GET | `/api/search` | Público |
| `PlaylistController` | GET | `/api/playlists` | JWT |
| `PlaylistController` | POST | `/api/playlists` | JWT |
| `PlaylistController` | GET | `/api/playlists/{id}` | JWT |
| `PlaylistController` | PUT | `/api/playlists/{id}` | JWT |
| `PlaylistController` | DELETE | `/api/playlists/{id}` | JWT |
| `PlaylistController` | POST | `/api/playlists/{id}/episodes` | JWT |
| `PlaylistController` | DELETE | `/api/playlists/{id}/episodes/{podcastId}` | JWT |
| `PlaylistController` | PUT | `/api/playlists/{id}/reorder` | JWT |
| `PlaylistController` | GET | `/api/playlists/friends-feed` | JWT |
| `PlaylistController` | GET | `/api/playlists/{id}/download` | JWT |
| `DailyPlaylistController` | GET | `/api/daily-playlists/today` | JWT |
| `DailyPlaylistController` | GET | `/api/daily-playlists/latest` | JWT |
| `DailyPlaylistController` | POST | `/api/daily-playlists/generate` | JWT |
| `ChatController` | GET | `/api/chats/{friendId}` | JWT |
| `ChatController` | GET | `/api/chats/unread-count` | JWT |
| `ChatController` | POST | `/api/chats/{messageId}/react` | JWT |
| `ChatController` | DELETE | `/api/chats/{messageId}` | JWT |
| `ChatWebSocketController` | MSG | `STOMP /app/chat.send` | JWT WS |
| `ChatWebSocketController` | MSG | `STOMP /app/chat.ack` | JWT WS |
| `ChatWebSocketController` | MSG | `STOMP /app/chat.reaction` | JWT WS |
| `AdminController` | GET | `/api/admin/analytics` | JWT + ADMIN |
| `AdminController` | POST | `/api/admin/users/{id}/ban` | JWT + ADMIN |
| `AdminController` | POST | `/api/admin/users/{id}/suspend` | JWT + ADMIN |
| `AdminController` | POST | `/api/admin/users/{id}/activate` | JWT + ADMIN |
| `AdminController` | DELETE | `/api/admin/podcasts/{id}` | JWT + ADMIN |
| `AdminController` | PUT | `/api/admin/podcasts/{id}/feature` | JWT + ADMIN |
| `AdminController` | PUT | `/api/admin/podcasts/{id}/hide` | JWT + ADMIN |
| `AdminController` | GET | `/api/admin/logs` | JWT + ADMIN |
| `AdminController` | GET | `/api/admin/export/users` | JWT + ADMIN |
| `AdminController` | GET | `/api/admin/export/podcasts` | JWT + ADMIN |
| `AdminController` | GET | `/api/admin/report` | JWT + ADMIN |
