package ru.open.khm.cofeebot.entity;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Data
@ToString
public class Request {

    @Id
    @GenericGenerator(name = "RequestGenerator", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "RequestGenerator")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Instant createTime;

    private Instant originalCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    private Request original;

    @Enumerated(EnumType.STRING)
    private RequestStatusType requestStatusType = RequestStatusType.CREATED;

    private boolean canPay = false;

    private boolean myPlace;

    private String location;

    private String place;

    private Integer maxWaitSeconds;
    private Integer sameoLocationRequestCount;
    private Integer secondsWaitEstimated;

    public void setActiveRequestStats(int size) {
        this.sameoLocationRequestCount = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
