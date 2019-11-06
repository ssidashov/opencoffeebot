package ru.open.khm.cofeebot.service;

import org.springframework.stereotype.Service;

@Service
public class ActiveRequestStatsMarkServiceImpl implements ActiveRequestStatsMarkService {
    @Override
    public void markStats(LocationPartition activeRequestPartitionsSnapshot) {
        int size = activeRequestPartitionsSnapshot.getRequests().size();
        activeRequestPartitionsSnapshot.getRequests().forEach(request -> {
            request.setActiveRequestStats(size);
        });
    }
}
