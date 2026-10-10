package com.gorkemuysal.minibank.fraud.check;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, Long> {

    Optional<FraudCheck> findByTransferId(Long transferId);

    /** Checks of this customer whose transfer was created in [from, to). Uses idx_fraud_checks_initiator_time. */
    @Query("""
            select count(c) from FraudCheck c
            where c.initiatorId = :initiatorId
              and c.requestedAt >= :from and c.requestedAt < :to
            """)
    long countInWindow(@Param("initiatorId") String initiatorId,
                       @Param("from") Instant from, @Param("to") Instant to);

    /** Literal APPROVED so the partial index idx_fraud_checks_approved_beneficiary can be used. */
    @Query("""
            select count(c) > 0 from FraudCheck c
            where c.initiatorId = :initiatorId and c.targetIban = :targetIban
              and c.decision = com.gorkemuysal.minibank.fraud.check.Decision.APPROVED
            """)
    boolean hasApprovedTransferTo(@Param("initiatorId") String initiatorId,
                                  @Param("targetIban") String targetIban);
}