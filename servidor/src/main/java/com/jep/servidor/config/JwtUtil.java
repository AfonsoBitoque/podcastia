package com.jep.servidor.config;

import com.jep.servidor.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Utilitário central para geração, validação e extração de dados de tokens JWT
 * (JSON Web Tokens) usados na autenticação stateless da aplicação Podcastia.
 *
 * <p>Utiliza a biblioteca JJWT (io.jsonwebtoken) com o algoritmo HMAC-SHA256 (HS256)
 * para assinar e verificar os tokens. A chave é derivada diretamente de uma string
 * constante via {@link Keys#hmacShaKeyFor(byte[])}.
 *
 * <p><b>Estrutura do token gerado:</b>
 * <ul>
 *   <li>{@code sub} (subject) — email do utilizador.</li>
 *   <li>{@code id} (claim personalizado) — ID Long do utilizador na base de dados.</li>
 *   <li>{@code type} (claim personalizado) — tipo de utilizador (nome do enum
 *       {@link User.UserType}: {@code "USER"} ou {@code "USERADMIN"}).</li>
 *   <li>{@code iat} (issued at) — timestamp de emissão.</li>
 *   <li>{@code exp} (expiration) — timestamp de expiração (24 horas após emissão).</li>
 * </ul>
 *
 * <p><b>Chave de assinatura:</b> A constante {@code SECRET_STRING} é uma string fixa
 * com mais de 256 bits, usada para garantir consistência entre reinícios do servidor.
 * <b>⚠ Em produção, esta chave deve ser externalizada para uma variável de ambiente
 * ou gestor de segredos</b> (ex: AWS Secrets Manager, HashiCorp Vault).
 *
 * <p><b>Expiração:</b> Os tokens expiram ao fim de 24 horas ({@code 86400000} ms).
 * Não existe mecanismo de refresh token; após expiração o utilizador deve fazer novo login.
 *
 * @see JwtAuthenticationFilter
 * @see JwtWebSocketHandshakeInterceptor
 */
@Component
public class JwtUtil {

    /**
     * String base usada para derivar a chave HMAC-SHA256.
     * Deve ter pelo menos 256 bits (32 caracteres) para ser válida com HS256.
     *
     * <p><b>⚠ Aviso de segurança:</b> valor fixo no código-fonte. Em produção
     * deve ser substituído por um valor proveniente de variável de ambiente.
     */
    // Em produção, esta chave deve vir de variáveis de ambiente
    // Usando uma chave fixa para consistência entre reinícios do servidor
    private static final String SECRET_STRING = "podcastia-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long";

    /**
     * Chave HMAC-SHA256 derivada de {@code SECRET_STRING}, instanciada uma única vez
     * de forma estática para reutilização eficiente em todas as operações de sign/verify.
     */
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    /**
     * Tempo de vida dos tokens JWT em milissegundos.
     * Valor atual: {@code 86400000} ms = 24 horas.
     */
    private static final long EXPIRATION_TIME = 86400000; // 24 horas

    /**
     * Gera um token JWT assinado para o utilizador fornecido.
     *
     * <p>O token contém o email como subject e os claims personalizados {@code id}
     * e {@code type}. É válido por {@link #EXPIRATION_TIME} ms a partir da emissão.
     *
     * @param user utilizador para o qual o token é gerado; não deve ser {@code null}.
     * @return string JWT compacta assinada, pronta para usar no cabeçalho
     *         {@code Authorization: Bearer <token>}.
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("type", user.getUserType().name())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Extrai o email do utilizador (claim {@code sub} / subject) de um token JWT.
     *
     * @param token string JWT válida e assinada.
     * @return email do utilizador presente no subject do token.
     * @throws io.jsonwebtoken.JwtException se o token for inválido, expirado ou malformado.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai um claim específico do token JWT usando uma função de mapeamento.
     *
     * <p>Método genérico que parseia todos os claims do token e aplica o
     * {@code claimsResolver} para devolver o claim pretendido no tipo correto.
     *
     * @param <T>            tipo do valor do claim a extrair.
     * @param token          string JWT válida e assinada.
     * @param claimsResolver função que mapeia {@link Claims} para o tipo {@code T} desejado
     *                       (ex: {@code Claims::getSubject}, {@code Claims::getExpiration}).
     * @return valor do claim extraído e convertido para o tipo {@code T}.
     * @throws io.jsonwebtoken.JwtException se o token for inválido ou expirado.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parseia o token JWT e retorna todos os seus claims (payload).
     *
     * <p>Usa {@link Jwts#parserBuilder()} com a {@link #SECRET_KEY} para verificar
     * a assinatura antes de descodificar o payload. Lança exceção se a assinatura
     * for inválida ou o token estiver expirado.
     *
     * @param token string JWT a parsear.
     * @return objeto {@link Claims} com todos os claims do payload.
     * @throws io.jsonwebtoken.JwtException se o token for inválido, expirado,
     *         malformado ou a assinatura não corresponder.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Verifica se um token JWT é válido para o email de utilizador fornecido.
     *
     * <p>A validação combina dois critérios:
     * <ol>
     *   <li>O email extraído do token deve ser igual a {@code userEmail}.</li>
     *   <li>O token não deve estar expirado.</li>
     * </ol>
     *
     * @param token     string JWT a validar.
     * @param userEmail email do utilizador esperado no token.
     * @return {@code true} se o token for válido e não expirado; {@code false} caso contrário.
     * @throws io.jsonwebtoken.JwtException se o token for malformado ou a assinatura inválida.
     */
    public boolean isTokenValid(String token, String userEmail) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(userEmail) && !isTokenExpired(token));
    }

    /**
     * Verifica se o token JWT está expirado, comparando a data de expiração com a data atual.
     *
     * @param token string JWT a verificar.
     * @return {@code true} se o token já expirou; {@code false} se ainda é válido.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
