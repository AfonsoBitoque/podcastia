package com.jep.servidor.service;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastFavorite;
import com.jep.servidor.model.PodcastProgress;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Serviço de feed filtrado de podcasts.
 *
 * <p>Usa a API {@link org.springframework.data.jpa.domain.Specification} do Spring Data JPA
 * para compor predicados dinâmicos de forma segura (sem concatenação de SQL).
 *
 * <p>Filtros suportados:
 * <ul>
 *   <li>{@code category} — filtra por {@link com.jep.servidor.model.PodcastTag}.</li>
 *   <li>{@code shorts} — limita a podcasts com duração ≤ 15 min ({@link #SHORT_MAX_DURATION_SECONDS}).</li>
 *   <li>{@code maxDuration} — duração máxima em segundos.</li>
 *   <li>{@code hidePlayed} — exclui podcasts completados pelo utilizador.</li>
 *   <li>{@code isFavorite} — inclui apenas podcasts favoritados pelo utilizador.</li>
 *   <li>{@code type} — aceita apenas {@code "podcast"} (outros valores devolvem 0 resultados).</li>
 * </ul>
 *
 * @see com.jep.servidor.controller.FeedController
 */
@Service
public class FeedService {

  /** Duração máxima (em segundos) para um podcast ser classificado como "short" (15 min). */
  public static final int SHORT_MAX_DURATION_SECONDS = 900;

  private final PodcastRepository podcastRepository;

  public FeedService(PodcastRepository podcastRepository) {
    this.podcastRepository = podcastRepository;
  }

  /**
   * Devolve uma página de podcasts filtrada e paginada.
   *
   * @param user        utilizador autenticado (necessário para filtros {@code hidePlayed} e {@code isFavorite}).
   * @param type        tipo de conteúdo; apenas {@code "podcast"} é válido.
   * @param category    nome da tag ({@link com.jep.servidor.model.PodcastTag}) ou {@code null}.
   * @param isFavorite  se {@code true}, inclui apenas favoritos do utilizador.
   * @param maxDuration duração máxima em segundos ou {@code null}.
   * @param hidePlayed  se {@code true}, exclui podcasts já completados.
   * @param shorts      se {@code true}, limita a podcasts ≤ {@link #SHORT_MAX_DURATION_SECONDS}.
   * @param pageable    configuração de paginação e ordenação.
   * @return página de podcasts que satisfazem todos os filtros ativos.
   */
  public Page<Podcast> getFilteredFeed(
      User user,
      String type,
      String category,
      Boolean isFavorite,
      Integer maxDuration,
      Boolean hidePlayed,
      Boolean shorts,
      Pageable pageable
  ) {
    Specification<Podcast> spec = Specification.where(isAvailable());

    PodcastTag tag = parseTag(category);
    if (category != null && tag == null) {
      spec = spec.and(noMatches());
    } else if (tag != null) {
      spec = spec.and(hasTag(tag));
    }

    if (Boolean.TRUE.equals(shorts)) {
      spec = spec.and(maxDuration(SHORT_MAX_DURATION_SECONDS / 60));
    }

    if (maxDuration != null) {
      spec = spec.and(maxDuration(maxDuration / 60));
    }

    if (Boolean.TRUE.equals(hidePlayed)) {
      spec = spec.and(notCompletedByUser(user));
    }

    if (Boolean.TRUE.equals(isFavorite)) {
      spec = spec.and(isFavoritedByUser(user));
    }

    if (type != null && !type.trim().isEmpty()) {
      String normalizedType = type.trim().toLowerCase(Locale.ROOT);
      if (!"podcast".equals(normalizedType)) {
        spec = spec.and(noMatches());
      }
    }

    return podcastRepository.findAll(spec, pageable);
  }

  /**
   * Verifica se existe algum podcast disponível para uma categoria (tag).
   * Usado para preencher o campo {@code categoryHasContent} no {@link com.jep.servidor.dto.FeedMeta}.
   *
   * @param category nome da tag a verificar.
   * @return {@code true} se existir pelo menos um podcast com essa tag; {@code false} se a categoria
   *         for inválida ou não houver podcasts.
   */
  public boolean categoryHasContent(String category) {
    PodcastTag tag = parseTag(category);
    if (tag == null) {
      return false;
    }
    return podcastRepository.existsByTag(tag);
  }

  private PodcastTag parseTag(String category) {
    if (category == null || category.trim().isEmpty()) {
      return null;
    }
    String normalized = category.trim().toUpperCase(Locale.ROOT);
    try {
      return PodcastTag.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private Specification<Podcast> isAvailable() {
    return (root, query, builder) -> builder.isTrue(root.get("available"));
  }

  private Specification<Podcast> hasTag(PodcastTag tag) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<Podcast, PodcastTag> tags = root.join("tags", JoinType.INNER);
      return builder.equal(tags, tag);
    };
  }

  private Specification<Podcast> maxDuration(int maxDuration) {
    return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("duracao"), maxDuration);
  }

  private Specification<Podcast> notCompletedByUser(User user) {
    return (root, query, builder) -> {
      Subquery<Long> subquery = query.subquery(Long.class);
      Root<PodcastProgress> progress = subquery.from(PodcastProgress.class);
      subquery.select(progress.get("podcast").get("id"))
          .where(
              builder.equal(progress.get("user"), user),
              builder.equal(progress.get("podcast"), root),
              builder.greaterThanOrEqualTo(progress.get("progressSeconds"), builder.prod(root.get("duracao"), 60))
          );
      return builder.not(builder.exists(subquery));
    };
  }

  private Specification<Podcast> isFavoritedByUser(User user) {
    return (root, query, builder) -> {
      Subquery<Long> subquery = query.subquery(Long.class);
      Root<PodcastFavorite> favorite = subquery.from(PodcastFavorite.class);
      subquery.select(favorite.get("podcast").get("id"))
          .where(
              builder.equal(favorite.get("user"), user),
              builder.equal(favorite.get("podcast"), root)
          );
      return builder.exists(subquery);
    };
  }

  private Specification<Podcast> noMatches() {
    return (root, query, builder) -> builder.disjunction();
  }
}
