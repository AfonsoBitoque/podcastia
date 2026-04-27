package com.jep.servidor.config;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Interceta o handshake WebSocket e valida o JWT antes de aceitar a ligação.
 */
@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  public JwtWebSocketHandshakeInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
  }

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) {
    String token = resolveToken(request);
    if (token == null || token.isBlank()) {
      return false;
    }

    try {
      Long userId = jwtUtil.extractClaim(token, claims -> {
        Number value = claims.get("id", Number.class);
        return value == null ? null : value.longValue();
      });
      String email = jwtUtil.extractEmail(token);

      if (userId == null || email == null || !jwtUtil.isTokenValid(token, email)) {
        return false;
      }

      Optional<User> userOpt = userRepository.findById(userId)
          .filter(user -> user.getStatus() == User.UserStatus.ACTIVE)
          .filter(user -> email.equals(user.getEmail()));

      if (userOpt.isEmpty()) {
        return false;
      }

      attributes.put("socketUserId", userId);
      attributes.put("socketUserEmail", email);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    // Sem ação adicional.
  }

  private String resolveToken(ServerHttpRequest request) {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      HttpServletRequest nativeRequest = servletRequest.getServletRequest();
      String authHeader = nativeRequest.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
      }
    }

    URI uri = request.getURI();
    String query = uri.getQuery();
    if (query == null || query.isBlank()) {
      return null;
    }

    for (String pair : query.split("&")) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2) {
        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
        if (List.of("token", "access_token", "authToken").contains(key)) {
          return value;
        }
      }
    }

    return null;
  }
}