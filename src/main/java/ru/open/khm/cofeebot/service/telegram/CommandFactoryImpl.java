package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.repository.UserRepository;
import ru.open.khm.cofeebot.service.TelegramService;
import ru.open.khm.cofeebot.service.request.RequestService;

@Slf4j
public class CommandFactoryImpl implements CommandFactory {
    @Autowired
    private TelegramService telegramService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private PairRepository pairRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CreateRequestCommand createCreateRequestCommand() {
        return new CreateRequestCommand(telegramService, userRepository, requestService);
    }

    @Override
    public StartCommand createStartCommand() {
        return new StartCommand(telegramService, userRepository, requestService);
    }

    @Override
    public CancelCommand createCancelCommand() {
        return new CancelCommand(telegramService, requestService);
    }

    public void setTelegramService(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    public void setRequestService(RequestService requestService) {
        this.requestService = requestService;
    }

    public void setPairRepository(PairRepository pairRepository) {
        this.pairRepository = pairRepository;
    }
}
