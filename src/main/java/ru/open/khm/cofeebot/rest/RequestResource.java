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
    public ResponseEntity<String> createNew(@RequestBody RequestInput requestInput) {
        try {
            return ResponseEntity.ok(requestService.createNew(requestInput));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("Cause", e.getMessage()).build();
        }
    }

    @PostMapping(path = "{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable String id) {
        try {
            requestService.cancelRequest(id);
            return ResponseEntity.ok(id);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("Cause", e.getMessage())
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Cause", e.getMessage())
                    .build();
        }

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
