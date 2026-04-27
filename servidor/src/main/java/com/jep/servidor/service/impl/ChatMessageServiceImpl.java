package com.jep.servidor.service.impl;

import com.jep.servidor.dto.ChatMessageAttachmentRequest;
import com.jep.servidor.dto.ChatMessageDto;
import com.jep.servidor.dto.ChatMessageHistoryResponse;
import com.jep.servidor.dto.ChatMessageRequest;
import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.dto.ChatReactionSummaryDto;
import com.jep.servidor.dto.ChatReactionUpdateResponse;
import com.jep.servidor.exceptions.ChatMessageException;
import com.jep.servidor.model.ChatMessage;
import com.jep.servidor.model.ChatMessageMetadata;
import com.jep.servidor.model.ChatMessageReaction;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.ChatMessageRepository;
import com.jep.servidor.repository.ChatMessageReactionRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.ChatMessageService;
import com.jep.servidor.service.NotificationService;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.context.event.EventListener;

/**
 * Implementação do serviço de mensagens privadas.
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

  private static final int MAX_CONTENT_LENGTH = 2000;
  private static final int MAX_MESSAGES_PER_MINUTE = 20;
  private static final int MAX_MESSAGES_PER_HOUR = 100;
  private static final int MAX_LINKS_PER_WINDOW = 3;
  private static final Set<String> ALLOWED_REACTIONS = Set.of("👍", "❤️", "😂", "😮", "😢", "🔥");
  private static final Duration LINK_WINDOW = Duration.ofMinutes(10);
  private static final Duration LINK_BLOCK_DURATION = Duration.ofMinutes(15);
  private static final Duration HISTORY_CURSOR_WINDOW = Duration.ofDays(30);
  private static final int DEFAULT_PAGE_SIZE = 30;
  private static final int MAX_PAGE_SIZE = 100;
  private static final Set<String> BLACKLISTED_DOMAINS = Set.of(
      "bit.ly",
      "tinyurl.com",
      "t.co",
      "cutt.ly",
      "malware.example",
      "phishing.example"
  );

  private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile(
      "(?i)\\b((?:https?://|www\\.)[^\\s<>()]+)"
  );

  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageReactionRepository chatMessageReactionRepository;
  private final UserRepository userRepository;
  private final UserRelationRepository userRelationRepository;
  private final PodcastRepository podcastRepository;
  private final NotificationService notificationService;
  private final SimpMessagingTemplate messagingTemplate;

  private final ConcurrentHashMap<Long, AtomicInteger> activeSessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> sessionUsers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Deque<Instant>> messageWindows = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Deque<Instant>> linkWindows = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Instant> linkBlockedUntil = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<PushNotificationJob> pushQueue = new ConcurrentLinkedQueue<>();

  public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository,
      ChatMessageReactionRepository chatMessageReactionRepository,
      UserRepository userRepository,
      UserRelationRepository userRelationRepository,
      PodcastRepository podcastRepository,
      NotificationService notificationService,
      SimpMessagingTemplate messagingTemplate) {
    this.chatMessageRepository = chatMessageRepository;
    this.chatMessageReactionRepository = chatMessageReactionRepository;
    this.userRepository = userRepository;
    this.userRelationRepository = userRelationRepository;
    this.podcastRepository = podcastRepository;
    this.notificationService = notificationService;
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  @Transactional
  public ChatMessageDto sendMessage(Long senderId, ChatMessageRequest request) {
    User sender = userRepository.findById(senderId)
        .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Remetente não encontrado."));
    User recipient = resolveRecipient(request.recipientId());

    validateSendPermissions(senderId, recipient.getId());
    validateContent(request.content());
    enforceRateLimits(senderId, request.content());

    ChatMessage message = new ChatMessage();
    message.setSender(sender);
    message.setRecipient(recipient);
    message.setContent(request.content().trim());
    message.setMetadata(mapMetadata(request.metadata()));
    message.setStatus(ChatMessage.MessageStatus.SENT);

    ChatMessage saved = chatMessageRepository.save(message);
    ChatMessageDto dto = toDto(saved);

    if (isOnline(recipient.getId())) {
      messagingTemplate.convertAndSendToUser(
          recipient.getId().toString(),
          "/queue/messages",
          Map.of("eventType", "NEW_MESSAGE", "message", dto)
      );
    } else {
      pushQueue.add(new PushNotificationJob(recipient.getId(), sender.getId(), preview(saved.getContent()), saved.getId()));
    }

    return dto;
  }

  @Override
  @Transactional
  public ChatMessageDto acknowledgeMessage(Long userId, Long messageId, String acknowledgementType) {
    ChatMessage message = chatMessageRepository.findById(messageId)
        .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Mensagem não encontrada."));

    if (!Objects.equals(message.getRecipient().getId(), userId)) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "Só o destinatário pode confirmar esta mensagem.");
    }

    String normalized = acknowledgementType == null ? "" : acknowledgementType.trim().toUpperCase();
    Instant now = Instant.now();
    if ("DELIVERED".equals(normalized)) {
      if (message.getDeliveredAt() == null) {
        message.setDeliveredAt(now);
      }
      if (message.getStatus() == ChatMessage.MessageStatus.SENT) {
        message.setStatus(ChatMessage.MessageStatus.DELIVERED);
      }
    } else if ("READ".equals(normalized)) {
      if (message.getDeliveredAt() == null) {
        message.setDeliveredAt(now);
      }
      message.setReadAt(now);
      message.setStatus(ChatMessage.MessageStatus.READ);
    } else {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Tipo de ACK inválido.");
    }

    ChatMessage saved = chatMessageRepository.save(message);
    ChatMessageDto dto = toDto(saved);
    messagingTemplate.convertAndSendToUser(
        saved.getSender().getId().toString(),
        "/queue/messages",
        Map.of("eventType", saved.getStatus().name(), "message", dto)
    );
    return dto;
  }

  @Override
  @Transactional
  public ChatReactionUpdateResponse reactToMessage(Long userId, Long messageId,
      ChatReactionRequest request) {
    ChatMessage message = loadAccessibleMessage(userId, messageId);
    String emoji = validateReactionEmoji(request.emoji());
    Instant clientEventAt = request.clientEventAt() == null ? Instant.now() : request.clientEventAt();

    ChatMessageReaction existingReaction = chatMessageReactionRepository
        .findByMessageIdAndUserId(messageId, userId)
        .orElse(null);

    if (existingReaction != null && !clientEventAt.isAfter(existingReaction.getClientEventAt())) {
      return new ChatReactionUpdateResponse(messageId, "IGNORED_STALE",
          reactionSummary(existingReaction, userId), aggregateReactions(message, userId));
    }

    String action;
    ChatReactionSummaryDto primaryReaction;

    if (existingReaction != null && emoji.equals(existingReaction.getEmoji())) {
      chatMessageReactionRepository.delete(existingReaction);
      action = "REMOVED";
      primaryReaction = new ChatReactionSummaryDto(emoji, 0L, List.of(), true);
    } else {
      ChatMessageReaction reaction = existingReaction == null ? new ChatMessageReaction() : existingReaction;
      reaction.setMessage(message);
      reaction.setUser(userRepository.findById(userId)
          .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Utilizador não encontrado.")));
      reaction.setEmoji(emoji);
      reaction.setClientEventAt(clientEventAt);
      chatMessageReactionRepository.save(reaction);
      action = existingReaction == null ? "ADDED" : "UPDATED";
      primaryReaction = reactionSummary(reaction, userId);
    }

    List<ChatReactionSummaryDto> reactions = aggregateReactions(message, userId);
    ChatReactionUpdateResponse response = new ChatReactionUpdateResponse(messageId, action,
        primaryReaction, reactions);
    broadcastReactionUpdate(message, response);
    return response;
  }

  @Override
  @Transactional
  public void deleteMessage(Long userId, Long messageId) {
    ChatMessage message = chatMessageRepository.findById(messageId)
        .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Mensagem não encontrada."));
    if (!Objects.equals(message.getSender().getId(), userId)) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "Só o remetente pode apagar a mensagem.");
    }
    chatMessageRepository.delete(message);
  }

  @Override
  @Transactional(readOnly = true)
  public ChatMessageHistoryResponse getConversation(Long userId, Long friendId, String cursor,
      int limit) {
    if (!userRepository.existsById(userId)) {
      throw new ChatMessageException(HttpStatus.NOT_FOUND, "Utilizador autenticado não encontrado.");
    }
    if (!userRepository.existsById(friendId)) {
      throw new ChatMessageException(HttpStatus.NOT_FOUND, "Destinatário não encontrado.");
    }

    if (limit <= 0) {
      limit = DEFAULT_PAGE_SIZE;
    }
    limit = Math.min(limit, MAX_PAGE_SIZE);

    Cursor cursorValue = parseCursor(cursor);
    List<ChatMessage> messages = chatMessageRepository.findConversationPage(
        userId,
        friendId,
        cursorValue == null ? null : cursorValue.createdAt,
        cursorValue == null ? null : cursorValue.id,
        PageRequest.of(0, limit + 1)
    );

    boolean hasMore = messages.size() > limit;
    if (hasMore) {
      messages = messages.subList(0, limit);
    }

    List<ChatMessage> chronological = new ArrayList<>(messages);
    java.util.Collections.reverse(chronological);
    List<ChatMessageDto> dtoMessages = chronological.stream().map(message -> toDto(message, userId)).toList();
    String nextCursor = hasMore && !chronological.isEmpty()
        ? encodeCursor(chronological.get(0).getCreatedAt(), chronological.get(0).getId())
        : null;

    return new ChatMessageHistoryResponse(dtoMessages, nextCursor, hasMore);
  }

  @Override
  public boolean isOnline(Long userId) {
    AtomicInteger count = activeSessions.get(userId);
    return count != null && count.get() > 0;
  }

  @Override
  public void handleConnect(Principal principal, String sessionId) {
    if (principal == null || principal.getName() == null) {
      return;
    }
    Long userId = Long.valueOf(principal.getName());
    if (sessionId != null) {
      sessionUsers.put(sessionId, userId);
    }
    activeSessions.compute(userId, (key, value) -> {
      AtomicInteger counter = value == null ? new AtomicInteger(0) : value;
      counter.incrementAndGet();
      return counter;
    });
  }

  @Override
  public void handleDisconnect(String sessionId) {
    if (sessionId == null) {
      return;
    }
    Long userId = sessionUsers.remove(sessionId);
    if (userId == null) {
      return;
    }
    activeSessions.computeIfPresent(userId, (key, counter) -> {
      int remaining = counter.decrementAndGet();
      return remaining <= 0 ? null : counter;
    });
  }

  @EventListener
  public void onSessionConnected(SessionConnectedEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    handleConnect(accessor.getUser(), accessor.getSessionId());
  }

  @EventListener
  public void onSessionDisconnect(SessionDisconnectEvent event) {
    handleDisconnect(event.getSessionId());
  }

  @Scheduled(fixedDelay = 2000)
  public void processPushQueue() {
    PushNotificationJob job;
    while ((job = pushQueue.poll()) != null) {
      String message = String.format("senderId=%d; messageId=%d; preview=%s",
          job.senderId(), job.messageId(), job.preview());
      notificationService.sendNotification(job.recipientId().toString(), message);
    }
  }

  private void validateSendPermissions(Long senderId, Long recipientId) {
    Optional<UserRelation> relationOpt = userRelationRepository.findRelationship(senderId, recipientId);
    if (relationOpt.isEmpty()) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "Só pode enviar mensagens a amigos.");
    }

    UserRelation relation = relationOpt.get();
    if (relation.getType() == UserRelation.RelationType.BLOQUEADO) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "A mensagem foi bloqueada por uma relação de bloqueio ativa.");
    }

    if (relation.getType() != UserRelation.RelationType.AMIGO) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "Só pode enviar mensagens a amigos.");
    }

    Optional<UserRelation> inverseBlock = userRelationRepository.findRelationship(recipientId, senderId)
        .filter(relationItem -> relationItem.getType() == UserRelation.RelationType.BLOQUEADO);
    if (inverseBlock.isPresent()) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "A mensagem foi bloqueada por uma relação de bloqueio ativa.");
    }
  }

  private void validateContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "A mensagem não pode ser vazia.");
    }
    if (content.trim().length() > MAX_CONTENT_LENGTH) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "A mensagem excede o limite de 2000 caracteres.");
    }
  }

  private void enforceRateLimits(Long senderId, String content) {
    Instant now = Instant.now();
    trimAndCount(messageWindows.computeIfAbsent(senderId, key -> new ArrayDeque<>()), now,
        Duration.ofHours(1));
    trimAndCount(linkWindows.computeIfAbsent(senderId, key -> new ArrayDeque<>()), now,
        LINK_WINDOW);

    Deque<Instant> messageWindow = messageWindows.get(senderId);
    Deque<Instant> linkWindow = linkWindows.get(senderId);

    if (messageWindow.size() >= MAX_MESSAGES_PER_HOUR) {
      throw new ChatMessageException(HttpStatus.TOO_MANY_REQUESTS, "Ultrapassou o limite de mensagens por hora.");
    }
    if (messageWindow.stream().filter(timestamp -> timestamp.isAfter(now.minus(Duration.ofMinutes(1)))).count()
        >= MAX_MESSAGES_PER_MINUTE) {
      throw new ChatMessageException(HttpStatus.TOO_MANY_REQUESTS, "Ultrapassou o limite de mensagens por minuto.");
    }

    if (linkBlockedUntil.getOrDefault(senderId, Instant.EPOCH).isAfter(now)) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Envio temporariamente bloqueado devido a links suspeitos.");
    }

    List<String> links = extractUrls(content);
    if (!links.isEmpty()) {
      for (String link : links) {
        validateLink(link);
      }
      for (int i = 0; i < links.size(); i++) {
        linkWindow.addLast(now);
      }
      long linkCount = linkWindow.stream().filter(timestamp -> timestamp.isAfter(now.minus(LINK_WINDOW))).count();
      if (linkCount > MAX_LINKS_PER_WINDOW) {
        linkBlockedUntil.put(senderId, now.plus(LINK_BLOCK_DURATION));
        throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Excedeu o limite de links permitido em curto espaço de tempo.");
      }
    }

    messageWindow.addLast(now);
  }

  private void validateLink(String rawLink) {
    try {
      String normalized = rawLink.startsWith("http://") || rawLink.startsWith("https://")
          ? rawLink
          : "https://" + rawLink;
      java.net.URI uri = java.net.URI.create(normalized);
      String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase();
      for (String blockedDomain : BLACKLISTED_DOMAINS) {
        if (host.equals(blockedDomain) || host.endsWith("." + blockedDomain) || host.contains(blockedDomain)) {
          throw new ChatMessageException(HttpStatus.BAD_REQUEST, "A mensagem contém um domínio suspeito bloqueado.");
        }
      }
    } catch (IllegalArgumentException exception) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "A mensagem contém uma URL inválida.");
    }
  }

  private List<String> extractUrls(String content) {
    List<String> links = new ArrayList<>();
    java.util.regex.Matcher matcher = URL_PATTERN.matcher(content == null ? "" : content);
    while (matcher.find()) {
      links.add(matcher.group(1));
    }
    return links;
  }

  private void trimAndCount(Deque<Instant> deque, Instant now, Duration window) {
    Instant cutoff = now.minus(window);
    while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
      deque.removeFirst();
    }
  }

  private ChatMessageMetadata mapMetadata(ChatMessageAttachmentRequest request) {
    if (request == null) {
      return null;
    }
    if (request.type() == null || request.type().isBlank()) {
      return null;
    }

    String normalizedType = request.type().trim().toLowerCase();
    if (request.podcastId() != null && !podcastRepository.existsById(request.podcastId())) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "O podcast anexado não existe no catálogo.");
    }
    if (request.episodeId() != null && !podcastRepository.existsById(request.episodeId())) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "O episódio anexado não existe no catálogo.");
    }
    if (!Set.of("podcast", "episode").contains(normalizedType)) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Tipo de anexo inválido.");
    }

    ChatMessageMetadata metadata = new ChatMessageMetadata();
    metadata.setType(normalizedType);
    metadata.setPodcastId(request.podcastId());
    metadata.setEpisodeId(request.episodeId());
    return metadata;
  }

  private User resolveRecipient(Long recipientId) {
    if (recipientId == null) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Destinatário obrigatório.");
    }
    return userRepository.findById(recipientId)
        .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Destinatário não encontrado."));
  }

  private ChatMessageDto toDto(ChatMessage message) {
    return toDto(message, null);
  }

  private ChatMessageDto toDto(ChatMessage message, Long viewerId) {
    return new ChatMessageDto(
        message.getId(),
        message.getSender().getId(),
        message.getRecipient().getId(),
        message.getContent(),
        message.getStatus().name(),
        message.getCreatedAt(),
        message.getDeliveredAt(),
        message.getReadAt(),
        message.getMetadata() == null ? null : new ChatMessageAttachmentRequest(
            message.getMetadata().getType(),
            message.getMetadata().getPodcastId(),
            message.getMetadata().getEpisodeId()),
        aggregateReactions(message, viewerId)
    );
  }

  private ChatMessage loadAccessibleMessage(Long userId, Long messageId) {
    ChatMessage message = chatMessageRepository.findById(messageId)
        .orElseThrow(() -> new ChatMessageException(HttpStatus.NOT_FOUND, "Mensagem não encontrada."));
    boolean isParticipant = Objects.equals(message.getSender().getId(), userId)
        || Objects.equals(message.getRecipient().getId(), userId);
    if (!isParticipant) {
      throw new ChatMessageException(HttpStatus.FORBIDDEN, "Não tem acesso a esta mensagem.");
    }
    validateConversationNotBlocked(message.getSender().getId(), message.getRecipient().getId(), userId);
    return message;
  }

  private void validateConversationNotBlocked(Long senderId, Long recipientId, Long userId) {
    Optional<UserRelation> relation = userRelationRepository.findRelationship(senderId, recipientId);
    if (relation.isPresent() && relation.get().getType() == UserRelation.RelationType.BLOQUEADO) {
      boolean userIsParticipant = Objects.equals(userId, senderId) || Objects.equals(userId, recipientId);
      if (userIsParticipant) {
        throw new ChatMessageException(HttpStatus.FORBIDDEN, "A conversa encontra-se bloqueada.");
      }
    }
  }

  private String validateReactionEmoji(String emoji) {
    if (emoji == null || emoji.isBlank()) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Emoji de reação obrigatório.");
    }
    if (!ALLOWED_REACTIONS.contains(emoji)) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Emoji não permitido.");
    }
    return emoji;
  }

  private List<ChatReactionSummaryDto> aggregateReactions(ChatMessage message, Long viewerId) {
    Map<String, List<Long>> grouped = new LinkedHashMap<>();
    for (ChatMessageReaction reaction : chatMessageReactionRepository.findByMessageId(message.getId())) {
      grouped.computeIfAbsent(reaction.getEmoji(), key -> new ArrayList<>()).add(reaction.getUser().getId());
    }

    List<ChatReactionSummaryDto> result = new ArrayList<>();
    for (Map.Entry<String, List<Long>> entry : grouped.entrySet()) {
      List<Long> reactorIds = List.copyOf(entry.getValue());
      result.add(new ChatReactionSummaryDto(
          entry.getKey(),
          reactorIds.size(),
          reactorIds,
          viewerId != null && reactorIds.contains(viewerId)
      ));
    }
    return result;
  }

  private ChatReactionSummaryDto reactionSummary(ChatMessageReaction reaction, Long viewerId) {
    return new ChatReactionSummaryDto(
        reaction.getEmoji(),
        1L,
        List.of(reaction.getUser().getId()),
        viewerId != null && Objects.equals(reaction.getUser().getId(), viewerId)
    );
  }

  private void broadcastReactionUpdate(ChatMessage message, ChatReactionUpdateResponse response) {
    List<Long> participants = List.of(message.getSender().getId(), message.getRecipient().getId());
    for (Long participantId : participants) {
      messagingTemplate.convertAndSendToUser(
          participantId.toString(),
          "/queue/messages",
          Map.of("eventType", "REACTION_UPDATED", "messageId", response.messageId(), "reaction", response)
      );
    }
  }

  private Cursor parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    String[] parts = cursor.split("\\|", 2);
    if (parts.length != 2) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Cursor inválido.");
    }
    try {
      return new Cursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
    } catch (Exception exception) {
      throw new ChatMessageException(HttpStatus.BAD_REQUEST, "Cursor inválido.");
    }
  }

  private String encodeCursor(Instant createdAt, Long id) {
    return createdAt.toString() + "|" + id;
  }

  private String preview(String content) {
    if (content == null) {
      return "";
    }
    String trimmed = content.trim();
    return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120);
  }

  private record Cursor(Instant createdAt, Long id) {
  }

  private record PushNotificationJob(Long recipientId, Long senderId, String preview, Long messageId) {
  }
}