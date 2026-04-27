package com.jep.servidor.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Cria o principal do WebSocket com base no utilizador autenticado pelo JWT.
 */
public class JwtWebSocketHandshakeHandler extends DefaultHandshakeHandler {

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