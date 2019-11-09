package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.CofeebotProperties;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.service.request.RequestService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;
import java.util.function.Function;

@Service
@Slf4j
public class PartitionSelectServiceImpl implements PartitionSelectService {
    private final CofeebotProperties cofeebotProperties;
    private final RequestService requestService;
    private static final int SCORE_TO_PROCESS = 100;

    public PartitionSelectServiceImpl(CofeebotProperties cofeebotProperties
            , RequestService requestService) {
        this.cofeebotProperties = cofeebotProperties;
        this.requestService = requestService;
    }

    @Override
    public boolean isEligibleForAccepting(LocationPartition locationPartition) {
        if (locationPartition.getRequests().size() < 2) {
            return false;
        }

        Pair<Integer, Long> baseScoreToMatch1 = getBaseScoreToMatch(locationPartition);
        int baseScoreToMatch = baseScoreToMatch1.getKey();
        long countNew = baseScoreToMatch1.getValue();

        OptionalInt minWaitTime = locationPartition.getRequests()
                .stream()
                .filter(request -> request.getMaxWaitSeconds() != null)
                .mapToInt(Request::getMaxWaitSeconds)
                .min();

        Function<Request, Instant> getOriginalCreated = Request::getOriginalCreated;
        Function<Request, Instant> getCreateTimeCreated = Request::getCreateTime;
        Function<Request, Instant> getTimeFunction = countNew == 0 ? getCreateTimeCreated : getOriginalCreated;
        Instant oldestCreated = Instant.ofEpochMilli((long)locationPartition.getRequests()
                .stream()
                .map(getTimeFunction)
                .mapToLong(Instant::toEpochMilli)
                .average()
                .orElseThrow(() -> new IllegalStateException("No oldest request")));

        int size = locationPartition.getRequests().size();
        int timePart = baseScoreToMatch - (int) (cofeebotProperties.getStagingPersonCountMultipler() * size);
        int scoreToMatch = minWaitTime.stream().map(minWaitTimeVal
                -> baseScoreToMatch - (int) (((double) timePart / minWaitTimeVal) * timePart))
                .findFirst()
                .orElse(baseScoreToMatch);

        long oldestWaitTimeSeconds = oldestCreated.until(Instant.now(), ChronoUnit.SECONDS);
        int currentScore = (int) (size * cofeebotProperties.getStagingPersonCountMultipler()
                + oldestWaitTimeSeconds * cofeebotProperties.getStagingWaitTimeMultiplier());

        int secondsToWaitEstimated = (int) ((float) (scoreToMatch - currentScore) / cofeebotProperties.getStagingWaitTimeMultiplier());
        if(secondsToWaitEstimated < 0) {
            secondsToWaitEstimated = 0;
        }
        setEstimatedOnRequests(locationPartition, secondsToWaitEstimated);

        log.debug("Partition location: " + locationPartition.getLocation() + ", current score:" + currentScore + ", score to match:" + scoreToMatch);
        return (currentScore >= scoreToMatch);
    }

    private Pair<Integer, Long> getBaseScoreToMatch(LocationPartition locationPartition) {
        long countOfNew = locationPartition.getRequests()
                .stream()
                .map(request -> {
                    int parentCount = requestService.getParentCount(request);
                    return parentCount == 0;
                }).filter(aBoolean -> aBoolean)
                .count();

        int baseScoreToMatch = SCORE_TO_PROCESS;
        if (countOfNew == 0) {
            baseScoreToMatch = SCORE_TO_PROCESS * 2;
        }
        return Pair.of(baseScoreToMatch, countOfNew);
    }

    private void setEstimatedOnRequests(LocationPartition locationPartition, int secondsToWaitEstimated) {
        if (locationPartition.getRequests().size() > 1) {
            locationPartition.getRequests().forEach(request -> request.setSecondsWaitEstimated(secondsToWaitEstimated));
        }else{
            locationPartition.getRequests().forEach(request -> request.setSecondsWaitEstimated(null));
        }
    }
}
