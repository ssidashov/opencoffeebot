package ru.open.khm.cofeebot.service.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.open.khm.cofeebot.entity.*;
import ru.open.khm.cofeebot.exception.AcceptTimedOutException;
import ru.open.khm.cofeebot.exception.SkippedException;
import ru.open.khm.cofeebot.repository.BlacklistRepository;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.repository.RequestRepository;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.rest.RequestInput;
import ru.open.khm.cofeebot.service.PairService;
import ru.open.khm.cofeebot.service.TelegramService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PairRepository pairRepository;
    private final PairService pairService;
    private final BlacklistRepository blacklistRepository;
    private final TelegramService telegramService;

    public RequestServiceImpl(RequestRepository requestRepository
            , UserRepository userRepository
            , PairRepository pairRepository
            , PairService pairService
            , BlacklistRepository blacklistRepository
            , TelegramService telegramService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.pairRepository = pairRepository;
        this.pairService = pairService;
        this.blacklistRepository = blacklistRepository;
        this.telegramService = telegramService;
    }

    @Override
    @Transactional
    public String createNew(RequestInput requestInput) {
        Optional<User> userByLogin = userRepository.findById(requestInput.getUserId());
        User user = userByLogin.orElseThrow(() -> new IllegalArgumentException("No user with id " + requestInput.getUserId()));
        Optional<Request> activeRequestByUser = requestRepository.getRequestByUser_IdEqualsAndRequestStatusTypeIn(user.getId()
                , Arrays.asList(RequestStatusType.CREATED, RequestStatusType.PAIRED));
        activeRequestByUser.ifPresent(request -> {
            throw new IllegalStateException("Already registered request with id " + request.getId());
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

        if (user.getTelegramAccount() != null) {
            try {
                telegramService.sendRequestCreated(request, user);
            } catch (Exception e) {
                log.error("Cannot notify by telegram");
            }
        }

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
            rejectRequest(id, RequestStatusType.CANCELLED);
        });
        request.setRequestStatusType(RequestStatusType.CANCELLED);
    }

    @Override
    @Transactional
    public Request renewRequest(Request request) {
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
    @Transactional
    public void rejectRequest(String id, RequestStatusType typeToReject) {
        Optional<Request> byId = requestRepository.findById(id);
        Request request = byId.orElseThrow(() -> new IllegalArgumentException("No request with id " + id));
        if (request.getRequestStatusType() != RequestStatusType.PAIRED) {
            throw new IllegalStateException("Cannot cancel already processed request");
        }
        Optional<Pair> pairExists = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);
        pairExists.ifPresent(pair -> {
            Optional<Request> requestToRenew = pairService.rejectByRequest(request, pair, typeToReject);
            requestToRenew.ifPresent(this::renewRequest);
            requestToRenew.ifPresent(telegramService::notifyOtherRejected);
            if (typeToReject == RequestStatusType.REJECTED_BLACKLIST) {
                addToBlackList(request, pair);
            }
        });
        if (typeToReject != RequestStatusType.CANCELLED && (!isTimedOut5Count(request, typeToReject))) {
            renewRequest(request);
        }
        request.setRequestStatusType(typeToReject);
    }

    private boolean isTimedOut5Count(Request request, RequestStatusType typeToReject) {
        if (typeToReject != RequestStatusType.ACCEPT_TIMED_OUT) {
            return false;
        }

        int parentCountByStatus = getParentCountByStatus(request, RequestStatusType.ACCEPT_TIMED_OUT);
        boolean isNeedCancel = parentCountByStatus >= 5;
        if (isNeedCancel) {
            log.debug("Max timeout count on request not expired:" + parentCountByStatus + ", retrying");
        }
        return isNeedCancel;
    }

    private void addToBlackList(Request request, Pair pair) {
        boolean isFirst = pair.getFirstRequest() == request;

        User you = isFirst ? pair.getFirstRequest().getUser() : pair.getSecondRequest().getUser();
        User other = isFirst ? pair.getSecondRequest().getUser() : pair.getFirstRequest().getUser();
        BlackListRecord record = new BlackListRecord();
        record.setBlacklistTime(Instant.now());
        record.setIssuer(you);
        record.setBlacklisted(other);
        blacklistRepository.save(record);
    }

    @Override
    @Transactional
    public void acceptRequest(String id) {
        Optional<Request> byId = requestRepository.findById(id);
        Request request = byId.orElseThrow(() -> new IllegalArgumentException("No request with id " + id));
        if (request.getRequestStatusType() != RequestStatusType.PAIRED) {
            throw new IllegalStateException("Request status is not paird: " + request.getRequestStatusType());
        }
        request.setRequestStatusType(RequestStatusType.ACCEPTED);
        Optional<Pair> pairExists = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(request, request);
        pairExists.ifPresent(pair -> {
            pairService.acceptByRequest(request, pair);
        });
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
        if (request.getRequestStatusType() == RequestStatusType.ACCEPT_TIMED_OUT) {
            Request finalChild = getFinalChild(request);
            if (finalChild != request) {
                throw new AcceptTimedOutException(finalChild.getId());
            }
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

    @Override
    public int getParentCountByStatus(Request request, RequestStatusType statusType) {
        Request current = request;
        int countTimeout = 0;
        while (current.getOriginal() != null) {
            if (current.getRequestStatusType() == statusType) {
                countTimeout++;
            }
            current = current.getOriginal();
        }
        return countTimeout;
    }

    @Override
    public int getParentCount(Request request) {
        Request current = request;
        int countTimeout = 0;
        while (current.getOriginal() != null) {
            countTimeout++;
            current = current.getOriginal();
        }
        return countTimeout;
    }

    @Override
    public Optional<Request> getInProcessRequestByUserId(String id) {
        Optional<User> one = userRepository.findById(id);
        return one.flatMap(user -> requestRepository.findByUserAndStatus(user, List.of(RequestStatusType.CREATED, RequestStatusType.PAIRED))
                .stream().findFirst());
    }

    @Override
    public Optional<Request> getCurrentRequest(String id) {
        Optional<User> one = userRepository.findById(id);
        return one.flatMap(user -> requestRepository.findTopByUserEqualsCreateTime(user).stream().findFirst());
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
        requestInfo.setEstimatedWaitSeconds(request.getSecondsWaitEstimated());
        requestInfo.setRequestStatusType(request.getRequestStatusType());
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