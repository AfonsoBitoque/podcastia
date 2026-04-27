package com.jep.servidor.service;

import com.jep.servidor.dto.ChatMessageDto;
import com.jep.servidor.dto.ChatMessageHistoryResponse;
import com.jep.servidor.dto.ChatMessageRequest;
import java.security.Principal;

/**
 * Serviço de mensagens privadas.
 */
public interface ChatMessageService {

  ChatMessageDto sendMessage(Long senderId, ChatMessageRequest request);

  ChatMessageDto acknowledgeMessage(Long userId, Long messageId, String acknowledgementType);

  ChatMessageHistoryResponse getConversation(Long userId, Long friendId, String cursor,
      int limit);

  boolean isOnline(Long userId);

  void handleConnect(Principal principal, String sessionId);

  void handleDisconnect(String sessionId);
}