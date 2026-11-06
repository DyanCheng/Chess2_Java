package com.chess2.api;

import com.chess2.domain.Puzzle;
import com.chess2.domain.UserAccount;
import com.chess2.service.PuzzleService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {
    private final PuzzleService puzzles;
    public PuzzleController(PuzzleService puzzles) { this.puzzles=puzzles; }
    @GetMapping public List<Puzzle> all() { return puzzles.all(); }
    @PostMapping("/{id}/submit") public PuzzleService.PuzzleResult submit(@AuthenticationPrincipal UserAccount user,@PathVariable UUID id,@RequestBody SubmitRequest request) { return puzzles.submit(user,id,request.from(),request.to()); }
    public record SubmitRequest(@NotBlank @Pattern(regexp="[a-h][1-8]") String from,@NotBlank @Pattern(regexp="[a-h][1-8]") String to) { }
}
