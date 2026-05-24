package com.jep.servidor.config;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de segurança responsável por interceptar todos os pedidos HTTP e validar
 * o token JWT presente no cabeçalho {@code Authorization}.
 *
 * <p>Estende {@link OncePerRequestFilter}, garantindo que o filtro é executado
 * <b>exatamente uma vez por pedido</b>, mesmo em cadeias de filtros que possam
 * reencaminhar internamente o pedido.
 *
 * <p><b>Fluxo de autenticação:</b>
 * <ol>
 *   <li>Verifica se o caminho do pedido corresponde a um dos {@code PUBLIC_PATHS};
 *       se sim, passa à frente sem validar ({@link #shouldNotFilter}).</li>
 *   <li>Lê o cabeçalho {@code Authorization}; se ausente ou sem prefixo {@code Bearer },
 *       passa à frente sem autenticar (o pedido será anónimo).</li>
 *   <li>Extrai o email do token via {@link JwtUtil#extractEmail(String)}.</li>
 *   <li>Se o token for válido ({@link JwtUtil#isTokenValid}) e não existir autenticação
 *       no contexto, cria um {@link UsernamePasswordAuthenticationToken} com o email
 *       como principal e as autoridades determinadas por {@link #buildAuthorities}.</li>
 *   <li>Regista a autenticação no {@link SecurityContextHolder}.</li>
 *   <li>Propaga o pedido pela cadeia de filtros.</li>
 * </ol>
 *
 * <p><b>Autoridades atribuídas:</b>
 * <ul>
 *   <li>Todos os utilizadores autenticados recebem {@code ROLE_USER}.</li>
 *   <li>Utilizadores com {@code UserType.USERADMIN} recebem adicionalmente
 *       {@code ROLE_USER_ADMIN} e {@code ROLE_USERADMIN} (redundância intencional
 *       para compatibilidade com {@code @PreAuthorize("hasRole('USER_ADMIN')")}).</li>
 * </ul>
 *
 * <p><b>Endpoints públicos ({@code PUBLIC_PATHS}):</b> Caminhos que começam com
 * {@code /api/auth/}, {@code /api/register/}, {@code /users}, {@code /api/search/},
 * {@code /images/}, {@code /audio/}, {@code /h2-console/}, {@code /v3/api-docs/},
 * {@code /swagger-ui/} e {@code /ws/} são excluídos da validação JWT.
 * Note-se que {@code /api/podcasts/} foi <b>removido</b> dos públicos — apenas
 * os pedidos GET são públicos, configurado via {@link SecurityConfig}.
 *
 * @see JwtUtil
 * @see SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lista de prefixos de caminhos URI que não requerem autenticação JWT.
     * Pedidos cujo URI começa com um destes prefixos, ou que contenham {@code /audio},
     * são ignorados por este filtro e passados diretamente à cadeia seguinte.
     */
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

    /**
     * Determina se este filtro deve ser ignorado para o pedido atual.
     *
     * <p>Retorna {@code true} (não filtra) se o URI do pedido começar com algum dos
     * {@code PUBLIC_PATHS} ou contiver o segmento {@code /audio}, permitindo streaming
     * de áudio sem autenticação.
     *
     * @param request pedido HTTP atual.
     * @return {@code true} se o filtro deve ser ignorado, {@code false} caso contrário.
     */
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

    /**
     * Constrói a lista de autoridades Spring Security para o utilizador identificado pelo email.
     *
     * <p>Consulta a base de dados para verificar o tipo de utilizador:
     * <ul>
     *   <li>Todos os utilizadores recebem {@code ROLE_USER}.</li>
     *   <li>Utilizadores {@code USERADMIN} recebem adicionalmente {@code ROLE_USER_ADMIN}
     *       e {@code ROLE_USERADMIN}, necessárias para os endpoints de administração.</li>
     * </ul>
     *
     * @param userEmail email do utilizador extraído do token JWT.
     * @return lista de {@link SimpleGrantedAuthority} atribuídas ao utilizador.
     */
    private List<SimpleGrantedAuthority> buildAuthorities(String userEmail) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        userRepository.findByEmail(userEmail)
                .filter(user -> user.getUserType() == User.UserType.USERADMIN)
                .ifPresent(user -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_USERADMIN"));
                });

        return authorities;
    }

    /**
     * Lógica principal do filtro: extrai, valida e processa o token JWT do cabeçalho
     * {@code Authorization} do pedido HTTP.
     *
     * <p>Passos detalhados:
     * <ol>
     *   <li>Lê o cabeçalho {@code Authorization}; se ausente ou sem prefixo {@code Bearer },
     *       delega para o filtro seguinte sem autenticar.</li>
     *   <li>Extrai o JWT (substring após "Bearer ").</li>
     *   <li>Extrai o email do token via {@link JwtUtil#extractEmail(String)}.</li>
     *   <li>Se o email não for nulo e ainda não existir autenticação no contexto:
     *     <ul>
     *       <li>Valida o token com {@link JwtUtil#isTokenValid(String, String)}.</li>
     *       <li>Se válido, cria {@link UsernamePasswordAuthenticationToken} com o email,
     *           credenciais {@code null} (stateless) e as autoridades calculadas.</li>
     *       <li>Associa os detalhes do pedido HTTP ao token de autenticação.</li>
     *       <li>Regista a autenticação no {@link SecurityContextHolder}.</li>
     *     </ul>
     *   </li>
     *   <li>Exceções de validação JWT (expirado, malformado, assinatura inválida) são
     *       capturadas silenciosamente — o pedido prossegue como anónimo.</li>
     *   <li>Delega para {@code filterChain.doFilter()} em todos os casos.</li>
     * </ol>
     *
     * @param request     pedido HTTP recebido.
     * @param response    resposta HTTP a ser enviada.
     * @param filterChain cadeia de filtros Spring Security.
     * @throws ServletException se ocorrer erro no processamento do servlet.
     * @throws IOException      se ocorrer erro de I/O durante o filtro.
     */
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
                            buildAuthorities(userEmail)
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
