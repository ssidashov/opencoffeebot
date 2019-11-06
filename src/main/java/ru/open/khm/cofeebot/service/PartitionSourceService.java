package ru.open.khm.cofeebot.service;

import java.util.List;

public interface PartitionSourceService {
    List<LocationPartition> getActiveRequestParitionsSnapshot();
}
