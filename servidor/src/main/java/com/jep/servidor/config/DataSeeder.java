package com.jep.servidor.config;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.RssSource;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.RssSourceRepository;
import com.jep.servidor.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * Componente de arranque responsável por popular a base de dados com dados iniciais
 * (seed data) para facilitar o desenvolvimento local e os testes da plataforma.
 *
 * <p>Implementa {@link CommandLineRunner}, sendo executado automaticamente pelo Spring
 * Boot após o contexto da aplicação estar completamente inicializado. É executado
 * com prioridade por omissão (sem {@code @Order}), após o {@code DataSeeder} ter prioridade 1.
 *
 * <p><b>Condição de ativação:</b> A anotação {@link ConditionalOnProperty} garante que
 * este bean só é criado se a propriedade {@code seeder.enabled} tiver o valor {@code true},
 * ou se a propriedade não estiver definida ({@code matchIfMissing = true}). Para desativar
 * o seeder em produção, basta definir {@code seeder.enabled=false} no
 * {@code application.properties}.
 *
 * <p><b>Dados populados:</b>
 * <ul>
 *   <li><b>Utilizador admin:</b> Cria o utilizador {@code admin@podcastia.com} com tipo
 *       {@code USERADMIN} caso ainda não exista. Credenciais por omissão:
 *       email {@code admin@podcastia.com}, password {@code admin} (hash BCrypt).</li>
 *   <li><b>10 podcasts de exemplo:</b> Criados associados ao utilizador admin, cobrindo
 *       todas as categorias disponíveis ({@code DESPORTO}, {@code POLITICA},
 *       {@code FINANCAS}, {@code GERAL}). Só são criados se existirem menos de 10
 *       podcasts na base de dados.</li>
 *   <li><b>4 fontes RSS:</b> Populadas caso não existam quaisquer fontes RSS na base de
 *       dados (Observador, Público Desporto, TechCrunch, BBC News World).</li>
 * </ul>
 *
 * <p><b>Idempotência:</b> O seeder verifica condições antes de inserir dados
 * ({@code podcastRepository.count() < 10} e {@code rssSourceRepository.count() == 0}),
 * tornando-o seguro para reinicializações da aplicação sem duplicação de dados.
 *
 * @see AudioPathSync
 * @see com.jep.servidor.model.User
 * @see com.jep.servidor.model.Podcast
 * @see com.jep.servidor.model.RssSource
 */
