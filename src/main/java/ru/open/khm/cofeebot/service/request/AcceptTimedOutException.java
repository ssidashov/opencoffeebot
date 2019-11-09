package ru.open.khm.cofeebot.service.request;

public class AcceptTimedOutException extends RuntimeException {
    private final String id;
    public AcceptTimedOutException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
