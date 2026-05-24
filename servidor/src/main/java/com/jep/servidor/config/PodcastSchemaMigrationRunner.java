package com.jep.servidor.config;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Componente de migração de schema que garante a existência da coluna {@code created_at}
 * na tabela {@code podcasts}, assegurando compatibilidade com bases de dados locais antigas.
 *
 * <p>É executado com {@code @Order(Ordered.HIGHEST_PRECEDENCE)}, ou seja, com a prioridade
 * mais alta de todos os {@link CommandLineRunner} da aplicação. Isto garante que a coluna
 * existe antes de qualquer outro componente tentar ler ou escrever dados de podcasts —
 * nomeadamente antes do {@link FixAudioPaths} (order 1), {@link DataSeeder} e
 * {@link AudioPathSync} (order 2).
 *
 * <p><b>Problema resolvido:</b> Em versões anteriores do schema, a tabela {@code podcasts}
 * podia não ter a coluna {@code created_at}. Após a sua adição ao modelo JPA
 * ({@link com.jep.servidor.model.Podcast}), bases de dados H2 persistidas localmente
 * (ex: ficheiro {@code podcastia.mv.db}) ficavam incompatíveis e geravam erros ao iniciar.
 * Esta migração ad-hoc adiciona a coluna se ela não existir e preenche os valores nulos
 * com o timestamp atual.
 *
 * <p><b>Alternativa recomendada:</b> Para um projeto em produção, esta lógica deveria
 * ser substituída por uma ferramenta de migração formal como Flyway ou Liquibase, que
 * gerem versões de schema de forma controlada e auditável.
 *
 * <p><b>Idempotência:</b> O componente verifica a existência da tabela e da coluna
 * antes de executar qualquer {@code ALTER TABLE}, sendo seguro para múltiplas
 * inicializações.
 *
 * @see FixAudioPaths
 * @see DataSeeder
 * @see AudioPathSync
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PodcastSchemaMigrationRunner implements CommandLineRunner {

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Cria uma instância do runner inicializando o {@link JdbcTemplate} a partir
   * do {@link DataSource} injetado pelo Spring Boot.
   *
   * @param dataSource fonte de dados da aplicação (H2 em desenvolvimento).
   */
  public PodcastSchemaMigrationRunner(DataSource dataSource) {
    this.dataSource = dataSource;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Executa a migração de schema no arranque da aplicação.
   *
   * <p>Fluxo:
   * <ol>
   *   <li>Verifica se a tabela {@code PODCASTS} existe; se não, termina sem erro.</li>
   *   <li>Verifica se a coluna {@code CREATED_AT} existe; se não, adiciona-a via
   *       {@code ALTER TABLE podcasts ADD COLUMN created_at TIMESTAMP}.</li>
   *   <li>Preenche todos os registos onde {@code created_at IS NULL} com
   *       {@code CURRENT_TIMESTAMP}.</li>
   * </ol>
   *
   * <p>A verificação de existência usa os metadados JDBC ({@link DatabaseMetaData})
   * em vez de SQL direto, garantindo compatibilidade com H2 e outros motores SQL.
   * Os nomes de tabela e coluna são passados em maiúsculas para compatibilidade
   * com o H2, que normaliza os identificadores para uppercase por omissão.
   *
   * @param args argumentos de linha de comando (não utilizados).
   * @throws Exception se ocorrer erro SQL durante a migração.
   */
  @Override
  public void run(String... args) throws Exception {
    if (!tableExists("PODCASTS")) {
      return;
    }

    if (!columnExists("PODCASTS", "CREATED_AT")) {
      jdbcTemplate.execute("ALTER TABLE podcasts ADD COLUMN created_at TIMESTAMP");
    }

    jdbcTemplate.execute(
        "UPDATE podcasts SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
  }

  /**
   * Verifica se uma tabela existe na base de dados usando os metadados JDBC.
   *
   * @param tableName nome da tabela a verificar (em maiúsculas para H2).
   * @return {@code true} se a tabela existir; {@code false} caso contrário.
   * @throws SQLException se ocorrer erro ao aceder aos metadados da BD.
   */
  private boolean tableExists(String tableName) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
        return tables.next();
      }
    }
  }

  /**
   * Verifica se uma coluna existe numa tabela usando os metadados JDBC.
   *
   * @param tableName  nome da tabela (em maiúsculas para H2).
   * @param columnName nome da coluna a verificar (em maiúsculas para H2).
   * @return {@code true} se a coluna existir; {@code false} caso contrário.
   * @throws SQLException se ocorrer erro ao aceder aos metadados da BD.
   */
  private boolean columnExists(String tableName, String columnName) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
        return columns.next();
      }
    }
  }
}