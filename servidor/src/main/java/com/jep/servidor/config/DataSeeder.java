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
 *   <li><b>15 podcasts de exemplo:</b> Criados associados ao admin e a 2 utilizadores
 *       demo, com ficheiros MP3 reais incluídos no repositório, cobrindo todas as categorias
 *       ({@code DESPORTO}, {@code POLITICA}, {@code FINANCAS}, {@code GERAL}). Só são
 *       criados se existirem menos de 10 podcasts na base de dados.</li>
 *   <li><b>Utilizadores demo:</b> {@code demo1@podcastia.com} e {@code demo2@podcastia.com},
 *       ambos com password {@code demo}, para facilitar testes sem registo manual.</li>
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

            // Podcasts com ficheiros de áudio reais (gerados e incluídos no repositório)
            createPodcast(admin, "História de Portugal", 10, List.of(PodcastTag.GERAL),
                    "user1_história_de_portugal_20260524_214401.mp3");
            createPodcast(admin, "Curiosidades sobre o Espaço Sideral", 8, List.of(PodcastTag.GERAL),
                    "user1_curiosidades_sobre_o_espaço_sideral_20260524_214527.mp3");
            createPodcast(admin, "A Importância do Sono para a Saúde", 9, List.of(PodcastTag.GERAL),
                    "user1_a_importância_do_sono_para_a_saúde_20260524_214629.mp3");
            createPodcast(admin, "Como Aprender Novas Línguas", 9, List.of(PodcastTag.GERAL),
                    "user1_como_aprender_novas_línguas_20260524_214727.mp3");
            createPodcast(admin, "Tecnologias que Vão Mudar o Futuro", 9, List.of(PodcastTag.GERAL),
                    "user1_tecnologias_que_vão_mudar_o_futuro_20260524_214836.mp3");
            createPodcast(admin, "Noções Básicas de Investimento", 10, List.of(PodcastTag.FINANCAS),
                    "user1_história_de_portugal_20260524_215033.mp3");
            createPodcast(admin, "História do Futebol em Portugal", 10, List.of(PodcastTag.DESPORTO),
                    "user1_história_do_futebol_em_portugal_20260524_215655.mp3");
            createPodcast(admin, "Nutrição e Suplementação para Desportistas", 9, List.of(PodcastTag.DESPORTO),
                    "user1_nutrição_e_suplementação_para_desportist_20260524_215910.mp3");
            createPodcast(admin, "Modalidades Olímpicas Pouco Conhecidas", 9, List.of(PodcastTag.DESPORTO),
                    "user1_modalidades_olímpicas_pouco_conhecidas_20260524_220019.mp3");
            createPodcast(admin, "Sistema Político Português Explicado", 10, List.of(PodcastTag.POLITICA),
                    "user1_sistema_político_português_explicado_20260524_220234.mp3");

            // Utilizador demo 1
            User demo1 = userRepository.findByEmail("demo1@podcastia.com").orElseGet(() -> {
                User user = new User();
                user.setUsername("demo1");
                user.setTag("1111");
                user.setEmail("demo1@podcastia.com");
                user.setPassword(passwordEncoder.encode("demo"));
                user.setUserType(User.UserType.USERNORMAL);
                user.setBio("Utilizador de demonstração — Desporto e Finanças");
                return userRepository.save(user);
            });
            createPodcast(demo1, "A Mentalidade Vencedora no Desporto", 9, List.of(PodcastTag.DESPORTO),
                    "user1_a_mentalidade_vencedora_no_desporto_20260524_220125.mp3");
            createPodcast(demo1, "Treino de Alta Performance para Atletas", 9, List.of(PodcastTag.DESPORTO),
                    "user1_treino_de_alta_performance_para_atletas_20260524_215757.mp3");
            createPodcast(demo1, "Economia Portuguesa", 10, List.of(PodcastTag.FINANCAS),
                    "user1_tecnologias_que_vão_mudar_o_futuro_20260524_215522.mp3");

            // Utilizador demo 2
            User demo2 = userRepository.findByEmail("demo2@podcastia.com").orElseGet(() -> {
                User user = new User();
                user.setUsername("demo2");
                user.setTag("2222");
                user.setEmail("demo2@podcastia.com");
                user.setPassword(passwordEncoder.encode("demo"));
                user.setUserType(User.UserType.USERNORMAL);
                user.setBio("Utilizador de demonstração — Política e Cultura");
                return userRepository.save(user);
            });
            createPodcast(demo2, "Relações Internacionais", 10, List.of(PodcastTag.POLITICA),
                    "user1_como_aprender_novas_línguas_20260524_215407.mp3");
            createPodcast(demo2, "Criptomoedas e Blockchain", 10, List.of(PodcastTag.FINANCAS),
                    "user1_a_importância_do_sono_para_a_saúde_20260524_215311.mp3");
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
     * <p>O caminho do ficheiro de áudio ({@code conteudoPath}) é construído como
     * {@code generated-podcasts/<audioFileName>}, apontando para um ficheiro MP3 real
     * incluído no repositório. A capa é definida com a imagem por omissão
     * {@code /images/default-podcast-cover.svg}.
     *
     * @param user          utilizador proprietário (host) do podcast.
     * @param title         título do podcast.
     * @param duration      duração do podcast em minutos.
     * @param tags          lista de categorias/tags ({@link PodcastTag}) associadas ao podcast.
     * @param audioFileName nome do ficheiro MP3 dentro de {@code generated-podcasts/}.
     */
    private void createPodcast(User user, String title, int duration, List<PodcastTag> tags, String audioFileName) {
        Podcast p = new Podcast();
        p.setUser(user);
        p.setTitulo(title);
        p.setDuracao(duration);
        p.setConteudoPath("generated-podcasts/" + audioFileName);
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
