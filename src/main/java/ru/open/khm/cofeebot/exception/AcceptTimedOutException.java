package ru.open.khm.cofeebot.exception;

public class AcceptTimedOutException extends RuntimeException {
    private final String id;
    public AcceptTimedOutException(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
