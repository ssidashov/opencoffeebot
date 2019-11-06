package ru.open.khm.cofeebot.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.open.khm.cofeebot.entity.RequestStatus;
import ru.open.khm.cofeebot.service.RequestService;
import ru.open.khm.cofeebot.service.SkippedException;

@RestController
@RequestMapping("/api/request")
public class RequestResource {

    private final RequestService requestService;

    public RequestResource(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public String createNew(@RequestBody RequestInput requestInput) {
        return requestService.createNew(requestInput);
    }

    @PostMapping(path = "{id}/cancel")
    public String cancel(@PathVariable String id) {
        requestService.cancelRequest(id);
        return id;
    }

    @GetMapping(path = "{id}/status")
    public ResponseEntity<RequestStatus> status(@PathVariable String id) {
        try {
            return ResponseEntity.ok(requestService.getRequestStatus(id));
        } catch (SkippedException e) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, "/api/request/" + e.getNewId() + "/status").build();
        }
    }
}
