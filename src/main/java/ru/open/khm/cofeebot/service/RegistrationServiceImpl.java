package ru.open.khm.cofeebot.service;

import org.springframework.stereotype.Service;
import ru.open.khm.cofeebot.entity.User;
import ru.open.khm.cofeebot.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;

    public RegistrationServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String register(User userRegistration) {
        if (userRegistration.getTelegramAccount() != null) {
            userRegistration.setTelegramAccount(userRegistration.getTelegramAccount().toUpperCase());
        }
        Optional<User> foundRegistered = userRepository.getByLogin(userRegistration.getLogin());
        return foundRegistered.map(user -> {
            userRegistration.setId(user.getId());
            userRegistration.setRegistrationTime(user.getRegistrationTime());
            userRegistration.setUpdateTime(Instant.now());
            userRepository.save(userRegistration);
            return user.getId();
        }).orElseGet(() -> {
            userRegistration.setRegistrationTime(Instant.now());
            User user = userRepository.save(userRegistration);
            return user.getId();
        });
    }
}
