package com.jep.servidor.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Componente de arranque que aplica correções pontuais e hardcoded aos caminhos de
 * áudio de podcasts específicos diretamente na base de dados via JDBC.
 *
 * <p>É executado com prioridade {@code @Order(1)}, sendo o <b>primeiro</b>
 * {@link CommandLineRunner} a correr no arranque, antes do {@code DataSeeder} (order omitido)
 * e do {@code AudioPathSync} (order 2). Isto garante que os caminhos corrigidos manualmente
 * estão já presentes quando o {@code AudioPathSync} faz a sua passagem de reconciliação.
 *
 * <p><b>Contexto e propósito:</b> Este componente foi criado como solução de emergência
 * para corrigir caminhos de áudio de podcasts gerados pelo utilizador com ID 194 durante
 * uma sessão de desenvolvimento em 13/05/2026. Os ficheiros MP3 foram gerados com nomes
 * normalizados (ASCII, sem acentos) mas os caminhos guardados na base de dados podem ter
 * ficado incorretos ou em falta devido a erros de codificação de caracteres.
 *
 * <p><b>Mecanismo:</b> Utiliza {@link JdbcTemplate} para executar diretamente instruções
 * SQL {@code UPDATE} sobre a tabela {@code podcasts}, evitando overhead do contexto JPA/Hibernate
 * (útil em fases muito precoces do arranque). Cada entrada no mapeamento corresponde a um
 * par {@code (id_podcast → caminho_ficheiro_mp3)}.
 *
 * <p><b>Atenção — dados hardcoded:</b> Os IDs de podcast e caminhos de ficheiro estão
 * embutidos diretamente no código, o que é uma abordagem de desenvolvimento rápido
 * não adequada para ambientes de produção escaláveis. Caso seja necessário corrigir outros
 * podcasts, deve-se adicionar entradas ao {@code pathMap} ou migrar esta lógica para um
 * script de migração SQL (ex: Flyway/Liquibase).
 *
 * @see AudioPathSync
 * @see com.jep.servidor.model.Podcast
 */
@Component
@Order(1)
public class FixAudioPaths implements CommandLineRunner {

    /**
     * Template JDBC para execução direta de SQL, sem overhead do contexto JPA.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Cria uma instância de {@code FixAudioPaths} construindo um {@link JdbcTemplate}
     * a partir do {@link DataSource} injetado pelo Spring.
     *
     * @param dataSource fonte de dados configurada pelo Spring Boot
     *                   (H2 em desenvolvimento, conforme {@code application.properties}).
     */
    public FixAudioPaths(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Ponto de execução do {@link CommandLineRunner}.
     *
     * <p>Aplica atualizações SQL diretas para corrigir a coluna {@code conteudo_path}
     * de podcasts específicos identificados pelos seus IDs primários.
     *
     * <p>Para cada entrada no mapeamento {@code (id → caminho)}:
     * <ul>
     *   <li>Executa {@code UPDATE podcasts SET conteudo_path = ? WHERE id = ?}.</li>
     *   <li>Se a linha foi atualizada ({@code updated > 0}), regista no stdout.</li>
     *   <li>Erros individuais são capturados e registados no stderr sem interromper
     *       o processamento das restantes entradas.</li>
     * </ul>
     *
     * <p>Podcasts corrigidos (todos do utilizador ID 194, gerados em 13/05/2026):
     * <ul>
     *   <li>ID 33 — história de portugal</li>
     *   <li>ID 34 — a importância do sono</li>
     *   <li>ID 35 — história do futebol português</li>
     *   <li>ID 36 — modalidades olímpicas</li>
     *   <li>ID 37 — a mentalidade vencedora no desporto</li>
     *   <li>ID 38 — relações internacionais</li>
     *   <li>ID 39 — noções básicas de investimento</li>
     *   <li>ID 40 — economia portuguesa</li>
     *   <li>ID 41 — criptomoedas e blockchain</li>
     * </ul>
     *
     * @param args argumentos da linha de comando (não utilizados).
     * @throws Exception se ocorrer erro geral inesperado (capturado internamente com log).
     */
    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("[FixAudioPaths] A corrigir caminhos de áudio...");

            // Mapeamento de ID do podcast para nome do ficheiro (ASCII)
            Map<Integer, String> pathMap = new HashMap<>();
            pathMap.put(33, "generated-podcasts/user194_historia_de_portugal_20260513_152452.mp3");
            pathMap.put(34, "generated-podcasts/user194_a_importancia_do_sono_20260513_152618.mp3");
            pathMap.put(35, "generated-podcasts/user194_historia_do_futebol_portugues_20260513_152806.mp3");
            pathMap.put(36, "generated-podcasts/user194_modalidades_olimpicas_20260513_152915.mp3");
            pathMap.put(37, "generated-podcasts/user194_a_mentalidade_vencedora_no_desporto_20260513_153309.mp3");
            pathMap.put(38, "generated-podcasts/user194_relacoes_internacionais_20260513_153653.mp3");
            pathMap.put(39, "generated-podcasts/user194_nocoes_basicas_de_investimento_20260513_153819.mp3");
            pathMap.put(40, "generated-podcasts/user194_economia_portuguesa_20260513_153927.mp3");
            pathMap.put(41, "generated-podcasts/user194_criptomoedas_e_blockchain_20260513_154050.mp3");

            int updatedCount = 0;
            for (Map.Entry<Integer, String> entry : pathMap.entrySet()) {
                try {
                    int updated = jdbcTemplate.update(
                        "UPDATE podcasts SET conteudo_path = ? WHERE id = ?",
                        entry.getValue(),
                        entry.getKey()
                    );
                    if (updated > 0) {
                        System.out.println("[FixAudioPaths] Atualizado podcast ID " + entry.getKey() + " -> " + entry.getValue());
                        updatedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("[FixAudioPaths] Erro ao atualizar ID " + entry.getKey() + ": " + e.getMessage());
                }
            }

            System.out.println("[FixAudioPaths] Concluído. " + updatedCount + " podcasts atualizados.");
        } catch (Exception e) {
            System.err.println("[FixAudioPaths] Erro geral: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
