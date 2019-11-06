package ru.open.khm.cofeebot.service;

import lombok.Data;
import lombok.ToString;
import ru.open.khm.cofeebot.entity.Request;

import java.util.List;

@Data
@ToString
public class LocationPartition {
    private final String location;
    private final List<Request> requests;
}
