package ru.open.khm.cofeebot.service.telegram;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.service.TelegramService;
import ru.open.khm.cofeebot.service.request.RequestService;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class StartCommand extends ChatBotCommand {
    public static final String COMMAND_IDENTIFIER = "start";
    public static final String COMMAND_DESCRIPTION = "Start using bot";
    private final Supplier<TelegramService> telegramServiceFactory;
    private final Supplier<UserRepository> userRepositoryFactory;
    private final Supplier<RequestService> requestServiceFactory;

    public StartCommand(Supplier<TelegramService> telegramServiceFactory
            , Supplier<UserRepository> userRepositoryFactory
            , Supplier<RequestService> requestServiceFactory) {
        super(COMMAND_IDENTIFIER, COMMAND_DESCRIPTION);
        this.telegramServiceFactory = telegramServiceFactory;
        this.userRepositoryFactory = userRepositoryFactory;
        this.requestServiceFactory = requestServiceFactory;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        UserRepository userRepository = userRepositoryFactory.get();
        RequestService requestService = requestServiceFactory.get();
        log.info("User " + user.getUserName());;
        TelegramService telegramService = telegramServiceFactory.get();
        telegramService.registerUserLink(user);
        SendMessage message = new SendMessage();
        message.setChatId(chat.getId().toString());

        boolean isRequestActive = false;
        String username = null;
        String userIdByTelegramUser = telegramService.getUserIdByTelegramUser(user.getUserName());
        if (null != userIdByTelegramUser) {
            ru.open.khm.cofeebot.entity.User cofeebotUser = userRepository.findById(userIdByTelegramUser).orElseThrow(() -> new IllegalArgumentException("No user for user"));
            username = Strings.nullToEmpty(cofeebotUser.getFirstname()) + " " + Strings.nullToEmpty(cofeebotUser.getMiddlename()) + " " + Strings.nullToEmpty(cofeebotUser.getLastname());
            Optional<Request> requestByUserId = requestService.getInProcessRequestByUserId(userIdByTelegramUser);
            if (requestByUserId.isPresent()) {
                isRequestActive = true;
            }
            ;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Доброе время суток");
        if (username == null) {
            sb.append(". Мы вас пока не знаем, но вы можете зарегистрироваться на https://net.open.ru");
        } else {
            sb.append("! Вас приветствует CofeeBot!" + (isRequestActive ? " У вас есть активная заявка на подбор пары!" : ""));
            if (!isRequestActive) {
                sb.append(" Наберите /iwantcoffee для поиска пары");
            }
        }
        message.setText(sb.toString());
        execute(absSender, message, user);
    }
}
