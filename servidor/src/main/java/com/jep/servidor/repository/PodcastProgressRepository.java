package com.jep.servidor.repository;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório Spring Data JPA para o progresso de audição de podcasts.
 *
 * <p>Usado pelo {@link com.jep.servidor.controller.PodcastController}
 * para guardar/recuperar o ponto de retoma de audição, e pelo
 * {@link com.jep.servidor.service.AdminService} para calcular
 * métricas de analytics no painel administrativo.
 */
public interface PodcastProgressRepository extends JpaRepository<PodcastProgress, Long> {

    /**
     * Elimina todos os registos de progresso de um utilizador.
     * Usado ao eliminar um utilizador da plataforma.
     *
     * @param user o utilizador cujo progresso será eliminado.
     */
    void deleteByUser(User user);

    /**
     * Encontra o progresso de um utilizador num podcast específico.
     *
     * @param user    o utilizador.
     * @param podcast o podcast.
     * @return o registo de progresso, ou {@link java.util.Optional#empty()} se inexistente.
     */
    Optional<PodcastProgress> findByUserAndPodcast(User user, Podcast podcast);

    /**
     * Devolve os 10 podcasts mais recentemente ouvidos pelo utilizador.
     * Usado pelo endpoint {@code GET /podcasts/continueListening}.
     *
     * @param user o utilizador.
     * @return lista de até 10 registos de progresso, ordenados por {@code lastListenedAt} desc.
     */
    List<PodcastProgress> findTop10ByUserOrderByLastListenedAtDesc(User user);

    /**
     * Calcula o total global de tempo de audição em segundos (todos os utilizadores).
     *
     * @return soma de todos os {@code totalListenedSeconds}; 0 se não houver registos.
     */
    @Query("SELECT COALESCE(SUM(pp.totalListenedSeconds), 0) FROM PodcastProgress pp")
    long sumTotalListeningTime();

    /**
     * Conta o número total de reproduções de um podcast específico.
     *
     * @param podcastId ID do podcast.
     * @return soma de playCount para o podcast; 0 se não houver.
     */
    @Query("SELECT COALESCE(SUM(pp.playCount), 0) FROM PodcastProgress pp WHERE pp.podcast.id = :podcastId")
    long countByPodcastId(Long podcastId);

    /**
     * Calcula o tempo total de audição de um podcast específico.
     *
     * @param podcastId ID do podcast.
     * @return soma de {@code totalListenedSeconds} para o podcast; 0 se não houver.
     */
    @Query("SELECT COALESCE(SUM(pp.totalListenedSeconds), 0) FROM PodcastProgress pp WHERE pp.podcast.id = :podcastId")
    long sumListeningTimeByPodcastId(Long podcastId);

    /**
     * Devolve os podcasts com mais tempo de audição, com dados agregados para o ranking admin.
     *
     * <p>Cada elemento do array contém: {@code [podcastId, titulo, username, playCount, totalSeconds]}.
     *
     * @return lista de arrays de objetos ordenada por tempo de audição desc.
     */
    @Query("SELECT p.id, p.titulo, u.username, COALESCE(SUM(pp.playCount), 0), COALESCE(SUM(pp.totalListenedSeconds), 0) " +
           "FROM PodcastProgress pp " +
           "JOIN pp.podcast p " +
           "JOIN p.user u " +
           "GROUP BY p.id, p.titulo, u.username " +
           "ORDER BY SUM(pp.totalListenedSeconds) DESC")
    List<Object[]> findTopPodcastsByPlays();

    /**
     * Calcula o tempo total de audição num intervalo de datas.
     * Usado para os pontos de dados de uso semanal/mensal no dashboard admin.
     *
     * @param start data/hora de início do intervalo (inclusivo).
     * @param end   data/hora de fim do intervalo (exclusivo).
     * @return soma de {@code totalListenedSeconds} no intervalo; 0 se não houver.
     */
    @Query("SELECT COALESCE(SUM(pp.totalListenedSeconds), 0) FROM PodcastProgress pp " +
           "WHERE pp.lastListenedAt >= :start AND pp.lastListenedAt < :end")
    long sumListeningTimeBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
}
