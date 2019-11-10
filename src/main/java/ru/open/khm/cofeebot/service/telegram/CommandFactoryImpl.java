package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.service.TelegramService;
import ru.open.khm.cofeebot.service.request.RequestService;

@Slf4j
@Component
public class CommandFactoryImpl implements CommandFactory {
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public CreateRequestCommand createCreateRequestCommand() {
        return new CreateRequestCommand(() -> applicationContext.getBean(TelegramService.class)
                , () -> applicationContext.getBean(UserRepository.class)
                , () -> applicationContext.getBean(RequestService.class));
    }

    @Override
    public StartCommand createStartCommand() {
        return new StartCommand((() -> applicationContext.getBean(TelegramService.class))
                , () -> applicationContext.getBean(UserRepository.class)
                , () -> applicationContext.getBean(RequestService.class));
    }

    @Override
    public CancelCommand createCancelCommand() {
        return new CancelCommand(() -> applicationContext.getBean(TelegramService.class)
                , () -> applicationContext.getBean(RequestService.class));
    }
}
