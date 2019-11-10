package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.entity.*;
import ru.open.khm.cofeebot.repository.PairRepository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class PairServiceImpl implements PairService {
    private final PairRepository pairRepository;

    @Autowired
    private TelegramService telegramService;

    @PersistenceContext
    private EntityManager entityManager;

    public PairServiceImpl(PairRepository pairRepository) {
        this.pairRepository = pairRepository;
    }

    @Override
    public Optional<Request> rejectByRequest(Request request, Pair pair, RequestStatusType outcome) {
        entityManager.lock(pair, LockModeType.PESSIMISTIC_WRITE);
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
            if (isFirst) {
                pair.setFirstAccepted(Instant.now());
                pair.setFirstDecision(outcome.getPairDecision());
            } else {
                pair.setSecondAccepted(Instant.now());
                pair.setSecondDecision(outcome.getPairDecision());
            }
            if (otherRequest.getRequestStatusType() == RequestStatusType.PAIRED
                    || otherRequest.getRequestStatusType() == RequestStatusType.ACCEPTED) {
                if (otherRequest.getRequestStatusType() == RequestStatusType.PAIRED) {
                    otherRequest.setRequestStatusType(RequestStatusType.SKIPPED);
                }
                return Optional.of(otherRequest);
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

        telegramService.pairCreatedNotify(pair);

        return pair;
    }

    @Override
    public void acceptByRequest(Request request, Pair pair) {
        entityManager.lock(pair, LockModeType.PESSIMISTIC_WRITE);
        boolean isFirst = pair.getFirstRequest() == request;
        Request otherRequest = isFirst ? pair.getSecondRequest() : pair.getFirstRequest();
        PairDecision otherState;
        if (isFirst) {
            pair.setFirstAccepted(Instant.now());
            pair.setFirstDecision(PairDecision.ACCEPTED);
            otherState = pair.getSecondDecision();
        } else {
            pair.setSecondAccepted(Instant.now());
            pair.setSecondDecision(PairDecision.ACCEPTED);
            otherState = pair.getFirstDecision();
        }
        if (otherState == null) {
            return;
        }
        if (otherState == PairDecision.ACCEPTED) {
            telegramService.pairAcceptedNotify(pair);
            pair.setPairStatus(PairStatus.ACCEPTED);
        } else {
            pair.setPairStatus(PairStatus.REJECTED);
        }
    }

    public void setTelegramService(TelegramService telegramService) {
        this.telegramService = telegramService;
    }
}
