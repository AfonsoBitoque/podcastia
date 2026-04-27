package com.jep.servidor.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exceção específica do sistema de mensagens, com estado HTTP associado.
 */
public class ChatMessageException extends RuntimeException {

  private final HttpStatus status;

  public ChatMessageException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}