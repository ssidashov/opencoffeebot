package ru.open.khm.cofeebot.entity;

public enum RequestStatusType {
    CREATED(false, null)
    , PAIRED(false, null)
    , CANCELLED(true, PairDecision.REJECTED)
    , REJECTED(true, PairDecision.REJECTED)
    , REJECTED_BLACKLIST(true, PairDecision.REJECTED)
    , ACCEPTED(true, PairDecision.ACCEPTED)
    , ACCEPT_TIMED_OUT(true, PairDecision.REJECTED_TIMEOUT)
    , SKIPPED(true, PairDecision.REJECTED);

    private final boolean aFinal;
    private final PairDecision pairDecision;

    RequestStatusType(boolean aFinal, PairDecision pairDecision) {
        this.pairDecision = pairDecision;
        this.aFinal = aFinal;
    }

    public boolean isFinal() {
        return aFinal;
    }

    public PairDecision getPairDecision() {
        return pairDecision;
    }
}
