package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.open.khm.cofeebot.CofeebotProperties;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.repository.RequestRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TimeoutRequestsServiceImpl implements TimeoutRequestsService {
    private final RequestService requestService;
    private final RequestRepository requestRepository;
    private final PairRepository pairRepository;
    private final CofeebotProperties cofeebotProperties;
    private final TelegramService telegramService;

    @PersistenceContext
    private EntityManager entityManager;

    public TimeoutRequestsServiceImpl(RequestService requestService
            , RequestRepository requestRepository
            , PairRepository pairRepository
            , CofeebotProperties cofeebotProperties
            , TelegramService telegramService) {
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.pairRepository = pairRepository;
        this.cofeebotProperties = cofeebotProperties;
        this.telegramService = telegramService;
    }

    @Override
    @Transactional
    public void timeoutRequests() {
        List<Request> requestsByRequestStatusTypeIn
                = requestRepository.getRequestsByRequestStatusTypeIn(Collections.singletonList(RequestStatusType.PAIRED));
        requestsByRequestStatusTypeIn
                .stream()
                .forEach(request -> {
                    entityManager.flush();
                    entityManager.refresh(request);
                    if (request.getRequestStatusType() != RequestStatusType.PAIRED) {
                        return;
                    }
                    Optional<Pair> currentPair = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);
                    Pair pair = currentPair.orElseThrow(() -> new IllegalStateException("No pair on paired request"));
                    Instant created = pair.getCreated();

                    long secondsElapsed = created.until(Instant.now(), ChronoUnit.SECONDS);
                    if (secondsElapsed > cofeebotProperties.getPairAcceptTimeoutSeconds()) {
                        log.debug("Timing out request " + request);
                        timeoutRequest(request, pair);
                        int parentCountByStatus = requestService.getParentCountByStatus(request, RequestStatusType.ACCEPT_TIMED_OUT);
                        if (parentCountByStatus < cofeebotProperties.getMaxTimeoutCount()) {
                            log.debug("Max timeout count on request not expired:" + parentCountByStatus + ", retrying");
                        }
                    }
                });
    }

    private void timeoutRequest(Request request, Pair pair) {
        requestService.rejectRequest(request.getId(), RequestStatusType.ACCEPT_TIMED_OUT);
        telegramService.notifyTimeoutFired(request, pair);
    }
}
