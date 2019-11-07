package ru.open.khm.cofeebot.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.open.khm.cofeebot.entity.RequestStatus;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.service.RequestService;
import ru.open.khm.cofeebot.service.SkippedException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/request")
public class RequestResource {

    private final RequestService requestService;

    public RequestResource(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createNew(@RequestBody RequestInput requestInput) {
        try {
            String registered = requestService.createNew(requestInput);
            HashMap<String, String> resultMap = new HashMap<>();
            resultMap.put("result", registered);
            return ResponseEntity.ok(resultMap);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).header("Cause", e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Cause", e.getMessage()).build();
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

    @PostMapping(path = "{id}/reject")
    public ResponseEntity<String> reject(@PathVariable String id) {
        try {
            requestService.rejectRequest(id, RequestStatusType.REJECTED);
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

    @PostMapping(path = "{id}/accept")
    public ResponseEntity<String> accept(@PathVariable String id) {
        try {
            requestService.acceptRequest(id);
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
