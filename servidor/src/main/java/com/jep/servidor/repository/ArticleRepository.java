package com.jep.servidor.repository;

import com.jep.servidor.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para operações de CRUD em {@link Article}.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

  /**
   * Verifica se já existe um artigo com o URL original fornecido.
   *
   * @param urlOriginal o URL original do artigo
   * @return true se o artigo existir, falso caso contrário
   */
  boolean existsByUrlOriginal(String urlOriginal);
}
