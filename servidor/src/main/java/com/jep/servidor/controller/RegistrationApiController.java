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
 * Controlador REST para operações relacionadas com o registo de utilizadores,
 * como verificação e geração de tags.
 */
@RestController
@RequestMapping("/api/register")
public class RegistrationApiController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Verifica se uma tag está disponível para um determinado username.
     *
     * @param username Nome de utilizador.
     * @param tag Tag a verificar.
     * @return Mensagem indicando se a tag está disponível ou ocupada.
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
     * Gera uma tag disponível para um determinado username.
     *
     * @param username Nome de utilizador.
     * @return Uma tag disponível ou mensagem de erro.
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
