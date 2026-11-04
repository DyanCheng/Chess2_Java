package com.chess2.repository;

import com.chess2.domain.UserCard;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCardRepository extends JpaRepository<UserCard, UUID> {
    Optional<UserCard> findByUserIdAndCardId(UUID userId, UUID cardId);
    java.util.List<UserCard> findByUserId(UUID userId);
}
