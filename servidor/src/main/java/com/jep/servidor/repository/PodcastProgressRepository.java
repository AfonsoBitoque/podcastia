package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PodcastProgressRepository extends JpaRepository<PodcastProgress, Long> {
    Optional<PodcastProgress> findByUserAndPodcast(User user, Podcast podcast);
    List<PodcastProgress> findTop10ByUserOrderByLastListenedAtDesc(User user);
    
    // Admin analytics methods
    @Query("SELECT COALESCE(SUM(pp.progressSeconds), 0) FROM PodcastProgress pp")
    long sumTotalListeningTime();
    
    @Query("SELECT COUNT(pp) FROM PodcastProgress pp WHERE pp.podcast.id = :podcastId")
    long countByPodcastId(Long podcastId);
    
    @Query("SELECT COALESCE(SUM(pp.progressSeconds), 0) FROM PodcastProgress pp WHERE pp.podcast.id = :podcastId")
    long sumListeningTimeByPodcastId(Long podcastId);
    
    @Query("SELECT p.id, p.titulo, u.username, COUNT(pp), COALESCE(SUM(pp.progressSeconds), 0) " +
           "FROM PodcastProgress pp " +
           "JOIN pp.podcast p " +
           "JOIN p.user u " +
           "GROUP BY p.id, p.titulo, u.username " +
           "ORDER BY COUNT(pp) DESC")
    List<Object[]> findTopPodcastsByPlays();
    
    @Query("SELECT COALESCE(SUM(pp.progressSeconds), 0) FROM PodcastProgress pp " +
           "WHERE DATE(pp.lastListenedAt) = :date")
    long sumListeningTimeByDate(LocalDate date);
    
    @Query("SELECT COALESCE(SUM(pp.progressSeconds), 0) FROM PodcastProgress pp " +
           "WHERE MONTH(pp.lastListenedAt) = :month AND YEAR(pp.lastListenedAt) = :year")
    long sumListeningTimeByMonth(int month, int year);
}
