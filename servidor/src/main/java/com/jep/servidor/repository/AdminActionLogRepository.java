package com.jep.servidor.repository;

import com.jep.servidor.model.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AdminActionLog entities
 */
@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    
    /**
     * Find all admin logs ordered by timestamp (most recent first)
     */
    List<AdminActionLog> findAllByOrderByTimestampDesc();
    
    /**
     * Find admin logs by admin username
     */
    List<AdminActionLog> findByAdminUsernameOrderByTimestampDesc(String adminUsername);
    
    /**
     * Find admin logs by action type
     */
    List<AdminActionLog> findByActionOrderByTimestampDesc(String action);
    
    /**
     * Find admin logs by target type
     */
    List<AdminActionLog> findByTargetTypeOrderByTimestampDesc(String targetType);
    
    /**
     * Find admin logs by target ID
     */
    List<AdminActionLog> findByTargetIdOrderByTimestampDesc(Long targetId);
    
    /**
     * Find admin logs by successful status
     */
    List<AdminActionLog> findBySuccessfulOrderByTimestampDesc(boolean successful);
    
    /**
     * Find admin logs by date range
     */
    List<AdminActionLog> findByTimestampBetweenOrderByTimestampDesc(
        java.time.LocalDateTime startDate, 
        java.time.LocalDateTime endDate
    );
}
