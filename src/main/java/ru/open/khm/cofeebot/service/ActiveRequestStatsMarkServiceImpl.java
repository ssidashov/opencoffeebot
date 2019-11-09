package ru.open.khm.cofeebot.service;

import org.springframework.stereotype.Service;

@Service
public class ActiveRequestStatsMarkServiceImpl implements ActiveRequestStatsMarkService {
    private final TelegramService telegramService;

    public ActiveRequestStatsMarkServiceImpl(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void markStats(LocationPartition activeRequestPartitionsSnapshot) {
        int size = activeRequestPartitionsSnapshot.getRequests().size();
        activeRequestPartitionsSnapshot.getRequests().forEach(request -> {
            request.setActiveRequestStats(size);
            if (size == 1) {
                request.setSecondsWaitEstimated(null);
            }
            telegramService.notifyWaiting(request, size);
        });
    }
}
