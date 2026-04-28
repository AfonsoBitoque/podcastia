package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório para operações de base de dados relacionadas com podcasts.
 */
public interface PodcastRepository extends JpaRepository<Podcast, Long>, JpaSpecificationExecutor<Podcast> {
  List<Podcast> findByUser(User user);

  List<Podcast> findByUserOrderByCreatedAtDesc(User user);

  List<Podcast> findByTituloContainingIgnoreCaseOrUser_UsernameContainingIgnoreCase(String titulo, String username, Pageable pageable);

  @Query("select count(p) > 0 from Podcast p join p.tags t where t = :tag")
  boolean existsByTag(@Param("tag") PodcastTag tag);
}
