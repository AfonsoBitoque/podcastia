package com.jep.servidor.repository;

import com.jep.servidor.model.DailyPlaylist;
import com.jep.servidor.model.User;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para entidade DailyPlaylist.
 */
@Repository
public interface DailyPlaylistRepository extends JpaRepository<DailyPlaylist, Long> {

  /**
   * Encontra a playlist diária de um utilizador para uma data específica.
   *
   * @param user o utilizador
   * @param date a data da playlist
   * @return a playlist diária se existir
   */
  Optional<DailyPlaylist> findByUserAndPlaylistDate(User user, LocalDate date);

  /**
   * Encontra a playlist diária mais recente de um utilizador.
   *
   * @param user o utilizador
   * @return a playlist diária mais recente
   */
  Optional<DailyPlaylist> findFirstByUserOrderByPlaylistDateDesc(User user);

  /**
   * Verifica se existe uma playlist diária para um utilizador numa data específica.
   *
   * @param user o utilizador
   * @param date a data
   * @return true se existe, false caso contrário
   */
  boolean existsByUserAndPlaylistDate(User user, LocalDate date);
}
