package com.chess2.repository;

import com.chess2.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findTop20ByOrderByRatingDescExperienceDesc();
}
