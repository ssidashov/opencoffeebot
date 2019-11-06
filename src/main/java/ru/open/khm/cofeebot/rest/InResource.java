package ru.open.khm.cofeebot.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
public class InResource {

    @GetMapping("in")
    public String doIn(HttpServletRequest request,
                       HttpServletResponse response) {
        String key = request.getHeader("key");
        log.info("key:" + key);
        return "ok";
    }
}
