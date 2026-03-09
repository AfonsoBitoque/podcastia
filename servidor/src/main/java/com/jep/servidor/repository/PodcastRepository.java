package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para operações de base de dados relacionadas com podcasts.
 */
public interface PodcastRepository extends JpaRepository<Podcast, Long> {
  List<Podcast> findByUser(User user);
}
