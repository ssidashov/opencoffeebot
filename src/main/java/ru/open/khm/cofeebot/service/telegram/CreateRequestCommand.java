package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.rest.RequestInput;
import ru.open.khm.cofeebot.service.request.RequestService;
import ru.open.khm.cofeebot.service.TelegramService;

import java.util.Optional;

@Slf4j
public class CreateRequestCommand extends ChatBotCommand  {
    private final TelegramService telegramService;
    private final UserRepository userRepository;
    private RequestService requestService;

    public CreateRequestCommand(TelegramService telegramService
            , UserRepository userRepository
            , RequestService requestService) {
        super("iwantcoffee", "Create request for pair");
        this.telegramService = telegramService;
        this.userRepository = userRepository;
        this.requestService = requestService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        log.info("User " + user.getUserName());
        telegramService.registerUserLink(user);
        String userName = user.getUserName();
        String userIdByTelegramUser = telegramService.getUserIdByTelegramUser(userName);
        if (null == userIdByTelegramUser) {
            sentNoUser(chat, absSender, user);
            return;
        }
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
}
