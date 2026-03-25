package com.jep.servidor.repository;

import com.jep.servidor.model.RssSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório para operações de CRUD em {@link RssSource}.
 */
public interface RssSourceRepository extends JpaRepository<RssSource, Long> {

  /**
   * Encontra todas as fontes RSS que estão ativas.
   *
   * @return Lista de fontes ativas
   */
  List<RssSource> findByAtivaTrue();
}
