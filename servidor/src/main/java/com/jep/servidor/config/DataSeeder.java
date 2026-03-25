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
 * Componente responsável por popular a base de dados com dados iniciais (seeding)
 * para facilitar testes e desenvolvimento local.
 * Só executa caso a propriedade seeder.enabled seja true ou esteja em falta.
 */
@Component
@ConditionalOnProperty(name="seeder.enabled", havingValue="true", matchIfMissing=true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;
    private final PasswordEncoder passwordEncoder;
    private final RssSourceRepository rssSourceRepository;

    /**
     * Construtor para injeção de dependências.
     *
     * @param userRepository Repositório de utilizadores.
     * @param podcastRepository Repositório de podcasts.
     * @param passwordEncoder Codificador de palavras-passe.
     * @param rssSourceRepository Repositório de fontes de feed RSS.
     */
    public DataSeeder(UserRepository userRepository, PodcastRepository podcastRepository, PasswordEncoder passwordEncoder, RssSourceRepository rssSourceRepository) {
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
        this.passwordEncoder = passwordEncoder;
        this.rssSourceRepository = rssSourceRepository;
    }

    /**
     * Método executado automaticamente no arranque da aplicação para inserir os dados.
     *
     * @param args Argumentos da linha de comandos.
     * @throws Exception Caso ocorra algum erro durante a execução.
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
     * Cria um novo podcast associado a um utilizador e guarda-o na base de dados.
     *
     * @param user Utilizador proprietário (host) do podcast.
     * @param title Título do podcast.
     * @param duration Duração em minutos.
     * @param tags Lista de categorias/tags do podcast.
     */
    private void createPodcast(User user, String title, int duration, List<PodcastTag> tags) {
        Podcast p = new Podcast();
        p.setUser(user);
        p.setTitulo(title);
        p.setDuracao(duration);
        p.setConteudoPath("test/" + title.replaceAll("\\s+", "").toLowerCase() + ".mp3");
        p.setTags(tags);
        podcastRepository.save(p);
    }

    /**
     * Cria uma nova fonte RSS parceira e guarda-a na base de dados.
     *
     * @param name Nome de apresentação da fonte.
     * @param url Link original do feed XML/RSS.
     */
    private void createRssSource(String name, String url) {
        RssSource source = new RssSource(name, url);
        rssSourceRepository.save(source);
    }
}
