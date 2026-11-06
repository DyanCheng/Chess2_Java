package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.Puzzle;
import com.chess2.domain.UserAccount;
import com.chess2.domain.Wallet;
import com.chess2.repository.PuzzleRepository;
import com.chess2.repository.PuzzleAttemptRepository;
import com.chess2.repository.WalletRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PuzzleService {
    private final PuzzleRepository puzzles; private final WalletRepository wallets; private final PuzzleAttemptRepository attempts;
    public PuzzleService(PuzzleRepository puzzles, WalletRepository wallets, PuzzleAttemptRepository attempts) { this.puzzles=puzzles; this.wallets=wallets; this.attempts=attempts; }
    public List<Puzzle> all() { return puzzles.findAll(); }
    @Transactional public PuzzleResult submit(UserAccount player, UUID puzzleId, String from, String to) {
        Puzzle puzzle=puzzles.findById(puzzleId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Puzzle not found")); boolean correct=puzzle.solvedBy(from,to);
        if (!correct) return new PuzzleResult(false, false);
        var attempt=attempts.findByUserIdAndPuzzleId(player.getId(), puzzleId).orElseGet(() -> attempts.save(new com.chess2.domain.PuzzleAttempt(player.getId(), puzzleId)));
        boolean rewardGranted=attempt.rewardIfFirstSolve();
        if(rewardGranted) wallets.findById(player.getId()).orElseThrow().creditGold(puzzle.getRewardGold());
        return new PuzzleResult(true, rewardGranted);
    }
    public record PuzzleResult(boolean correct, boolean rewardGranted) { }
}
