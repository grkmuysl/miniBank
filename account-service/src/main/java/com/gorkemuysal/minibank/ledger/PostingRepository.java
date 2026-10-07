package com.gorkemuysal.minibank.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    List<Posting> findAllByTransferId(Long transferId);
}