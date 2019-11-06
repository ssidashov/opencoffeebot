package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.open.khm.cofeebot.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
     Optional<User> getByLogin(String login);
}
