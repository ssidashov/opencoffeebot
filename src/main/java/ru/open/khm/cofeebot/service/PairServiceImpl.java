package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.entity.*;
import ru.open.khm.cofeebot.repository.PairRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class PairServiceImpl implements PairService {
    private final PairRepository pairRepository;

    public PairServiceImpl(PairRepository pairRepository) {
        this.pairRepository = pairRepository;
    }

    @Override
    public Optional<Request> rejectByRequest(Request request, Pair pair, RequestStatusType outcome) {
        Optional<Pair> byFirstRequestEqualsOrSecondRequestEquals
                = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);
        if (request.getRequestStatusType().isFinal()) {
            throw new IllegalStateException("Request was already processed");
        }
        if (byFirstRequestEqualsOrSecondRequestEquals.isPresent()) {
            pair.setPairStatus(PairStatus.REJECTED);
            boolean isFirst = pair.getFirstRequest() == request;
            request.setRequestStatusType(RequestStatusType.REJECTED);
            Request otherRequest = isFirst ? pair.getSecondRequest() : pair.getFirstRequest();
            if (otherRequest.getRequestStatusType() == RequestStatusType.PAIRED) {
                otherRequest.setRequestStatusType(RequestStatusType.SKIPPED);
                return Optional.of(otherRequest);
            }
            if (isFirst) {
                pair.setFirstAccepted(Instant.now());
                pair.setFirstDecision(outcome.getPairDecision());
            }else{
                pair.setSecondAccepted(Instant.now());
                pair.setSecondDecision(outcome.getPairDecision());
            }
        }
        return Optional.empty();
    }

    @Override
    public Pair makePair(Request request1, Request request2) {
        Pair pair = new Pair();
        pair.setCreated(Instant.now());
        pair.setFirstRequest(request1);
        pair.setSecondRequest(request2);
        pairRepository.save(pair);
        request1.setRequestStatusType(RequestStatusType.PAIRED);
        request2.setRequestStatusType(RequestStatusType.PAIRED);
        return pair;
    }
}
