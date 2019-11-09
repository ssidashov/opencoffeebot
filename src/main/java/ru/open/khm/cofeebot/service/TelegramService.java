package ru.open.khm.cofeebot.service;

import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.User;

public interface TelegramService {
    void sendRequestCreated(Request request, User user);

    void registerUserLink(org.telegram.telegrambots.meta.api.objects.User user);

    String getUserIdByTelegramUser(String telegramUserName);

    void pairCreatedNotify(Pair pair);

    void notifyOtherRejected(Request request);

    void notifyTimeoutFired(Request request, Pair pair);

    void notifyWaiting(Request request, int size);

    void pairAcceptedNotify(Pair pair);

    void sendToAcceptedPair(User from, User to, String message);
}
