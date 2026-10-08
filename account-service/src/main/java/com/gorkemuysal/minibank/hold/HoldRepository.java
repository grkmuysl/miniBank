package com.gorkemuysal.minibank.hold;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {

    Optional<Hold> findByTransferId(Long transferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hold h where h.transferId = :transferId")
    Optional<Hold> findByTransferIdForUpdate(@Param("transferId") Long transferId);

    @Query("""
            select new com.gorkemuysal.minibank.hold.HoldRef(h.accountId, h.targetAccountId, h.status)
            from Hold h where h.transferId = :transferId
            """)
    Optional<HoldRef> findRefByTransferId(@Param("transferId") Long transferId);
}