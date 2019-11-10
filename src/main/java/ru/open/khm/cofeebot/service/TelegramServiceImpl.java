package ru.open.khm.cofeebot.service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.User;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.service.telegram.TelegramBot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Lazy
public class TelegramServiceImpl implements TelegramService {
    private final Map<String, Integer> telegramUserIdsByUserNames = new HashMap<>();
    private final TelegramBot telegramBot;
    private final UserRepository userRepository;

    public TelegramServiceImpl(TelegramBot telegramBot, UserRepository userRepository) {
        this.telegramBot = telegramBot;
        this.userRepository = userRepository;
    }

    @Override
    public void sendRequestCreated(Request request, User user) {
        String telegramAccount = user.getTelegramAccount();
        if (telegramAccount != null) {
            telegramAccount = telegramAccount.toUpperCase();
        }
        Integer userId = telegramUserIdsByUserNames.get(telegramAccount);
        if (null == userId) {
            return;
        }
        SendMessage method = new SendMessage();
        method.setChatId(userId.longValue());
        method.setText("Вы создали заявку на подбор пары для кофе. " +
                "В чат будут выводиться предложения пары, а также вы можете отменить заявку командой /cancel");
        try {
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    @Override
    public void registerUserLink(org.telegram.telegrambots.meta.api.objects.User user) {
        telegramUserIdsByUserNames.put(user.getUserName().toUpperCase(), user.getId());
    }

    @Override
    public String getUserIdByTelegramUser(String userName) {
        Optional<User> byTelegramUsername = userRepository.findByTelegramUsername(userName.toUpperCase());
        return byTelegramUsername.map(User::getId).orElse(null);
    }

    @Override
    public void pairCreatedNotify(Pair pair) {
        sentPairNotifyForRequest(pair.getFirstRequest(), pair.getSecondRequest());
        sentPairNotifyForRequest(pair.getSecondRequest(), pair.getFirstRequest());
    }

    @Override
    public void notifyOtherRejected(Request request) {
        String telegramAccount = request.getUser().getTelegramAccount();
        if (telegramAccount != null) {
            telegramAccount = telegramAccount.toUpperCase();
        }
        Integer userId = telegramUserIdsByUserNames.get(telegramAccount);
        if (null == userId) {
            return;
        }
        SendMessage method = new SendMessage();
        method.setChatId(userId.longValue());
        method.setText("Ваша пара не согласовала вас, будем искать пару дальше...");
        try {
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    @Override
    public void notifyTimeoutFired(Request request, Pair pair) {
        String telegramAccount = request.getUser().getTelegramAccount();
        if (telegramAccount != null) {
            telegramAccount = telegramAccount.toUpperCase();
        }
        Integer userId = telegramUserIdsByUserNames.get(telegramAccount);
        if (null == userId) {
            return;
        }
        SendMessage method = new SendMessage();
        method.setChatId(userId.longValue());
        method.setText("Вы слишком долго думали над решением...Поищем еще.");
        try {
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    @Override
    public void notifyWaiting(Request request, int size) {
        String telegramAccount = request.getUser().getTelegramAccount();
        if (telegramAccount != null) {
            telegramAccount = telegramAccount.toUpperCase();
        }
        Integer userId = telegramUserIdsByUserNames.get(telegramAccount);
        if (null == userId) {
            return;
        }
        if (size == 1) {
            SendMessage method = new SendMessage();
            method.setChatId(userId.longValue());
            method.setText("Пока никого нет рядом с вами, подождем.");
            try {
                telegramBot.execute(method);
            } catch (TelegramApiException e) {
                log.error("Cannot send to telegram", e);
            }
        }
        if (request.getSecondsWaitEstimated() != null && request.getSecondsWaitEstimated() != 0) {
            int seconds = (request.getSecondsWaitEstimated() / 5) * 5;
            SendMessage method = new SendMessage();
            method.setChatId(userId.longValue());
            method.setText("Пожалуйста, подождите, предположительное время: " + seconds + " секунд");
            try {
                telegramBot.execute(method);
            } catch (TelegramApiException e) {
                log.error("Cannot send to telegram", e);
            }
        }
    }

    @Override
    public void pairAcceptedNotify(Pair pair) {
        sentPairAcceptedNotifyForRequest(pair.getFirstRequest(), pair.getSecondRequest());
        sentPairAcceptedNotifyForRequest(pair.getSecondRequest(), pair.getFirstRequest());
    }

    private void sentPairAcceptedNotifyForRequest(Request firstRequest, Request secondRequest) {
        Integer userId = telegramUserIdsByUserNames.get(firstRequest.getUser().getTelegramAccount());
        if (null == userId) {
            return;
        }

        try {
            SendMessage method = new SendMessage();
            method.setChatId(userId.longValue());
            StringBuilder sb = new StringBuilder();
            sb.append("Пара согласована!");
            if (secondRequest.getUser().getTelegramAccount() != null) {
                sb.append("Теперь вы и " + getUserDescription(secondRequest.getUser()) + " можете переписываться, посылая сообщения в чат! ");
            }
            sb.append("Приятного аппетита!");
            method.setText(sb.toString());
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    private void sentPairNotifyForRequest(Request request, Request pairRequest) {
        Integer userId = telegramUserIdsByUserNames.get(request.getUser().getTelegramAccount());
        if (null == userId) {
            return;
        }

        try {
            SendMessage method = new SendMessage();
            method.setChatId(userId.longValue());
            method.setText("Мы нашли вам пару! Это " + getDecriptionByRequest(pairRequest) + "! Вы готовы пойти пить кофе? (Да/Нет/ЧС(Черный список))");
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    @Override
    public void sendToAcceptedPair(User from, User to, String message) {
        Integer userId = telegramUserIdsByUserNames.get(to.getTelegramAccount());
        if (null == userId) {
            return;
        }

        try {
            SendMessage method = new SendMessage();
            method.setChatId(userId.longValue());
            StringBuilder sb = new StringBuilder();
            sb.append("Вам сообщение от <" + getUserDescription(from) + ">");
            sb.append(": " + message);
            method.setText(sb.toString());
            telegramBot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Cannot send to telegram", e);
        }
    }

    private String getUserDescription(User from) {
        return Strings.nullToEmpty(from.getFirstname())
                + " " + Strings.nullToEmpty(from.getMiddlename())
                + " " + Strings.nullToEmpty(from.getLastname());
    }

    private String getDecriptionByRequest(Request request) {
        StringBuilder sb = new StringBuilder();
        String name = request.getUser().getLastname() + " " + request.getUser().getFirstname() + " " + request.getUser().getMiddlename();
        sb.append(name + "\n");
        sb.append(" из отдела " + request.getUser().getDepartment() + "\n");
        sb.append(" место встречи: " + request.getPlace() + "\n");
        if (request.isCanPay()) {
            sb.append("Он даже готов угостить вас!");
        }
        return sb.toString();
    }
}
