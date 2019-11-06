package ru.open.khm.cofeebot.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
public class Pair {
    @Id
    @GenericGenerator(name = "PairGenerator", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "PairGenerator")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Request firstRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    private Request secondRequest;

    private Instant created;

    private Instant firstAccepted;
    private Instant secondAccepted;

    @Enumerated(EnumType.STRING)
    private PairDecision firstDecision;

    @Enumerated(EnumType.STRING)
    private PairDecision secondDecision;

    @Enumerated(EnumType.STRING)
    private PairStatus pairStatus;
}
