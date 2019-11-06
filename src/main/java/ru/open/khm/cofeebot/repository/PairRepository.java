package ru.open.khm.cofeebot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;

import java.util.Optional;

public interface PairRepository extends CrudRepository<Pair, String> {
    Optional<Pair> findByFirstRequestEqualsOrSecondRequestEquals(Request request1, Request request2);
}
