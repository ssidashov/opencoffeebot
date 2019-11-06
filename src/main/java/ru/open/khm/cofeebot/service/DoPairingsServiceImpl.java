package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;

import java.util.Iterator;

@Slf4j
@Service
public class DoPairingsServiceImpl implements DoPairingsService {
    private final PairService pairService;

    public DoPairingsServiceImpl(PairService pairService) {
        this.pairService = pairService;
    }

    @Override
    public void pairPartition(LocationPartition locationPartition) {
        Assert.state(locationPartition.getRequests().size() % 2 == 0, "Size must not be odd");

        Iterator<Request> iterator = locationPartition.getRequests().iterator();
        while (iterator.hasNext()) {
            Request request1 = iterator.next();
            Request request2 = iterator.next();
            processPair(request1, request2);
        }
    }

    private void processPair(Request request1, Request request2) {
        Pair pair = pairService.makePair(request1, request2);
    }
}
