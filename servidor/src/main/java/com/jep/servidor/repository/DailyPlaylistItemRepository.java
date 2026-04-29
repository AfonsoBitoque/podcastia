package com.jep.servidor.repository;

import com.jep.servidor.model.DailyPlaylistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para entidade DailyPlaylistItem.
 */
@Repository
public interface DailyPlaylistItemRepository extends JpaRepository<DailyPlaylistItem, Long> {

  /**
   * Encontra todos os itens de uma playlist diária.
   *
   * @param dailyPlaylistId o ID da playlist diária
   * @return lista de itens ordenados por posição
   */
  List<DailyPlaylistItem> findByDailyPlaylistIdOrderByPosition(Long dailyPlaylistId);
}
