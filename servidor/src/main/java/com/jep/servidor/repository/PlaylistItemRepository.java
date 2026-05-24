package com.jep.servidor.repository;

import com.jep.servidor.model.Playlist;
import com.jep.servidor.model.PlaylistItem;
import com.jep.servidor.model.Podcast;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data JPA para itens de playlists.
 *
 * <p>Usado pelo {@link com.jep.servidor.service.PlaylistService} para
 * gerir adição, remoção e reordenação de podcasts em playlists.
 */
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

  /**
   * Devolve todos os itens de uma playlist ordenados pela posição.
   *
   * @param playlist a playlist a consultar.
   * @return lista de {@link PlaylistItem} ordenada por {@code position} ascendente.
   */
  List<PlaylistItem> findByPlaylistOrderByPositionAsc(Playlist playlist);

  /**
   * Encontra o item que corresponde a um podcast específico numa playlist.
   *
   * @param playlist playlist a pesquisar.
   * @param podcast  podcast a localizar.
   * @return o item se existir, ou {@link java.util.Optional#empty()} se não.
   */
  Optional<PlaylistItem> findByPlaylistAndPodcast(Playlist playlist, Podcast podcast);

  /**
   * Verifica se um podcast já está presente numa playlist.
   *
   * @param playlist playlist a verificar.
   * @param podcast  podcast a verificar.
   * @return {@code true} se o podcast já estiver na playlist.
   */
  boolean existsByPlaylistAndPodcast(Playlist playlist, Podcast podcast);
}
