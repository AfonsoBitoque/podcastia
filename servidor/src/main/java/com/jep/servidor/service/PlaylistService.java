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
 * Serviço de negócio para gestão de playlists.
 */
@Service
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistItemRepository playlistItemRepository;
  private final PodcastRepository podcastRepository;

  /**
   * Construtor para injeção de dependências.
   */
  public PlaylistService(PlaylistRepository playlistRepository,
                         PlaylistItemRepository playlistItemRepository,
                         PodcastRepository podcastRepository) {
    this.playlistRepository = playlistRepository;
    this.playlistItemRepository = playlistItemRepository;
    this.podcastRepository = podcastRepository;
  }

  @Transactional
  public Playlist create(User owner, PlaylistCreateRequest request) {
    Playlist playlist = new Playlist();
    playlist.setOwner(owner);
    playlist.setTitle(request.getTitle().trim());
    playlist.setDescription(request.getDescription());
    playlist.setCoverImagePath(request.getCoverImagePath());
    playlist.setPublic(Boolean.TRUE.equals(request.getIsPublic()));
    return playlistRepository.save(playlist);
  }

  @Transactional(readOnly = true)
  public List<Playlist> listMine(User viewer) {
    return playlistRepository.findByOwnerOrderByUpdatedAtDesc(viewer);
  }

  @Transactional(readOnly = true)
  public List<Playlist> listByUser(User viewer, Long targetUserId) {
    if (viewer.getId().equals(targetUserId)) {
      return playlistRepository.findByOwnerOrderByUpdatedAtDesc(viewer);
    }
    return playlistRepository.findByOwnerIdAndIsPublicTrueOrderByUpdatedAtDesc(targetUserId);
  }

  @Transactional(readOnly = true)
  public List<Playlist> listFriendsFeed(User viewer) {
    return playlistRepository.findPublicPlaylistsFromFriends(viewer.getId());
  }

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
