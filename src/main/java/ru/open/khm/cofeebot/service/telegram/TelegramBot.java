package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.open.khm.cofeebot.CofeebotProperties;

import java.util.function.Consumer;

@Slf4j
public class TelegramBot extends TelegramLongPollingCommandBot {
    private final TextCommandHandler textCommandHandler;
    private final CofeebotProperties cofeebotProperties;

    public TelegramBot(DefaultBotOptions botOptions
            , TextCommandHandler textCommandHandler
            , CofeebotProperties cofeebotProperties) {
        super(botOptions, cofeebotProperties.getBotName());
        this.textCommandHandler = textCommandHandler;
        this.cofeebotProperties = cofeebotProperties;
        log.info("Initializing Bot...");

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
    }

    @Override
    public String getBotToken() {
        return cofeebotProperties.getBotToken();
    }

    // обработка сообщения не начинающегося с '/'
    @Override
    public void processNonCommandUpdate(Update update) {
        Consumer<SendMessage> sender = (SendMessage s) -> {
            try{
                this.execute(s);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        };

        textCommandHandler.handle(update, sender);

//        log.info("Processing non-command update...");
//
//        if (!update.hasMessage()) {
//            log.error("Update doesn't have a body!");
//            throw new IllegalStateException("Update doesn't have a body!");
//        }
//
//        Message msg = update.getMessage();
//        User user = msg.getFrom();
//
//        log.info("PROCESSING MESSAGE", user.getId());
//
//        if (!canSendMessage(user, msg)) {
//            return;
//        }
//
//        String clearMessage = msg.getText();
//        String messageForUsers = String.format("%s:\n%s", mAnonymouses.getDisplayedName(user), msg.getText());
//
//        SendMessage answer = new SendMessage();
//
//        // отправка ответа отправителю о том, что его сообщение получено
//        answer.setText(clearMessage);
//        answer.setChatId(msg.getChatId());
//        replyToUser(answer, user, clearMessage);
//
//        // отправка сообщения всем остальным пользователям бота
//        answer.setText(messageForUsers);
//        Stream<Anonymous> anonymouses = mAnonymouses.anonymouses();
//        anonymouses.filter(a -> !a.getUser().equals(user))
//                .forEach(a -> {
//                    answer.setChatId(a.getChat().getId());
//                    sendMessageToUser(answer, a.getUser(), user);
//                });
    }
}
