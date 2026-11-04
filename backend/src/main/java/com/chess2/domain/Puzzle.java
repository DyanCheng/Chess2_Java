package com.chess2.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "puzzles")
public class Puzzle {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, columnDefinition = "text") private String board;
    @Column(name = "solution_from", nullable = false, length = 2) private String solutionFrom;
    @Column(name = "solution_to", nullable = false, length = 2) private String solutionTo;
    @Column(nullable = false) private int rewardGold;
    protected Puzzle() { }
    public Puzzle(String board, String solutionFrom, String solutionTo, int rewardGold) { this.board=board; this.solutionFrom=solutionFrom; this.solutionTo=solutionTo; this.rewardGold=rewardGold; }
    public UUID getId() { return id; } public String getBoard() { return board; } public boolean solvedBy(String from, String to) { return solutionFrom.equals(from) && solutionTo.equals(to); } public int getRewardGold() { return rewardGold; }
}
