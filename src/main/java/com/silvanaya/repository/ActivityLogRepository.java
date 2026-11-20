package com.silvanaya.repository;

import com.silvanaya.model.*;
import com.silvanaya.model.Enums.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /* ---------- Zilizopo (hazijabadilika) ---------- */
    List<ActivityLog> findTop10ByOrderByIdDesc();
    List<ActivityLog> findTop10ByUserIdOrderByIdDesc(Long userId);
    List<ActivityLog> findTop200ByOrderByIdDesc();
    List<ActivityLog> findTop200ByUserIdOrderByIdDesc(Long userId);
    long countByUserId(Long userId);

    @Query("select a from ActivityLog a where not exists "
            + "(select u.id from User u where u.id = a.userId and u.role = :role) "
            + "order by a.id desc")
    List<ActivityLog> latestExcludingRole(@Param("role") Role role, Pageable pageable);

    @Query("select count(a) from ActivityLog a where not exists "
            + "(select u.id from User u where u.id = a.userId and u.role = :role)")
    long countExcludingRole(@Param("role") Role role);
}