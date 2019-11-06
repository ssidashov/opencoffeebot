package ru.open.khm.cofeebot.entity;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Data
@ToString
public class User {
    @Id
    @GenericGenerator(name = "UserGenerator", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "UserGenerator")
    private String id;

    @NotNull
    private String login;

    @NotNull
    private String firstname;

    private String middlename;

    @NotNull
    private String lastname;

    private Instant registrationTime;

    private Instant updateTime;

    private String location;

    private String workplace;

    private String department;

    private String position;

    private String telegramAccount;
}
