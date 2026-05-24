package com.jep.servidor.repository;

import com.jep.servidor.model.PodcastFavorite;
import com.jep.servidor.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data JPA para favoritos de podcasts.
 *
 * <p>Usado pelo {@link com.jep.servidor.controller.PodcastFavoriteController}
 * e pelo {@link com.jep.servidor.service.FeedService} para filtrar
 * podcasts favoritados no feed.
 */
public interface PodcastFavoriteRepository extends JpaRepository<PodcastFavorite, Long> {

  /**
   * Devolve a lista de IDs de podcasts favoritados por um utilizador.
   *
   * <p>Retorna apenas os IDs (e não as entidades completas) para
   * eficiência nas queries de filtro do feed.
   *
   * @param user o utilizador cujos favoritos são consultados.
   * @return lista de IDs de podcasts favoritados.
   */
  @Query("select pf.podcast.id from PodcastFavorite pf where pf.user = :user")
  List<Long> findPodcastIdsByUser(@Param("user") User user);
}
