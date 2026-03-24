package com.jep.servidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação Spring Boot.
 */
@SpringBootApplication
@EnableScheduling
public class ServidorApplication {

  /**
   * Método principal para iniciar a aplicação.
   *
   * @param args Argumentos da linha de comando.
   */
  public static void main(String[] args) {
    SpringApplication.run(ServidorApplication.class, args);
  }

}
