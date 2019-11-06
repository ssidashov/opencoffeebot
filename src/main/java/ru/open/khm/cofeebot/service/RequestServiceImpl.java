package ru.open.khm.cofeebot.service;

import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.open.khm.cofeebot.entity.*;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.repository.RequestRepository;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.rest.RequestInput;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

@Service
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PairRepository pairRepository;
    private final PairService pairService;

    public RequestServiceImpl(RequestRepository requestRepository
            , UserRepository userRepository
            , PairRepository pairRepository
            , PairService pairService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.pairRepository = pairRepository;
        this.pairService = pairService;
    }

    @Override
    @Transactional
    public String createNew(RequestInput requestInput) {
        Optional<User> userByLogin = userRepository.findById(requestInput.getUserId());
        User user = userByLogin.orElseThrow(() -> new IllegalArgumentException("No user with id " + requestInput.getUserId()));
        Optional<Request> activeRequestByUser = requestRepository.getRequestByUser_IdEqualsAndRequestStatusTypeIn(user.getId()
                , Collections.singletonList(RequestStatusType.CREATED));
        activeRequestByUser.ifPresent(request -> {
            throw new IllegalArgumentException("Already registered request with id " + request.getId());
        });

        Instant now = Instant.now();
        Request request = new Request();
        request.setUser(user);
        request.setLocation(user.getLocation());
        request.setPlace(requestInput.isMyPlace() ? user.getWorkplace() + " " + user.getLocation() : user.getLocation());
        request.setCanPay(requestInput.isCanPay());
        request.setCreateTime(now);
        request.setOriginalCreated(now);
        request.setMaxWaitSeconds(requestInput.getMaxWaitTime());

        Request saved = requestRepository.save(request);

        return saved.getId();
    }

    @Override
    @Transactional
    public void cancelRequest(String id) {
        Optional<Request> byId = requestRepository.findById(id);
        Request request = byId.orElseThrow(() -> new IllegalArgumentException("No request with id " + id));
        if (request.getRequestStatusType() != RequestStatusType.CREATED && request.getRequestStatusType() != RequestStatusType.PAIRED) {
            throw new IllegalStateException("Cannot cancel already processed request");
        }
        Optional<Pair> pairExists = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);
        pairExists.ifPresent(pair -> {
            Optional<Request> requestToRenew = pairService.rejectByRequest(request, pair, RequestStatusType.CANCELLED);
            requestToRenew.ifPresent(this::renewRequest);
        });
        request.setRequestStatusType(RequestStatusType.CANCELLED);
    }

    private Request renewRequest(Request request) {
        Request newRequest = new Request();
        newRequest.setOriginalCreated(request.getOriginalCreated());
        newRequest.setLocation(request.getLocation());
        newRequest.setMyPlace(request.isMyPlace());
        newRequest.setPlace(request.getPlace());
        newRequest.setUser(request.getUser());
        newRequest.setCreateTime(Instant.now());
        newRequest.setOriginal(request);
        newRequest.setMaxWaitSeconds(request.getMaxWaitSeconds());

        return requestRepository.save(newRequest);
    }

    @Override
    public void rejectRequest(String id, RequestStatusType typeToReject) {

    }

    @Override
    public void acceptRequest(String id) {

    }

    @Override
    @Transactional
    public RequestStatus getRequestStatus(String id) throws SkippedException {
        Optional<Request> byId = requestRepository.findById(id);
        Request request = byId.orElseThrow(() -> new IllegalArgumentException("No request with id " + id));
        if (request.getRequestStatusType() == RequestStatusType.SKIPPED) {
            Request finalChild = getFinalChild(request);
            throw new SkippedException(finalChild.getId());
        }
        boolean yourAccepted = request.getRequestStatusType() != RequestStatusType.CREATED;
        Optional<Pair> requestPair = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);

        PairStatusData desciptionAndYourDecision = getDesciptionAndYourDecision(request, requestPair);

        RequestStatus status = new RequestStatus();
        status.setYouAccepted(yourAccepted);
        status.setBuddyDescription(desciptionAndYourDecision.getBuddyDescription());
        status.setPaired(desciptionAndYourDecision.getBuddyDescription() != null);
        status.setRequest(requestInfoFromRequest(request));
        status.setYourDecision(desciptionAndYourDecision.getYourDecision());
        status.setPairStatus(desciptionAndYourDecision.getPairStatus());

        return status;
    }

    private Request getFinalChild(Request request) {
        Request current = request;
        while (current.getRequestStatusType() == RequestStatusType.SKIPPED) {
            Optional<Request> requestByOriginalEquals = requestRepository.getRequestByOriginalEquals(current);
            current = requestByOriginalEquals.orElseThrow(() -> new IllegalStateException("No non-skiped status child"));
        }
        return current;
    }

    private RequestInfo requestInfoFromRequest(Request request) {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setPlace(request.getPlace());
        requestInfo.setMyPlace(request.isMyPlace());
        requestInfo.setCreateTime(request.getCreateTime());
        requestInfo.setOriginalCreated(request.getOriginalCreated());
        requestInfo.setLocation(request.getLocation());
        requestInfo.setMaxWaitSeconds(request.getMaxWaitSeconds());
        requestInfo.setCanPay(request.isCanPay());
        requestInfo.setCurrentPersonCount(request.getSameoLocationRequestCount());
        return requestInfo;
    }

    private PairStatusData getDesciptionAndYourDecision(Request request, Optional<Pair> requestPair) {
        return requestPair.map(pair -> {
            BuddyDescription buddyDescriptionCreated = new BuddyDescription();
            boolean first;
            if (pair.getFirstRequest() == request) {
                first = true;
            } else {
                first = false;
            }
            Request buddyRequest = first ? pair.getSecondRequest() : pair.getFirstRequest();

            buddyDescriptionCreated.setLogin(buddyRequest.getUser().getLogin());
            buddyDescriptionCreated.setCanPay(buddyRequest.isCanPay());
            buddyDescriptionCreated.setLastname(buddyRequest.getUser().getLastname());
            buddyDescriptionCreated.setFirstname(buddyRequest.getUser().getFirstname());
            buddyDescriptionCreated.setMiddlename(buddyRequest.getUser().getMiddlename());
            buddyDescriptionCreated.setDepartment(buddyRequest.getUser().getDepartment());
            buddyDescriptionCreated.setPlace(buddyRequest.getPlace());
            buddyDescriptionCreated.setPosition(buddyRequest.getUser().getPosition());

            PairDecision yourDecision = first ? pair.getFirstDecision() : pair.getSecondDecision();

            return new PairStatusData(buddyDescriptionCreated, yourDecision, pair.getPairStatus());
        }).orElse(new PairStatusData(null, null, null));
    }

    @Data
    private static class PairStatusData {
        private final BuddyDescription buddyDescription;
        private final PairDecision yourDecision;
        private final PairStatus pairStatus;
    }
}