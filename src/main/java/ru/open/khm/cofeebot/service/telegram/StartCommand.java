package ru.open.khm.cofeebot.service.telegram;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.service.RequestService;
import ru.open.khm.cofeebot.service.TelegramService;

import java.util.Optional;

@Slf4j
public class StartCommand extends ChatBotCommand {
    private final ApplicationContext context;

    public StartCommand(ApplicationContext context) {
        super("start", "Start using bot");
        this.context = context;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        TelegramService telegramService = getTelegramService();
        log.info("User " + user.getUserName());
        telegramService.registerUserLink(user);
        SendMessage message = new SendMessage();
        message.setChatId(chat.getId().toString());

        boolean isRequestActive = false;
        String username = null;
        String userIdByTelegramUser = getTelegramService().getUserIdByTelegramUser(user.getUserName());
        if (null != userIdByTelegramUser) {
            ru.open.khm.cofeebot.entity.User cofeebotUser = context.getBean(UserRepository.class).findById(userIdByTelegramUser).orElseThrow(() -> new IllegalArgumentException("No user for user"));
            username = Strings.nullToEmpty(cofeebotUser.getFirstname()) + " " + Strings.nullToEmpty(cofeebotUser.getMiddlename()) + " " + Strings.nullToEmpty(cofeebotUser.getLastname());
            Optional<Request> requestByUserId = getRequestService().getInProcessRequestByUserId(userIdByTelegramUser);
            if (requestByUserId.isPresent()) {
                isRequestActive = true;
            };
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Доброе время суток");
        if (username == null) {
            sb.append(". Мы вас пока не знаем, но вы можете зарегистрироваться на https://net.open.ru");
        }else{
            sb.append("! Вас приветствует CofeeBot!" + (isRequestActive ? " У вас есть активная заявка на подбор пары!" : ""));
            if (!isRequestActive) {
                sb.append(" Наберите /iwantcoffee для поиска пары");
            }
        }
        message.setText(sb.toString());
        execute(absSender, message, user);
    }

    private TelegramService getTelegramService() {
        return context.getBean(TelegramService.class);
    }

    private RequestService getRequestService() {
        return context.getBean(RequestService.class);
    }
}
