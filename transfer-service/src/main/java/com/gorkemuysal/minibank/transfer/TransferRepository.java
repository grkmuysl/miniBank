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

    @Query("""
            select t.id from Transfer t
            where t.status = com.gorkemuysal.minibank.transfer.TransferStatus.FUNDS_HELD
              and t.stateDeadlineAt <= :now
            order by t.stateDeadlineAt
            """)
    List<Long> findIdsPastFraudDeadline(@Param("now") Instant now, Pageable pageable);

    @Query("""
            select t.id from Transfer t
            where t.status in (com.gorkemuysal.minibank.transfer.TransferStatus.CAPTURING,
                               com.gorkemuysal.minibank.transfer.TransferStatus.RELEASING)
              and t.updatedAt < :before
            """)
    List<Long> findIdsInFlightSince(@Param("before") Instant before, Pageable pageable);
}