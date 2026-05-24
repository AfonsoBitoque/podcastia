# C4 Nível 4 — Camada de Segurança

> Gerado a partir da leitura directa do código-fonte em `com.jep.servidor.config`.  
> Representa o fluxo JWT, filtros de autenticação, configuração de segurança e WebSocket.

---

## Diagrama de Classes — Segurança

```mermaid
classDiagram
    direction LR

    class SecurityConfig {
        <<@Configuration>>
        <<@EnableWebSecurity>>
        <<@EnableMethodSecurity>>
        -JwtAuthenticationFilter jwtAuthFilter
        +securityFilterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +corsConfigurationSource() CorsConfigurationSource
    }

    class JwtAuthenticationFilter {
        <<@Component>>
        <<OncePerRequestFilter>>
        -JwtUtil jwtUtil
        -UserDetailsService userDetailsService
        +doFilterInternal(request, response, filterChain)
        -extractTokenFromHeader(request) String
    }

    class JwtUtil {
        <<@Component>>
        -String SECRET_STRING
        -Key SECRET_KEY
        -long EXPIRATION_TIME = 86400000
        +generateToken(user) String
        +extractEmail(token) String
        +extractClaim(token, resolver) T
        +isTokenValid(token, userEmail) boolean
        -extractAllClaims(token) Claims
        -isTokenExpired(token) boolean
    }

    class JwtWebSocketHandshakeInterceptor {
        <<@Component>>
        -JwtUtil jwtUtil
        -UserRepository userRepository
        +beforeHandshake(request, response, wsHandler, attributes) boolean
        +afterHandshake(request, response, wsHandler, ex)
    }

    class JwtWebSocketHandshakeHandler {
        <<@Component>>
        +determineUser(request, wsHandler, attributes) Principal
    }

    class WebSocketConfig {
        <<@Configuration>>
        <<@EnableWebSocketMessageBroker>>
        -JwtWebSocketHandshakeInterceptor interceptor
        -JwtWebSocketHandshakeHandler handshakeHandler
        +configureMessageBroker(registry)
        +registerStompEndpoints(registry)
    }

    class OpenApiConfig {
        <<@Configuration>>
        +customOpenAPI() OpenAPI
    }

    class StaticResourceConfig {
        <<@Configuration>>
        +addResourceHandlers(registry)
    }

    class DailyPlaylistScheduler {
        <<@Component>>
        <<@EnableScheduling>>
        -DailyPlaylistService dailyPlaylistService
        +regenerateDailyPlaylistsAtMidnight()
    }

    class DataSeeder {
        <<@Component>>
        <<ApplicationRunner>>
        -RssSourceRepository rssSourceRepository
        +run(args)
    }

    %% ─── Relações ───
    SecurityConfig --> JwtAuthenticationFilter : configura
    SecurityConfig ..> PasswordEncoder : produz bean
    SecurityConfig ..> CorsConfigurationSource : produz bean
    JwtAuthenticationFilter --> JwtUtil : valida tokens
    JwtWebSocketHandshakeInterceptor --> JwtUtil : valida token WS
    WebSocketConfig --> JwtWebSocketHandshakeInterceptor : usa
    WebSocketConfig --> JwtWebSocketHandshakeHandler : usa
```

---

## Fluxo de Autenticação JWT — Pedido REST

```mermaid
flowchart TD
    A([HTTP Request]) --> B{Tem header\nAuthorization: Bearer ?}
    B -- Não --> C[Segue sem autenticação]
    C --> D{Endpoint público?}
    D -- Sim --> E([200 OK / Recurso])
    D -- Não --> F([403 Forbidden])

    B -- Sim --> G[JwtAuthenticationFilter\nextractTokenFromHeader]
    G --> H[JwtUtil.extractEmail\ndo token]
    H --> I{Token válido\ne não expirado?}
    I -- Não --> J([401 Unauthorized])
    I -- Sim --> K[Carrega UserDetails\npelo email]
    K --> L[Popula SecurityContextHolder\ncom UsernamePasswordAuthenticationToken]
    L --> M{Endpoint protegido\ncom @PreAuthorize?}
    M -- Sim --> N{Utilizador tem\no papel requerido?}
    N -- Não --> O([403 Forbidden])
    N -- Sim --> P([Controller Handler])
    M -- Não --> P
```

---

## Fluxo de Autenticação WebSocket

```mermaid
flowchart TD
    A([WS Handshake\nGET /ws?token=JWT]) --> B[JwtWebSocketHandshakeInterceptor\nbeforeHandshake]
    B --> C[JwtUtil.extractEmail\ndo parâmetro token]
    C --> D{Token válido?}
    D -- Não --> E([403 — Handshake recusado])
    D -- Sim --> F[Carrega User pelo email]
    F --> G[Guarda user nos\nwebsocket session attributes]
    G --> H[JwtWebSocketHandshakeHandler\ndetermineUser → Principal]
    H --> I([Ligação STOMP estabelecida])
    I --> J[/user/queue/messages\nfila pessoal activa]
```

---

## Endpoints Públicos (sem JWT)

| Padrão | Motivo |
|---|---|
| `OPTIONS /**` | Preflight CORS |
| `/api/auth/**` | Login |
| `/users`, `/users/**`, `/api/users/**` | Registo e perfis |
| `/api/register/**` | Gerar/verificar tag |
| `/api/search/**` | Pesquisa pública |
| `GET /api/podcasts`, `GET /api/podcasts/**` | Listagem pública de podcasts |
| `GET /podcasts`, `GET /podcasts/**` | PodcastController endpoints públicos |
| `/images/**`, `/audio/**` | Recursos estáticos |
| `/ws/**` | Handshake WebSocket (auth por token na query string) |
| `/h2-console/**` | Consola H2 (apenas dev) |
| `/v3/api-docs/**`, `/swagger-ui/**` | Documentação OpenAPI |
| `/error` | Página de erro Spring |

## Estrutura do Token JWT

```mermaid
graph LR
    T["JWT Token (HS256)"] --> H["Header\nalg: HS256\ntyp: JWT"]
    T --> P["Payload\nsub: email\nid: Long\ntype: USERNORMAL|USERADMIN\niat: timestamp\nexp: iat + 24h"]
    T --> S["Signature\nHMAC-SHA256(SECRET_STRING)"]
```
