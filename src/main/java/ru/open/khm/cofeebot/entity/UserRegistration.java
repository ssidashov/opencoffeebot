package ru.open.khm.cofeebot.entity;

import lombok.Data;

import java.time.Instant;

@Data
public class UserRegistration {
    private String login;

    private String firstname;

    private String middlename;

    private String lastname;

    private Instant registrationTime;

    private String location;

    private String place;

    private String department;

    private String position;

    private String telegramId;
}
