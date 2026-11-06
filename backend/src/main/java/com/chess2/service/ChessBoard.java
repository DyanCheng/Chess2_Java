package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.BotDifficulty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;

final class ChessBoard {
    private ChessBoard() { }

    static String initial(ObjectMapper mapper) {
        return write(mapper, new String[][] {{"bR","bN","bB","bQ","bK","bB","bN","bR"},{"bP","bP","bP","bP","bP","bP","bP","bP"},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{null,null,null,null,null,null,null,null},{"wP","wP","wP","wP","wP","wP","wP","wP"},{"wR","wN","wB","wQ","wK","wB","wN","wR"}});
    }

    static String move(ObjectMapper mapper, String json, String from, String to, String turn) {
        String[][] board = read(mapper, json);
        int[] start = square(from); int[] end = square(to); String piece = board[start[0]][start[1]];
        char colour = turn.equals("WHITE") ? 'w' : 'b';
        if (piece == null || piece.charAt(0) != colour) throw new ApiException(HttpStatus.CONFLICT, "Select a piece belonging to the current player");
        if (!legal(board, start[0], start[1], end[0], end[1])) throw new ApiException(HttpStatus.BAD_REQUEST, "Illegal move");
        board[end[0]][end[1]] = piece.charAt(1) == 'P' && (end[0] == 0 || end[0] == 7) ? piece.charAt(0) + "Q" : piece;
        board[start[0]][start[1]] = null;
        return write(mapper, board);
    }

    static String botMove(ObjectMapper mapper, String json, BotDifficulty difficulty) {
        return botMove(mapper, json, difficulty, null);
    }

    static String botMove(ObjectMapper mapper, String json, BotDifficulty difficulty, String protectedSquare) {
        String[][] board = read(mapper, json);
        List<CandidateMove> moves = candidates(board, 'b');
        if (protectedSquare != null) moves = moves.stream().filter(move -> !protectedSquare.equals(move.to())).toList();
        if (moves.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "Bot has no available move");
        CandidateMove selected = switch (difficulty) {
            case EASY -> moves.get(ThreadLocalRandom.current().nextInt(moves.size()));
            case MEDIUM -> chooseMedium(moves);
            case HARD -> moves.stream().max(Comparator.comparingInt(CandidateMove::score)).orElseThrow();
        };
        return move(mapper, json, selected.from(), selected.to(), "BLACK");
    }

    static boolean hasKing(ObjectMapper mapper, String json, char colour) {
        for (String[] row : read(mapper, json)) for (String piece : row) if (("" + colour + "K").equals(piece)) return true;
        return false;
    }

    static String pieceAt(ObjectMapper mapper, String json, String square) { int[] point = square(square); return read(mapper, json)[point[0]][point[1]]; }

    static String addQueen(ObjectMapper mapper, String json, char colour) {
        String[][] board = read(mapper, json); List<int[]> empty = new ArrayList<>();
        int homeRow = colour == 'w' ? 7 : 0;
        for (int col = 0; col < 8; col++) if (board[homeRow][col] == null) empty.add(new int[] {homeRow, col});
        if (empty.isEmpty()) for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) if (board[row][col] == null) empty.add(new int[] {row, col});
        if (empty.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "No empty square is available for the extra queen");
        int[] target = empty.get(ThreadLocalRandom.current().nextInt(empty.size())); board[target[0]][target[1]] = colour + "Q"; return write(mapper, board);
    }

    static List<List<String>> view(ObjectMapper mapper, String json) {
        String[][] board = read(mapper, json); List<List<String>> rows = new ArrayList<>();
        for (String[] row : board) rows.add(Arrays.asList(row)); return rows;
    }

    private static CandidateMove chooseMedium(List<CandidateMove> moves) {
        List<CandidateMove> captures = moves.stream().filter(move -> move.captureValue() > 0).toList();
        List<CandidateMove> pool = captures.isEmpty() ? moves : captures;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static List<CandidateMove> candidates(String[][] board, char colour) {
        List<CandidateMove> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            String piece = board[row][col]; if (piece == null || piece.charAt(0) != colour) continue;
            for (int targetRow = 0; targetRow < 8; targetRow++) for (int targetCol = 0; targetCol < 8; targetCol++) {
                if (!legal(board, row, col, targetRow, targetCol)) continue;
                String target = board[targetRow][targetCol]; int capture = target == null ? 0 : value(target.charAt(1));
                int centre = 6 - (Math.abs(3 - targetRow) + Math.abs(3 - targetCol));
                moves.add(new CandidateMove(coordinate(row, col), coordinate(targetRow, targetCol), capture, capture * 100 + centre));
            }
        }
        return moves;
    }

    private static int value(char piece) { return switch (piece) { case 'K' -> 100; case 'Q' -> 9; case 'R' -> 5; case 'B', 'N' -> 3; default -> 1; }; }
    private static boolean legal(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow && fromCol == toCol) return false;
        String piece = board[fromRow][fromCol], target = board[toRow][toCol];
        if (target != null && target.charAt(0) == piece.charAt(0)) return false;
        int deltaRow = toRow - fromRow, deltaCol = toCol - fromCol;
        return switch (piece.charAt(1)) {
            case 'P' -> pawn(board, piece.charAt(0), fromRow, fromCol, toRow, toCol);
            case 'N' -> Math.abs(deltaRow) * Math.abs(deltaCol) == 2;
            case 'K' -> Math.max(Math.abs(deltaRow), Math.abs(deltaCol)) == 1;
            case 'B' -> Math.abs(deltaRow) == Math.abs(deltaCol) && clear(board, fromRow, fromCol, toRow, toCol);
            case 'R' -> (deltaRow == 0 || deltaCol == 0) && clear(board, fromRow, fromCol, toRow, toCol);
            case 'Q' -> (deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol)) && clear(board, fromRow, fromCol, toRow, toCol);
            default -> false;
        };
    }
    private static boolean pawn(String[][] board, char colour, int fromRow, int fromCol, int toRow, int toCol) {
        int direction = colour == 'w' ? -1 : 1, deltaRow = toRow - fromRow, deltaCol = Math.abs(toCol - fromCol);
        if (deltaCol == 0 && board[toRow][toCol] == null) { if (deltaRow == direction) return true; int start = colour == 'w' ? 6 : 1; return fromRow == start && deltaRow == 2 * direction && board[fromRow + direction][fromCol] == null; }
        return deltaCol == 1 && deltaRow == direction && board[toRow][toCol] != null && board[toRow][toCol].charAt(0) != colour;
    }
    private static boolean clear(String[][] board, int fromRow, int fromCol, int toRow, int toCol) { int rowStep = Integer.compare(toRow, fromRow), colStep = Integer.compare(toCol, fromCol), row = fromRow + rowStep, col = fromCol + colStep; while (row != toRow || col != toCol) { if (board[row][col] != null) return false; row += rowStep; col += colStep; } return true; }
    private static int[] square(String value) { return new int[] {'8' - value.charAt(1), value.charAt(0) - 'a'}; }
    private static String coordinate(int row, int col) { return "" + (char) ('a' + col) + (char) ('8' - row); }
    private static String[][] read(ObjectMapper mapper, String json) { try { return mapper.readValue(json, String[][].class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Stored game board is invalid", exception); } }
    private static String write(ObjectMapper mapper, String[][] board) { try { return mapper.writeValueAsString(board); } catch (JsonProcessingException exception) { throw new IllegalStateException(exception); } }
    private record CandidateMove(String from, String to, int captureValue, int score) { }
}
