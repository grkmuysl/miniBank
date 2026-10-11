package com.gorkemuysal.minibank.fraud.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // Literal status so the partial index (WHERE status = 'PENDING') can be used; same idea as in 3.4.
    @Query("""
            select e from OutboxEvent e
            where e.status = com.gorkemuysal.minibank.messaging.outbox.OutboxStatus.PENDING
            order by e.id
            """)
    List<OutboxEvent> findPending(Pageable pageable);

    @Transactional
    @Modifying
    @Query("""
            update OutboxEvent e
            set e.status = com.gorkemuysal.minibank.messaging.outbox.OutboxStatus.PUBLISHED,
                e.publishedAt = :now
            where e.id = :id
            """)
    int markPublished(@Param("id") Long id, @Param("now") Instant now);
}