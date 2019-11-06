package ru.open.khm.cofeebot.service;

import ru.open.khm.cofeebot.entity.RequestStatus;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.rest.RequestInput;

public interface RequestService {
    String createNew(RequestInput requestInput);

    void cancelRequest(String id);

    void rejectRequest(String id, RequestStatusType typeToReject);

    void acceptRequest(String id);

    RequestStatus getRequestStatus(String id) throws SkippedException;
}
