package com.jep.servidor.controller;

import com.jep.servidor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST de suporte ao registo de novos utilizadores — verificação e geração de tags.
 *
 * <p>Na plataforma Podcastia, cada utilizador é identificado de forma única pelo par
 * {@code (username, tag)}, onde a tag é um código numérico de 4 dígitos (0000–9999).
 * Dois utilizadores podem ter o mesmo {@code username} desde que as tags sejam diferentes.
 *
 * <p>Este controller é usado pelo frontend durante o fluxo de registo para:
 * <ol>
 *   <li>Verificar se uma tag específica está disponível para o username pretendido.</li>
 *   <li>Gerar automaticamente a primeira tag disponível para um dado username.</li>
 * </ol>
 *
 * <p><b>Base path:</b> {@code /api/register} (público, sem autenticação JWT)
 *
 * @see com.jep.servidor.repository.UserRepository#existsByUsernameAndTag
 */
@RestController
@RequestMapping("/api/register")
public class RegistrationApiController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Verifica se uma tag específica está disponível para um dado username.
     *
     * <p>A tag deve ter exatamente 4 caracteres; caso contrário devolve {@code 400 Bad Request}.
     *
     * @param username nome de utilizador a verificar.
     * @param tag      tag de 4 dígitos a verificar (ex: {@code "0042"}).
     * @return {@code 200 OK} com {@code "Tag disponível"} ou {@code "Tag ocupada"};
     *         {@code 400 Bad Request} se a tag não tiver exatamente 4 caracteres.
     */
    @GetMapping("/check-tag")
    public ResponseEntity<String> checkTag(@RequestParam("username") String username,
                                           @RequestParam("tag") String tag) {
        if (tag.length() != 4) {
            return ResponseEntity.badRequest().body("Tag inválida");
        }
        boolean exists = userRepository.existsByUsernameAndTag(username, tag);
        return ResponseEntity.ok(exists ? "Tag ocupada" : "Tag disponível");
    }

    /**
     * Gera automaticamente a primeira tag disponível para o username indicado.
     *
     * <p>Itera sequencialmente de {@code 0000} a {@code 9999} e devolve a primeira tag
     * que não esteja em uso para o username fornecido. Se todas as 10 000 tags estiverem
     * ocupadas (cenário extremamente improvavável), devolve {@code 404 Not Found}.
     *
     * @param username nome de utilizador para o qual gerar a tag.
     * @return {@code 200 OK} com a tag gerada (ex: {@code "0007"});
     *         {@code 400 Bad Request} se o username for nulo ou vazio;
     *         {@code 404 Not Found} se não houver tags disponíveis.
     */
    @GetMapping("/generate-tag")
    public ResponseEntity<String> generateTag(@RequestParam("username") String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nome de utilizador é obrigatório para gerar tag.");
        }
        for (int i = 0; i <= 9999; i++) {
            String tag = String.format("%04d", i);
            if (!userRepository.existsByUsernameAndTag(username, tag)) {
                return ResponseEntity.ok(tag);
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nenhuma tag disponível");
    }
}
