package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.open.khm.cofeebot.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> getByLogin(String login);

    @Query("select u from User u where u.telegramAccount = :username")
    Optional<User> findByTelegramUsername(String username);
}
