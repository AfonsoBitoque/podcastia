package com.jep.servidor.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central de segurança da aplicação Podcastia, baseada em Spring Security
 * com autenticação JWT stateless.
 *
 * <p>As três anotações de classe têm os seguintes efeitos:
 * <ul>
 *   <li>{@code @Configuration} — declara esta classe como fonte de beans Spring.</li>
 *   <li>{@code @EnableWebSecurity} — ativa a configuração personalizada do Spring Security,
 *       substituindo a configuração automática por omissão.</li>
 *   <li>{@code @EnableMethodSecurity} — habilita a segurança ao nível de método com
 *       {@code @PreAuthorize}, {@code @PostAuthorize}, etc., usada nos endpoints de admin
 *       ({@code @PreAuthorize("hasRole('USER_ADMIN')")}).</li>
 * </ul>
 *
 * <p><b>Política de sessão:</b> {@code STATELESS} — o servidor nunca cria nem usa sessões HTTP
 * ({@code JSESSIONID}). Cada pedido deve incluir um token JWT válido no cabeçalho
 * {@code Authorization: Bearer <token>}.
 *
 * <p><b>CSRF:</b> Desativado — desnecessário em APIs REST stateless com autenticação por token.
 *
 * <p><b>Endpoints públicos (sem autenticação):</b>
 * <ul>
 *   <li>{@code OPTIONS /**} — pedidos preflight CORS.</li>
 *   <li>{@code /api/auth/**} — login.</li>
 *   <li>{@code /users}, {@code /users/**}, {@code /api/users/**} — registo e perfis públicos.</li>
 *   <li>{@code /api/register/**} — verificação/geração de tag.</li>
 *   <li>{@code /api/search/**} — pesquisa pública.</li>
 *   <li>{@code GET /api/podcasts}, {@code GET /api/podcasts/**} — listagem de podcasts.</li>
 *   <li>{@code GET /podcasts}, {@code GET /podcasts/**} — endpoints públicos do PodcastController.</li>
 *   <li>{@code /images/**}, {@code /audio/**} — recursos estáticos.</li>
 *   <li>{@code /ws/**} — handshake WebSocket (autenticado pelo JWT no interceptor).</li>
 *   <li>{@code /h2-console/**} — consola H2 (apenas desenvolvimento).</li>
 *   <li>{@code /v3/api-docs/**}, {@code /swagger-ui/**} — documentação OpenAPI.</li>
 *   <li>{@code /error} — página de erro Spring.</li>
 * </ul>
 *
 * <p><b>CORS:</b> Permite origens {@code localhost:5173}, {@code 127.0.0.1:5173},
 * {@code localhost:5174} e {@code 127.0.0.1:5174} (frontend Vite/React em dev).
 * Todos os métodos HTTP são permitidos, todos os cabeçalhos são aceites,
 * credentials são permitidas, e o preflight é cacheado por 3600 segundos.
 *
 * @see JwtAuthenticationFilter
 * @see JwtUtil
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Filtro JWT injetado que é adicionado à cadeia de filtros de segurança antes do
   * {@link UsernamePasswordAuthenticationFilter} padrão do Spring Security.
   */
  @Autowired
  private JwtAuthenticationFilter jwtAuthFilter;

  /**
   * Configura e produz o bean {@link SecurityFilterChain} com as regras de autorização,
   * gestão de sessão, CORS, CSRF e integração do filtro JWT.
   *
   * @param http objeto {@link HttpSecurity} fornecido pelo Spring para configuração.
   * @return cadeia de filtros de segurança totalmente configurada.
   * @throws Exception se ocorrer erro durante a configuração do HttpSecurity.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable) // Desativar CSRF pois usamos JWT
        .authorizeHttpRequests(auth -> auth
            // Allow preflight CORS requests without authentication
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Endpoints públicos
            .requestMatchers("/api/auth/**").permitAll() // Login REST
            .requestMatchers("/users", "/users/**", "/api/users/**").permitAll() // Registo de conta e perfis
            .requestMatchers("/api/register/**").permitAll() // Gerar/Verificar Tag REST
            .requestMatchers("/api/search/**").permitAll() // Pesquisa publica
            .requestMatchers(HttpMethod.GET, "/api/podcasts/**").permitAll() // Todos os GET requests de podcasts são públicos
            .requestMatchers("/api/podcasts/**").permitAll() // Allow all podcast endpoints (POST still needs auth but will be handled by method security)
            .requestMatchers(HttpMethod.GET, "/podcasts", "/podcasts/**").permitAll() // PodcastController endpoints publicos
            .requestMatchers("/images/**").permitAll() // Imagens estáticas
            .requestMatchers("/audio/**").permitAll() // Ficheiros de áudio
            .requestMatchers("/ws/**").permitAll() // Handshake WebSocket autenticado por token
            .requestMatchers("/h2-console/**").permitAll() // H2 Console
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Swagger OpenAPI
            .requestMatchers("/error").permitAll()
            
            // Tudo o resto exige estar autenticado com um JWT válido
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            // A API REST é STATELESS. Não guarda sessões JSESSIONID.
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        // Adicionar o nosso filtro JWT antes do filtro padrão do Spring
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Produz o bean {@link PasswordEncoder} usando o algoritmo BCrypt.
   *
   * <p>BCrypt é usado para hash de palavras-passe na criação de contas e para
   * verificação no login ({@link com.jep.servidor.controller.AuthController}).
   * O fator de custo por omissão (10) é adequado para desenvolvimento.
   *
   * @return instância de {@link BCryptPasswordEncoder}.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configura e produz o bean {@link CorsConfigurationSource} com as políticas CORS globais.
   *
   * <p>Configuração aplicada a todos os caminhos ({@code /**}):
   * <ul>
   *   <li><b>Origens permitidas:</b> {@code localhost:5173}, {@code 127.0.0.1:5173},
   *       {@code localhost:5174}, {@code 127.0.0.1:5174} (servidores de dev Vite).</li>
   *   <li><b>Métodos permitidos:</b> GET, POST, PUT, DELETE, PATCH, OPTIONS.</li>
   *   <li><b>Cabeçalhos permitidos:</b> Todos ({@code *}).</li>
   *   <li><b>Credentials:</b> Permitidas ({@code Access-Control-Allow-Credentials: true}),
   *       necessário para envio de cookies e cabeçalhos de autorização.</li>
   *   <li><b>Max-Age preflight:</b> 3600 segundos (1 hora de cache).</li>
   * </ul>
   *
   * @return fonte de configuração CORS registada para todos os caminhos da API.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "http://localhost:5173", 
        "http://127.0.0.1:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5174"
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
