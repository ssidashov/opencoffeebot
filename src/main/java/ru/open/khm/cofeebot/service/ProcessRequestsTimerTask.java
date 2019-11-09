package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.service.request.RequestProcessingService;
import ru.open.khm.cofeebot.service.request.TimeoutRequestsService;

@Service
@Slf4j
public class ProcessRequestsTimerTask {
    private final RequestProcessingService requestProcessingService;
    private final TimeoutRequestsService timeoutRequestsService;

    public ProcessRequestsTimerTask(RequestProcessingService requestProcessingService, TimeoutRequestsService timeoutRequestsService) {
        this.requestProcessingService = requestProcessingService;
        this.timeoutRequestsService = timeoutRequestsService;
    }

    @Scheduled(fixedDelayString = "${cofeebot.timerDelay}")
    public void check() {
        log.info("Timer fired");
        requestProcessingService.processRequests();
        timeoutRequestsService.timeoutRequests();
    }
}
