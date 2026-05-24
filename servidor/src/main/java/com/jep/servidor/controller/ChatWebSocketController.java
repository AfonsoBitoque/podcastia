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
 * Controller STOMP responsável pelo processamento em tempo real de mensagens de chat,
 * acknowledgements de entrega/leitura e reações emoji.
 *
 * <p>Anotado com {@code @Controller} (não {@code @RestController}) pois os métodos
 * {@code @MessageMapping} não retornam respostas HTTP — as respostas são enviadas
 * diretamente para filas de utilizador via {@link SimpMessagingTemplate}.
 *
 * <p><b>Destinos STOMP mapeados (prefixo {@code /app} configurado em
 * {@link com.jep.servidor.config.WebSocketConfig}):</b>
 * <ul>
 *   <li>{@code /app/chat.send} — envio de uma nova mensagem.</li>
 *   <li>{@code /app/chat.ack} — acknowledgement de entrega ({@code DELIVERED})
 *       ou leitura ({@code READ}) de uma mensagem.</li>
 *   <li>{@code /app/chat.reaction} — adição/remoção de reação emoji a uma mensagem.</li>
 * </ul>
 *
 * <p><b>Autenticação STOMP:</b> O {@link Principal} é injetado pelo Spring a partir
 * do {@link com.jep.servidor.config.JwtWebSocketHandshakeHandler}, cujo
 * {@code getName()} retorna o ID do utilizador como String. Se o principal for
 * {@code null}, o pedido é rejeitado com {@link ChatMessageException}.
 *
 * <p><b>Padrão de resposta:</b> Todas as respostas são enviadas para a fila privada
 * do remetente: {@code /user/{id}/queue/messages}. O payload é um mapa JSON com:
 * <ul>
 *   <li>{@code "eventType"} — tipo de evento ({@code "SENT"}, {@code "DELIVERED"},
 *       {@code "READ"}, {@code "ERROR"}).</li>
 *   <li>{@code "message"} — o {@link ChatMessageDto} atualizado ou mensagem de erro.</li>
 * </ul>
 *
 * <p><b>Tratamento de erros:</b> Exceções {@link ChatMessageException} retornam a
 * mensagem de erro original; outras exceções retornam uma mensagem genérica.
 * Em ambos os casos, o evento {@code "ERROR"} é enviado ao remetente.
 *
 * @see ChatController
 * @see com.jep.servidor.service.ChatMessageService
 * @see com.jep.servidor.config.WebSocketConfig
 */
@Controller
public class ChatWebSocketController {

  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Cria o controller com as dependências necessárias para processamento de mensagens STOMP.
   *
   * @param chatMessageService serviço de mensagens com lógica de negócio (rate limiting,
   *                           validação, persistência, notificações).
   * @param messagingTemplate  template STOMP para envio de mensagens para utilizadores específicos.
   */
  public ChatWebSocketController(ChatMessageService chatMessageService,
      SimpMessagingTemplate messagingTemplate) {
    this.chatMessageService = chatMessageService;
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Processa o envio de uma nova mensagem de chat em tempo real.
   *
   * <p>Delega para {@link com.jep.servidor.service.ChatMessageService#sendMessage} que
   * aplica rate limiting, validação de conteúdo (links blacklistados), verificação
   * de amizade/bloqueio e persistência. Após envio bem-sucedido, notifica o remetente
   * com o evento {@code "SENT"} e a mensagem criada.
   *
   * @param request   payload STOMP com o conteúdo e destinatário da mensagem
   *                  ({@link com.jep.servidor.dto.ChatMessageRequest}).
   * @param principal principal da sessão WebSocket; {@code getName()} = ID do remetente.
   */
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

  /**
   * Processa o acknowledgement de entrega ou leitura de uma mensagem.
   *
   * <p>O payload deve conter:
   * <ul>
   *   <li>{@code "messageId"} (Long) — ID da mensagem a confirmar.</li>
   *   <li>{@code "type"} (String) — tipo de ACK: {@code "DELIVERED"} ou {@code "READ"}.</li>
   * </ul>
   *
   * <p>Após processamento, envia ao remetente o evento com o novo estado da mensagem
   * (o {@code eventType} é o próprio {@code message.status()}).
   *
   * @param payload   mapa JSON desserializado com {@code messageId} e {@code type}.
   * @param principal principal da sessão WebSocket; {@code getName()} = ID do utilizador.
   */
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

  /**
   * Processa a adição ou remoção de uma reação emoji a uma mensagem via WebSocket.
   *
   * <p>O payload deve conter:
   * <ul>
   *   <li>{@code "messageId"} (Long) — ID da mensagem alvo.</li>
   *   <li>{@code "emoji"} (String) — emoji da reação (ex: {@code "👍"}).</li>
   *   <li>{@code "clientEventAt"} (String ISO-8601, opcional) — timestamp do evento
   *       no cliente, para sincronização de estado.</li>
   * </ul>
   *
   * <p>Não envia resposta em caso de sucesso. Em caso de erro, envia o evento
   * {@code "ERROR"} ao utilizador via a sua fila privada. Para obter o estado
   * atualizado das reações, usar {@code GET /api/chats/messages/{id}/reactions}.
   *
   * @param payload   mapa JSON com {@code messageId}, {@code emoji} e {@code clientEventAt}.
   * @param principal principal da sessão WebSocket; {@code getName()} = ID do utilizador.
   */
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