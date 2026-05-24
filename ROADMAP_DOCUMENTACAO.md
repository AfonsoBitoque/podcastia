# ROADMAP DE DOCUMENTAÇÃO — Podcastia Backend

> Legenda: ⬜ Pendente | 🔄 Em progresso | ✅ Concluído

---

## Ponto de Entrada

| Estado | Ficheiro |
|--------|----------|
| ✅ | `ServidorApplication.java` |

---

## config/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `config/AudioPathSync.java` |
| ✅ | `config/DailyPlaylistScheduler.java` |
| ✅ | `config/DataSeeder.java` |
| ✅ | `config/FixAudioPaths.java` |
| ✅ | `config/JwtAuthenticationFilter.java` |
| ✅ | `config/JwtUtil.java` |
| ✅ | `config/JwtWebSocketHandshakeHandler.java` |
| ✅ | `config/JwtWebSocketHandshakeInterceptor.java` |
| ✅ | `config/OpenApiConfig.java` |
| ✅ | `config/PodcastSchemaMigrationRunner.java` |
| ✅ | `config/SecurityConfig.java` |
| ✅ | `config/StaticResourceConfig.java` |
| ✅ | `config/WebSocketConfig.java` |

---

## controller/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `controller/AdminController.java` |
| ✅ | `controller/AuthController.java` |
| ✅ | `controller/AuthUserController.java` |
| ✅ | `controller/ChatController.java` |
| ✅ | `controller/ChatWebSocketController.java` |
| ✅ | `controller/DailyPlaylistController.java` |
| ✅ | `controller/FeedController.java` |
| ✅ | `controller/PlaylistController.java` |
| ✅ | `controller/PodcastController.java` |
| ✅ | `controller/PodcastFavoriteController.java` |
| ✅ | `controller/PodcastGenerationController.java` |
| ✅ | `controller/ProfileImageController.java` |
| ✅ | `controller/RegistrationApiController.java` |
| ✅ | `controller/SearchController.java` |
| ✅ | `controller/TopicController.java` |
| ✅ | `controller/UserController.java` |
| ✅ | `controller/UserRelationController.java` |

---

## dto/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `dto/AdminActionLogDTO.java` |
| ✅ | `dto/AdminAnalyticsDTO.java` |
| ✅ | `dto/AdminPodcastManagementDTO.java` |
| ✅ | `dto/ChangePasswordRequest.java` |
| ✅ | `dto/ChatMessageAttachmentRequest.java` |
| ✅ | `dto/ChatMessageDto.java` |
| ✅ | `dto/ChatMessageHistoryResponse.java` |
| ✅ | `dto/ChatMessageRequest.java` |
| ✅ | `dto/ChatReactionRequest.java` |
| ✅ | `dto/ChatReactionSummaryDto.java` |
| ✅ | `dto/ChatReactionUpdateResponse.java` |
| ✅ | `dto/DailyPlaylistItemResponse.java` |
| ✅ | `dto/DailyPlaylistResponse.java` |
| ✅ | `dto/FeedMeta.java` |
| ✅ | `dto/FeedResponse.java` |
| ✅ | `dto/FriendDto.java` |
| ✅ | `dto/OnboardingDTO.java` |
| ✅ | `dto/PendingRequestDto.java` |
| ✅ | `dto/PlaylistAddEpisodeRequest.java` |
| ✅ | `dto/PlaylistCreateRequest.java` |
| ✅ | `dto/PlaylistReorderRequest.java` |
| ✅ | `dto/PlaylistUpdateRequest.java` |
| ✅ | `dto/RelationStatusDto.java` |
| ✅ | `dto/SearchResultDto.java` |
| ✅ | `dto/TopicResponse.java` |
| ✅ | `dto/TopicSelectionRequest.java` |
| ✅ | `dto/UserProfileDto.java` |
| ✅ | `dto/UserUpdateRequest.java` |

---

## exceptions/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `exceptions/BusinessException.java` |
| ✅ | `exceptions/ChatMessageException.java` |
| ✅ | `exceptions/FriendshipNotFoundException.java` |

---

## model/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `model/AdminActionLog.java` |
| ✅ | `model/Article.java` |
| ✅ | `model/ChatMessage.java` |
| ✅ | `model/ChatMessageMetadata.java` |
| ✅ | `model/ChatMessageReaction.java` |
| ✅ | `model/DailyPlaylist.java` |
| ✅ | `model/DailyPlaylistItem.java` |
| ✅ | `model/Playlist.java` |
| ✅ | `model/PlaylistItem.java` |
| ✅ | `model/Podcast.java` |
| ✅ | `model/PodcastFavorite.java` |
| ✅ | `model/PodcastProgress.java` |
| ✅ | `model/PodcastTag.java` |
| ✅ | `model/RssSource.java` |
| ✅ | `model/User.java` |
| ✅ | `model/UserRelation.java` |

---

## repository/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `repository/AdminActionLogRepository.java` |
| ✅ | `repository/ArticleRepository.java` |
| ✅ | `repository/ChatMessageReactionRepository.java` |
| ✅ | `repository/ChatMessageRepository.java` |
| ✅ | `repository/DailyPlaylistItemRepository.java` |
| ✅ | `repository/DailyPlaylistRepository.java` |
| ✅ | `repository/PlaylistItemRepository.java` |
| ✅ | `repository/PlaylistRepository.java` |
| ✅ | `repository/PodcastFavoriteRepository.java` |
| ✅ | `repository/PodcastProgressRepository.java` |
| ✅ | `repository/PodcastRepository.java` |
| ✅ | `repository/RssSourceRepository.java` |
| ✅ | `repository/UserRelationRepository.java` |
| ✅ | `repository/UserRepository.java` |

---

## service/

| Estado | Ficheiro |
|--------|----------|
| ✅ | `service/AdminService.java` |
| ✅ | `service/ChatMessageService.java` |
| ✅ | `service/DailyPlaylistService.java` |
| ✅ | `service/EmailService.java` |
| ✅ | `service/FeedService.java` |
| ✅ | `service/NotificationService.java` |
| ✅ | `service/PlaylistService.java` |
| ✅ | `service/PodcastGenerationService.java` |
| ✅ | `service/ProfileImageService.java` |
| ✅ | `service/RecommendationService.java` |
| ✅ | `service/RssService.java` |
| ✅ | `service/SearchService.java` |
| ✅ | `service/UserRelationshipService.java` |
| ✅ | `service/impl/ChatMessageServiceImpl.java` |
| ✅ | `service/impl/NotificationServiceImpl.java` |
| ✅ | `service/impl/UserRelationshipServiceImpl.java` |

---

## Progresso Global

**Total de ficheiros:** 91  
**Documentados:** 91 / 91  
**Percentagem:** 100% ✅ — Documentação completa!
