package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.service.AccountService.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChessService {
    private static final String BOT_NAME = "Chess2Bot";
    private final AccountService accounts;
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public ChessService(AccountService accounts) {
        this.accounts = accounts;
    }

    public MatchView createMatch(String authorization, String requestedMode) {
        Player player = accounts.requirePlayer(authorization);
        Mode mode;
        try {
            mode = Mode.valueOf(requestedMode.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mode must be BOT or LOCAL");
        }
        Game game = new Game(UUID.randomUUID().toString(), player, mode);
        games.put(game.id, game);
        return view(game);
    }

    public MatchView getMatch(String authorization, String matchId) {
        Game game = requireGame(authorization, matchId);
        synchronized (game) {
            return view(game);
        }
    }

    public MatchView move(String authorization, String matchId, String from, String to) {
        Player player = accounts.requirePlayer(authorization);
        Game game = requireGame(player, matchId);
        synchronized (game) {
            ensureActive(game);
            int[] start = parseSquare(from);
            int[] end = parseSquare(to);
            String piece = game.board[start[0]][start[1]];
            if (piece == null || piece.charAt(0) != game.turn) {
                throw new ApiException(HttpStatus.CONFLICT, "Select a piece belonging to the current player");
            }
            if (game.mode == Mode.BOT && game.turn != 'w') {
                throw new ApiException(HttpStatus.CONFLICT, "Wait for the bot to move");
            }
            if (!isLegalMove(game.board, start[0], start[1], end[0], end[1])) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Illegal move");
            }
            applyMove(game, start[0], start[1], end[0], end[1]);
            if (game.mode == Mode.BOT && game.turn == 'b' && game.status.equals("ACTIVE")) {
                botMove(game);
            }
            return view(game);
        }
    }

    public MatchView resign(String authorization, String matchId) {
        Player player = accounts.requirePlayer(authorization);
        Game game = requireGame(player, matchId);
        synchronized (game) {
            ensureActive(game);
            game.winner = game.turn == 'w' ? 'b' : 'w';
            finish(game);
            return view(game);
        }
    }

    private Game requireGame(String authorization, String matchId) {
        return requireGame(accounts.requirePlayer(authorization), matchId);
    }

    private Game requireGame(Player player, String matchId) {
        Game game = games.get(matchId);
        if (game == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Match not found");
        }
        if (!game.owner.username().equals(player.username())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This match belongs to another player");
        }
        return game;
    }

    private static void ensureActive(Game game) {
        if (!game.status.equals("ACTIVE")) {
            throw new ApiException(HttpStatus.CONFLICT, "Match has already ended");
        }
    }

    private static int[] parseSquare(String square) {
        return new int[] { '8' - square.charAt(1), square.charAt(0) - 'a' };
    }

    private static boolean isLegalMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }
        String piece = board[fromRow][fromCol];
        String target = board[toRow][toCol];
        if (target != null && (target.charAt(0) == piece.charAt(0) || target.charAt(1) == 'K')) {
            return false;
        }
        int dr = toRow - fromRow;
        int dc = toCol - fromCol;
        return switch (piece.charAt(1)) {
            case 'P' -> pawnMove(board, piece.charAt(0), fromRow, fromCol, toRow, toCol);
            case 'N' -> Math.abs(dr) * Math.abs(dc) == 2;
            case 'K' -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
            case 'B' -> Math.abs(dr) == Math.abs(dc) && clearPath(board, fromRow, fromCol, toRow, toCol);
            case 'R' -> (dr == 0 || dc == 0) && clearPath(board, fromRow, fromCol, toRow, toCol);
            case 'Q' -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc))
                    && clearPath(board, fromRow, fromCol, toRow, toCol);
            default -> false;
        };
    }

    private static boolean pawnMove(String[][] board, char color, int fromRow, int fromCol, int toRow, int toCol) {
        int direction = color == 'w' ? -1 : 1;
        int rowDelta = toRow - fromRow;
        int colDelta = Math.abs(toCol - fromCol);
        if (colDelta == 0 && board[toRow][toCol] == null) {
            if (rowDelta == direction) {
                return true;
            }
            int startRow = color == 'w' ? 6 : 1;
            return fromRow == startRow && rowDelta == 2 * direction
                    && board[fromRow + direction][fromCol] == null;
        }
        return colDelta == 1 && rowDelta == direction && board[toRow][toCol] != null
                && board[toRow][toCol].charAt(0) != color;
    }

    private static boolean clearPath(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int row = fromRow + rowStep;
        int col = fromCol + colStep;
        while (row != toRow || col != toCol) {
            if (board[row][col] != null) {
                return false;
            }
            row += rowStep;
            col += colStep;
        }
        return true;
    }

    private static void applyMove(Game game, int fromRow, int fromCol, int toRow, int toCol) {
        String piece = game.board[fromRow][fromCol];
        game.board[toRow][toCol] = piece.charAt(1) == 'P' && (toRow == 0 || toRow == 7)
                ? piece.charAt(0) + "Q"
                : piece;
        game.board[fromRow][fromCol] = null;
        game.moves.add((game.moves.size() / 2 + 1) + ". " + fromSquare(fromRow, fromCol) + "-" + fromSquare(toRow, toCol));
        game.turn = game.turn == 'w' ? 'b' : 'w';
    }

    private static String fromSquare(int row, int col) {
        return "" + (char) ('a' + col) + (char) ('8' - row);
    }

    private static void botMove(Game game) {
        List<int[]> possible = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = game.board[row][col];
                if (piece != null && piece.charAt(0) == 'b') {
                    for (int targetRow = 0; targetRow < 8; targetRow++) {
                        for (int targetCol = 0; targetCol < 8; targetCol++) {
                            if (isLegalMove(game.board, row, col, targetRow, targetCol)) {
                                possible.add(new int[] { row, col, targetRow, targetCol });
                            }
                        }
                    }
                }
            }
        }
        if (possible.isEmpty()) {
            game.status = "DRAW";
            return;
        }
        List<int[]> captures = possible.stream()
                .filter(move -> game.board[move[2]][move[3]] != null)
                .toList();
        List<int[]> candidates = captures.isEmpty() ? possible : captures;
        int[] move = candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
        applyMove(game, move[0], move[1], move[2], move[3]);
    }

    private void finish(Game game) {
        game.status = game.winner == null ? "DRAW" : "FINISHED";
        if (game.winner != null) {
            accounts.recordResult(game.owner, game.winner == 'w');
        }
    }

    private static MatchView view(Game game) {
        List<List<String>> rows = new ArrayList<>();
        for (String[] row : game.board) {
            List<String> cells = new ArrayList<>();
            Collections.addAll(cells, row);
            rows.add(cells);
        }
        return new MatchView(game.id, game.mode.name(), game.owner.username(),
                game.mode == Mode.BOT ? BOT_NAME : "Local player 2",
                rows, String.valueOf(game.turn), game.status,
                game.winner == null ? null : String.valueOf(game.winner), List.copyOf(game.moves));
    }

    public record MatchView(
            String id, String mode, String whitePlayer, String blackPlayer,
            List<List<String>> board, String turn, String status, String winner, List<String> moves) {}

    private enum Mode {
        BOT, LOCAL
    }

    private static final class Game {
        private final String id;
        private final Player owner;
        private final Mode mode;
        private final String[][] board = initialBoard();
        private final List<String> moves = new ArrayList<>();
        private char turn = 'w';
        private String status = "ACTIVE";
        private Character winner;

        private Game(String id, Player owner, Mode mode) {
            this.id = id;
            this.owner = owner;
            this.mode = mode;
        }

        private static String[][] initialBoard() {
            return new String[][] {
                    {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"},
                    {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
                    {null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null},
                    {null, null, null, null, null, null, null, null},
                    {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
                    {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"}
            };
        }
    }
}
