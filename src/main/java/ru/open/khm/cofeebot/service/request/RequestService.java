package ru.open.khm.cofeebot.service.request;

import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatus;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.exception.SkippedException;
import ru.open.khm.cofeebot.rest.RequestInput;

import java.util.Optional;

public interface RequestService {
    String createNew(RequestInput requestInput);

    void cancelRequest(String id);

    Request renewRequest(Request request);

    void rejectRequest(String id, RequestStatusType typeToReject);

    void acceptRequest(String id);

    RequestStatus getRequestStatus(String id) throws SkippedException;

    int getParentCountByStatus(Request request, RequestStatusType requestStatusType);

    int getParentCount(Request request);

    Optional<Request> getInProcessRequestByUserId(String id);

    Optional<Request> getCurrentRequest(String id);
}
