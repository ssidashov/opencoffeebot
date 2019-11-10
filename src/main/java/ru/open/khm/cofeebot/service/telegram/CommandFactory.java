package ru.open.khm.cofeebot.service.telegram;

import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;

public interface CommandFactory {
    CreateRequestCommand createCreateRequestCommand();

    StartCommand createStartCommand();

    CancelCommand createCancelCommand();
}
