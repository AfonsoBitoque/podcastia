package com.jep.servidor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração para servir recursos estáticos (imagens, áudio, etc.)
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.podcasts.directory:generated-podcasts}")
    private String podcastsDirectory;

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
