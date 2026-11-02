package com.chess2.api;

import com.chess2.service.AccountService;
import com.chess2.service.ChessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChessController {
    private final AccountService accounts;
    private final ChessService chess;

    public ChessController(AccountService accounts, ChessService chess) {
        this.accounts = accounts;
        this.chess = chess;
    }

    @PostMapping("/auth/register")
    public AccountService.AuthResponse register(@Valid @RequestBody Credentials request) {
        return accounts.register(request.username(), request.password());
    }

    @PostMapping("/auth/login")
    public AccountService.AuthResponse login(@Valid @RequestBody Credentials request) {
        return accounts.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public AccountService.PlayerView me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return accounts.profile(authorization);
    }

    @GetMapping("/lobby")
    public Map<String, Object> lobby() {
        return Map.of(
                "modes", List.of("BOT", "LOCAL"),
                "note", "LOCAL means two players take turns on the same device; online multiplayer is not configured."
        );
    }

    @PostMapping("/matches")
    public ChessService.MatchView createMatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody NewMatchRequest request) {
        return chess.createMatch(authorization, request.mode());
    }

    @GetMapping("/matches/{matchId}")
    public ChessService.MatchView getMatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String matchId) {
        return chess.getMatch(authorization, matchId);
    }

    @PostMapping("/matches/{matchId}/moves")
    public ChessService.MatchView move(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String matchId,
            @Valid @RequestBody MoveRequest request) {
        return chess.move(authorization, matchId, request.from(), request.to());
    }

    @PostMapping("/matches/{matchId}/resign")
    public ChessService.MatchView resign(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String matchId) {
        return chess.resign(authorization, matchId);
    }

    @GetMapping("/leaderboard")
    public List<AccountService.PlayerView> leaderboard() {
        return accounts.leaderboard();
    }

    public record Credentials(
            @NotBlank @Size(min = 3, max = 24) @Pattern(regexp = "[A-Za-z0-9_]+") String username,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record NewMatchRequest(@NotBlank String mode) {}

    public record MoveRequest(
            @NotBlank @Pattern(regexp = "[a-h][1-8]") String from,
            @NotBlank @Pattern(regexp = "[a-h][1-8]") String to) {}
}
