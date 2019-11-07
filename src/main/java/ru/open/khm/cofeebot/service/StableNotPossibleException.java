package ru.open.khm.cofeebot.service;

import ru.open.khm.cofeebot.entity.Request;

public class StableNotPossibleException extends RuntimeException {
    private final Request request;

    public StableNotPossibleException(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
