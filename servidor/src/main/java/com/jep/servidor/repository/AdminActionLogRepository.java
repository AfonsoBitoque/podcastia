package com.jep.servidor.repository;

import com.jep.servidor.model.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade {@link AdminActionLog}.
 *
 * <p>Fornece consultas de auditoria usadas pelo
 * {@link com.jep.servidor.service.AdminService} para exibir e filtrar
 * logs no painel de administração.
 */
@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    /**
     * Devolve todos os logs ordenados do mais recente para o mais antigo.
     *
     * @return lista de todos os {@link AdminActionLog} por ordem descendente de timestamp.
     */
    List<AdminActionLog> findAllByOrderByTimestampDesc();

    /**
     * Devolve logs de um administrador específico.
     *
     * @param adminUsername username do administrador a filtrar.
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findByAdminUsernameOrderByTimestampDesc(String adminUsername);

    /**
     * Devolve logs de um tipo de ação específico.
     *
     * @param action código da ação (ex: {@code "DELETE_PODCAST"}).
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Devolve logs filtrados por tipo de alvo.
     *
     * @param targetType tipo de alvo ({@code "PODCAST"}, {@code "USER"}, {@code "SYSTEM"}).
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findByTargetTypeOrderByTimestampDesc(String targetType);

    /**
     * Devolve logs relacionados com uma entidade específica.
     *
     * @param targetId ID da entidade alvo.
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findByTargetIdOrderByTimestampDesc(Long targetId);

    /**
     * Devolve logs filtrados pelo resultado da operação.
     *
     * @param successful {@code true} para ações bem-sucedidas; {@code false} para falhadas.
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findBySuccessfulOrderByTimestampDesc(boolean successful);

    /**
     * Devolve logs dentro de um intervalo de datas.
     *
     * @param startDate data/hora de início (inclusivo).
     * @param endDate   data/hora de fim (exclusivo).
     * @return logs ordenados por timestamp descendente.
     */
    List<AdminActionLog> findByTimestampBetweenOrderByTimestampDesc(
        java.time.LocalDateTime startDate,
        java.time.LocalDateTime endDate
    );
}
