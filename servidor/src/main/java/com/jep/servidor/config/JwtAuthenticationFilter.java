package com.jep.servidor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que interceta todos os pedidos HTTP para validar o token JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Endpoints que não devem ser filtrados (públicos)
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/login",
        "/api/auth/",
        "/api/register/",
        "/users",
        "/api/search/",
        // NOTA: /api/podcasts/ foi removido - apenas GET é público (configurado no SecurityConfig)
        // POST /api/podcasts/generate requer autenticação
        "/images/",
        "/audio/",
        "/h2-console/",
        "/v3/api-docs/",
        "/swagger-ui/",
        "/ws/"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Não filtrar endpoints públicos
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath) || path.contains("/audio")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out.println("=== JWT Filter === URL: " + request.getRequestURI() + " | Auth Header: " + (authHeader != null ? "present" : "missing"));

        // 1. Verifica se o header Authorization existe e começa por "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JWT Filter: No Bearer token found, skipping...");
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("JWT Filter: Bearer token found, validating...");

        // 2. Extrai o token
        jwt = authHeader.substring(7);
        try {
            userEmail = jwtUtil.extractEmail(jwt);

            // 3. Se temos o email e o utilizador ainda não está autenticado no contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Valida o token
                if (jwtUtil.isTokenValid(jwt, userEmail)) {
                    System.out.println("JWT Filter: Token valid for user: " + userEmail);
                    // Como não estamos a usar UserDetailsService completo por agora,
                    // criamos a autenticação diretamente com o email
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("USER"))
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Guarda a autenticação no contexto de segurança do Spring
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("JWT Filter: Authentication set in context");
                } else {
                    System.out.println("JWT Filter: Token INVALID for user: " + userEmail);
                }
            }
        } catch (Exception e) {
            // Se o token for inválido, malformado, ou expirado, simplesmente ignora e não autentica
            System.err.println("Erro na validação do JWT: " + e.getMessage());
        }

        // 4. Verificar se autenticação foi definida
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("JWT Filter: No authentication set in context - request will be anonymous");
        } else {
            System.out.println("JWT Filter: Authentication set for: " + SecurityContextHolder.getContext().getAuthentication().getName());
        }
        
        // 5. Continua a cadeia de filtros
        filterChain.doFilter(request, response);
    }
}
