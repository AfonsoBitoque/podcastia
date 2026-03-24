package com.jep.servidor.config;

import com.jep.servidor.model.Podcast;
import com.jep.servidor.model.PodcastTag;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.PodcastRepository;
import com.jep.servidor.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PodcastRepository podcastRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PodcastRepository podcastRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.podcastRepository = podcastRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
    }

    private void createPodcast(User user, String title, int duration, List<PodcastTag> tags) {
        Podcast p = new Podcast();
        p.setUser(user);
        p.setTitulo(title);
        p.setDuracao(duration);
        p.setConteudoPath("test/" + title.replaceAll("\\s+", "").toLowerCase() + ".mp3");
        p.setTags(tags);
        podcastRepository.save(p);
    }
}
