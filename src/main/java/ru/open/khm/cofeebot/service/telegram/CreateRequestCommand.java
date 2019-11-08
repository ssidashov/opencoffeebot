package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.rest.RequestInput;
import ru.open.khm.cofeebot.service.RequestService;
import ru.open.khm.cofeebot.service.TelegramService;

import java.util.Optional;

@Slf4j
public class CreateRequestCommand extends ChatBotCommand  {
    private final ApplicationContext context;

    public CreateRequestCommand(ApplicationContext context) {
        super("iwantcoffee", "Create request for pair");
        this.context = context;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        TelegramService telegramService = getTelegramService();
        log.info("User " + user.getUserName());
        telegramService.registerUserLink(user);
        String userName = user.getUserName();
        String userIdByTelegramUser = getTelegramService().getUserIdByTelegramUser(userName);
        UserRepository userRepository = context.getBean(UserRepository.class);
        if (null == userIdByTelegramUser) {
            sentNoUser(chat, absSender, user);
            return;
        }
        RequestService requestService = getRequestService();
        Optional<Request> requestByUserId = requestService.getInProcessRequestByUserId(userIdByTelegramUser);
        requestByUserId.ifPresent(request -> {
            sentAlreadyHasRequest(chat, absSender, user);
        });
        if(requestByUserId.isEmpty()){
            RequestInput requestInput = new RequestInput();
            ru.open.khm.cofeebot.entity.User cofeebotUser = userRepository.getOne(userIdByTelegramUser);
            requestInput.setUserId(cofeebotUser.getId());
            String requestId = requestService.createNew(requestInput);
        }

    }

    private void sentAlreadyHasRequest(Chat chat, AbsSender absSender, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chat.getId().toString());
        message.setText("У вас уже есть заявка. Но вы можете её отменить командой /cancel");
        execute(absSender, message, user);
    }

    private void sentNoUser(Chat chat, AbsSender absSender, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chat.getId().toString());
        message.setText("Мы не знаем кто вы! Пожалайста, зарегистрируйтесть на https://net.open.ru");
        execute(absSender, message, user);
    }

    private TelegramService getTelegramService() {
        return context.getBean(TelegramService.class);
    }

    private RequestService getRequestService() {
        return context.getBean(RequestService.class);
    }
}
