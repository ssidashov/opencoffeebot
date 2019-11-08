package ru.open.khm.cofeebot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.open.khm.cofeebot.service.telegram.*;

@Configuration
@Slf4j
public class TelegramConfiguration {
    @Autowired
    private final ApplicationContext applicationContext;
    private final CofeebotProperties cofeebotProperties;

    public TelegramConfiguration(ApplicationContext applicationContext
            , CofeebotProperties cofeebotProperties) {
        this.applicationContext = applicationContext;
        this.cofeebotProperties = cofeebotProperties;
    }

    @Bean
    public TelegramBot telegramBot() {
        try {
            log.info("Initializing API context...");
            ApiContextInitializer.init();

            TelegramBotsApi botsApi = new TelegramBotsApi();

            log.info("Configuring bot options...");
            DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);

            log.info("Registering TelegramBot...");
            TelegramBot bot = new TelegramBot(botOptions, textCommandHandler(), cofeebotProperties);
            bot.register(startCommand());
            bot.register(cancelCommand());
            bot.register(createRequestCommand());
            botsApi.registerBot(bot);

            log.info("TelegramBot bot is ready for work!");
            return bot;
        } catch (TelegramApiRequestException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public TextCommandHandler textCommandHandler() {
        return new TextCommandHandlerImpl(applicationContext);
    }

    @Bean
    public ChatBotCommand cancelCommand() {
        return new CancelCommand(applicationContext);
    }

    @Bean
    public ChatBotCommand startCommand() {
        return new StartCommand(applicationContext);
    }

    @Bean
    public CreateRequestCommand createRequestCommand() {
        return new CreateRequestCommand(applicationContext);
    }
}
