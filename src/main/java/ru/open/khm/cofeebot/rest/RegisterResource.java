package ru.open.khm.cofeebot.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.open.khm.cofeebot.entity.User;
import ru.open.khm.cofeebot.service.RegistrationService;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class RegisterResource {

    @Autowired
    private final RegistrationService registrationService;

    public RegisterResource(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping(consumes = "application/json")
    @Transactional
    public Map<String, String> register(@Validated @RequestBody User userRegistration) {
        String registered = registrationService.register(userRegistration);
        HashMap<String, String> resultMap = new HashMap<>();
        resultMap.put("result", registered);
        return resultMap;
    }
}
