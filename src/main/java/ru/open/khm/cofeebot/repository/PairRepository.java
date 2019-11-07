package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;

import java.util.List;
import java.util.Optional;

public interface PairRepository extends CrudRepository<Pair, String> {
    Optional<Pair> findByFirstRequestEqualsOrSecondRequestEquals(Request request1, Request request2);

    @Query("select p from Pair p where (p.firstRequest = :requestOne and p.secondRequest = :requestTwo) or (p.secondRequest = :requestOne and p.firstRequest = :requestTwo)")
    List<Pair> findByFirstRequestEqualsAndSecondRequestEqualsOrReverse(Request requestOne, Request requestTwo);
}
