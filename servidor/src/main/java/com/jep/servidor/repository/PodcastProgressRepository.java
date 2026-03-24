package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PodcastProgressRepository extends JpaRepository<PodcastProgress, Long> {
    Optional<PodcastProgress> findByUserAndPodcast(User user, Podcast podcast);
    List<PodcastProgress> findTop10ByUserOrderByLastListenedAtDesc(User user);
}
