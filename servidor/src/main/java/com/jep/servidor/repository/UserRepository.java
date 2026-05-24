package com.jep.servidor.repository;

import com.jep.servidor.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Repositório Spring Data JPA para a entidade {@link User}.
 *
 * <p>Fornece consultas de autenticação, pesquisa, registo e analytics
 * administrativas para o sistema Podcastia.
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Verifica se já existe um utilizador com o email fornecido.
   *
   * @param email email a verificar.
   * @return {@code true} se o email já estiver registado.
   */
  boolean existsByEmail(String email);

  /**
   * Verifica se a combinação (username, tag) já está em uso.
   * Usado pelo {@link com.jep.servidor.controller.RegistrationApiController}.
   *
   * @param username username a verificar.
   * @param tag      tag de 4 dígitos a verificar.
   * @return {@code true} se o par (username, tag) já existir.
   */
  boolean existsByUsernameAndTag(String username, String tag);

  /**
   * Encontra utilizador pelo email (usado na autenticação JWT).
   *
   * @param email email do utilizador.
   * @return utilizador se encontrado, ou {@link java.util.Optional#empty()}.
   */
  Optional<User> findByEmail(String email);

  /**
   * Encontra utilizador pelo username (pesquisa exata).
   *
   * @param username username exato a procurar.
   * @return utilizador se encontrado, ou {@link java.util.Optional#empty()}.
   */
  Optional<User> findByUsername(String username);

  /**
   * Encontra utilizador pela combinação (username, tag).
   * Usado no login alternativo ao email.
   *
   * @param username username do utilizador.
   * @param tag      tag de 4 dígitos.
   * @return utilizador se encontrado, ou {@link java.util.Optional#empty()}.
   */
  Optional<User> findByUsernameAndTag(String username, String tag);

  /**
   * Pesquisa utilizadores por username ou email (case-insensitive).
   * Usado pelo painel de administração.
   *
   * @param username termo a pesquisar no username.
   * @param email    termo a pesquisar no email.
   * @return lista de utilizadores que correspondem a um dos termos.
   */
  List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
      String username, String email);

  /**
   * Pesquisa utilizadores por username (case-insensitive) com limite de resultados.
   * Usado pelo {@link com.jep.servidor.service.SearchService}.
   *
   * @param username termo a pesquisar.
   * @param pageable limitação de resultados.
   * @return lista paginada de utilizadores.
   */
  List<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

  /**
   * Conta o número de amizades aceites de um utilizador.
   *
   * <p><b>Nota:</b> conta registos {@code AMIGO} em ambas as direções
   * ({@code r.user.id} e {@code r.friend.id}), pelo que o resultado já
   * inclui os registos duplicados criados por amizades bidirecionais.
   * Dividir por 2 para obter o número real de amigos únicos.
   *
   * @param userId ID do utilizador.
   * @return contagem de registos {@code AMIGO} associados ao utilizador.
   */
  @Query("SELECT COUNT(r) FROM UserRelation r "
      + "WHERE (r.user.id = :userId OR r.friend.id = :userId) AND r.type = 'AMIGO'")
  long countFriendships(@RequestParam("userId") Long userId);

  /**
   * Conta utilizadores ativos após uma data (para métricas de utilizadores diários/mensais).
   *
   * @param dateTime data/hora a partir da qual contar.
   * @return número de utilizadores com {@code lastActiveAt} após a data.
   */
  long countByLastActiveAtAfter(LocalDateTime dateTime);

  /**
   * Conta utilizadores registados após uma data.
   *
   * @param dateTime data/hora de referência.
   * @return número de utilizadores criados após a data.
   */
  long countByCreatedAtAfter(LocalDateTime dateTime);

  /**
   * Conta utilizadores ativos num intervalo de datas.
   *
   * @param start início do intervalo (inclusivo).
   * @param end   fim do intervalo (exclusivo).
   * @return número de utilizadores ativos no intervalo.
   */
  long countByLastActiveAtBetween(LocalDateTime start, LocalDateTime end);

  /**
   * Conta utilizadores registados num intervalo de datas.
   *
   * @param start início do intervalo (inclusivo).
   * @param end   fim do intervalo (exclusivo).
   * @return número de utilizadores criados no intervalo.
   */
  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
