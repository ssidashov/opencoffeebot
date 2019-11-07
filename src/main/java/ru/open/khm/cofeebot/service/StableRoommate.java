package ru.open.khm.cofeebot.service;

import ru.open.khm.cofeebot.entity.Request;

import java.util.List;
import java.util.Map;

public interface StableRoommate {
    Map<Request, Request> getPairs(Map<Request, List<Request>> preferences);
}
