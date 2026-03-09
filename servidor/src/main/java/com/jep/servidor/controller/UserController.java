package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
