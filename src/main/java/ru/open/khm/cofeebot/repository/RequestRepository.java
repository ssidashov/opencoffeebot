package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.entity.User;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, String> {
    Optional<Request> getRequestByUser_IdEqualsAndRequestStatusTypeIn(String userId, List<RequestStatusType> statuses);

    List<Request> getRequestsByRequestStatusTypeIn(List<RequestStatusType> statuses);

    Optional<Request> getRequestByOriginalEquals(Request current);

    List<Request> getRequestsByRequestStatusTypeInAndUser(List<RequestStatusType> types, User user);

    @Query("select r from Request r where r.user = :user")
    Optional<Request> findByUser(User user);

    @Query("select r from Request r where r.user = :user and r.requestStatusType in (:statusTypeList)")
    List<Request> findByUserAndStatus(User user, List<RequestStatusType> statusTypeList);

    @Query("select r from Request r where r.user = :user order by r.createTime desc")
    List<Request> findTopByUserEqualsCreateTime(User user);
}
