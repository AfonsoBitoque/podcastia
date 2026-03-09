package com.jep.servidor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da aplicação.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Define a cadeia de filtros de segurança.
   *
   * @param http Objeto HttpSecurity para configuração.
   * @return A cadeia de filtros configurada.
   * @throws Exception Se ocorrer um erro na configuração.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable) // Desativar CSRF para simplificar testes de API
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/users", "/users/**").permitAll() // Permitir registo
            .requestMatchers("/h2-console/**").permitAll() // Permitir H2 Console
            .anyRequest().permitAll() // Permitir tudo por enquanto (dev mode)
        )
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)); // Para H2 Console

    return http.build();
  }

  /**
   * Define o codificador de palavras-passe.
   *
   * @return Uma instância de BCryptPasswordEncoder.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
