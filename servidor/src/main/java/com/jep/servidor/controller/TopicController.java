package com.jep.servidor.controller;

import com.jep.servidor.dto.TopicResponse;
import com.jep.servidor.dto.TopicSelectionRequest;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.RecommendationService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para listagem e atualização dos tópicos de interesse dos utilizadores.
 *
 * <p>Os tópicos correspondem diretamente aos valores do enum {@link PodcastTag}
 * ({@code DESPORTO}, {@code POLITICA}, {@code FINANCAS}, {@code GERAL}) e são usados
 * pelo {@link RecommendationService} para personalizar o feed de podcasts.
 *
 * <p><b>Base path:</b> {@code /api} (nota: endpoints mapeados sob prefixos distintos)
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /api/topics} — lista todos os tópicos disponíveis (público).</li>
 *   <li>{@code PUT /api/users/{id}/topics} — atualiza os tópicos do utilizador
 *       (requer autenticação e o ID deve corresponder ao utilizador autenticado).</li>
 * </ul>
 *
 * @see PodcastTag
 * @see RecommendationService
 * @see com.jep.servidor.dto.TopicResponse
 * @see com.jep.servidor.dto.TopicSelectionRequest
 */
@RestController
@RequestMapping("/api")
public class TopicController {

  private final UserRepository userRepository;
  private final RecommendationService recommendationService;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param userRepository        repositório JPA de utilizadores.
   * @param recommendationService serviço de recomendação (também usado para invalidar cache).
   */
  public TopicController(UserRepository userRepository, RecommendationService recommendationService) {
    this.userRepository = userRepository;
    this.recommendationService = recommendationService;
  }

  /**
   * Resolve o utilizador autenticado a partir do contexto de segurança Spring.
   *
   * @return {@link Optional} com o utilizador, ou vazio se não autenticado.
   */
  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

  /**
   * Lista todos os tópicos/tags disponíveis, com filtragem opcional por texto.
   *
   * <p>Itera pelos valores de {@link PodcastTag} e aplica um filtro case-insensitive
   * pelo parâmetro {@code search} (corresponde ao nome do enum ou ao label human-readable).
   *
   * @param search termo de pesquisa opcional para filtrar tópicos por nome ou label.
   * @return {@code 200 OK} com lista de {@link com.jep.servidor.dto.TopicResponse};
   *         {@code 503 Service Unavailable} em caso de erro de acesso à BD;
   *         {@code 500 Internal Server Error} em caso de outro erro inesperado.
   */
  @GetMapping("/topics")
  public ResponseEntity<?> listTopics(@RequestParam(name = "search", required = false) String search) {
    try {
      String query = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
      List<TopicResponse> topics = new ArrayList<>();

      for (PodcastTag tag : PodcastTag.values()) {
        String label = toLabel(tag);
        if (query == null || query.isEmpty()
            || tag.name().toLowerCase(Locale.ROOT).contains(query)
            || label.toLowerCase(Locale.ROOT).contains(query)) {
          topics.add(new TopicResponse(tag.name(), label));
        }
      }

      return ResponseEntity.ok(topics);
    } catch (DataAccessException ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "topics-unavailable"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "topics-failed"));
    }
  }

  /**
   * Atualiza os tópicos de interesse do utilizador identificado por {@code id}.
   *
   * <p>Regras de validação:
   * <ul>
   *   <li>Só o próprio utilizador pode atualizar os seus tópicos (verificação por ID).</li>
   *   <li>IDs de tópico inválidos (não correspondêm a {@link PodcastTag}) são rejeitados
   *       com {@code 422 Unprocessable Entity} e lista dos IDs inválidos.</li>
   *   <li>Mínimo de 3 tópicos únicos (após deduplicar); menos retorna {@code 422}.</li>
   *   <li>Se o payload for nulo ou vazio, limpa todos os tópicos do utilizador.</li>
   * </ul>
   *
   * <p>Após guardar, invalida o cache do feed de recomendações via
   * {@link RecommendationService#invalidateFeedCache}.
   *
   * @param id      ID do utilizador cujos tópicos se pretendem atualizar.
   * @param request lista de IDs de tópico (valores de {@link PodcastTag}); pode ser nulo.
   * @return {@code 200 OK} com {@code {"topics": [...]}} em caso de sucesso;
   *         {@code 401 Unauthorized} se não autenticado;
   *         {@code 403 Forbidden} se o ID não corresponder ao utilizador autenticado;
   *         {@code 404 Not Found} se o utilizador não existir;
   *         {@code 422 Unprocessable Entity} se os IDs forem inválidos ou insuficientes;
   *         {@code 503 Service Unavailable} em caso de erro de BD.
   */
  @PutMapping("/users/{id}/topics")
  public ResponseEntity<?> saveTopics(
      @PathVariable("id") Long id,
      @RequestBody(required = false) TopicSelectionRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!authUser.get().getId().equals(id)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "forbidden"));
    }

    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "user-not-found"));
    }

    List<String> topicIds = request == null ? null : request.getTopicIds();
    if (topicIds == null || topicIds.isEmpty()) {
      User user = userOptional.get();
      user.setTopics(List.of());
      userRepository.save(user);
      recommendationService.invalidateFeedCache(user.getId());
      return ResponseEntity.ok(Map.of("topics", user.getTopics()));
    }

    List<String> invalidIds = new ArrayList<>();
    LinkedHashSet<PodcastTag> uniqueTopics = new LinkedHashSet<>();

    for (String rawId : topicIds) {
      PodcastTag tag = parseTag(rawId);
      if (tag == null) {
        invalidIds.add(rawId);
      } else {
        uniqueTopics.add(tag);
      }
    }

    if (!invalidIds.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(Map.of("error", "invalid-topic-ids", "invalidIds", invalidIds));
    }

    if (uniqueTopics.size() < 3) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(Map.of("error", "minimum-topics", "min", 3));
    }

    try {
      User user = userOptional.get();
      user.setTopics(new ArrayList<>(uniqueTopics));
      userRepository.save(user);
      recommendationService.invalidateFeedCache(user.getId());
      return ResponseEntity.ok(Map.of("topics", user.getTopics()));
    } catch (DataAccessException ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "topics-unavailable"));
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "topics-failed"));
    }
  }

  /**
   * Converte uma string de ID de tópico para o enum {@link PodcastTag} correspondente.
   *
   * <p>Normaliza para maiúsculas antes de tentar o parse. Retorna {@code null} se o
   * valor for nulo, vazio ou não corresponder a nenhum valor do enum.
   *
   * @param value string a converter (ex: {@code "desporto"}, {@code "GERAL"}).
   * @return {@link PodcastTag} correspondente, ou {@code null} se inválido.
   */
  private PodcastTag parseTag(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    try {
      return PodcastTag.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Converte um {@link PodcastTag} para o seu label human-readable em português.
   *
   * @param tag o enum a converter.
   * @return label legivel para o utilizador (ex: {@code "Desporto"}).
   */
  private String toLabel(PodcastTag tag) {
    return switch (tag) {
      case DESPORTO -> "Desporto";
      case POLITICA -> "Politica";
      case FINANCAS -> "Financas";
      case GERAL -> "Geral";
    };
  }
}
