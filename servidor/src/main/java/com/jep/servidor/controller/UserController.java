package com.jep.servidor.controller;

import com.jep.servidor.dto.ChangePasswordRequest;
import com.jep.servidor.dto.UserUpdateRequest;
import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
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
 * Controller REST para gestão de utilizadores — CRUD, perfil público e alteração de password.
 *
 * <p>Este controller usa o base path {@code /users} (sem prefixo {@code /api}),
 * e inclui operações de leitura públicas e de escrita que requerem autenticação JWT.
 *
 * <p><b>Base path:</b> {@code /users}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code GET /} — lista todos os utilizadores (público).</li>
 *   <li>{@code GET /{id}/profile} — perfil público de um utilizador.</li>
 *   <li>{@code POST /} — cria um novo utilizador (registo).</li>
 *   <li>{@code PATCH /{id}} — atualiza metadados (username, bio, playbackSpeed).</li>
 *   <li>{@code PUT /{id}/password} — altera a password após verificação da atual.</li>
 * </ul>
 *
 * <p><b>Nota:</b> O campo {@code tag} é imutável após o registo. A alteração de email
 * e password seguem fluxos separados.
 *
 * @see com.jep.servidor.dto.UserUpdateRequest
 * @see com.jep.servidor.dto.ChangePasswordRequest
 * @see com.jep.servidor.dto.UserProfileDto
 */
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param userRepository  repositório JPA de utilizadores.
   * @param passwordEncoder codificador BCrypt para hashing e verificação de passwords.
   */
  public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Lista todos os utilizadores registados na plataforma.
   *
   * <p><b>Aviso:</b> Este endpoint não tem paginação e devolve todos os registos.
   * Para grandes volumes de dados, deverá ser substituído por um endpoint paginado.
   *
   * @return lista de todos os utilizadores.
   */
  @GetMapping
  public List<User> all() {
    return userRepository.findAll();
  }

  /**
   * Retorna o perfil público de um utilizador.
   *
   * <p>Exposta como um DTO {@link com.jep.servidor.dto.UserProfileDto} que inclui
   * username, tag, bio, imagem de perfil, pontos de afinidade por categoria,
   * timestamps e tópicos de interesse, mas omite email e password.
   *
   * @param id ID do utilizador.
   * @return {@code 200 OK} com {@link com.jep.servidor.dto.UserProfileDto};
   *         {@code 404 Not Found} se o utilizador não existir.
   */
  @GetMapping("/{id}/profile")
  public ResponseEntity<?> getProfile(@PathVariable Long id) {
    Optional<User> optionalUser = userRepository.findById(id);
    if (optionalUser.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Utilizador não encontrado no sistema."));
    }
    User user = optionalUser.get();
    com.jep.servidor.dto.UserProfileDto profile = new com.jep.servidor.dto.UserProfileDto(
      user.getId(),
      user.getUsername(),
      user.getTag(),
      user.getBio(),
      user.getProfilePicturePath(),
      user.getPontosDesporto(),
      user.getPontosPolitica(),
      user.getPontosFinancas(),
      user.getPontosGeral(),
      user.getCreatedAt(),
      user.getLastActiveAt(),
      user.getTopics()
    );
    return ResponseEntity.ok(profile);
  }

  /**
   * Regista um novo utilizador na plataforma.
   *
   * <p>Valida unicidade de email e do par (username, tag) antes de guardar.
   * A password é codificada com BCrypt antes da persistência.
   *
   * @param user entidade {@link User} com os dados de registo ({@code email}, {@code username},
   *             {@code tag}, {@code password}, etc.).
   * @return {@code 201 Created} com o utilizador criado;
   *         {@code 409 Conflict} com {@code "email-already-exists"} se o email já estiver em uso;
   *         {@code 409 Conflict} com {@code "username+tag-already-exists"} se o par já existir.
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
   * Atualiza parcialmente os metadados editaveis de um utilizador.
   *
   * <p>Campos atualizáveis via {@link com.jep.servidor.dto.UserUpdateRequest}:
   * <ul>
   *   <li>{@code username} — verificado contra colisão com a tag preexistente do utilizador.</li>
   *   <li>{@code bio} — texto de apresentação.</li>
   *   <li>{@code playbackSpeed} — velocidade de reprodução preferida.</li>
   * </ul>
   *
   * <p>A {@code tag} é sempre imutável. Alterações de email e password devem usar
   * endpoints dedicados.
   *
   * @param id     ID do utilizador a atualizar.
   * @param update payload com os campos a alterar.
   * @return {@code 200 OK} com o utilizador atualizado;
   *         {@code 404 Not Found} se não existir;
   *         {@code 409 Conflict} com {@code "username+tag-already-exists"} em caso de colisão.
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

    if (update.getPlaybackSpeed() != null) {
       user.setPlaybackSpeed(update.getPlaybackSpeed());
    }

    // Nenhuma redefinição de password ou e-mail deve ocorrer aqui (estas funcionalidades precisam de outros fluxos).
    // Salvamos na Base de Dados e injetamos o estado final atualizado.
    User saved = userRepository.save(user);

    return ResponseEntity.ok(saved);
  }

  /**
   * Altera a password de um utilizador, exigindo confirmação da password atual.
   *
   * <p>Fluxo de segurança:
   * <ol>
   *   <li>Verifica se o utilizador existe.</li>
   *   <li>Valida a {@code currentPassword} contra o hash BCrypt armazenado.</li>
   *   <li>Codifica e persiste a {@code newPassword}.</li>
   * </ol>
   *
   * @param userId  ID do utilizador.
   * @param request DTO com {@code currentPassword} e {@code newPassword}.
   * @return {@code 200 OK} com mensagem de sucesso;
   *         {@code 401 Unauthorized} se a password atual não coincidir;
   *         {@code 404 Not Found} se o utilizador não existir.
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
