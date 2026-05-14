package com.jep.servidor;

import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.dto.ChatReactionUpdateResponse;
import com.jep.servidor.model.ChatMessage;
import com.jep.servidor.model.ChatMessageReaction;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.ChatMessageReactionRepository;
import com.jep.servidor.repository.ChatMessageRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.service.impl.ChatMessageServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.http.HttpStatus;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatReactionServiceTest {

  @InjectMocks
  private ChatMessageServiceImpl chatMessageService;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatMessageReactionRepository chatMessageReactionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserRelationRepository userRelationRepository;

  @Mock
  private PodcastRepository podcastRepository;

  @Mock
  private NotificationService notificationService;

  @Mock
  private SimpMessageSendingOperations messagingTemplate;

  private User sender;
  private User recipient;
  private ChatMessage message;

  @BeforeEach
  void setUp() {
    sender = new User();
    sender.setId(1L);
    sender.setEmail("sender@example.com");

    recipient = new User();
    recipient.setId(2L);
    recipient.setEmail("recipient@example.com");

    message = new ChatMessage();
    message.setId(10L);
    message.setSender(sender);
    message.setRecipient(recipient);
    message.setContent("hello");

    UserRelation relation = new UserRelation(sender, recipient, UserRelation.RelationType.AMIGO);
    when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(relation));
    when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.of(relation));
    when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
    when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
    when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
    when(chatMessageReactionRepository.findByMessageId(any())).thenReturn(List.of());
  }

  @Test
  void reactToMessageCreatesReactionAndAggregates() {
    when(chatMessageReactionRepository.findByMessageIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
    when(chatMessageReactionRepository.findByMessageId(10L)).thenReturn(List.of());
    when(chatMessageReactionRepository.save(any(ChatMessageReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ChatReactionUpdateResponse response = chatMessageService.reactToMessage(1L, 10L,
        new ChatReactionRequest("👍", Instant.parse("2026-04-28T10:00:00Z")));

    assertEquals("ADDED", response.action());
    assertEquals(10L, response.messageId());
    verify(messagingTemplate, org.mockito.Mockito.times(2)).convertAndSendToUser(any(), any(), any());
  }

  @Test
  void reactToMessageRejectsInvalidEmoji() {
    ChatMessageServiceImpl service = chatMessageService;
    when(chatMessageReactionRepository.findByMessageIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

    var exception = assertThrows(com.jep.servidor.exceptions.ChatMessageException.class, () ->
        service.reactToMessage(1L, 10L, new ChatReactionRequest("😎", Instant.now())));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
  }

  @Test
  void reactToMessageIgnoresStaleOfflineSync() {
    ChatMessageReaction existing = new ChatMessageReaction();
    existing.setMessage(message);
    existing.setUser(sender);
    existing.setEmoji("😂");
    existing.setClientEventAt(Instant.parse("2026-04-28T10:10:00Z"));
    when(chatMessageReactionRepository.findByMessageIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
    when(chatMessageReactionRepository.findByMessageId(10L)).thenReturn(List.of(existing));

    ChatReactionUpdateResponse response = chatMessageService.reactToMessage(1L, 10L,
        new ChatReactionRequest("🔥", Instant.parse("2026-04-28T10:05:00Z")));

    assertEquals("IGNORED_STALE", response.action());
    verify(chatMessageReactionRepository, never()).save(any());
  }
}