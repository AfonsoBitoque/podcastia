package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.ProfileImageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller REST para upload, visualização e remoção de imagens de perfil dos utilizadores.
 *
 * <p>Delega a validação, redimensionamento e persistência das imagens para o
 * {@link ProfileImageService}.
 *
 * <p><b>Base path:</b> {@code /users/{userId}/profile-image}
 *
 * <p><b>Endpoints disponíveis:</b>
 * <ul>
 *   <li>{@code POST /} — upload de nova imagem de perfil (substitui a atual).</li>
 *   <li>{@code GET /} — obter a imagem de perfil atual (ou a imagem por defeito).</li>
 *   <li>{@code DELETE /} — remover a imagem de perfil atual.</li>
 * </ul>
 *
 * <p><b>Imagem por defeito:</b> Se um utilizador não tiver imagem de perfil definida
 * (ou se o ficheiro não existir), o endpoint GET devolve a imagem estática de classpath
 * {@code static/images/profile_picture.png} com {@code Content-Type: image/png}.
 *
 * @see ProfileImageService
 */
@RestController
@RequestMapping("/users/{userId}/profile-image")
public class ProfileImageController {

  private static final String DEFAULT_PROFILE_IMAGE = "static/images/profile_picture.png";

  private final UserRepository userRepository;
  private final ProfileImageService profileImageService;

  /**
   * Cria o controller com as dependências necessárias.
   *
   * @param userRepository      repositório JPA para carregar e atualizar o utilizador.
   * @param profileImageService serviço com lógica de validação, armazenamento e eliminação.
   */
  public ProfileImageController(UserRepository userRepository, 
                                ProfileImageService profileImageService) {
    this.userRepository = userRepository;
    this.profileImageService = profileImageService;
  }

  /**
   * Faz upload de uma nova imagem de perfil e associa-a ao utilizador indicado.
   *
   * <p>Fluxo:
   * <ol>
   *   <li>Valida o ficheiro via {@link ProfileImageService#validate} (formato JPG/PNG, tamanho ≤ 5 MB).</li>
   *   <li>Armazena o novo ficheiro via {@link ProfileImageService#store}.</li>
   *   <li>Atualiza o campo {@code profilePicturePath} do utilizador na BD.</li>
   *   <li>Elimina a imagem anterior via {@link ProfileImageService#deleteOldImage} (apenas após
   *       guardar com sucesso o novo caminho, evitando perda de dados em caso de falha).</li>
   * </ol>
   *
   * @param userId ID do utilizador.
   * @param file   ficheiro de imagem a carregar (multipart/form-data, campo {@code "file"}).
   * @return {@code 200 OK} com o novo caminho da imagem;
   *         {@code 400 Bad Request} se o ficheiro for inválido;
   *         {@code 404 Not Found} se o utilizador não existir;
   *         {@code 500 Internal Server Error} em caso de erro de I/O.
   */
  @PostMapping
  public ResponseEntity<?> uploadProfileImage(
      @PathVariable Long userId,
      @RequestParam("file") MultipartFile file) {

    Optional<User> userOptional = userRepository.findById(userId);
    if (userOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Utilizador não encontrado.");
    }
    User user = userOptional.get();

    try {
      profileImageService.validate(file);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    try {
      String oldImagePath = user.getProfilePicturePath();
      String newImagePath = profileImageService.store(file);

      user.setProfilePicturePath(newImagePath);
      userRepository.save(user);

      // Apagar a imagem velha só depois de salvar com sucesso a nova path
      profileImageService.deleteOldImage(oldImagePath);

      return ResponseEntity.ok(newImagePath);

    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Ocorreu um erro ao guardar a imagem: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
  }

  /**
   * Retorna a imagem de perfil do utilizador.
   *
   * <p>Se o utilizador não tiver imagem de perfil definida, ou se o ficheiro não existir
   * ou não for legível, devolve a imagem por defeito via {@link #defaultProfileImageResponse()}.
   * O {@code Content-Type} é detetado automaticamente pelo sistema operativo via
   * {@code Files.probeContentType}; se indisponível, usa {@code application/octet-stream}.
   *
   * @param userId ID do utilizador.
   * @return {@code 200 OK} com o recurso de imagem e o {@code Content-Type} correto;
   *         {@code 404 Not Found} se o utilizador não existir;
   *         fallback para imagem por defeito em caso de erro.
   */
  @GetMapping
  public ResponseEntity<Resource> getProfileImage(@PathVariable Long userId) {
    Optional<User> userOptional = userRepository.findById(userId);

    if (userOptional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    User user = userOptional.get();
    if (user.getProfilePicturePath() == null || user.getProfilePicturePath().isBlank()) {
      return defaultProfileImageResponse();
    }

    try {
      Path imagePath = Paths.get(user.getProfilePicturePath());
      Resource resource = new UrlResource(imagePath.toUri());

      if (resource.exists() || resource.isReadable()) {
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
          contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
      } else {
        return defaultProfileImageResponse();
      }
    } catch (IOException e) {
      return defaultProfileImageResponse();
    }
  }

  /**
   * Constrói a resposta com a imagem de perfil por defeito do classpath.
   *
   * <p>Carrega {@code static/images/profile_picture.png} a partir do classpath.
   * Retorna {@code 500 Internal Server Error} se o recurso não existir.
   *
   * @return resposta com a imagem PNG por defeito, ou 500 se não encontrada.
   */
  private ResponseEntity<Resource> defaultProfileImageResponse() {
    Resource resource = new ClassPathResource(DEFAULT_PROFILE_IMAGE);

    if (!resource.exists()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(resource);
  }

  /**
   * Remove a imagem de perfil atual do utilizador.
   *
   * <p>Limpa o campo {@code profilePicturePath} na BD e elimina o ficheiro físico
   * via {@link ProfileImageService#deleteOldImage}. Se o utilizador não tiver
   * imagem definida, retorna {@code 204 No Content} sem fazer nada.
   *
   * @param userId ID do utilizador.
   * @return {@code 204 No Content} em caso de sucesso ou se não havia imagem;
   *         {@code 404 Not Found} se o utilizador não existir.
   */
  @DeleteMapping
  public ResponseEntity<?> deleteProfileImage(@PathVariable Long userId) {
    Optional<User> userOptional = userRepository.findById(userId);

    if (userOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Utilizador não encontrado.");
    }

    User user = userOptional.get();
    String currentImagePath = user.getProfilePicturePath();

    if (currentImagePath == null || currentImagePath.isBlank()) {
      return ResponseEntity.noContent().build();
    }

    user.setProfilePicturePath(null);
    userRepository.save(user);
    profileImageService.deleteOldImage(currentImagePath);

    return ResponseEntity.noContent().build();
  }
}
