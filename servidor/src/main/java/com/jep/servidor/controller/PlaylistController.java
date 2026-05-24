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
 * Controller REST para gestão completa de playlists de utilizadores.
 *
 * <p>Permite criar, listar, atualizar e eliminar playlists, bem como gerir os seus
 * episódios (adicionar, remover e reordenar). Inclui também suporte para feed social
 * (playlists públicas de amigos) e download de playlist em formato ZIP.
 *
 * <p><b>Base path:</b> {@code /api/playlists} (requer autenticação JWT)
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code POST /} — criar nova playlist.</li>
 *   <li>{@code GET /mine} — listar playlists do utilizador autenticado.</li>
 *   <li>{@code GET /user/{userId}} — listar playlists de um utilizador (públicas ou todas se próprio).</li>
 *   <li>{@code GET /{id}} — obter playlist por ID (respeita visibilidade).</li>
 *   <li>{@code PUT /{id}} — atualizar metadados da playlist.</li>
 *   <li>{@code DELETE /{id}} — eliminar playlist.</li>
 *   <li>{@code POST /{id}/episodes} — adicionar episódio à playlist.</li>
 *   <li>{@code DELETE /{id}/episodes/{podcastId}} — remover episódio da playlist.</li>
 *   <li>{@code PUT /{id}/episodes/order} — reordenar episódios da playlist.</li>
 *   <li>{@code GET /feed} — feed de playlists públicas dos amigos.</li>
 *   <li>{@code GET /{id}/download} — descarregar playlist como ficheiro ZIP de MP3s.</li>
 * </ul>
 *
 * <p><b>Visibilidade:</b> Playlists podem ser públicas ou privadas. Utilizadores não
 * autorizados só veem playlists públicas. O dono vê sempre todas as suas playlists.
 *
 * <p><b>Tratamento de erros:</b>
 * <ul>
 *   <li>{@link SecurityException} → {@code 403 Forbidden} (operação não autorizada).</li>
 *   <li>{@link IllegalArgumentException} → {@code 400 Bad Request} (dados inválidos).</li>
 *   <li>{@link IllegalStateException} → {@code 409 Conflict} (episódio já na playlist).</li>
 * </ul>
 *
 * @see PlaylistService
 * @see com.jep.servidor.dto.PlaylistCreateRequest
 * @see com.jep.servidor.dto.PlaylistUpdateRequest
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

  private final PlaylistService playlistService;
  private final UserRepository userRepository;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param playlistService serviço com a lógica de negócio de playlists.
   * @param userRepository  repositório para resolver o utilizador autenticado.
   */
  public PlaylistController(PlaylistService playlistService,
                            UserRepository userRepository) {
    this.playlistService = playlistService;
    this.userRepository = userRepository;
  }

  /**
   * Cria uma nova playlist para o utilizador autenticado.
   *
   * @param request dados da nova playlist (título, descrição, visibilidade).
   * @return {@code 201 Created} com a playlist criada; {@code 401 Unauthorized} se não autenticado.
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
   * Lista todas as playlists do utilizador autenticado (públicas e privadas).
   *
   * @return {@code 200 OK} com lista de playlists; {@code 401 Unauthorized} se não autenticado.
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
   * Lista playlists de um utilizador específico.
   *
   * <p>Se o utilizador autenticado for o próprio dono, inclui playlists privadas.
   * Caso contrário, retorna apenas as playlists públicas.
   *
   * @param userId ID do utilizador cujas playlists se pretendem listar.
   * @return {@code 200 OK} com lista de playlists visíveis; {@code 401 Unauthorized} se não autenticado.
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
   * Retorna uma playlist por ID, respeitando as regras de visibilidade.
   *
   * <p>Playlists privadas só são visíveis para o seu dono.
   *
   * @param playlistId ID da playlist a obter.
   * @return {@code 200 OK} com a playlist; {@code 404 Not Found} se não existir ou não for visível.
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
   * Atualiza os metadados de uma playlist (nome, descrição, imagem de capa, visibilidade).
   *
   * @param playlistId ID da playlist a atualizar.
   * @param request    novos valores dos metadados.
   * @return {@code 200 OK} com a playlist atualizada; {@code 403 Forbidden} se não for o dono;
   *         {@code 404 Not Found} se não existir.
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
   * Elimina permanentemente uma playlist do utilizador autenticado.
   *
   * @param playlistId ID da playlist a eliminar.
   * @return {@code 204 No Content} se eliminada; {@code 403 Forbidden} se não for o dono;
   *         {@code 404 Not Found} se não existir.
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
   * Adiciona um episódio (podcast) à playlist do utilizador autenticado.
   *
   * <p>Lança {@link IllegalStateException} se o episódio já estiver na playlist
   * (retornado como {@code 409 Conflict}).
   *
   * @param playlistId ID da playlist.
   * @param request    corpo com o ID do podcast a adicionar.
   * @return {@code 200 OK} com a playlist atualizada; {@code 403 Forbidden}, {@code 404 Not Found}
   *         ou {@code 400 Bad Request} consoante o erro.
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
   * Remove um episódio (podcast) da playlist do utilizador autenticado.
   *
   * @param playlistId ID da playlist.
   * @param podcastId  ID do podcast a remover.
   * @return {@code 200 OK} com a playlist atualizada; {@code 403}, {@code 404} ou {@code 400}
   *         consoante o erro.
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
   * Reordena os episódios de uma playlist, atualizando os valores de {@code position}.
   *
   * @param playlistId ID da playlist.
   * @param request    lista ordenada de IDs de podcasts na nova ordem desejada.
   * @return {@code 200 OK} com a playlist reordenada; {@code 403}, {@code 404} ou {@code 400}
   *         consoante o erro.
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
   * Retorna o feed social de playlists públicas dos amigos do utilizador autenticado.
   *
   * <p>Usa {@link com.jep.servidor.repository.PlaylistRepository#findPublicPlaylistsFromFriends}
   * para obter playlists públicas apenas de utilizadores com relação de amizade aceite.
   *
   * @return {@code 200 OK} com lista de playlists públicas dos amigos;
   *         {@code 401 Unauthorized} se não autenticado.
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

  /**
   * Gera e devolve um ficheiro ZIP com todos os MP3s da playlist para download.
   *
   * <p>Itera pelos {@link PlaylistItem}s da playlist e localiza o ficheiro de áudio de cada
   * {@link Podcast} via {@link #findAudioFile(Podcast)}. Episódios sem ficheiro de áudio
   * disponível são silenciosamente ignorados. O nome de cada ficheiro no ZIP segue o formato
   * {@code NN_Titulo_do_Podcast.mp3} (com número de ordem de 2 dígitos).
   *
   * <p><b>Nota:</b> A geração do ZIP ocorre em memória ({@link java.io.ByteArrayOutputStream}),
   * pelo que playlists muito grandes podem consumir memória significativa.
   *
   * @param playlistId ID da playlist a descarregar.
   * @return {@code 200 OK} com o ZIP como array de bytes e cabeçalho
   *         {@code Content-Disposition: attachment};
   *         {@code 400 Bad Request} se a playlist estiver vazia;
   *         {@code 404 Not Found} se não existir ou não for visível;
   *         {@code 500 Internal Server Error} em caso de erro de I/O.
   */
  @GetMapping("/{playlistId}/download")
  public ResponseEntity<?> downloadPlaylistZip(@PathVariable("playlistId") Long playlistId) {
    Optional<User> authUser = getAuthenticatedUser();
    if (authUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Utilizador não autenticado"));
    }

    Optional<Playlist> playlistOpt = playlistService.findVisibleById(authUser.get(), playlistId);
    if (playlistOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Playlist não encontrada"));
    }

    Playlist playlist = playlistOpt.get();
    if (playlist.getItems().isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Playlist vazia"));
    }

    try {
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

      int index = 1;
      for (PlaylistItem item : playlist.getItems()) {
        Podcast podcast = item.getPodcast();
        java.io.File audioFile = findAudioFile(podcast);
        if (audioFile == null || !audioFile.exists()) continue;

        String fileName = String.format("%02d_", index)
            + podcast.getTitulo().replaceAll("[^a-zA-Z0-9\\s\\-_]", "")
                .trim().replaceAll("\\s+", "_") + ".mp3";

        zos.putNextEntry(new java.util.zip.ZipEntry(fileName));
        java.nio.file.Files.copy(audioFile.toPath(), zos);
        zos.closeEntry();
        index++;
      }

      zos.close();

      String zipName = playlist.getTitle().replaceAll("[^a-zA-Z0-9\\s\\-_]", "")
          .trim().replaceAll("\\s+", "_") + ".zip";

      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=\"" + zipName + "\"")
          .header("Content-Type", "application/zip")
          .body(baos.toByteArray());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Erro ao criar ZIP: " + e.getMessage()));
    }
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
   * Localiza o ficheiro MP3 de um podcast no sistema de ficheiros.
   *
   * <p>Tenta resolver o ficheiro por ordem de prioridade:
   * <ol>
   *   <li>Caminho absoluto {@code podcast.getConteudoPath()} diretamente.</li>
   *   <li>Caminho via {@code java.nio.file.Paths}.</li>
   *   <li>Busca heurística no diretório {@code generated-podcasts/} por nome parcial do título.</li>
   * </ol>
   *
   * @param podcast entidade {@link Podcast} cujo ficheiro de áudio se pretende localizar.
   * @return {@link java.io.File} se encontrado; {@code null} caso contrário.
   */
  private java.io.File findAudioFile(Podcast podcast) {
    String conteudoPath = podcast.getConteudoPath();
    if (conteudoPath == null || conteudoPath.isEmpty()) return null;

    java.io.File audioFile = new java.io.File(conteudoPath);
    if (audioFile.exists()) return audioFile;

    try {
      java.nio.file.Path path = java.nio.file.Paths.get(conteudoPath);
      if (java.nio.file.Files.exists(path)) return path.toFile();

      java.io.File podcastsDir = new java.io.File("generated-podcasts");
      if (podcastsDir.exists() && podcastsDir.isDirectory()) {
        java.io.File[] files = podcastsDir.listFiles((dir, name) ->
            name.endsWith(".mp3") && name.contains("user" + podcast.getUser().getId() + "_"));
        if (files != null && files.length > 0) {
          String podcastTitle = podcast.getTitulo().toLowerCase().replace(" ", "_");
          for (java.io.File f : files) {
            if (f.getName().toLowerCase().contains(
                podcastTitle.substring(0, Math.min(10, podcastTitle.length())))) {
              return f;
            }
          }
          return files[0];
        }
      }
    } catch (Exception e) {
      System.err.println("Error finding audio file for ZIP: " + e.getMessage());
    }
    return null;
  }

  /**
   * Converte a entidade {@link Playlist} para um mapa de resposta JSON.
   *
   * <p>Inclui dados do dono (id, username, tag) e lista de episódios via
   * {@link #toEpisodeResponse(PlaylistItem)}.
   *
   * @param playlist entidade a converter.
   * @return mapa com os campos da playlist serializáveis para JSON.
   */
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

  /**
   * Converte um {@link PlaylistItem} para um mapa de resposta JSON com dados do episódio.
   *
   * @param item item da playlist a converter.
   * @return mapa com {@code position}, {@code podcastId}, {@code title}, {@code duration},
   *         {@code host}, {@code hostId} e {@code available}.
   */
  private Map<String, Object> toEpisodeResponse(PlaylistItem item) {
    Podcast podcast = item.getPodcast();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("position", item.getPosition());
    response.put("podcastId", podcast.getId());
    response.put("title", podcast.getTitulo());
    response.put("duration", podcast.getDuracao());
    response.put("host", podcast.getUser().getUsername());
    response.put("hostId", podcast.getUser().getId());
    response.put("available", podcast.isAvailable());
    return response;
  }
}
