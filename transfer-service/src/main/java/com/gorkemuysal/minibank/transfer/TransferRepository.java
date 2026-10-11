package com.gorkemuysal.minibank.transfer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    record StuckTransfer(Long id, TransferStatus status) {
    }

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
            select new com.gorkemuysal.minibank.transfer.TransferRepository.StuckTransfer(t.id, t.status)
            from Transfer t
            where t.status in (com.gorkemuysal.minibank.transfer.TransferStatus.PENDING,
                               com.gorkemuysal.minibank.transfer.TransferStatus.CAPTURING,
                               com.gorkemuysal.minibank.transfer.TransferStatus.RELEASING)
              and t.updatedAt < :before
            order by t.updatedAt
            """)
    List<StuckTransfer> findStuckSince(@Param("before") Instant before, Pageable pageable);
}