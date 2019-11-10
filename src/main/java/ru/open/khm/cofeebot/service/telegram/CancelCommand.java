package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.service.TelegramService;
import ru.open.khm.cofeebot.service.request.RequestService;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class CancelCommand extends ChatBotCommand {
    public static final String COMMAND_IDENTIFIER = "cancel";
    public static final String COMMAND_DESCRIPTION = "Cancel current coffee request";
    private final Supplier<TelegramService> telegramServiceFactory;
    private final Supplier<RequestService> requestServiceFactory;

    public CancelCommand(Supplier<TelegramService> telegramServiceFactory, Supplier<RequestService> requestServiceFactory) {
        super(COMMAND_IDENTIFIER, COMMAND_DESCRIPTION);
        this.requestServiceFactory = requestServiceFactory;
        this.telegramServiceFactory = telegramServiceFactory;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        RequestService requestService = requestServiceFactory.get();
        String userName = user.getUserName();
        log.info("User " + user.getUserName());
        TelegramService telegramService = telegramServiceFactory.get();
        telegramService.registerUserLink(user);
        String userIdByTelegramUser = telegramService.getUserIdByTelegramUser(userName);
        if (null == userIdByTelegramUser) {
            return;
        }
        Optional<Request> requestByUserId = requestService.getInProcessRequestByUserId(userIdByTelegramUser);
        requestByUserId.ifPresent(request -> {
            requestService.cancelRequest(request.getId());
            SendMessage message = new SendMessage();
            message.setChatId(chat.getId().toString());
            message.setText("Заявка отменена!");
            execute(absSender, message, user);
        });
        if(requestByUserId.isEmpty()){
            SendMessage message = new SendMessage();
            message.setChatId(chat.getId().toString());
            message.setText("Заявки нет, но вы всегда можете её создать!");
            execute(absSender, message, user);
        }
    }
}
