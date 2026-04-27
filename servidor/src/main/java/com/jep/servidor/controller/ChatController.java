package com.jep.servidor.controller;

import com.jep.servidor.dto.ChatMessageHistoryResponse;
import com.jep.servidor.dto.ChatReactionRequest;
import com.jep.servidor.dto.ChatReactionUpdateResponse;
import com.jep.servidor.exceptions.ChatMessageException;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.ChatMessageService;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint para histórico de conversas.
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

  private final ChatMessageService chatMessageService;
  private final UserRepository userRepository;

  public ChatController(ChatMessageService chatMessageService, UserRepository userRepository) {
    this.chatMessageService = chatMessageService;
    this.userRepository = userRepository;
  }

  @GetMapping("/{friendId}/messages")
  public ResponseEntity<?> getMessages(@PathVariable Long friendId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "30") int limit) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      ChatMessageHistoryResponse response = chatMessageService.getConversation(
          authUser.get().getId(),
          friendId,
          cursor,
          limit);
      return ResponseEntity.ok(response);
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  @PostMapping("/messages/{messageId}/reactions")
  public ResponseEntity<?> reactToMessage(@PathVariable Long messageId,
      @RequestBody ChatReactionRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      ChatReactionUpdateResponse response = chatMessageService.reactToMessage(
          authUser.get().getId(), messageId, request);
      return ResponseEntity.ok(response);
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  @DeleteMapping("/messages/{messageId}")
  public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      chatMessageService.deleteMessage(authUser.get().getId(), messageId);
      return ResponseEntity.noContent().build();
    } catch (ChatMessageException exception) {
      return ResponseEntity.status(exception.getStatus()).body(Map.of("error", exception.getMessage()));
    }
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }
}