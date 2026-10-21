package com.sovereign.sentinel.repository;

import com.sovereign.sentinel.model.HeartbeatRecord;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeartbeatRepository extends JpaRepository<HeartbeatRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from HeartbeatRecord record where record.id = :id")
    Optional<HeartbeatRecord> findLocked(@Param("id") Long id);
}
