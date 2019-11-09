package ru.open.khm.cofeebot.exception;

public class SkippedException extends Exception {
    private final String newId;
    public SkippedException(String newId) {
        this.newId = newId;
    }

    public String getNewId() {
        return newId;
    }
}
