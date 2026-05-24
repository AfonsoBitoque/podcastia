package com.jep.servidor.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção de negócio genérica lançada quando uma operação viola
 * uma regra de negócio da aplicação.
 *
 * <p>A anotação {@code @ResponseStatus(HttpStatus.BAD_REQUEST)} garante
 * que, quando não intercetada por um handler específico, o Spring
 * devolve automaticamente um {@code 400 Bad Request} ao cliente.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {
    /**
     * Cria a exceção com a mensagem descritiva da violação de negócio.
     *
     * @param message descrição da regra de negócio violada.
     */
    public BusinessException(String message) {
        super(message);
    }
}
