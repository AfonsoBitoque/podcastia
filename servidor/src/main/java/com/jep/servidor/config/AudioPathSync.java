package com.jep.servidor.config;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.repository.PodcastRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;

/**
 * Sincroniza caminhos de áudio com ficheiros reais no sistema
 */
@Component
@Order(2)
public class AudioPathSync implements CommandLineRunner {

    private final PodcastRepository podcastRepository;

    public AudioPathSync(PodcastRepository podcastRepository) {
        this.podcastRepository = podcastRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            String baseDir = "generated-podcasts";
            Path podcastsDir = Paths.get(baseDir);

            if (!Files.exists(podcastsDir)) {
                System.out.println("[AudioPathSync] Diretório " + baseDir + " não encontrado");
                return;
            }

            // Lista todos os ficheiros MP3 (incluindo links simbólicos)
            File[] files = podcastsDir.toFile().listFiles((dir, name) -> name.endsWith(".mp3"));
            if (files == null || files.length == 0) {
                System.out.println("[AudioPathSync] Nenhum ficheiro MP3 encontrado");
                return;
            }

            System.out.println("[AudioPathSync] Encontrados " + files.length + " ficheiros de áudio");

            List<Podcast> allPodcasts = podcastRepository.findAll();
            int updatedCount = 0;

            for (Podcast podcast : allPodcasts) {
                String currentPath = podcast.getConteudoPath();
                Long userId = podcast.getUser() != null ? podcast.getUser().getId() : null;

                // Se já tem caminho válido, pula
                if (currentPath != null && !currentPath.isEmpty()) {
                    File testFile = new File(currentPath);
                    if (testFile.exists()) {
                        continue; // Caminho já está correto
                    }
                }

                // Procura ficheiro correspondente
                File match = findMatchingFile(files, userId, podcast.getTitulo());

                if (match != null) {
                    String newPath = baseDir + "/" + match.getName();
                    podcast.setConteudoPath(newPath);
                    podcastRepository.save(podcast);
                    System.out.println("[AudioPathSync] Atualizado: " + podcast.getTitulo() + " -> " + match.getName());
                    updatedCount++;
                } else {
                    System.out.println("[AudioPathSync] Não encontrado ficheiro para: " + podcast.getTitulo() + " (user=" + userId + ")");
                }
            }

            System.out.println("[AudioPathSync] Sincronização completa. " + updatedCount + " podcasts atualizados.");
        } catch (Exception e) {
            System.err.println("[AudioPathSync] Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File findMatchingFile(File[] files, Long userId, String podcastTitle) {
        if (userId == null) return null;

        String userPrefix = "user" + userId + "_";
        String normalizedTitle = normalize(podcastTitle.toLowerCase().replace(" ", "_"));

        File bestMatch = null;
        int bestScore = 0;

        for (File file : files) {
            String filename = file.getName();

            // Só considera ficheiros do mesmo user
            if (!filename.startsWith(userPrefix)) {
                continue;
            }

            // Extrai título do ficheiro
            String fileTitle = extractTitleFromFilename(filename);
            String normalizedFileTitle = normalize(fileTitle);

            int score = 0;

            // Match exato
            if (fileTitle.equals(podcastTitle.toLowerCase().replace(" ", "_"))) {
                score += 100;
            }

            // Match normalizado
            if (normalizedFileTitle.equals(normalizedTitle)) {
                score += 90;
            }

            // Contém substring (ambos os sentidos)
            if (normalizedFileTitle.contains(normalizedTitle) || normalizedTitle.contains(normalizedFileTitle)) {
                score += 50;
            }

            // Palavras em comum
            String[] words = normalizedTitle.split("_");
            for (String word : words) {
                if (word.length() > 3 && normalizedFileTitle.contains(word)) {
                    score += 15;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = file;
            }
        }

        return bestScore >= 30 ? bestMatch : null;
    }

    private String extractTitleFromFilename(String filename) {
        // Remove userNNN_ prefixo e _datahora.mp3 sufixo
        String noPrefix = filename.replaceAll("^user\\d+_", "");
        String noSuffix = noPrefix.replaceAll("_\\d{8}_\\d{6}\\.mp3$", "");
        return noSuffix.toLowerCase();
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }
}
