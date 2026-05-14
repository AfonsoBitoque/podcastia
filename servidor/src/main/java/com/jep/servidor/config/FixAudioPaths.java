package com.jep.servidor.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Corrige caminhos de áudio na base de dados
 */
@Component
@Order(1)
public class FixAudioPaths implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public FixAudioPaths(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

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
