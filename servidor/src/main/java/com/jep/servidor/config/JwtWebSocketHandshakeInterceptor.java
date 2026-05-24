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
 * Interceptor de handshake WebSocket responsável por validar o token JWT
 * antes de aceitar uma ligação STOMP/WebSocket.
 *
 * <p>Implementa {@link HandshakeInterceptor} e é registado em {@link WebSocketConfig}
 * no endpoint {@code /ws}. É executado durante o upgrade HTTP → WebSocket, antes
 * de qualquer troca de mensagens STOMP.
 *
 * <p><b>Fluxo de validação em {@link #beforeHandshake}:</b>
 * <ol>
 *   <li>Extrai o token JWT via {@link #resolveToken} (cabeçalho {@code Authorization}
 *       ou parâmetro de query string).</li>
 *   <li>Extrai o {@code id} (Long) e o {@code email} do token via {@link JwtUtil}.</li>
 *   <li>Valida o token com {@link JwtUtil#isTokenValid(String, String)}.</li>
 *   <li>Verifica que o utilizador existe na base de dados, está {@code ACTIVE}
 *       e que o email do token corresponde ao email registado.</li>
 *   <li>Se tudo for válido, coloca {@code socketUserId} (Long) e
 *       {@code socketUserEmail} (String) nos atributos da sessão WebSocket.</li>
 *   <li>Retorna {@code true} para aceitar a ligação, ou {@code false} para recusar.</li>
 * </ol>
 *
 * <p><b>Extração do token ({@link #resolveToken}):</b> Suporta dois métodos:
 * <ul>
 *   <li>Cabeçalho HTTP {@code Authorization: Bearer <token>} — método preferido.</li>
 *   <li>Query string com parâmetros {@code token}, {@code access_token} ou {@code authToken}
 *       — fallback para clientes WebSocket que não suportam cabeçalhos personalizados
 *       (ex: alguns clientes de browser nativos).</li>
 * </ul>
 *
 * <p><b>Integração com o principal STOMP:</b> Os atributos {@code socketUserId}
 * e {@code socketUserEmail} são depois lidos pelo {@link JwtWebSocketHandshakeHandler}
 * para criar o {@link java.security.Principal} da sessão, permitindo ao Spring STOMP
 * encaminhar mensagens para filas de utilizador específicas
 * ({@code /user/{id}/queue/messages}).
 *
 * @see JwtWebSocketHandshakeHandler
 * @see WebSocketConfig
 * @see JwtUtil
 */
@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  /**
   * Cria uma instância do interceptor com as dependências necessárias para validação JWT.
   *
   * @param jwtUtil        utilitário para extração e validação de claims JWT.
   * @param userRepository repositório para verificação do estado do utilizador na BD.
   */
  public JwtWebSocketHandshakeInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
  }

  /**
   * Intercepta o pedido de handshake WebSocket e decide se a ligação deve ser aceite.
   *
   * <p>Valida o JWT e, se válido, popula os atributos {@code socketUserId} e
   * {@code socketUserEmail} na sessão WebSocket para uso posterior pelo
   * {@link JwtWebSocketHandshakeHandler}.
   *
   * @param request    pedido HTTP de upgrade WebSocket.
   * @param response   resposta HTTP do handshake.
   * @param wsHandler  handler WebSocket que irá gerir a ligação.
   * @param attributes mapa mutável de atributos da sessão WebSocket a popular.
   * @return {@code true} se o JWT for válido e o utilizador estiver ativo;
   *         {@code false} para rejeitar a ligação (HTTP 403).
   */
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

  /**
   * Chamado após o handshake ser completado (com ou sem sucesso).
   * Não executa nenhuma ação adicional nesta implementação.
   *
   * @param request   pedido HTTP do handshake.
   * @param response  resposta HTTP do handshake.
   * @param wsHandler handler WebSocket da ligação.
   * @param exception exceção ocorrida durante o handshake, ou {@code null} se bem-sucedido.
   */
  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {
    // Sem ação adicional.
  }

  /**
   * Extrai o token JWT do pedido de handshake WebSocket.
   *
   * <p>Tenta extrair o token por dois métodos, por ordem de preferência:
   * <ol>
   *   <li><b>Cabeçalho HTTP:</b> Se o pedido for um {@link ServletServerHttpRequest},
   *       lê o cabeçalho {@code Authorization} e extrai o token após o prefixo
   *       {@code "Bearer "}.</li>
   *   <li><b>Query string:</b> Percorre os parâmetros da URI e retorna o valor do
   *       primeiro parâmetro com nome {@code "token"}, {@code "access_token"} ou
   *       {@code "authToken"} (URL-decoded).</li>
   * </ol>
   *
   * @param request pedido HTTP de upgrade WebSocket.
   * @return string do token JWT extraído, ou {@code null} se não encontrado.
   */
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