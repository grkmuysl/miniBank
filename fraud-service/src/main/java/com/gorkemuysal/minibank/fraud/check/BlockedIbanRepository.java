package com.gorkemuysal.minibank.fraud.check;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIbanRepository extends JpaRepository<BlockedIban, String> {
}