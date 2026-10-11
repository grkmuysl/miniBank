package com.gorkemuysal.minibank.fraud.check;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "blocked_ibans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedIban {

    @Id
    @Column(length = 26)
    private String iban;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}