@Component
@ConditionalOnProperty(name="seeder.enabled", havingValue="true", matchIfMissing=true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;
    private final PasswordEncoder passwordEncoder;
    private final RssSourceRepository rssSourceRepository;

    /**
     * Cria uma instância de {@code DataSeeder} com injeção das dependências necessárias.
     *
     * @param userRepository      repositório JPA para persistência de utilizadores.
     * @param podcastRepository   repositório JPA para persistência de podcasts.
     * @param passwordEncoder     codificador BCrypt para hash de palavras-passe.
     * @param rssSourceRepository repositório JPA para persistência de fontes RSS.
     */
    public DataSeeder(UserRepository userRepository, PodcastRepository podcastRepository, PasswordEncoder passwordEncoder, RssSourceRepository rssSourceRepository) {
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
        this.passwordEncoder = passwordEncoder;
        this.rssSourceRepository = rssSourceRepository;
    }

    /**
     * Ponto de execução do {@link CommandLineRunner}.
     *
     * <p>Fluxo:
     * <ol>
     *   <li>Se existirem menos de 10 podcasts na BD:
     *     <ul>
     *       <li>Cria (ou obtém) o utilizador admin com email {@code admin@podcastia.com}.</li>
     *       <li>Cria 10 podcasts de exemplo com diferentes tags e durações.</li>
     *     </ul>
     *   </li>
     *   <li>Se não existirem fontes RSS, cria 4 fontes parceiras predefinidas.</li>
     * </ol>
     *
     * @param args argumentos da linha de comando (não utilizados).
     * @throws Exception se ocorrer erro durante a persistência (propagado ao Spring Boot).
     */
    @Override
    public void run(String... args) throws Exception {
        if (podcastRepository.count() < 10) {
            User admin = userRepository.findByEmail("admin@podcastia.com").orElseGet(() -> {
                User user = new User();
                user.setUsername("admin");
                user.setTag("0000");
                user.setEmail("admin@podcastia.com");
                user.setPassword(passwordEncoder.encode("admin"));
                user.setUserType(User.UserType.USERADMIN);
                user.setBio("System Administrator");
                return userRepository.save(user);
            });

            createPodcast(admin, "Resumo Desportivo", 45, List.of(PodcastTag.DESPORTO));
            createPodcast(admin, "Debate Semanal", 60, List.of(PodcastTag.POLITICA));
            createPodcast(admin, "Mercados em Alta", 30, List.of(PodcastTag.FINANCAS));
            createPodcast(admin, "Conversa de Cafe", 90, List.of(PodcastTag.GERAL));
            createPodcast(admin, "Futebol e Negocios", 50, List.of(PodcastTag.DESPORTO, PodcastTag.FINANCAS));
            createPodcast(admin, "Politica Internacional", 75, List.of(PodcastTag.POLITICA));
            createPodcast(admin, "Dicas de Poupanca", 20, List.of(PodcastTag.FINANCAS));
            createPodcast(admin, "Entrevista Especial", 120, List.of(PodcastTag.GERAL));
            createPodcast(admin, "Olimpiadas em Foco", 40, List.of(PodcastTag.DESPORTO));
            createPodcast(admin, "Analise Eleitoral", 80, List.of(PodcastTag.POLITICA));
        }

        // Seeding RSS Sources
        if (rssSourceRepository.count() == 0) {
            createRssSource("Observador - Últimas", "https://observador.pt/feed/");
            createRssSource("Público - Desporto", "https://feeds.feedburner.com/PublicoDesporto");
            createRssSource("TechCrunch", "https://techcrunch.com/feed/");
            createRssSource("BBC News - World", "https://feeds.bbci.co.uk/news/world/rss.xml");
        }
    }

    /**
     * Cria e persiste um novo podcast de exemplo associado ao utilizador indicado.
     *
     * <p>O caminho do ficheiro de áudio ({@code conteudoPath}) é gerado como um
     * caminho de teste no formato {@code test/titulonormalizado.mp3}, sem ficheiro
     * real associado — serve apenas como placeholder para testes.
     * A capa é definida com a imagem por omissão {@code /images/default-podcast-cover.svg}.
     *
     * @param user     utilizador proprietário (host) do podcast.
     * @param title    título do podcast.
     * @param duration duração do podcast em minutos.
     * @param tags     lista de categorias/tags ({@link PodcastTag}) associadas ao podcast.
     */
    private void createPodcast(User user, String title, int duration, List<PodcastTag> tags) {
        Podcast p = new Podcast();
        p.setUser(user);
        p.setTitulo(title);
        p.setDuracao(duration);
        p.setConteudoPath("test/" + title.replaceAll("\\s+", "").toLowerCase() + ".mp3");
        p.setCoverImagePath("/images/default-podcast-cover.svg");
        p.setTags(tags);
        podcastRepository.save(p);
    }

    /**
     * Cria e persiste uma nova fonte RSS parceira.
     *
     * <p>Delega para o construtor {@link RssSource#RssSource(String, String)} que
     * inicializa a fonte com o estado {@code ativa = true} por omissão.
     *
     * @param name nome de apresentação da fonte (ex: {@code "BBC News - World"}).
     * @param url  URL do feed RSS/Atom (ex: {@code "https://feeds.bbci.co.uk/news/world/rss.xml"}).
     */
    private void createRssSource(String name, String url) {
        RssSource source = new RssSource(name, url);
        rssSourceRepository.save(source);
    }
}
