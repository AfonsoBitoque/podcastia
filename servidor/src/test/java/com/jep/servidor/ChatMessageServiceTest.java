package com.jep.servidor;

import com.jep.servidor.dto.ChatMessageAttachmentRequest;
import com.jep.servidor.dto.ChatMessageRequest;
import com.jep.servidor.exceptions.ChatMessageException;
import java.util.List;
import com.jep.servidor.model.ChatMessage;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.model.UserRelation;
import com.jep.servidor.repository.ChatMessageRepository;
import com.jep.servidor.repository.ChatMessageReactionRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRelationRepository;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.NotificationService;
import com.jep.servidor.service.impl.ChatMessageServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

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

  @BeforeEach
  void setUp() {
    sender = new User();
    sender.setId(1L);
    sender.setUsername("sender");
    sender.setEmail("sender@example.com");

    recipient = new User();
    recipient.setId(2L);
    recipient.setUsername("recipient");
    recipient.setEmail("recipient@example.com");
  }

  @Test
  void sendMessageRejectsWhenNotFriends() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
    when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
    when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.empty());

    ChatMessageException exception = assertThrows(ChatMessageException.class, () ->
        chatMessageService.sendMessage(1L, new ChatMessageRequest(2L, "Olá", null)));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
  }

  @Test
  void sendMessageQueuesNotificationWhenRecipientOffline() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
    when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

    UserRelation relation = new UserRelation(sender, recipient, UserRelation.RelationType.AMIGO);
    when(userRelationRepository.findRelationship(1L, 2L)).thenReturn(Optional.of(relation));
    when(userRelationRepository.findRelationship(2L, 1L)).thenReturn(Optional.of(relation));

    when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
      ChatMessage message = invocation.getArgument(0);
      message.setId(99L);
      return message;
    });
    when(chatMessageReactionRepository.findByMessageId(anyLong())).thenReturn(List.of());

    chatMessageService.sendMessage(1L, new ChatMessageRequest(2L, "Mensagem offline", null));
    chatMessageService.processPushQueue();

    verify(notificationService).sendNotification(any(), any());
    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
  }

  @Test
  void acknowledgeDeliveredUpdatesMessageAndNotifiesSender() {
    ChatMessage message = new ChatMessage();
    message.setId(10L);
    message.setSender(sender);
    message.setRecipient(recipient);
    message.setContent("Olá");
    message.setStatus(ChatMessage.MessageStatus.SENT);

    when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
    when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(chatMessageReactionRepository.findByMessageId(anyLong())).thenReturn(List.of());

    chatMessageService.acknowledgeMessage(2L, 10L, "DELIVERED");

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSendToUser(any(), any(), payloadCaptor.capture());
    assertEquals(ChatMessage.MessageStatus.DELIVERED.name(), message.getStatus().name());
  }
}