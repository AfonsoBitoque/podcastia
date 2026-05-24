package com.jep.servidor.controller;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastFavorite;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastFavoriteRepository;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para gestão de favoritos de podcasts do utilizador autenticado.
 *
 * <p>Permite ao utilizador adicionar, remover, alternar e verificar o estado de favorito
 * de podcasts. Os favoritos são persistidos na tabela {@code podcast_favorites} via
 * {@link PodcastFavoriteRepository}.
 *
 * <p><b>Base path:</b> {@code /api/favorites} (requer autenticação JWT)
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /} — listar todos os podcasts favoritos.</li>
 *   <li>{@code GET /{podcastId}/check} — verificar se um podcast é favorito.</li>
 *   <li>{@code POST /{podcastId}} — adicionar podcast aos favoritos.</li>
 *   <li>{@code DELETE /{podcastId}} — remover podcast dos favoritos.</li>
 *   <li>{@code POST /{podcastId}/toggle} — alternar estado de favorito (add/remove).</li>
 * </ul>
 *
 * <p><b>Nota de performance:</b> Os métodos {@link #removeFavorite} e {@link #toggleFavorite}
 * usam {@code favoriteRepository.findAll()} seguido de filtragem em memória, o que é
 * ineficiente para grandes volumes. Deveriam usar uma query direcionada por utilizador+podcast.
 *
 * @see PodcastFavoriteRepository
 * @see com.jep.servidor.model.PodcastFavorite
 */
@RestController
@RequestMapping("/api/favorites")
public class PodcastFavoriteController {

  private final PodcastFavoriteRepository favoriteRepository;
  private final PodcastRepository podcastRepository;
  private final UserRepository userRepository;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param favoriteRepository repositório JPA de favoritos.
   * @param podcastRepository  repositório JPA de podcasts.
   * @param userRepository     repositório JPA de utilizadores.
   */
  public PodcastFavoriteController(PodcastFavoriteRepository favoriteRepository,
                                   PodcastRepository podcastRepository,
                                   UserRepository userRepository) {
    this.favoriteRepository = favoriteRepository;
    this.podcastRepository = podcastRepository;
    this.userRepository = userRepository;
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
   * Retorna a lista de podcasts marcados como favoritos pelo utilizador autenticado.
   *
   * <p>Obtém os IDs dos favoritos via {@link PodcastFavoriteRepository#findPodcastIdsByUser}
   * e carrega os podcasts correspondentes com {@code findAllById}.
   *
   * @return {@code 200 OK} com lista de {@link Podcast} favoritos (pode ser vazia);
   *         {@code 401 Unauthorized} se não autenticado.
   */
  @GetMapping
  public ResponseEntity<List<Podcast>> getFavorites() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    User user = authUser.get();
    List<Long> favoriteIds = favoriteRepository.findPodcastIdsByUser(user);

    if (favoriteIds.isEmpty()) {
      return ResponseEntity.ok(List.of());
    }

    List<Podcast> favorites = podcastRepository.findAllById(favoriteIds);
    return ResponseEntity.ok(favorites);
  }

  /**
   * Verifica se um podcast específico está marcado como favorito pelo utilizador autenticado.
   *
   * @param podcastId ID do podcast a verificar.
   * @return {@code 200 OK} com {@code {"isFavorite": true/false}};
   *         {@code 401 Unauthorized} se não autenticado.
   */
  @GetMapping("/{podcastId}/check")
  public ResponseEntity<Map<String, Boolean>> isFavorite(@PathVariable Long podcastId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    User user = authUser.get();
    List<Long> favoriteIds = favoriteRepository.findPodcastIdsByUser(user);
    boolean isFav = favoriteIds.contains(podcastId);

    Map<String, Boolean> response = new HashMap<>();
    response.put("isFavorite", isFav);
    return ResponseEntity.ok(response);
  }

  /**
   * Adiciona um podcast à lista de favoritos do utilizador autenticado.
   *
   * <p>Verifica se o podcast já está nos favoritos antes de inserir,
   * devolvendo {@code 409 Conflict} em caso de duplicado.
   *
   * @param podcastId ID do podcast a adicionar.
   * @return {@code 201 Created} com mensagem de sucesso e o ID do podcast;
   *         {@code 401 Unauthorized} se não autenticado;
   *         {@code 404 Not Found} se o podcast não existir;
   *         {@code 409 Conflict} se o podcast já estiver nos favoritos.
   */
  @PostMapping("/{podcastId}")
  public ResponseEntity<?> addFavorite(@PathVariable Long podcastId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    User user = authUser.get();

    Optional<Podcast> podcastOpt = podcastRepository.findById(podcastId);
    if (podcastOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Podcast não encontrado"));
    }

    Podcast podcast = podcastOpt.get();

    // Verificar se já existe
    List<Long> existingIds = favoriteRepository.findPodcastIdsByUser(user);
    if (existingIds.contains(podcastId)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "Podcast já está nos favoritos"));
    }

    PodcastFavorite favorite = new PodcastFavorite(user, podcast);
    favoriteRepository.save(favorite);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Podcast adicionado aos favoritos", "podcastId", podcastId));
  }

  /**
   * Remove um podcast da lista de favoritos do utilizador autenticado.
   *
   * <p><b>Aviso de performance:</b> Esta implementação usa {@code favoriteRepository.findAll()}
   * seguido de filtragem em memória por {@code userId} e {@code podcastId}. Para bases
   * de dados com muitos registos, deverá ser substituida por uma query direcionada.
   *
   * @param podcastId ID do podcast a remover dos favoritos.
   * @return {@code 200 OK} com mensagem de sucesso;
   *         {@code 401 Unauthorized} se não autenticado;
   *         {@code 404 Not Found} se o podcast ou o favorito não existir.
   */
  @DeleteMapping("/{podcastId}")
  public ResponseEntity<?> removeFavorite(@PathVariable Long podcastId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    User user = authUser.get();
    Optional<Podcast> podcastOpt = podcastRepository.findById(podcastId);
    if (podcastOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Podcast não encontrado"));
    }

    Podcast podcast = podcastOpt.get();

    // Buscar e remover o favorito
    List<PodcastFavorite> favorites = favoriteRepository.findAll().stream()
        .filter(f -> f.getUser().getId().equals(user.getId()) && f.getPodcast().getId().equals(podcastId))
        .collect(Collectors.toList());

    if (favorites.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Podcast não está nos favoritos"));
    }

    favoriteRepository.deleteAll(favorites);

    return ResponseEntity.ok(Map.of("message", "Podcast removido dos favoritos", "podcastId", podcastId));
  }

  /**
   * Alterna o estado de favorito de um podcast (toggle).
   *
   * <p>Se o podcast já for favorito, remove-o; caso contrário, adiciona-o.
   * Esta operação é idêmpotente — invocar duas vezes em sequência retorna ao estado original.
   *
   * <p><b>Aviso de performance:</b> A remoção usa {@code favoriteRepository.findAll()}
   * — ver nota em {@link #removeFavorite}.
   *
   * @param podcastId ID do podcast a alternar.
   * @return {@code 201 Created} com {@code {"isFavorite": true}} se adicionado;
   *         {@code 200 OK} com {@code {"isFavorite": false}} se removido;
   *         {@code 401 Unauthorized} se não autenticado;
   *         {@code 404 Not Found} se o podcast não existir.
   */
  @PostMapping("/{podcastId}/toggle")
  public ResponseEntity<?> toggleFavorite(@PathVariable Long podcastId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    User user = authUser.get();
    Optional<Podcast> podcastOpt = podcastRepository.findById(podcastId);
    if (podcastOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Podcast não encontrado"));
    }

    Podcast podcast = podcastOpt.get();

    // Verificar se já existe
    List<Long> existingIds = favoriteRepository.findPodcastIdsByUser(user);
    boolean isFavorite = existingIds.contains(podcastId);

    if (isFavorite) {
      // Remover
      List<PodcastFavorite> favorites = favoriteRepository.findAll().stream()
          .filter(f -> f.getUser().getId().equals(user.getId()) && f.getPodcast().getId().equals(podcastId))
          .collect(Collectors.toList());
      favoriteRepository.deleteAll(favorites);
      return ResponseEntity.ok(Map.of("isFavorite", false, "message", "Podcast removido dos favoritos"));
    } else {
      // Adicionar
      PodcastFavorite favorite = new PodcastFavorite(user, podcast);
      favoriteRepository.save(favorite);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("isFavorite", true, "message", "Podcast adicionado aos favoritos"));
    }
  }
}
