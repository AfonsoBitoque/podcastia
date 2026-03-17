package com.jep.servidor.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Serviço responsável pelo upload, validação e redimensionamento de imagens
 * de perfil.
 */
@Service
public class ProfileImageService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg", "image/png"
  );

  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB

  private final Path uploadDirectory;
  private final int maxWidth;
  private final int maxHeight;

  /**
   * Construtor com injeção de configurações de propriedades.
   */
  public ProfileImageService(
      @Value("${app.profile-images.directory}") String uploadDir,
      @Value("${app.profile-images.max-width}") int maxWidth,
      @Value("${app.profile-images.max-height}") int maxHeight) {
    this.uploadDirectory = Paths.get(uploadDir);
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
  }

  /**
   * Valida o ficheiro para garantir que é JPG/PNG e tem no máximo 5MB.
   */
  public void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("O ficheiro é obrigatório.");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "O ficheiro excede o tamanho máximo de 5MB.");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new IllegalArgumentException(
          "Apenas ficheiros JPG e PNG são permitidos.");
    }
  }

  /**
   * Processa o ficheiro submetido: redimensiona se necessário e guarda em disco.
   * Retorna o path do ficheiro guardado.
   */
  public String store(MultipartFile file) throws IOException {
    Files.createDirectories(uploadDirectory);

    BufferedImage originalImage = ImageIO.read(file.getInputStream());
    if (originalImage == null) {
      throw new IllegalArgumentException("O ficheiro não é uma imagem válida.");
    }

    BufferedImage processedImage = resizeIfNeeded(originalImage);

    String extension = getExtension(file.getContentType());
    String filename = UUID.randomUUID().toString() + "." + extension;
    Path destinationFilePath = uploadDirectory.resolve(filename);

    ImageIO.write(processedImage, extension, destinationFilePath.toFile());

    return destinationFilePath.toString().replace("\\", "/");
  }

  /**
   * Remove a imagem antiga do servidor, para não acumular lixo.
   */
  public void deleteOldImage(String path) {
    if (path == null || path.isBlank()) {
      return;
    }
    try {
      Files.deleteIfExists(Paths.get(path));
    } catch (IOException e) {
      // Falha a apagar não deve interromper o fluxo principal
    }
  }

  /**
   * Redimensiona mantendo a proporção (aspect ratio) caso a imagem exceda 
   * as dimensões máximas configuradas.
   */
  private BufferedImage resizeIfNeeded(BufferedImage originalImage) {
    int currentWidth = originalImage.getWidth();
    int currentHeight = originalImage.getHeight();

    // Se estiver dentro dos limites, não alterar
    if (currentWidth <= maxWidth && currentHeight <= maxHeight) {
      return originalImage;
    }

    double ratio = Math.min(
        (double) maxWidth / currentWidth,
        (double) maxHeight / currentHeight
    );

    int newWidth = (int) (currentWidth * ratio);
    int newHeight = (int) (currentHeight * ratio);

    BufferedImage resizedImage = new BufferedImage(
        newWidth, newHeight, 
        originalImage.getType() != 0 ? originalImage.getType() : BufferedImage.TYPE_INT_ARGB
    );

    Graphics2D g2d = resizedImage.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                         RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
    g2d.dispose();

    return resizedImage;
  }

  private String getExtension(String contentType) {
    if ("image/png".equals(contentType)) {
      return "png";
    }
    // Default fallback para jpeg uma vez que apenas deixamos entrar jpg e png.
    return "jpg";
  }
}
