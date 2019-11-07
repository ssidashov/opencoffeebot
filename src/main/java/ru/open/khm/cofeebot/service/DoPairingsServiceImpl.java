package ru.open.khm.cofeebot.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.open.khm.cofeebot.entity.*;
import ru.open.khm.cofeebot.repository.BlacklistRepository;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.repository.RequestRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoPairingsServiceImpl implements DoPairingsService {
    private final PairService pairService;
    private final RequestRepository requestRepository;
    private final BlacklistRepository blacklistRepository;
    private final PairRepository pairRepository;
    private final StableRoommate stableRoommate;

    private static final int REPEAT_ACCEPTED_PAIR_SCORE = -10;
    private static final int REPEAT_REJECTED_PAIR_SCORE = -20;

    private static final int BLACKLISTED_SCORE = -10000;

    public DoPairingsServiceImpl(PairService pairService
            , RequestRepository requestRepository
            , BlacklistRepository blacklistRepository
            , PairRepository pairRepository
            , StableRoommate stableRoommate) {
        this.pairService = pairService;
        this.requestRepository = requestRepository;
        this.blacklistRepository = blacklistRepository;
        this.pairRepository = pairRepository;
        this.stableRoommate = stableRoommate;
    }

    @Override
    public void pairPartition(LocationPartition locationPartition) {
        Assert.state(locationPartition.getRequests().size() % 2 == 0, "Size must not be odd");

        List<Request> requests = new ArrayList<>(locationPartition.getRequests());
        Map<Request, List<Request>> priorities = getPriorities(requests);
        log.debug("Priorities for pairs:" + priorities);
        boolean isStable = false;
        Map<Request, Request> alreadyPaired = new HashMap<>();
        Map<Request, Request> pairs = new HashMap<>();
        while (!isStable && priorities.size() > 0) {
            try {
                pairs = stableRoommate.getPairs(priorities);
                isStable = true;
            } catch (StableNotPossibleException e) {
                mapLooserToPair(e.getRequest(), alreadyPaired, priorities);
            }
        }
        log.debug("Already paired: " + alreadyPaired);
        log.debug("Roommate pairs:" + pairs);

        Set<Request> wasPaired = new HashSet<>();
        alreadyPaired.forEach((request, request2) -> tryCreatePair(request, request2, wasPaired));
        pairs.forEach((request1, request2) -> tryCreatePair(request1, request2, wasPaired));
    }

    private void tryCreatePair(Request request1, Request request2, Set<Request> wasPaired) {
        if (!wasPaired.contains(request1) && !wasPaired.contains(request2)) {
            wasPaired.add(request1);
            wasPaired.add(request2);
            if (isUsersBlacklisted(request1, request2)) {
                log.debug("Pair blacklisted: " + request1 + ", " + request2);
                return;
            }
            processPair(request1, request2);
        }
    }

    private void mapLooserToPair(Request looserRequest
            , Map<Request, Request> alreadyPaired
            , Map<Request, List<Request>> priorities) {
        List<Request> looserQueue = priorities.get(looserRequest);
        priorities.remove(looserRequest);
        Request looserBuddy = looserQueue.get(0);
        priorities.remove(looserBuddy);
        alreadyPaired.put(looserBuddy, looserRequest);
        alreadyPaired.put(looserRequest, looserBuddy);
    }

    private boolean isUsersBlacklisted(Request request1, Request request2) {
        int i = blacklistRepository.countByIssuerAndBlacklistedOrReverse(request1.getUser(), request2.getUser());
        if (i != 0) {
            return true;
        } else {
            return false;
        }
    }

    private Map<Request, List<Request>> getPriorities(List<Request> requests) {
        Map<Request, Integer> baseScores = requests.stream()
                .map(request -> ImmutablePair.of(request, getUserBaseScore(request)))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));

        Map<org.apache.commons.lang3.tuple.Pair<Request, Request>, Integer> memoizedScores = new HashMap<>();
        Map<Request, List<Request>> priorityMap = new HashMap<>();

        requests.forEach(requestOne -> {
            ArrayList<RequestWithScore> scores = new ArrayList<>();
            requests.forEach(requestTwo -> {
                if (requestOne == requestTwo) {
                    return;
                }
                Integer memoizedScore = null;
                int pairScore;
                if ((memoizedScore = memoizedScores.get(org.apache.commons.lang3.tuple.Pair.of(requestOne, requestTwo))) != null) {
                    pairScore = memoizedScore;
                } else {
                    pairScore = getPairScore(requestOne, requestTwo);
                    memoizedScores.put(org.apache.commons.lang3.tuple.Pair.of(requestOne, requestTwo), pairScore);
                }
                RequestWithScore score = new RequestWithScore();
                score.request = requestTwo;
                score.score = baseScores.get(requestTwo) + pairScore;
                scores.add(score);
            });
            Comparator<RequestWithScore> scoreComparator = Comparator.comparing(requestWithScore -> requestWithScore.score);
            List<RequestWithScore> scoresList = scores.stream()
                    .sorted(scoreComparator.reversed())
                    .collect(Collectors.toList());
            log.debug("Priorities with scores for request " + requestOne + ": " + scoresList);

            List<Request> orderedRequests = scoresList.stream()
                    .map(RequestWithScore::getRequest)
                    .collect(Collectors.toList());
            priorityMap.put(requestOne, orderedRequests);
        });

        return priorityMap;
    }

    private int getPairScore(Request requestOne, Request requestTwo) {
        // -10 за согласованную пару
        int score = 0;
        List<Pair> formedPairs = pairRepository.findByFirstRequestEqualsAndSecondRequestEqualsOrReverse(requestOne, requestTwo);
        long countAccepted = formedPairs.stream()
                .filter(pair -> pair.getPairStatus() == PairStatus.ACCEPTED)
                .count();
        score -= countAccepted * REPEAT_ACCEPTED_PAIR_SCORE;

        long countRejected = formedPairs.stream()
                .filter(pair -> pair.getPairStatus() == PairStatus.ACCEPTED)
                .count();
        score -= countRejected * REPEAT_REJECTED_PAIR_SCORE;

        //blacklist
        int blacklistCount = blacklistRepository.countByIssuerAndBlacklistedOrReverse(requestOne.getUser(), requestTwo.getUser());
        if (blacklistCount > 0) {
            return BLACKLISTED_SCORE;
        }
        return score;
    }

    private int getUserBaseScore(Request request) {
        Instant now = Instant.now();
        int userScore = 0;
        // + 20 за то, что время ожидания превышено
        if (request.getMaxWaitSeconds() != null) {
            if (request.getOriginalCreated().until(now, ChronoUnit.SECONDS) > request.getMaxWaitSeconds()) {
                userScore += 20;
            }
        }

        // + Количество минут ожидания
        userScore += request.getOriginalCreated().until(now, ChronoUnit.MINUTES);

        // -10 За каждый отказ
        List<Request> rejectedRequests = requestRepository.getRequestsByRequestStatusTypeInAndUser(Arrays.asList(RequestStatusType.REJECTED
                , RequestStatusType.ACCEPT_TIMED_OUT), request.getUser());
        userScore += rejectedRequests.size() * -10;

        // -20 За каждый элемент черного списка
        List<BlackListRecord> allByIssuer = blacklistRepository.getAllByIssuer(request.getUser());
        userScore -= allByIssuer.size() * -20;

        return userScore;
    }

    private void processPair(Request request1, Request request2) {
        Pair pair = pairService.makePair(request1, request2);
    }

    @Data
    private static final class RequestWithScore {
        private int score;
        private Request request;
    }
}
