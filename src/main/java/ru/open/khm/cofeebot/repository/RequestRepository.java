package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, String> {
    Optional<Request> getRequestByUser_IdEqualsAndRequestStatusTypeIn(String userId, List<RequestStatusType> statuses);

    List<Request> getRequestsByRequestStatusTypeIn(List<RequestStatusType> statuses);

    Optional<Request> getRequestByOriginalEquals(Request current);

}
