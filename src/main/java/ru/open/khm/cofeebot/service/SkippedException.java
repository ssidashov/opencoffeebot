package ru.open.khm.cofeebot.service;

public class SkippedException extends Exception {
    private final String newId;
    public SkippedException(String newId) {
        this.newId = newId;
    }

    public String getNewId() {
        return newId;
    }
}
