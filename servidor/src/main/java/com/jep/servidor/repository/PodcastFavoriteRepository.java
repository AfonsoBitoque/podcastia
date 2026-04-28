package com.jep.servidor.repository;

import com.jep.servidor.model.PodcastFavorite;
import com.jep.servidor.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio para favoritos de podcasts.
 */
public interface PodcastFavoriteRepository extends JpaRepository<PodcastFavorite, Long> {

  @Query("select pf.podcast.id from PodcastFavorite pf where pf.user = :user")
  List<Long> findPodcastIdsByUser(@Param("user") User user);
}
