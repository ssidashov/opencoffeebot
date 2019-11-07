package ru.open.khm.cofeebot.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Data
public class BlackListRecord {
    @Id
    @GenericGenerator(name = "BlackListRecordGenerator", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "BlackListRecordGenerator")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User issuer;

    @ManyToOne(fetch = FetchType.LAZY)
    private User blacklisted;

    private Instant blacklistTime;
}
