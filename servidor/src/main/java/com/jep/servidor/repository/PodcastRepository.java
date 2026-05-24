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
 * Repositório Spring Data JPA para a entidade {@link Podcast}.
 *
 * <p>Implementa {@link JpaSpecificationExecutor} para suportar filtros
 * dinâmicos no feed ({@link com.jep.servidor.service.FeedService})
 * usando a API {@code Specification}.
 */
public interface PodcastRepository extends JpaRepository<Podcast, Long>, JpaSpecificationExecutor<Podcast> {

  /**
   * Devolve todos os podcasts criados por um utilizador.
   *
   * @param user o utilizador criador.
   * @return lista de podcasts do utilizador.
   */
  List<Podcast> findByUser(User user);

  /**
   * Devolve todos os podcasts de um utilizador, ordenados do mais recente.
   *
   * @param user o utilizador criador.
   * @return lista ordenada por {@code createdAt} descendente.
   */
  List<Podcast> findByUserOrderByCreatedAtDesc(User user);

  /**
   * Pesquisa podcasts por título ou username do autor (case-insensitive).
   * Usado pelo {@link com.jep.servidor.service.SearchService}.
   *
   * @param titulo   termo a pesquisar no título.
   * @param username termo a pesquisar no username do autor.
   * @param pageable limitação de resultados.
   * @return lista de podcasts que correspondem a um dos termos.
   */
  List<Podcast> findByTituloContainingIgnoreCaseOrUser_UsernameContainingIgnoreCase(String titulo, String username, Pageable pageable);

  /**
   * Verifica se existe pelo menos um podcast com uma determinada tag.
   * Usado para {@code categoryHasContent} em {@link com.jep.servidor.dto.FeedMeta}.
   *
   * @param tag a tag a verificar.
   * @return {@code true} se existir pelo menos um podcast com essa tag.
   */
  @Query("select count(p) > 0 from Podcast p join p.tags t where t = :tag")
  boolean existsByTag(@Param("tag") PodcastTag tag);

  /**
   * Devolve todos os podcasts públicos e disponíveis (não soft-deleted).
   * Usado para o feed geral e pela playlist diária.
   *
   * @return lista de podcasts com {@code publico=true} e {@code available=true}.
   */
  List<Podcast> findAllByPublicoTrueAndAvailableTrue();
}
