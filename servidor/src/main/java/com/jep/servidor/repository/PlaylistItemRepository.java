package com.jep.servidor.repository;

import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.PlaylistItem;
import com.jep.servidor.model.Podcast;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para itens de playlists.
 */
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

  List<PlaylistItem> findByPlaylistOrderByPositionAsc(Playlist playlist);

  Optional<PlaylistItem> findByPlaylistAndPodcast(Playlist playlist, Podcast podcast);

  boolean existsByPlaylistAndPodcast(Playlist playlist, Podcast podcast);
}
