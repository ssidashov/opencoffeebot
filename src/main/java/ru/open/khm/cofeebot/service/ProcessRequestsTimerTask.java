package ru.open.khm.cofeebot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProcessRequestsTimerTask {
    private final RequestProcessingService requestProcessingService;

    public ProcessRequestsTimerTask(RequestProcessingService requestProcessingService) {
        this.requestProcessingService = requestProcessingService;
    }

    @Scheduled(fixedDelayString = "${cofeebot.timerDelay}")
    public void check() {
        log.info("Timer accepted");
        requestProcessingService.processRequests();
    }
}
