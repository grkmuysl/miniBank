package com.gorkemuysal.minibank.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account , Long> {
    Optional<Account> findByIdAndOwnerId(Long id, String ownerId);
    List<Account> findAllByOwnerId(String ownerId);
}
