package com.jep.servidor.service;

import com.jep.servidor.dto.PlaylistAddEpisodeRequest;
import com.jep.servidor.dto.PlaylistCreateRequest;
import com.jep.servidor.dto.PlaylistReorderRequest;
import com.jep.servidor.dto.PlaylistUpdateRequest;
import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.PlaylistItem;
import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PlaylistItemRepository;
import com.jep.servidor.repository.PlaylistRepository;
import com.jep.servidor.repository.PodcastRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de negócio para gestão de playlists de utilizadores.
 *
 * <p>Encapsula todas as operações CRUD sobre {@link Playlist} e {@link PlaylistItem},
 * incluindo controlo de acesso (apenas o dono pode modificar),
 * reordenação atómica de episódios e normalização de posições após remoção.
 *
 * <p>A reordenação usa dois saves sequenciais para evitar violações de
 * constraints de unicidade na coluna {@code position}: primeiro desloca
 * todas as posições para valores fora do intervalo válido, depois aplica
 * a nova ordenação.
 *
 * @see com.jep.servidor.controller.PlaylistController
 */
@Service
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistItemRepository playlistItemRepository;
  private final PodcastRepository podcastRepository;

  public PlaylistService(PlaylistRepository playlistRepository,
                         PlaylistItemRepository playlistItemRepository,
                         PodcastRepository podcastRepository) {
    this.playlistRepository = playlistRepository;
    this.playlistItemRepository = playlistItemRepository;
    this.podcastRepository = podcastRepository;
  }

  /**
   * Cria uma nova playlist para o utilizador.
   * Se {@code request.initialPodcastId} for não-nulo, o podcast é adicionado imediatamente.
   *
   * @param owner   utilizador dono da playlist.
   * @param request dados da nova playlist.
   * @return playlist criada e persistida.
   */
  @Transactional
  public Playlist create(User owner, PlaylistCreateRequest request) {
    Playlist playlist = new Playlist();
    playlist.setOwner(owner);
    playlist.setTitle(request.getTitle().trim());
    playlist.setDescription(request.getDescription());
    playlist.setCoverImagePath(request.getCoverImagePath());
    playlist.setPublic(Boolean.TRUE.equals(request.getIsPublic()));
    Playlist savedPlaylist = playlistRepository.save(playlist);

    if (request.getInitialPodcastId() != null) {
      PlaylistAddEpisodeRequest addRequest = new PlaylistAddEpisodeRequest();
      addRequest.setPodcastId(request.getInitialPodcastId());
      addEpisode(owner, savedPlaylist.getId(), addRequest);
    }
    
    return savedPlaylist;
  }

  /**
   * Devolve todas as playlists do utilizador autenticado, ordenadas pela última atualização.
   *
   * @param viewer o utilizador autenticado.
   * @return lista de playlists do utilizador.
   */
  @Transactional(readOnly = true)
  public List<Playlist> listMine(User viewer) {
    return playlistRepository.findByOwnerOrderByUpdatedAtDesc(viewer);
  }

  /**
   * Devolve as playlists de um utilizador alvo.
   * Se o viewer for o próprio alvo, devolve todas; caso contrário, apenas as públicas.
   *
   * @param viewer       utilizador autenticado.
   * @param targetUserId ID do utilizador cujas playlists são listadas.
   * @return lista de playlists visíveis para o viewer.
   */
  @Transactional(readOnly = true)
  public List<Playlist> listByUser(User viewer, Long targetUserId) {
    if (viewer.getId().equals(targetUserId)) {
      return playlistRepository.findByOwnerOrderByUpdatedAtDesc(viewer);
    }
    return playlistRepository.findByOwnerIdAndIsPublicTrueOrderByUpdatedAtDesc(targetUserId);
  }

  /**
   * Devolve as playlists públicas de todos os amigos do utilizador.
   *
   * @param viewer o utilizador autenticado.
   * @return playlists públicas dos amigos.
   */
  @Transactional(readOnly = true)
  public List<Playlist> listFriendsFeed(User viewer) {
    return playlistRepository.findPublicPlaylistsFromFriends(viewer.getId());
  }

  /**
   * Devolve uma playlist se for visível para o viewer (pública ou do próprio).
   *
   * @param viewer     utilizador autenticado.
   * @param playlistId ID da playlist.
   * @return playlist se visível, ou {@link Optional#empty()} se não encontrada ou privada.
   */
  @Transactional(readOnly = true)
  public Optional<Playlist> findVisibleById(User viewer, Long playlistId) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return Optional.empty();
    }

    Playlist playlist = playlistOpt.get();
    if (playlist.isPublic() || playlist.getOwner().getId().equals(viewer.getId())) {
      return Optional.of(playlist);
    }
    return Optional.empty();
  }

  /**
   * Atualiza os metadados de uma playlist (título, descrição, capa, visibilidade).
   * Campos {@code null} no request são ignorados (atualização parcial).
   *
   * @param viewer     utilizador autenticado (deve ser o dono).
   * @param playlistId ID da playlist.
   * @param request    campos a atualizar.
   * @return playlist atualizada, ou {@link Optional#empty()} se não encontrada.
   * @throws SecurityException se o viewer não for o dono.
   */
  @Transactional
  public Optional<Playlist> update(User viewer, Long playlistId, PlaylistUpdateRequest request) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return Optional.empty();
    }

    Playlist playlist = playlistOpt.get();
    validateOwner(viewer, playlist);

    if (request.getTitle() != null) {
      playlist.setTitle(request.getTitle().trim());
    }
    if (request.getDescription() != null) {
      playlist.setDescription(request.getDescription());
    }
    if (request.getCoverImagePath() != null) {
      playlist.setCoverImagePath(request.getCoverImagePath());
    }
    if (request.getIsPublic() != null) {
      playlist.setPublic(request.getIsPublic());
    }

    return Optional.of(playlistRepository.save(playlist));
  }

  /**
   * Elimina uma playlist do utilizador.
   *
   * @param viewer     utilizador autenticado (deve ser o dono).
   * @param playlistId ID da playlist.
   * @return {@code true} se eliminada; {@code false} se não encontrada.
   * @throws SecurityException se o viewer não for o dono.
   */
  @Transactional
  public boolean delete(User viewer, Long playlistId) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return false;
    }

    Playlist playlist = playlistOpt.get();
    validateOwner(viewer, playlist);
    playlistRepository.delete(playlist);
    return true;
  }

  /**
   * Adiciona um podcast ao final da playlist.
   *
   * @param viewer     utilizador autenticado (deve ser o dono).
   * @param playlistId ID da playlist.
   * @param request    payload com o {@code podcastId} a adicionar.
   * @return playlist atualizada, ou {@link Optional#empty()} se não encontrada.
   * @throws IllegalArgumentException se o podcast não existir.
   * @throws IllegalStateException    se o podcast já existir na playlist.
   */
  @Transactional
  public Optional<Playlist> addEpisode(User viewer, Long playlistId, PlaylistAddEpisodeRequest request) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return Optional.empty();
    }

    Playlist playlist = playlistOpt.get();
    validateOwner(viewer, playlist);

    Podcast podcast = podcastRepository.findById(request.getPodcastId())
        .orElseThrow(() -> new IllegalArgumentException("Podcast não encontrado"));

    if (playlistItemRepository.existsByPlaylistAndPodcast(playlist, podcast)) {
      throw new IllegalStateException("Podcast já existe na playlist");
    }

    List<PlaylistItem> currentItems = playlistItemRepository.findByPlaylistOrderByPositionAsc(playlist);

    PlaylistItem item = new PlaylistItem();
    item.setPlaylist(playlist);
    item.setPodcast(podcast);
    item.setPosition(currentItems.size());
    playlistItemRepository.save(item);

    return playlistRepository.findById(playlistId);
  }

  /**
   * Remove um podcast da playlist e renormaliza as posições restantes.
   *
   * @param viewer     utilizador autenticado (deve ser o dono).
   * @param playlistId ID da playlist.
   * @param podcastId  ID do podcast a remover.
   * @return playlist atualizada, ou {@link Optional#empty()} se não encontrada.
   * @throws IllegalArgumentException se o podcast não existir na playlist.
   */
  @Transactional
  public Optional<Playlist> removeEpisode(User viewer, Long playlistId, Long podcastId) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return Optional.empty();
    }

    Playlist playlist = playlistOpt.get();
    validateOwner(viewer, playlist);

    Podcast podcast = podcastRepository.findById(podcastId)
        .orElseThrow(() -> new IllegalArgumentException("Podcast não encontrado"));

    PlaylistItem item = playlistItemRepository.findByPlaylistAndPodcast(playlist, podcast)
        .orElseThrow(() -> new IllegalArgumentException("Podcast não existe na playlist"));

    playlistItemRepository.delete(item);
    normalizePositions(playlist);

    return playlistRepository.findById(playlistId);
  }

  /**
   * Reordena todos os episódios de uma playlist segundo a lista de IDs fornecida.
   *
   * <p>O request deve conter todos os IDs dos podcasts da playlist (sem omissões nem duplicados).
   * A operação é atómica: primeiro desloca as posições para evitar conflitos de unicidade,
   * depois aplica a nova ordenação.
   *
   * @param viewer     utilizador autenticado (deve ser o dono).
   * @param playlistId ID da playlist.
   * @param request    lista ordenada de IDs de podcasts.
   * @return playlist reordenada, ou {@link Optional#empty()} se não encontrada.
   * @throws IllegalArgumentException se o número de IDs for diferente ou contiver IDs inválidos/duplicados.
   */
  @Transactional
  public Optional<Playlist> reorderEpisodes(User viewer, Long playlistId, PlaylistReorderRequest request) {
    Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
    if (playlistOpt.isEmpty()) {
      return Optional.empty();
    }

    Playlist playlist = playlistOpt.get();
    validateOwner(viewer, playlist);

    List<PlaylistItem> currentItems = playlistItemRepository.findByPlaylistOrderByPositionAsc(playlist);
    List<Long> currentPodcastIds = currentItems.stream()
        .map(item -> item.getPodcast().getId())
        .toList();

    List<Long> requestedOrder = request.getPodcastIds();
    if (requestedOrder.size() != currentPodcastIds.size()) {
      throw new IllegalArgumentException("A ordenação deve incluir todos os episódios da playlist");
    }

    Set<Long> requestSet = new HashSet<>(requestedOrder);
    Set<Long> currentSet = new HashSet<>(currentPodcastIds);
    if (!requestSet.equals(currentSet) || requestSet.size() != requestedOrder.size()) {
      throw new IllegalArgumentException("A ordenação contém episódios inválidos ou duplicados");
    }

    Map<Long, PlaylistItem> itemByPodcast = new HashMap<>();
    for (PlaylistItem item : currentItems) {
      itemByPodcast.put(item.getPodcast().getId(), item);
      item.setPosition(item.getPosition() + currentItems.size());
    }
    playlistItemRepository.saveAll(currentItems);
    playlistItemRepository.flush();

    for (int index = 0; index < requestedOrder.size(); index++) {
      Long podcastId = requestedOrder.get(index);
      PlaylistItem item = itemByPodcast.get(podcastId);
      item.setPosition(index);
    }
    playlistItemRepository.saveAll(currentItems);

    return playlistRepository.findById(playlistId);
  }

  private void normalizePositions(Playlist playlist) {
    List<PlaylistItem> items = playlistItemRepository.findByPlaylistOrderByPositionAsc(playlist);
    for (int i = 0; i < items.size(); i++) {
      items.get(i).setPosition(i);
    }
    playlistItemRepository.saveAll(items);
  }

  private void validateOwner(User viewer, Playlist playlist) {
    if (!playlist.getOwner().getId().equals(viewer.getId())) {
      throw new SecurityException("Apenas o dono da playlist pode realizar esta operação");
    }
  }
}
