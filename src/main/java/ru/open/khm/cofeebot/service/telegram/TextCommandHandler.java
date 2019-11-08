package ru.open.khm.cofeebot.service.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

public interface TextCommandHandler {
    void handle(Update update, Consumer<SendMessage> sender);
}
