package ru.open.khm.cofeebot.rest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.open.khm.cofeebot.entity.BlackListRecord;
import ru.open.khm.cofeebot.repository.BlacklistRepository;

import java.util.List;

@Service
public class BlackListServiceImpl implements BlackListService {
    private final BlacklistRepository blacklistRepository;

    public BlackListServiceImpl(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    @Transactional
    public void clearByUser(String userId) {
        List<BlackListRecord> byUserId = blacklistRepository.findByUserId(userId);
        byUserId.forEach(blacklistRepository::delete);
    }
}
