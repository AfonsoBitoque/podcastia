package com.jep.servidor.controller;

import com.jep.servidor.dto.ChatMessageDto;
import com.jep.servidor.dto.ChatMessageRequest;
import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.exceptions.ChatMessageException;
import com.jep.servidor.service.ChatMessageService;
import java.security.Principal;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Endpoint STOMP para envio e ACK de mensagens em tempo real.
 */
@Controller
public class ChatWebSocketController {

  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  public ChatWebSocketController(ChatMessageService chatMessageService,
      SimpMessagingTemplate messagingTemplate) {
    this.chatMessageService = chatMessageService;
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/chat.send")
  public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
    try {
      if (principal == null) {
        throw new ChatMessageException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Ligação WebSocket não autenticada.");
      }
      ChatMessageDto message = chatMessageService.sendMessage(Long.valueOf(principal.getName()), request);
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/messages",
          Map.of("eventType", "SENT", "message", message)
      );
    } catch (Exception exception) {
      String errorMessage = exception instanceof ChatMessageException
          ? exception.getMessage()
          : "Falha ao enviar a mensagem.";
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/messages",
          Map.of("eventType", "ERROR", "message", errorMessage)
      );
    }
  }

  @MessageMapping("/chat.ack")
  public void acknowledge(@Payload Map<String, Object> payload, Principal principal) {
    try {
      if (principal == null) {
        throw new ChatMessageException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Ligação WebSocket não autenticada.");
      }
      Long messageId = payload.get("messageId") == null ? null : Long.valueOf(payload.get("messageId").toString());
      String acknowledgementType = payload.get("type") == null ? null : payload.get("type").toString();
      ChatMessageDto message = chatMessageService.acknowledgeMessage(
          Long.valueOf(principal.getName()),
          messageId,
          acknowledgementType);
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/messages",
          Map.of("eventType", message.status(), "message", message)
      );
    } catch (Exception exception) {
      String errorMessage = exception instanceof ChatMessageException
          ? exception.getMessage()
          : "Falha ao processar o ACK.";
      messagingTemplate.convertAndSendToUser(
          principal.getName(),
          "/queue/messages",
          Map.of("eventType", "ERROR", "message", errorMessage)
      );
    }
  }

  @MessageMapping("/chat.reaction")
  public void react(@Payload Map<String, Object> payload, Principal principal) {
    try {
      if (principal == null) {
        throw new ChatMessageException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Ligação WebSocket não autenticada.");
      }
      Long messageId = payload.get("messageId") == null ? null : Long.valueOf(payload.get("messageId").toString());
      String emoji = payload.get("emoji") == null ? null : payload.get("emoji").toString();
      java.time.Instant clientEventAt = payload.get("clientEventAt") == null
          ? null
          : java.time.Instant.parse(payload.get("clientEventAt").toString());
      ChatReactionRequest request = new ChatReactionRequest(emoji, clientEventAt);
      chatMessageService.reactToMessage(Long.valueOf(principal.getName()), messageId, request);
    } catch (Exception exception) {
      String errorMessage = exception instanceof ChatMessageException
          ? exception.getMessage()
          : "Falha ao processar a reação.";
      messagingTemplate.convertAndSendToUser(
          principal == null ? null : principal.getName(),
          "/queue/messages",
          Map.of("eventType", "ERROR", "message", errorMessage)
      );
    }
  }
}