package ru.open.khm.cofeebot.entity;

import lombok.Data;

@Data
public class RequestStatus {
    private RequestInfo request;
    private boolean paired;
    private boolean youAccepted;
    private PairDecision yourDecision;
    private BuddyDescription buddyDescription;
    private PairStatus pairStatus;
}
