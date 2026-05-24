package com.jep.servidor.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração STOMP/WebSocket para suporte a mensagens em tempo real na plataforma Podcastia.
 *
 * <p>Ativa o broker de mensagens STOMP via {@code @EnableWebSocketMessageBroker}, que
 * configura o Spring para gerir ligações WebSocket com o protocolo STOMP
 * (Simple Text Orientated Messaging Protocol) sobre WebSocket.
 *
 * <p><b>Endpoint WebSocket:</b>
 * <ul>
 *   <li>URL: {@code ws://localhost:8080/ws/chat}</li>
 *   <li>Origens permitidas: {@code localhost:5173} e {@code 127.0.0.1:5173}
 *       (frontend React/Vite em desenvolvimento).</li>
 *   <li>Autenticação: via {@link JwtWebSocketHandshakeInterceptor} que valida o JWT
 *       durante o upgrade HTTP → WebSocket.</li>
 *   <li>Principal: determinado pelo {@link JwtWebSocketHandshakeHandler} com base
 *       no {@code socketUserId} injetado pelo interceptor.</li>
 * </ul>
 *
 * <p><b>Configuração do broker de mensagens:</b>
 * <ul>
 *   <li><b>Prefixo de aplicação ({@code /app}):</b> Mensagens enviadas para destinos com
 *       este prefixo (ex: {@code /app/chat.send}) são encaminhadas para métodos
 *       {@code @MessageMapping} nos controllers STOMP
 *       ({@link com.jep.servidor.controller.ChatWebSocketController}).</li>
 *   <li><b>Broker simples ({@code /topic}, {@code /queue}):</b> Broker em memória do Spring
 *       para distribuição de mensagens. {@code /topic} é usado para broadcast a múltiplos
 *       subscritores; {@code /queue} para mensagens ponto-a-ponto.</li>
 *   <li><b>Prefixo de utilizador ({@code /user}):</b> Permite enviar mensagens a um
 *       utilizador específico via {@code /user/{id}/queue/messages}. O Spring resolve
 *       automaticamente o ID a partir do {@link java.security.Principal} da sessão.</li>
 * </ul>
 *
 * <p><b>Fluxo de uma mensagem de chat:</b>
 * <ol>
 *   <li>Cliente A envia para {@code /app/chat.send} com payload JSON.</li>
 *   <li>{@code @MessageMapping("/chat.send")} no {@code ChatWebSocketController} processa.</li>
 *   <li>O controller usa {@code SimpMessagingTemplate.convertAndSendToUser(recipientId, ...)}
 *       para enviar para {@code /user/{recipientId}/queue/messages}.</li>
 *   <li>Cliente B, subscrito em {@code /user/queue/messages}, recebe a mensagem.</li>
 * </ol>
 *
 * @see JwtWebSocketHandshakeInterceptor
 * @see JwtWebSocketHandshakeHandler
 * @see com.jep.servidor.controller.ChatWebSocketController
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  /**
   * Interceptor JWT injetado, responsável por validar o token e popular os atributos
   * da sessão WebSocket ({@code socketUserId}, {@code socketUserEmail}).
   */
  private final JwtWebSocketHandshakeInterceptor handshakeInterceptor;

  /**
   * Cria a configuração WebSocket com o interceptor de autenticação JWT.
   *
   * @param handshakeInterceptor interceptor que valida o JWT no handshake WebSocket.
   */
  public WebSocketConfig(JwtWebSocketHandshakeInterceptor handshakeInterceptor) {
    this.handshakeInterceptor = handshakeInterceptor;
  }

  /**
   * Regista o endpoint STOMP WebSocket {@code /ws/chat}.
   *
   * <p>Configura:
   * <ul>
   *   <li>Origens permitidas para o handshake WebSocket.</li>
   *   <li>O interceptor JWT para autenticação durante o upgrade HTTP.</li>
   *   <li>O handler de handshake para criação do {@link java.security.Principal}
   *       da sessão.</li>
   * </ul>
   *
   * <p><b>Nota:</b> SockJS não está ativado ({@code withSockJS()} não é chamado),
   * pelo que o cliente deve usar WebSocket nativo. Clientes que não suportam
   * WebSocket nativo não poderão conectar-se.
   *
   * @param registry registo de endpoints STOMP do Spring WebSocket.
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat")
        .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
        .addInterceptors(handshakeInterceptor)
        .setHandshakeHandler(new JwtWebSocketHandshakeHandler());
  }

  /**
   * Configura o broker de mensagens STOMP em memória.
   *
   * <p>Prefixos configurados:
   * <ul>
   *   <li>{@code /app} — destinos processados por {@code @MessageMapping} nos controllers.</li>
   *   <li>{@code /topic} e {@code /queue} — destinos geridos pelo broker simples em memória.</li>
   *   <li>{@code /user} — prefixo para mensagens dirigidas a utilizadores específicos
   *       ({@code /user/{principal}/queue/messages}).</li>
   * </ul>
   *
   * @param registry registo do broker de mensagens STOMP.
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }
}