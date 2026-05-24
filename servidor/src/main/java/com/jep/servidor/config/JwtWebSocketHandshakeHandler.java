package com.jep.servidor.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Handler de handshake WebSocket responsável por criar o {@link Principal} da ligação
 * STOMP com base no ID do utilizador autenticado via JWT.
 *
 * <p>Estende {@link DefaultHandshakeHandler} e sobrepõe apenas o método
 * {@link #determineUser}, que é chamado pelo Spring durante o upgrade HTTP → WebSocket
 * para determinar o principal (identidade) da sessão WebSocket.
 *
 * <p><b>Integração com o fluxo de autenticação WebSocket:</b>
 * <ol>
 *   <li>O {@link JwtWebSocketHandshakeInterceptor#beforeHandshake} valida o token JWT
 *       e, se válido, coloca o ID do utilizador no mapa de atributos da sessão
 *       sob a chave {@code "socketUserId"}.</li>
 *   <li>Este handler lê esse atributo e cria um {@link Principal} anónimo cujo
 *       {@code getName()} retorna o ID do utilizador como string.</li>
 *   <li>O Spring STOMP usa este principal para encaminhar mensagens para o utilizador
 *       correto via {@code /user/{id}/queue/messages}.</li>
 * </ol>
 *
 * <p><b>Formato do principal:</b> O nome do principal é o ID do utilizador (Long)
 * convertido para String. Isto é consistente com o uso em
 * {@link com.jep.servidor.controller.ChatWebSocketController} onde
 * {@code principal.getName()} é interpretado como ID para operações de chat.
 *
 * <p><b>Nota:</b> Se o atributo {@code "socketUserId"} estiver ausente (handshake
 * rejeitado pelo interceptor ou ligação anónima), retorna {@code null}, o que fará
 * o Spring tratar a sessão como sem principal — sem acesso a filas de utilizador.
 *
 * @see JwtWebSocketHandshakeInterceptor
 * @see WebSocketConfig
 * @see com.jep.servidor.controller.ChatWebSocketController
 */
public class JwtWebSocketHandshakeHandler extends DefaultHandshakeHandler {

  /**
   * Determina o {@link Principal} para a sessão WebSocket com base no atributo
   * {@code "socketUserId"} injetado pelo {@link JwtWebSocketHandshakeInterceptor}.
   *
   * @param request    pedido HTTP de upgrade para WebSocket.
   * @param wsHandler  handler WebSocket associado à ligação.
   * @param attributes mapa de atributos da sessão WebSocket, populado pelo interceptor.
   * @return um {@link Principal} cujo {@code getName()} retorna o ID do utilizador
   *         como String, ou {@code null} se o atributo não estiver presente.
   */
  @Override
  protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    Object socketUserId = attributes.get("socketUserId");
    if (socketUserId == null) {
      return null;
    }
    return () -> socketUserId.toString();
  }
}