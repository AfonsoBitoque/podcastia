package com.jep.servidor.controller;

import com.jep.servidor.model.User;
import com.jep.servidor.repository.UserRepository;
import com.jep.servidor.service.ProfileImageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller responsável pelos uploads e transferências de imagens de perfil.
 */
@RestController
@RequestMapping("/users/{userId}/profile-image")
public class ProfileImageController {

  private final UserRepository userRepository;
  private final ProfileImageService profileImageService;

  /**
   * Construtor para injeção de dependências.
   */
  public ProfileImageController(UserRepository userRepository, 
                                ProfileImageService profileImageService) {
    this.userRepository = userRepository;
    this.profileImageService = profileImageService;
  }

  /**
   * Faz upload de uma nova imagem de perfil e associa-a ao utilizador.
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
   * Retorna a imagem de perfil atual do utilizador para visualização no frontend.
   */
  @GetMapping
  public ResponseEntity<Resource> getProfileImage(@PathVariable Long userId) {
    Optional<User> userOptional = userRepository.findById(userId);
    
    if (userOptional.isEmpty() || userOptional.get().getProfilePicturePath() == null) {
      return ResponseEntity.notFound().build();
    }

    try {
      Path imagePath = Paths.get(userOptional.get().getProfilePicturePath());
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
        return ResponseEntity.notFound().build();
      }
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
