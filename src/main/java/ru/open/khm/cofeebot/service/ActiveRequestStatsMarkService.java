package ru.open.khm.cofeebot.service;

public interface ActiveRequestStatsMarkService {
    void markStats(LocationPartition activeRequestPartitionsSnapshot);
}
