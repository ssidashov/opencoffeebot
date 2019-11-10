package ru.open.khm.cofeebot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import ru.open.khm.cofeebot.service.telegram.*;

import java.util.function.Supplier;

@Configuration
@Slf4j
public class TelegramConfiguration {
    private final CofeebotProperties cofeebotProperties;
    private final ApplicationContext applicationContext;

    public TelegramConfiguration(CofeebotProperties cofeebotProperties, ApplicationContext applicationContext) {
        this.cofeebotProperties = cofeebotProperties;
        this.applicationContext = applicationContext;
    }

    @Bean
    public TelegramBot telegramBot() {
        log.info("Initializing API context...");
        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        log.info("Configuring bot options...");
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);

        return new TelegramBot(botOptions
                , commandFactorySupplier()
                , this::textCommandHandler
                , botsApi
                , cofeebotProperties);
    }

    @Bean
    public TextCommandHandler textCommandHandler() {
        return new TextCommandHandlerImpl();
    }

    @Bean
    public Supplier<CommandFactory> commandFactorySupplier() {
        return () -> applicationContext.getBean(CommandFactory.class);
    }
}