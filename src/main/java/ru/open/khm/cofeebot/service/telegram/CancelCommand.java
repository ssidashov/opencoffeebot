package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.service.request.RequestService;
import ru.open.khm.cofeebot.service.TelegramService;

import java.util.Optional;

@Slf4j
public class CancelCommand extends ChatBotCommand {
    private final ApplicationContext context;

    public CancelCommand(ApplicationContext context) {
        super("cancel", "Cancel current coffee request");
        this.context = context;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String userName = user.getUserName();
        TelegramService telegramService = getTelegramService();
        log.info("User " + user.getUserName());
        telegramService.registerUserLink(user);
        String userIdByTelegramUser = getTelegramService().getUserIdByTelegramUser(userName);
        if (null == userIdByTelegramUser) {
            return;
        }
        Optional<Request> requestByUserId = getRequestService().getInProcessRequestByUserId(userIdByTelegramUser);
        requestByUserId.ifPresent(request -> {
            getRequestService().cancelRequest(request.getId());
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

    private TelegramService getTelegramService() {
        return context.getBean(TelegramService.class);
    }

    private RequestService getRequestService() {
        return context.getBean(RequestService.class);
    }
}
