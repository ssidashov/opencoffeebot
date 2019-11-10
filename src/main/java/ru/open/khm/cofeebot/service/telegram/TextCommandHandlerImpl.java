package ru.open.khm.cofeebot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.open.khm.cofeebot.entity.Pair;
import ru.open.khm.cofeebot.entity.PairStatus;
import ru.open.khm.cofeebot.entity.Request;
import ru.open.khm.cofeebot.entity.RequestStatusType;
import ru.open.khm.cofeebot.repository.PairRepository;
import ru.open.khm.cofeebot.service.TelegramService;
import ru.open.khm.cofeebot.service.request.RequestService;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TextCommandHandlerImpl implements TextCommandHandler {
    @Autowired
    private PairRepository pairRepository;

    @Autowired
    private TelegramService telegramService;

    @Autowired
    private RequestService requestService;

    @Override
    @Transactional
    public void handle(Update update, Consumer<SendMessage> sender) {
        if (update.getMessage() == null) {
            return;
        }
        Request currentRequest = getCurrentRequest(update.getMessage().getFrom());
        boolean waitDecision = isWaitDecision(currentRequest);
        if (waitDecision) {
            handleDecisionMessage(update, currentRequest, sender);
            return;
        }
        Optional<ru.open.khm.cofeebot.entity.User> currentRequestPaired = getUserPaired(currentRequest);
        if (currentRequestPaired.isPresent()) {
            handleChat(update
                    , currentRequest
                    , currentRequestPaired.orElseThrow(() -> new IllegalStateException("No user in pair"))
                    , update.getMessage().getText());
            return;
        }

        sendWrongMessage(update, sender);
    }

    private Optional<ru.open.khm.cofeebot.entity.User> getUserPaired(Request currentRequest) {
        if (currentRequest == null || currentRequest.getRequestStatusType() != RequestStatusType.ACCEPTED) {
            return Optional.empty();
        }
        Optional<Pair> byFirstRequestEqualsOrSecondRequestEquals = pairRepository.findByFirstRequestEqualsOrSecondRequestEquals(currentRequest, currentRequest);
        if (byFirstRequestEqualsOrSecondRequestEquals.isEmpty() || byFirstRequestEqualsOrSecondRequestEquals.get().getPairStatus() != PairStatus.ACCEPTED) {
            return Optional.empty();
        }
        Pair pair = byFirstRequestEqualsOrSecondRequestEquals.get();
        boolean isFirst = currentRequest.getId().equals(pair.getFirstRequest().getId());
        if (isFirst) {
            ru.open.khm.cofeebot.entity.User user = pair.getSecondRequest().getUser();
            return Optional.of(user);
        } else {
            ru.open.khm.cofeebot.entity.User user = pair.getFirstRequest().getUser();
            return Optional.of(user);
        }
    }

    private void sendWrongMessage(Update update, Consumer<SendMessage> sender) {
        SendMessage answer = new SendMessage();

        // отправка ответа отправителю о том, что его сообщение получено
        answer.setText("Неопознанное сообщение.");
        answer.setChatId(update.getMessage().getChatId());

        sender.accept(answer);
    }

    private void handleChat(Update update, Request currentRequest, ru.open.khm.cofeebot.entity.User user, String message) {
        if (user.getTelegramAccount() != null) {
            telegramService.sendToAcceptedPair(currentRequest.getUser(), user, message);
        }
    }

    private void handleDecisionMessage(Update update, Request waitDecision, Consumer<SendMessage> sender) {
        String message = update.getMessage().getText().trim().toUpperCase();
        boolean isOk = false;
        switch (message) {
            case "ДА":
                requestService.acceptRequest(waitDecision.getId());
                isOk = true;
                break;
            case "НЕТ":
                requestService.rejectRequest(waitDecision.getId(), RequestStatusType.REJECTED);
                isOk = true;
                break;
            case "ЧС":
                requestService.rejectRequest(waitDecision.getId(), RequestStatusType.REJECTED_BLACKLIST);
                isOk = true;
                break;
        }
        if (isOk) {
            SendMessage answer = new SendMessage();

            // отправка ответа отправителю о том, что его сообщение получено
            answer.setText("Ваш ответ принят");
            answer.setChatId(update.getMessage().getChatId());

            sender.accept(answer);
        }
    }

    private boolean isWaitDecision(Request currentRequest) {
        return currentRequest == null || currentRequest.getRequestStatusType() == RequestStatusType.PAIRED;
    }

    private Request getCurrentRequest(User user) {
        String userName = user.getUserName();
        log.info("User " + user.getUserName());
        telegramService.registerUserLink(user);
        String userIdByTelegramUser = telegramService.getUserIdByTelegramUser(userName);
        if (null == userIdByTelegramUser) {
            return null;
        }
        Optional<Request> requestByUserId = requestService.getCurrentRequest(userIdByTelegramUser);

        return requestByUserId.orElse(null);
    }
}
