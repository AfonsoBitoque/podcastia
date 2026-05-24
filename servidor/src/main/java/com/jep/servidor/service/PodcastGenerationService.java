package com.jep.servidor.service;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço de geração automática de podcasts com IA.
 *
 * <p>Orquestra o pipeline de dois passos:
 * <ol>
 *   <li>Gera o guião narrativo com a API Google Gemini ({@code gemini-flash-latest}).</li>
 *   <li>Sintetiza o áudio a partir do guião com Python {@code edge-tts}
 *       (voz {@code pt-PT-RaquelNeural}).</li>
 * </ol>
 *
 * <p>O Python é localizado automaticamente: tenta primeiro o venv local
 * ({@code venv/Scripts/python.exe} no Windows, {@code venv/bin/python3} no Unix),
 * e faz fallback para o {@code python} do sistema.
 *
 * <p>Require a chave da API Gemini na propriedade {@code gemini.api.key}
 * ou na variável de ambiente {@code GEMINI_API_KEY}.
 *
 * @see com.jep.servidor.controller.PodcastGenerationController
 */
@Service
public class PodcastGenerationService {

    private final PodcastRepository podcastRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${app.podcasts.directory:generated-podcasts}")
    private String podcastsDirectory;

    public PodcastGenerationService(PodcastRepository podcastRepository) {
        this.podcastRepository = podcastRepository;
    }

    /**
     * Define a chave da API Gemini programaticamente (usado em testes).
     *
     * @param geminiApiKey chave da API Gemini.
     */
    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Gera um podcast completo (guião + áudio) e persiste a entidade.
     *
     * <p>A duração é estimada com base na contagem de palavras do guião
     * (~150 palavras/min para TTS pt-PT). O podcast é criado com visibilidade
     * privada ({@code publico=false}) por defeito.
     *
     * @param user  utilizador criador do podcast.
     * @param tema  tema/título do podcast (usado como prompt para o Gemini).
     * @param tags  lista de tags; se {@code null} ou vazia, usa {@link com.jep.servidor.model.PodcastTag#GERAL}.
     * @return entidade {@link Podcast} persistida com o caminho do áudio gerado.
     * @throws Exception se a chamada à API Gemini falhar ou se o processo edge-tts terminar com erro.
     */
    public Podcast generatePodcast(User user, String tema, List<PodcastTag> tags) throws Exception {
        // 1. Generate script with Gemini
        String script = generateScript(tema);
        String cleanScript = cleanScript(script);

        // 2. Generate audio with edge-tts
        String filename = generateFilename(user, tema);
        String outputPath = generateAudio(cleanScript, filename);

        // 3. Estimate duration (rough: ~150 words/min for pt-PT TTS)
        int wordCount = cleanScript.split("\\s+").length;
        int estimatedDurationMinutes = Math.max(1, wordCount / 150);

        // 4. Save podcast entity
        Podcast podcast = new Podcast();
        podcast.setUser(user);
        podcast.setTitulo(tema);
        podcast.setDuracao(estimatedDurationMinutes);
        podcast.setConteudoPath(outputPath);
        podcast.setCoverImagePath("/images/default-podcast-cover.svg");
        podcast.setTags(tags != null && !tags.isEmpty() ? tags : List.of(PodcastTag.GERAL));
        podcast.setPublico(false);

        return podcastRepository.save(podcast);
    }

    private String generateScript(String tema) throws IOException, InterruptedException {
        String prompt = String.format("""
            Atua como um locutor profissional de podcast. Cria um guião para um episódio de podcast de 3-5 minutos sobre o seguinte tema:

            Tema: %s

            Instruções:
            - O guião deve ser narrativo, como se estivesses a falar diretamente para o ouvinte.
            - Mantém um tom envolvente e informativo.
            - Começa com uma introdução cativante.
            - Desenvolve o tema com factos e perspetivas interessantes.
            - Termina com uma conclusão que convide o ouvinte a refletir.
            - Não incluas formatações como **negrito**, (risos), ou [sons].
            - Escreve em português de Portugal.

            Guião:
            """, tema);

        return callGemini(prompt);
    }

    private String callGemini(String prompt) throws IOException, InterruptedException {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IOException("Chave Gemini nao configurada. Define gemini.api.key em env.properties ou GEMINI_API_KEY no ambiente.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey.trim();

        JSONObject contentPart = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(contentPart));
        JSONObject requestBody = new JSONObject().put("contents", new JSONArray().put(content));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } else {
            throw new IOException("Erro na API Gemini (" + response.statusCode() + "): " + response.body());
        }
    }

    private String cleanScript(String script) {
        script = script.replaceAll("\\*\\*.*?\\*\\*", "");
        script = script.replaceAll("\\*.*?\\*", "");
        script = script.replaceAll("\\(.*?\\)", "");
        script = script.replaceAll("\\[.*?\\]", "");
        script = script.replaceAll("#+ ", "");
        return script.trim();
    }

    private String generateFilename(User user, String tema) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeTema = tema.replaceAll("[^a-zA-Z0-9áàãâéèêíìóòõôúùçÁÀÃÂÉÈÊÍÌÓÒÕÔÚÙÇ]", "_")
                .replaceAll("_+", "_")
                .toLowerCase();
        if (safeTema.length() > 40) {
            safeTema = safeTema.substring(0, 40);
        }
        return String.format("user%d_%s_%s.mp3", user.getId(), safeTema, timestamp);
    }

    private String generateAudio(String script, String filename) throws IOException, InterruptedException {
        File podcastsDir = new File(podcastsDirectory);
        if (!podcastsDir.exists()) {
            podcastsDir.mkdirs();
        }

        String outputPath = podcastsDirectory + "/" + filename;

        Path tempScript = Files.createTempFile("podcastia_tts_", ".py");
        Path tempText = Files.createTempFile("podcastia_text_", ".txt");

        String escapedOutput = outputPath.replace("\\", "\\\\");
        String pythonCode = "import asyncio\n" +
                "import edge_tts\n" +
                "import sys\n" +
                "TEXT = open(sys.argv[1], encoding='utf-8').read()\n" +
                "async def main():\n" +
                "    communicate = edge_tts.Communicate(TEXT, 'pt-PT-RaquelNeural')\n" +
                "    await communicate.save('" + escapedOutput + "')\n" +
                "asyncio.run(main())\n";

        Files.writeString(tempText, script, StandardCharsets.UTF_8);
        Files.writeString(tempScript, pythonCode, StandardCharsets.UTF_8);

        String pythonExe = findPython();

        ProcessBuilder pb = new ProcessBuilder(pythonExe, tempScript.toString(), tempText.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = p.waitFor();

        Files.deleteIfExists(tempScript);
        Files.deleteIfExists(tempText);

        if (exitCode != 0) {
            throw new IOException("EdgeTTS falhou (exit " + exitCode + "): " + output.toString().trim());
        }

        return outputPath;
    }

    private String findPython() {
        // Try venv first (Windows + Unix), then fall back to system python
        String projectDir = System.getProperty("user.dir");
        File windowsVenvPython = new File(projectDir + "/venv/Scripts/python.exe");
        if (windowsVenvPython.exists()) {
            return windowsVenvPython.getAbsolutePath();
        }
        File unixVenvPython3 = new File(projectDir + "/venv/bin/python3");
        if (unixVenvPython3.exists()) {
            return unixVenvPython3.getAbsolutePath();
        }
        File unixVenvPython = new File(projectDir + "/venv/bin/python");
        if (unixVenvPython.exists()) {
            return unixVenvPython.getAbsolutePath();
        }
        return "python";
    }
}
