package com.jep.servidor.controller;

import com.jep.servidor.dto.ChangePasswordRequest;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.dto.UserUpdateRequest;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controlador REST para gerir utilizadores.
 */
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Construtor para injeção de dependências.
   *
   * @param userRepository  Repositório de utilizadores.
   * @param passwordEncoder Codificador de palavras-passe.
   */
  public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Retorna todos os utilizadores.
   *
   * @return Lista de utilizadores.
   */
  @GetMapping
  public List<User> all() {
    return userRepository.findAll();
  }

  /**
   * Cria um novo utilizador.
   *
   * @param user Dados do utilizador a criar.
   * @return Resposta com o utilizador criado ou erro.
   */
  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("email-already-exists");
    }
    if (userRepository.existsByUsernameAndTag(user.getUsername(), user.getTag())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("username+tag-already-exists");
    }

    // Encriptar a password antes de salvar
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    User saved = userRepository.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /**
   * Endpoint de atualização pacífica (PATCH) de metadados do utilizador.
   * A "tag" permanece sempre imutável, o DTO expõe apenas username e bio.
   *
   * @param id     ID do utilizador.
   * @param update Payload de campos atualizáveis (pode ser validado em anexo).
   * @return Utilizador modificado ou códigos de conflito 409 DB overlap.
   */
  @PatchMapping("/{id}")
  public ResponseEntity<?> updateUserMetadata(@PathVariable Long id, 
                                            @Valid @RequestBody UserUpdateRequest update) {
    Optional<User> optionalUser = userRepository.findById(id);

    if (optionalUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilizador não encontrado no sistema.");
    }

    User user = optionalUser.get();

    // Se houver uma tentativa legítima de mudar o Username, validá-lo contra colisão com a nossa "Tag" natural.
    if (update.getUsername() != null && !update.getUsername().equals(user.getUsername())) {
       // Se o username pretendido e a SUA PRÓPRIA tag preexistente colidirem no backend com outro utilizador na BD:
       if (userRepository.existsByUsernameAndTag(update.getUsername(), user.getTag())) {
           return ResponseEntity.status(HttpStatus.CONFLICT).body("username+tag-already-exists");
       }
       // Passando a verificação, atualiza ativamente o username
       user.setUsername(update.getUsername());
    }

    // A bio será sempre truncada/validada de forma invisível ou falha logo no nível de DTO
    if (update.getBio() != null) {
       user.setBio(update.getBio());
    }

    // Nenhuma redefinição de password ou e-mail deve ocorrer aqui (estas funcionalidades precisam de outros fluxos).
    // Salvamos na Base de Dados e injetamos o estado final atualizado.
    User saved = userRepository.save(user);

    return ResponseEntity.ok(saved);
  }

  /**
   * Altera a password de um utilizador.
   *
   * @param userId ID do utilizador.
   * @param request Dados da alteração de password.
   * @return Resposta de sucesso ou erro.
   */
  @PutMapping("/{userId}/password")
  public ResponseEntity<?> changePassword(
      @PathVariable Long userId,
      @Valid @RequestBody ChangePasswordRequest request) {

    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Utilizador não encontrado"));
    }

    User user = userOpt.get();

    // Verifica se a password atual coincide
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "A password atual não coincide"));
    }

    // Atualiza a password
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    return ResponseEntity.ok(Map.of("message", "Password alterada com sucesso"));
  }
}
