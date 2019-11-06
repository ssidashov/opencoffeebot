package ru.open.khm.cofeebot.service;

import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.repository.RequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartitionSourceServiceImpl implements PartitionSourceService {
    private final RequestRepository requestRepository;

    public PartitionSourceServiceImpl(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Override
    public List<LocationPartition> getActiveRequestParitionsSnapshot() {
        List<Request> requests = requestRepository.getRequestsByRequestStatusTypeIn(Collections.singletonList(RequestStatusType.CREATED));

        Map<String, List<Request>> grouped = requests.stream()
                .collect(Collectors.groupingBy(request -> clearLocation(request.getLocation())));

        List<LocationPartition> partitions = grouped.entrySet().stream()
                .map(stringListEntry -> new LocationPartition(stringListEntry.getValue().get(0).getLocation(), requests))
                .collect(Collectors.toList());
        return partitions;
    }

    private String clearLocation(String location) {
        return location
                .toUpperCase()
                .replaceAll("Ё", "Е")
                .replaceAll("[^\\dА-ЯA-Z]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
