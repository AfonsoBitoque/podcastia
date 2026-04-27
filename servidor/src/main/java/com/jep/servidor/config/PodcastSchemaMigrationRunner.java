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
 * Garante que a coluna created_at existe na tabela podcasts em bases locais antigas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PodcastSchemaMigrationRunner implements CommandLineRunner {

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public PodcastSchemaMigrationRunner(DataSource dataSource) {
    this.dataSource = dataSource;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

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

  private boolean tableExists(String tableName) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
        return tables.next();
      }
    }
  }

  private boolean columnExists(String tableName, String columnName) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
        return columns.next();
      }
    }
  }
}