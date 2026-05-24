package com.jep.servidor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação OpenAPI 3 (Swagger UI) para a API REST do Podcastia.
 *
 * <p>Utiliza a biblioteca SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui) para gerar
 * automaticamente a especificação OpenAPI 3.0 a partir das anotações dos controllers,
 * disponibilizando-a em:
 * <ul>
 *   <li><b>JSON/YAML:</b> {@code GET /v3/api-docs}</li>
 *   <li><b>Swagger UI:</b> {@code GET /swagger-ui.html} ou {@code /swagger-ui/index.html}</li>
 * </ul>
 *
 * <p>Estes endpoints estão configurados como públicos (sem autenticação) em
 * {@link SecurityConfig}, permitindo acesso à documentação sem token JWT.
 *
 * <p>Esta classe define apenas os metadados gerais da API (título, versão, descrição e licença).
 * A documentação dos endpoints individuais é gerada automaticamente pelo SpringDoc a partir
 * dos controllers anotados com {@code @RestController}.
 *
 * @see SecurityConfig
 */
@Configuration
public class OpenApiConfig {

  /**
   * Produz o bean {@link OpenAPI} com os metadados da API Podcastia.
   *
   * <p>Metadados configurados:
   * <ul>
   *   <li><b>Título:</b> {@code "Podcastia API"}</li>
   *   <li><b>Versão:</b> {@code "v1.0.0"}</li>
   *   <li><b>Descrição:</b> {@code "Documentação da API REST do projeto Podcastia."}</li>
   *   <li><b>Licença:</b> Apache 2.0</li>
   * </ul>
   *
   * @return instância configurada de {@link OpenAPI} registada como bean Spring.
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Podcastia API")
            .version("v1.0.0")
            .description("Documentação da API REST do projeto Podcastia.")
            .license(new License().name("Apache 2.0").url("http://springdoc.org")));
  }
}
