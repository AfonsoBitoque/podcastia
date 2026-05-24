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
 * Componente de arranque responsável por sincronizar os caminhos de áudio dos podcasts
 * com os ficheiros MP3 realmente presentes no sistema de ficheiros.
 *
 * <p>É executado automaticamente no arranque da aplicação (implementa {@link CommandLineRunner})
 * com prioridade {@code @Order(2)}, ou seja, após o {@code DataSeeder} (order 1) popular a base
 * de dados com dados iniciais.
 *
 * <p><b>Problema que resolve:</b> Após mover, reinstalar ou restaurar o servidor, os caminhos
 * armazenados na coluna {@code conteudo_path} da tabela {@code podcasts} podem deixar de
 * apontar para ficheiros válidos. Este componente percorre todos os podcasts e, para aqueles
 * cujo caminho é inválido ou está em falta, tenta localizar o ficheiro MP3 correspondente no
 * diretório {@code generated-podcasts/} usando um algoritmo de correspondência por pontuação.
 *
 * <p><b>Algoritmo de correspondência (scoring):</b>
 * <ol>
 *   <li>Filtra apenas ficheiros do mesmo utilizador (prefixo {@code userNNN_}).</li>
 *   <li>Atribui pontuação progressiva:
 *     <ul>
 *       <li>+100 — correspondência exata do título.</li>
 *       <li>+90 — correspondência após normalização Unicode (remove acentos).</li>
 *       <li>+50 — um dos títulos contém o outro como substring.</li>
 *       <li>+15 por palavra — palavras com mais de 3 caracteres presentes em ambos.</li>
 *     </ul>
 *   </li>
 *   <li>Seleciona o ficheiro com maior pontuação, desde que seja ≥ 30.</li>
 * </ol>
 *
 * <p><b>Formato esperado dos nomes de ficheiro MP3:</b>
 * {@code userNNN_titulo_do_podcast_YYYYMMDD_HHmmss.mp3}
 * (gerado pelo {@code PodcastGenerationService}).
 *
 * @see PodcastGenerationService
 * @see com.jep.servidor.model.Podcast
 */
@Component
@Order(2)
public class AudioPathSync implements CommandLineRunner {

    private final PodcastRepository podcastRepository;

    /**
     * Cria uma instância de {@code AudioPathSync} com injeção do repositório de podcasts.
     *
     * @param podcastRepository repositório JPA para leitura e atualização de podcasts.
     */
    public AudioPathSync(PodcastRepository podcastRepository) {
        this.podcastRepository = podcastRepository;
    }

    /**
     * Ponto de execução do {@link CommandLineRunner}.
     *
     * <p>Fluxo de execução:
     * <ol>
     *   <li>Verifica se o diretório {@code generated-podcasts/} existe; caso contrário, termina.</li>
     *   <li>Lista todos os ficheiros {@code .mp3} presentes no diretório.</li>
     *   <li>Para cada podcast na base de dados:
     *     <ul>
     *       <li>Se o caminho atual é válido (ficheiro existe), ignora.</li>
     *       <li>Caso contrário, tenta encontrar um ficheiro correspondente via
     *           {@link #findMatchingFile(File[], Long, String)}.</li>
     *       <li>Se encontrado, atualiza {@code conteudoPath} e persiste.</li>
     *     </ul>
     *   </li>
     *   <li>Imprime no stdout o número de podcasts atualizados.</li>
     * </ol>
     *
     * @param args argumentos de linha de comando (não utilizados).
     * @throws Exception se ocorrer um erro inesperado (capturado internamente com log).
     */
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

    /**
     * Procura o ficheiro MP3 mais provável para um podcast com base no ID do utilizador
     * e no título do podcast, usando um sistema de pontuação (scoring).
     *
     * <p>Apenas ficheiros cujo nome começa com {@code userNNN_} (onde NNN é o {@code userId})
     * são considerados candidatos, garantindo que não há colisão entre utilizadores diferentes.
     *
     * @param files       array de ficheiros MP3 presentes no diretório de áudio.
     * @param userId      ID do utilizador proprietário do podcast; se {@code null}, retorna {@code null}.
     * @param podcastTitle título do podcast a pesquisar.
     * @return o {@link File} com maior pontuação (≥ 30), ou {@code null} se nenhum corresponder.
     */
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

    /**
     * Extrai o segmento de título do nome de um ficheiro MP3 gerado pelo sistema.
     *
     * <p>Remove o prefixo {@code userNNN_} e o sufixo {@code _YYYYMMDD_HHmmss.mp3},
     * devolvendo apenas a parte central que corresponde ao título do podcast.
     *
     * <p>Exemplo: {@code user5_inteligencia_artificial_20240101_120000.mp3}
     * → {@code inteligencia_artificial}
     *
     * @param filename nome do ficheiro MP3 (com extensão).
     * @return título extraído em minúsculas, sem prefixo nem sufixo de timestamp.
     */
    private String extractTitleFromFilename(String filename) {
        // Remove userNNN_ prefixo e _datahora.mp3 sufixo
        String noPrefix = filename.replaceAll("^user\\d+_", "");
        String noSuffix = noPrefix.replaceAll("_\\d{8}_\\d{6}\\.mp3$", "");
        return noSuffix.toLowerCase();
    }

    /**
     * Normaliza uma string removendo acentos e outros caracteres não-ASCII.
     *
     * <p>Aplica decomposição canónica Unicode (NFD) e remove todos os caracteres que não
     * sejam ASCII básico, garantindo comparações insensíveis a diacríticos (ex: {@code "música"}
     * torna-se {@code "musica"}).
     *
     * @param input string a normalizar; se {@code null}, retorna string vazia.
     * @return string normalizada em minúsculas sem acentos.
     */
    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase();
    }
}
