package com.taekwondogym.backend.repository;

import com.taekwondogym.backend.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
	List<UserActivityLog> findByEmail(String email);
    Page<UserActivityLog> findByEmail(String email, Pageable pageable);
    Page<UserActivityLog> findByTimestampBetween(ZonedDateTime start, ZonedDateTime end, Pageable pageable);
    List<UserActivityLog> findByEmailAndTimestampBetween(String email, ZonedDateTime start, ZonedDateTime end);
    Page<UserActivityLog> findByAction(String action, Pageable pageable);
}
