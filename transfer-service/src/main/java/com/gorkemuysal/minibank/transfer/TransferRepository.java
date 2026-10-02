package com.gorkemuysal.minibank.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByInitiatorIdAndIdempotencyKey(String initiatorId, String idempotencyKey);

    Optional<Transfer> findByIdAndInitiatorId(Long id, String initiatorId);

    List<Transfer> findAllByInitiatorIdOrderByCreatedAtDesc(String initiatorId);
}