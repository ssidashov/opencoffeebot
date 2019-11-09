package ru.open.khm.cofeebot.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistResource {
    private final BlackListService blackListService;

    public BlacklistResource(BlackListService blackListService) {
        this.blackListService = blackListService;
    }

    @PostMapping(path = "{userId}/clearBlacklist")
    public void clearBlacklist(String userId) {
        blackListService.clearByUser(userId);
    }
}
