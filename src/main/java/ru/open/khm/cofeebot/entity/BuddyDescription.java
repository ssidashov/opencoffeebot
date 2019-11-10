package ru.open.khm.cofeebot.entity;

import lombok.Data;

@Data
public class BuddyDescription {
    private String login;

    private String firstname;

    private String middlename;

    private String lastname;

    private String department;

    private String position;

    private boolean canPay;

    private String place;
}
