package com.jep.servidor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Spring MVC para servir recursos estáticos (imagens e ficheiros de áudio)
 * através de handlers HTTP dedicados.
 *
 * <p>Implementa {@link WebMvcConfigurer} e sobrepõe {@link #addResourceHandlers} para
 * registar dois mapeamentos de recursos estáticos:
 *
 * <ul>
 *   <li><b>{@code /images/**}:</b> Serve imagens do classpath
 *       {@code classpath:/static/images/}. Estas imagens estão empacotadas no JAR
 *       (ex: {@code default-podcast-cover.svg}) e são acessíveis sem autenticação
 *       conforme configurado em {@link SecurityConfig}.</li>
 *   <li><b>{@code /audio/**}:</b> Serve ficheiros MP3 do sistema de ficheiros local,
 *       no diretório configurado pela propriedade {@code app.podcasts.directory}
 *       (por omissão: {@code generated-podcasts/}). O prefixo {@code file:} indica
 *       que o caminho é relativo ao diretório de trabalho do servidor, não do classpath.</li>
 * </ul>
 *
 * <p><b>Configuração via propriedades:</b> O diretório de podcasts pode ser alterado
 * no {@code application.properties}:
 * <pre>{@code app.podcasts.directory=/caminho/absoluto/para/podcasts}</pre>
 * Por omissão usa {@code generated-podcasts} (relativo ao working directory).
 *
 * <p><b>Integração com outros componentes:</b>
 * <ul>
 *   <li>O {@link com.jep.servidor.service.PodcastGenerationService} gera os ficheiros MP3
 *       no diretório {@code generated-podcasts/}.</li>
 *   <li>O {@link com.jep.servidor.controller.PodcastGenerationController} expõe endpoints
 *       de streaming ({@code /api/podcasts/{id}/stream}) que também leem deste diretório
 *       diretamente via {@code FileSystemResource}.</li>
 *   <li>Os URLs {@code /images/**} e {@code /audio/**} estão declarados como públicos
 *       em {@link SecurityConfig} (sem autenticação JWT).</li>
 * </ul>
 *
 * @see SecurityConfig
 * @see com.jep.servidor.service.PodcastGenerationService
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    /**
     * Diretório do sistema de ficheiros onde os ficheiros MP3 gerados são armazenados.
     * Injetado da propriedade {@code app.podcasts.directory}; por omissão: {@code generated-podcasts}.
     */
    @Value("${app.podcasts.directory:generated-podcasts}")
    private String podcastsDirectory;

    /**
     * Regista os handlers de recursos estáticos para imagens e ficheiros de áudio.
     *
     * <p>Mapeamentos registados:
     * <ul>
     *   <li>{@code /images/**} → {@code classpath:/static/images/}
     *       (recursos empacotados no JAR)</li>
     *   <li>{@code /audio/**} → {@code file:<podcastsDirectory>/}
     *       (sistema de ficheiros local, caminho configurável)</li>
     * </ul>
     *
     * @param registry registo de handlers de recursos do Spring MVC.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir imagens da pasta static/images
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");

        // Servir ficheiros de áudio da pasta generated-podcasts
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file:" + podcastsDirectory + "/");
    }
}
