package com.gorkemuysal.minibank.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account , Long> {
    Optional<Account> findByIdAndOwnerId(Long id, String ownerId);
    List<Account> findAllByOwnerId(String ownerId);

    /**
     * Loads the account with a row-level write lock (SELECT ... FOR UPDATE).
     * Other transactions trying to lock the same row wait until this transaction ends.
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
