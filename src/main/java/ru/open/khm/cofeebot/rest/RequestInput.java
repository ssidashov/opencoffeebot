package ru.open.khm.cofeebot.rest;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RequestInput {
    @NotNull
    private String userId;
    private boolean canPay;
    private boolean myPlace;
    private Integer maxWaitTime;
}
