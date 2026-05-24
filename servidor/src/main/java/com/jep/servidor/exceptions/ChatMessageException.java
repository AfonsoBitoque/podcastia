package com.jep.servidor.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exceção específica do sistema de mensagens de chat, que transporta
 * o código HTTP a devolver ao cliente.
 *
 * <p>Ao contrário de {@link BusinessException}, esta exceção não usa
 * {@code @ResponseStatus} estático — o código HTTP é dinâmico e
 * definido no momento da construção, permitindo lançar erros como
 * {@code 429 Too Many Requests} (rate-limit), {@code 403 Forbidden}
 * (link bloqueado), etc.
 */
public class ChatMessageException extends RuntimeException {

  private final HttpStatus status;

  /**
   * Cria a exceção com o estado HTTP e mensagem descritivos.
   *
   * @param status  código HTTP a devolver (ex: {@link HttpStatus#TOO_MANY_REQUESTS}).
   * @param message descrição do erro.
   */
  public ChatMessageException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  /**
   * Retorna o código HTTP associado a esta exceção.
   *
   * @return {@link HttpStatus} a usar na resposta HTTP.
   */
  public HttpStatus getStatus() {
    return status;
  }
}