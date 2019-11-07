package ru.open.khm.cofeebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.open.khm.cofeebot.entity.BlackListRecord;
import ru.open.khm.cofeebot.entity.User;

import java.util.List;

public interface BlacklistRepository extends JpaRepository<BlackListRecord, String> {
    List<BlackListRecord> getAllByIssuer(User issuer);

    @Query("select count(p) from BlackListRecord p where (p.issuer = :user1 and p.blacklisted = :user2) or (p.issuer = :user2 and p.blacklisted = :user1)")
    int countByIssuerAndBlacklistedOrReverse(User user1, User user2);
}
