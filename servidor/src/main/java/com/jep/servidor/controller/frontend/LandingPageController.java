package com.jep.servidor.controller.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para a página inicial.
 */
@Controller
public class LandingPageController {

  /**
   * Exibe a página inicial.
   *
   * @return Nome da view da página inicial.
   */
  @GetMapping("/")
  public String landing() {
    return "landing";
  }
}
