package com.jep.servidor.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração STOMP/WebSocket para mensagens em tempo real.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtWebSocketHandshakeInterceptor handshakeInterceptor;

  public WebSocketConfig(JwtWebSocketHandshakeInterceptor handshakeInterceptor) {
    this.handshakeInterceptor = handshakeInterceptor;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat")
        .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
        .addInterceptors(handshakeInterceptor)
        .setHandshakeHandler(new JwtWebSocketHandshakeHandler());
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }
}