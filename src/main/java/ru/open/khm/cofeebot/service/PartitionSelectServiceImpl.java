package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.Ordering;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.CofeebotProperties;
import ru.open.khm.cofeebot.entity.Request;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;

@Service
@Slf4j
public class PartitionSelectServiceImpl implements PartitionSelectService {

    private final CofeebotProperties cofeebotProperties;
    private static final int SCORE_TO_PROCESS = 50;

    public PartitionSelectServiceImpl(CofeebotProperties cofeebotProperties) {
        this.cofeebotProperties = cofeebotProperties;
    }

    @Override
    public boolean isEligibleForAccepting(LocationPartition locationPartition) {
        if (locationPartition.getRequests().size() < 2) {
            return false;
        }
        int baseScoreToMatch = SCORE_TO_PROCESS;
        OptionalInt minWaitTime = locationPartition.getRequests()
                .stream()
                .filter(request -> request.getMaxWaitSeconds() != null)
                .mapToInt(Request::getMaxWaitSeconds)
                .min();

        Instant oldestCreated = locationPartition.getRequests()
                .stream()
                .map(Request::getOriginalCreated)
                .min(Ordering.natural()).orElseThrow(() -> new IllegalStateException("No oldest request"));

        int size = locationPartition.getRequests().size();
        int timePart = baseScoreToMatch - (int) (cofeebotProperties.getStagingPersonCountMultipler() * size);
        int scoreToMatch = minWaitTime.stream().map(minWaitTimeVal
                -> baseScoreToMatch - (int) (((double) timePart / minWaitTimeVal) * timePart))
                .findFirst()
                .orElse(baseScoreToMatch);

        int currentScore = (int) (size * cofeebotProperties.getStagingPersonCountMultipler()
                + oldestCreated.until(Instant.now(), ChronoUnit.SECONDS) * cofeebotProperties.getStagingWaitTimeMultiplier());

        log.debug("Partition location: " + locationPartition.getLocation() + ", current score:" + currentScore + ", score to match:" + scoreToMatch);
        return (currentScore >= scoreToMatch);
    }
}
