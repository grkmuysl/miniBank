package com.gorkemuysal.minibank.transfer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByInitiatorIdAndIdempotencyKey(String initiatorId, String idempotencyKey);

    Optional<Transfer> findByIdAndInitiatorId(Long id, String initiatorId);

    List<Transfer> findAllByInitiatorIdOrderByCreatedAtDesc(String initiatorId);

    /**
     * IDs of PENDING transfers whose next attempt is due, oldest first.
     * The status is written as a literal (not a parameter) on purpose: it lets PostgreSQL
     * match the partial index "WHERE status = 'PENDING'" even with cached query plans.
     */
    @Query("""
            select t.id from Transfer t
            where t.status = com.gorkemuysal.minibank.transfer.TransferStatus.PENDING
              and t.nextAttemptAt <= :now
            order by t.nextAttemptAt
            """)
    List<Long> findIdsDueForRetry(@Param("now") Instant now, Pageable pageable);
}