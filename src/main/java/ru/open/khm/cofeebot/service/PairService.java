package ru.open.khm.cofeebot.service;


import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;

import java.util.Optional;

public interface PairService {
    Optional<Request> rejectByRequest(Request request, Pair pair, RequestStatusType outcome);

    Pair makePair(Request request1, Request request2);
}
