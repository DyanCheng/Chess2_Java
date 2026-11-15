package com.chess2.api;

import com.chess2.domain.UserAccount;
import com.chess2.dto.AuthDtos;
import com.chess2.repository.UserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final UserRepository users;
    public LeaderboardController(UserRepository users) { this.users = users; }

    @GetMapping
    public List<AuthDtos.LeaderboardEntry> top() {
        return users.findTop20ByOrderByRatingDescExperienceDesc().stream()
                .map(user -> new AuthDtos.LeaderboardEntry(user.getUsername(), user.getLevel(), user.getExperience(), user.getRating(), user.getWins(), user.getLosses(), user.getDraws()))
                .toList();
    }
}
