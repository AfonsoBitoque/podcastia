package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jep.servidor.model.User;
import java.util.List;

public interface PodcastRepository extends JpaRepository<Podcast, Long> {
	List<Podcast> findByUser(User user);
}