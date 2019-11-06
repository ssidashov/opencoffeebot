package ru.open.khm.cofeebot;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories("ru.open.khm.cofeebot.repository")
public class JpaConfiguration {
}
