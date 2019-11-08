package ru.open.khm.cofeebot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cofeebot")
@Data
public class CofeebotProperties {
    private double stagingPersonCountMultipler;
    private double stagingWaitTimeMultiplier;
    private long timerDelay;
    private long pairAcceptTimeoutSeconds;
    private long maxTimeoutCount;
    private String botName;
    private String botToken;
}
