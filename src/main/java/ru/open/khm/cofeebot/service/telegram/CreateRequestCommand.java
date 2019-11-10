package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Supplier;

@Slf4j
public class CreateRequestCommand extends ChatBotCommand  {
    public static final String COMMAND_IDENTIFIER = "iwantcoffee";
    public static final String COMMAND_DESCRIPTION = "Create request for pair";
    private final Supplier<TelegramService> telegramServiceFactory;
    private final Supplier<UserRepository> userRepositoryFactory;
    private final Supplier<RequestService> requestServiceFactory;

    public CreateRequestCommand(Supplier<TelegramService> telegramServiceFactory
            , Supplier<UserRepository> userRepositoryFactory
            , Supplier<RequestService> requestServiceFactory) {
        super(COMMAND_IDENTIFIER, COMMAND_DESCRIPTION);
        this.userRepositoryFactory = userRepositoryFactory;
        this.requestServiceFactory = requestServiceFactory;
        this.telegramServiceFactory = telegramServiceFactory;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        log.info("User " + user.getUserName());
        TelegramService telegramService = telegramServiceFactory.get();
        telegramService.registerUserLink(user);
        String userName = user.getUserName();
        String userIdByTelegramUser = telegramService.getUserIdByTelegramUser(userName);
        if (null == userIdByTelegramUser) {
            sentNoUser(chat, absSender, user);
            return;
        }
        Optional<Request> requestByUserId = requestServiceFactory.get().getInProcessRequestByUserId(userIdByTelegramUser);
        requestByUserId.ifPresent(request -> {
            sentAlreadyHasRequest(chat, absSender, user);
        });
        if(requestByUserId.isEmpty()){
            RequestInput requestInput = new RequestInput();
            ru.open.khm.cofeebot.entity.User cofeebotUser = userRepositoryFactory.get().getOne(userIdByTelegramUser);
            requestInput.setUserId(cofeebotUser.getId());
            String requestId = requestServiceFactory.get().createNew(requestInput);
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
