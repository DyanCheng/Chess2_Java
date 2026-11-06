package com.chess2.api;

import com.chess2.domain.GameType;
import com.chess2.domain.UserAccount;
import com.chess2.dto.GameDtos;
import com.chess2.service.GameService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService games;
    public GameController(GameService games) { this.games=games; }
    @PostMapping public GameDtos.GameResponse create(@AuthenticationPrincipal UserAccount user,@Valid @RequestBody GameDtos.CreateGameRequest request) { return games.create(user,GameType.valueOf(request.type()),request.difficulty()); }
    @PostMapping("/quick-match") public GameDtos.GameResponse quickMatch(@AuthenticationPrincipal UserAccount user) { return games.quickMatch(user); }
    @GetMapping("/waiting") public List<GameDtos.GameResponse> waiting() { return games.waiting(); }
    @GetMapping("/history") public List<GameDtos.GameResponse> history(@AuthenticationPrincipal UserAccount user) { return games.history(user); }
    @PostMapping("/{id}/join") public GameDtos.GameResponse join(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id) { return games.join(user,id); }
    @GetMapping("/{id}") public GameDtos.GameResponse get(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id) { return games.get(user,id); }
    @PostMapping("/{id}/moves") public GameDtos.GameResponse move(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id,@Valid @RequestBody GameDtos.MoveRequest request) { return games.move(user,id,request); }
    @PostMapping("/{id}/cards") public GameDtos.GameResponse useCard(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id,@Valid @RequestBody GameDtos.UseCardRequest request) { return games.useCard(user,id,request); }
    @PostMapping("/{id}/resign") public GameDtos.GameResponse resign(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id) { return games.resign(user,id); }
}
