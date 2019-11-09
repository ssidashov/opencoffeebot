package ru.open.khm.cofeebot.service.request;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.service.*;

import java.util.Comparator;
import java.util.List;

@Service
public class RequestProcessingServiceImpl implements RequestProcessingService {
    private final PartitionSourceService requestPartitionService;
    private final PartitionSelectService partitionSelectService;
    private final DoPairingsService doPairingsService;
    private final ActiveRequestStatsMarkService activeRequestStatsMarkService;

    public RequestProcessingServiceImpl(PartitionSourceService requestPartitionService
            , PartitionSelectService partitionSelectService
            , DoPairingsService doPairingsService
            , ActiveRequestStatsMarkService activeRequestStatsMarkService) {
        this.requestPartitionService = requestPartitionService;
        this.partitionSelectService = partitionSelectService;
        this.doPairingsService = doPairingsService;
        this.activeRequestStatsMarkService = activeRequestStatsMarkService;
    }

    @Override
    @Transactional
    public void processRequests() {
        List<LocationPartition> activeRequestPartitionsSnapshot = requestPartitionService.getActiveRequestParitionsSnapshot();
        activeRequestPartitionsSnapshot.stream()
                .peek(activeRequestStatsMarkService::markStats)
                .peek(this::tryKickLooser)
                .filter(partitionSelectService::isEligibleForAccepting)
                .forEach(doPairingsService::pairPartition);
    }

    private void tryKickLooser(LocationPartition locationPartition) {
        if (locationPartition.getRequests().size() % 2 != 0) {
            Request request = locationPartition.getRequests()
                    .stream()
                    .max(Comparator.comparing(Request::getCreateTime))
                    .orElseThrow(() -> new IllegalStateException("Wrong size of partition"));
            locationPartition.getRequests().remove(request);
        }
    }
}
