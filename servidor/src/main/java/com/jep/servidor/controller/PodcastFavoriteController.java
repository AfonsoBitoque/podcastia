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
 * Controller para gerir favoritos de podcasts.
 */
@RestController
@RequestMapping("/api/favorites")
public class PodcastFavoriteController {

  private final PodcastFavoriteRepository favoriteRepository;
  private final PodcastRepository podcastRepository;
  private final UserRepository userRepository;

  public PodcastFavoriteController(PodcastFavoriteRepository favoriteRepository,
                                   PodcastRepository podcastRepository,
                                   UserRepository userRepository) {
    this.favoriteRepository = favoriteRepository;
    this.podcastRepository = podcastRepository;
    this.userRepository = userRepository;
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

  /**
   * Retorna todos os podcasts favoritos do utilizador autenticado.
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
   * Verifica se um podcast está nos favoritos do utilizador.
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
   * Adiciona um podcast aos favoritos.
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
   * Remove um podcast dos favoritos.
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
   * Alterna o estado de favorito (adiciona se não existir, remove se existir).
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
