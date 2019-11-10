package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.open.khm.cofeebot.CofeebotProperties;

import javax.annotation.PostConstruct;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class TelegramBot extends TelegramLongPollingCommandBot {
    private final TelegramBotsApi telegramBotsApi;
    private final CofeebotProperties cofeebotProperties;
    private final Supplier<CommandFactory> commandFactorySupplier;
    private final Supplier<TextCommandHandler> textCommandHandlerFactory;

    public TelegramBot(DefaultBotOptions botOptions
            , Supplier<CommandFactory> commandFactorySupplier
            , Supplier<TextCommandHandler> textCommandHandlerFactory
            , TelegramBotsApi telegramBotsApi
            , CofeebotProperties cofeebotProperties) {
        super(botOptions, cofeebotProperties.getBotName());
        this.cofeebotProperties = cofeebotProperties;
        this.telegramBotsApi = telegramBotsApi;
        this.commandFactorySupplier = commandFactorySupplier;
        this.textCommandHandlerFactory = textCommandHandlerFactory;
    }

    @Override
    public String getBotToken() {
        return cofeebotProperties.getBotToken();
    }

    // обработка сообщения не начинающегося с '/'
    @Override
    public void processNonCommandUpdate(Update update) {
        Consumer<SendMessage> sender = (SendMessage s) -> {
            try {
                this.execute(s);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        };

        textCommandHandlerFactory.get().handle(update, sender);
    }

    @PostConstruct
    public void init() throws TelegramApiRequestException {
        CommandFactory commandFactory = commandFactorySupplier.get();
        this.register(commandFactory.createStartCommand());
        this.register(commandFactory.createCancelCommand());
        this.register(commandFactory.createCreateRequestCommand());

        // обработка неизвестной команды
        log.info("Registering default action'...");
        registerDefaultAction(((absSender, message) -> {
            log.warn("User {} is trying to execute unknown command '{}'.", message.getFrom().getId(), message.getText());

            SendMessage text = new SendMessage();
            text.setChatId(message.getChatId());
            text.setText(message.getText() + " command not found!");

            try {
                absSender.execute(text);
            } catch (TelegramApiException e) {
                log.error("Error while replying unknown command to user {}.", message.getFrom(), e);
            }
        }));

        telegramBotsApi.registerBot(this);

        log.info("Registering TelegramBot...");
        log.info("TelegramBot bot is ready for work!");
    }
}
