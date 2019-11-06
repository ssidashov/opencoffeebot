package ru.open.khm.cofeebot.entity;

import lombok.Data;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.Instant;

@Data
public class RequestInfo {
    private Instant createTime;

    private Instant originalCreated;

    private boolean canPay = false;

    private boolean myPlace;

    private String location;

    private String place;

    private Integer maxWaitSeconds;

    private Integer currentPersonCount;
}
