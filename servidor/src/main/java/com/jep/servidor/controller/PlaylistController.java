package com.jep.servidor.controller;

import com.jep.servidor.dto.PlaylistAddEpisodeRequest;
import com.jep.servidor.dto.PlaylistCreateRequest;
import com.jep.servidor.dto.PlaylistReorderRequest;
import com.jep.servidor.dto.PlaylistUpdateRequest;
import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.PlaylistItem;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.PlaylistService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestão de playlists de utilizadores.
 */
@RestController
@RequestMapping("/playlists")
public class PlaylistController {

  private final PlaylistService playlistService;
  private final UserRepository userRepository;

  /**
   * Construtor para injeção de dependências.
   */
  public PlaylistController(PlaylistService playlistService,
                            UserRepository userRepository) {
    this.playlistService = playlistService;
    this.userRepository = userRepository;
  }

  /**
   * Cria uma nova playlist para o utilizador autenticado.
   */
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody PlaylistCreateRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    Playlist created = playlistService.create(authUser.get(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toPlaylistResponse(created));
  }

  /**
   * Lista playlists do utilizador autenticado (públicas e privadas).
   */
  @GetMapping("/mine")
  public ResponseEntity<?> mine() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    List<Map<String, Object>> playlists = playlistService.listMine(authUser.get()).stream()
        .map(this::toPlaylistResponse)
        .toList();
    return ResponseEntity.ok(playlists);
  }

  /**
   * Lista playlists de um utilizador: se for o próprio inclui privadas, caso contrário apenas públicas.
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<?> listByUser(@PathVariable Long userId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    List<Map<String, Object>> playlists = playlistService.listByUser(authUser.get(), userId).stream()
        .map(this::toPlaylistResponse)
        .toList();
    return ResponseEntity.ok(playlists);
  }

  /**
   * Retorna uma playlist específica, respeitando regras de visibilidade.
   */
  @GetMapping("/{playlistId}")
  public ResponseEntity<?> getById(@PathVariable Long playlistId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    Optional<Playlist> playlist = playlistService.findVisibleById(authUser.get(), playlistId);
    return playlist.map(value -> ResponseEntity.ok(toPlaylistResponse(value)))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Playlist não encontrada")));
  }

  /**
   * Atualiza metadados da playlist (nome, descrição, capa, visibilidade).
   */
  @PutMapping("/{playlistId}")
  public ResponseEntity<?> update(@PathVariable Long playlistId,
                                  @Valid @RequestBody PlaylistUpdateRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    try {
      Optional<Playlist> updated = playlistService.update(authUser.get(), playlistId, request);
      return updated.map(value -> ResponseEntity.ok(toPlaylistResponse(value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("error", "Playlist não encontrada")));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Elimina uma playlist.
   */
  @DeleteMapping("/{playlistId}")
  public ResponseEntity<?> delete(@PathVariable Long playlistId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    try {
      boolean deleted = playlistService.delete(authUser.get(), playlistId);
      if (!deleted) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Playlist não encontrada"));
      }
      return ResponseEntity.noContent().build();
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Adiciona um episódio (podcast) à playlist.
   */
  @PostMapping("/{playlistId}/episodes")
  public ResponseEntity<?> addEpisode(@PathVariable Long playlistId,
                                      @Valid @RequestBody PlaylistAddEpisodeRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    try {
      Optional<Playlist> updated = playlistService.addEpisode(authUser.get(), playlistId, request);
      return updated.map(value -> ResponseEntity.ok(toPlaylistResponse(value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("error", "Playlist não encontrada")));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Remove um episódio (podcast) da playlist.
   */
  @DeleteMapping("/{playlistId}/episodes/{podcastId}")
  public ResponseEntity<?> removeEpisode(@PathVariable Long playlistId,
                                         @PathVariable Long podcastId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    try {
      Optional<Playlist> updated = playlistService.removeEpisode(authUser.get(), playlistId, podcastId);
      return updated.map(value -> ResponseEntity.ok(toPlaylistResponse(value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("error", "Playlist não encontrada")));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Atualiza a ordem dos episódios de uma playlist.
   */
  @PutMapping("/{playlistId}/episodes/order")
  public ResponseEntity<?> reorderEpisodes(@PathVariable Long playlistId,
                                           @Valid @RequestBody PlaylistReorderRequest request) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    try {
      Optional<Playlist> updated = playlistService.reorderEpisodes(authUser.get(), playlistId, request);
      return updated.map(value -> ResponseEntity.ok(toPlaylistResponse(value)))
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("error", "Playlist não encontrada")));
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Feed de playlists públicas dos amigos do utilizador autenticado.
   */
  @GetMapping("/feed")
  public ResponseEntity<?> friendsFeed() {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    List<Map<String, Object>> playlists = playlistService.listFriendsFeed(authUser.get()).stream()
        .map(this::toPlaylistResponse)
        .toList();
    return ResponseEntity.ok(playlists);
  }

  private Optional<User> getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      return Optional.empty();
    }
    return userRepository.findByEmail(authentication.getName());
  }

  private Map<String, Object> toPlaylistResponse(Playlist playlist) {
    Map<String, Object> owner = new LinkedHashMap<>();
    owner.put("id", playlist.getOwner().getId());
    owner.put("username", playlist.getOwner().getUsername());
    owner.put("tag", playlist.getOwner().getTag());

    List<Map<String, Object>> episodes = playlist.getItems().stream()
        .map(this::toEpisodeResponse)
        .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", playlist.getId());
    response.put("title", playlist.getTitle());
    response.put("description", playlist.getDescription());
    response.put("coverImagePath", playlist.getCoverImagePath());
    response.put("isPublic", playlist.isPublic());
    response.put("owner", owner);
    response.put("createdAt", playlist.getCreatedAt());
    response.put("updatedAt", playlist.getUpdatedAt());
    response.put("episodes", episodes);
    response.put("isEmpty", episodes.isEmpty());
    return response;
  }

  private Map<String, Object> toEpisodeResponse(PlaylistItem item) {
    Podcast podcast = item.getPodcast();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("position", item.getPosition());
    response.put("podcastId", podcast.getId());
    response.put("title", podcast.getTitulo());
    response.put("duration", podcast.getDuracao());
    response.put("host", podcast.getUser().getUsername());
    response.put("hostId", podcast.getUser().getId());
    return response;
  }
}
