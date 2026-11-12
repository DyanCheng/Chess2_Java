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
        return move(mapper, json, from, to, turn, false);
    }

    static String move(ObjectMapper mapper, String json, String from, String to, String turn, boolean backwardPawns) {
        String[][] board = read(mapper, json);
        int[] start = square(from); int[] end = square(to); String piece = board[start[0]][start[1]];
        char colour = turn.equals("WHITE") ? 'w' : 'b';
        if (piece == null || piece.charAt(0) != colour) throw new ApiException(HttpStatus.CONFLICT, "Select a piece belonging to the current player");
        if (!legal(board, start[0], start[1], end[0], end[1], backwardPawns)) throw new ApiException(HttpStatus.BAD_REQUEST, "Illegal move");
        board[end[0]][end[1]] = piece.charAt(1) == 'P' && (end[0] == 0 || end[0] == 7) ? piece.charAt(0) + "Q" : piece;
        board[start[0]][start[1]] = null;
        return write(mapper, board);
    }

    static String botMove(ObjectMapper mapper, String json, BotDifficulty difficulty) {
        return botMove(mapper, json, difficulty, null);
    }

    static String botMove(ObjectMapper mapper, String json, BotDifficulty difficulty, String protectedSquare) {
        return botMove(mapper, json, difficulty, protectedSquare, null);
    }

    static String botMove(ObjectMapper mapper, String json, BotDifficulty difficulty, String protectedSquare, String frozenSquare) {
        return botMove(mapper,json,difficulty,protectedSquare,frozenSquare,"BLACK",false).board();
    }

    static BotMove botMove(ObjectMapper mapper, String json, BotDifficulty difficulty, String protectedSquare, String frozenSquare, String side, boolean backwardPawns) {
        String[][] board = read(mapper, json);
        char colour=Character.toLowerCase(side.charAt(0));
        List<CandidateMove> moves = candidates(board, colour, backwardPawns);
        if (protectedSquare != null) moves = moves.stream().filter(move -> !protectedSquare.equals(move.to())).toList();
        if (frozenSquare != null) moves = moves.stream().filter(move -> !frozenSquare.equals(move.from())).toList();
        if (moves.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "Bot has no available move");
        CandidateMove selected = switch (difficulty) {
            case EASY -> moves.get(ThreadLocalRandom.current().nextInt(moves.size()));
            case MEDIUM -> chooseMedium(moves);
            case HARD -> moves.stream().max(Comparator.comparingInt(candidate -> lookAhead(board, candidate, colour))).orElseThrow();
        };
        String captured=pieceAt(mapper,json,selected.to());
        return new BotMove(move(mapper, json, selected.from(), selected.to(), side, backwardPawns),selected.from(),selected.to(),captured);
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

    static String advancePawn(ObjectMapper mapper, String json, String square, String side) {
        String[][] board=read(mapper,json); int[] at=square(square); String piece=board[at[0]][at[1]];
        char colour=Character.toLowerCase(side.charAt(0)); int direction=colour=='w'?-1:1, row=at[0]+direction;
        if(piece==null||piece.charAt(0)!=colour||piece.charAt(1)!='P') throw new ApiException(HttpStatus.BAD_REQUEST,"Hãy chọn một Tốt của bạn");
        if(row<0||row>7||board[row][at[1]]!=null) throw new ApiException(HttpStatus.CONFLICT,"Tốt không thể tiến thêm ở vị trí này");
        board[row][at[1]]=row==0||row==7?colour+"Q":piece; board[at[0]][at[1]]=null; return write(mapper,board);
    }

    static String swap(ObjectMapper mapper,String json,String first,String second){
        String[][] board=read(mapper,json);int[] a=square(first),b=square(second);String one=board[a[0]][a[1]],two=board[b[0]][b[1]];
        if(one==null||two==null||one.charAt(1)=='K'||two.charAt(1)=='K') throw new ApiException(HttpStatus.BAD_REQUEST,"Portal cần hai quân không phải Vua");
        board[a[0]][a[1]]=two;board[b[0]][b[1]]=one;return write(mapper,board);
    }

    static String teleport(ObjectMapper mapper,String json,String square,char colour){
        String[][] board=read(mapper,json);int[] source=square(square);String piece=board[source[0]][source[1]];
        if(piece==null||piece.charAt(0)!=colour) throw new ApiException(HttpStatus.BAD_REQUEST,"Hãy chọn một quân của bạn");
        List<int[]> empty=new ArrayList<>();for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(board[r][c]==null)empty.add(new int[]{r,c});
        if(empty.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"Không còn ô trống để dịch chuyển");
        int[] target=empty.get(ThreadLocalRandom.current().nextInt(empty.size()));board[target[0]][target[1]]=piece;board[source[0]][source[1]]=null;return write(mapper,board);
    }

    static String hideKing(ObjectMapper mapper,String json,char colour){
        String[][] board=read(mapper,json);int kr=-1,kc=-1;List<int[]> rooks=new ArrayList<>();
        for(int r=0;r<8;r++)for(int c=0;c<8;c++){if((""+colour+"K").equals(board[r][c])){kr=r;kc=c;}if((""+colour+"R").equals(board[r][c]))rooks.add(new int[]{r,c});}
        if(kr<0||rooks.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"Không có Xe để hoán đổi với Vua");
        final int kingRow=kr,kingCol=kc;int[] rook=rooks.stream().min(Comparator.comparingInt(p->Math.abs(p[0]-kingRow)+Math.abs(p[1]-kingCol))).orElseThrow();
        board[kr][kc]=colour+"R";board[rook[0]][rook[1]]=colour+"K";return write(mapper,board);
    }

    static String removePiece(ObjectMapper mapper,String json,String square){String[][] board=read(mapper,json);int[] p=square(square);board[p[0]][p[1]]=null;return write(mapper,board);}

    static String revive(ObjectMapper mapper,String json,char colour,char piece){
        String[][] board=read(mapper,json);List<int[]> empty=new ArrayList<>();int home=colour=='w'?7:0;
        for(int c=0;c<8;c++)if(board[home][c]==null)empty.add(new int[]{home,c});
        if(empty.isEmpty())for(int r=0;r<8;r++)for(int c=0;c<8;c++)if(board[r][c]==null)empty.add(new int[]{r,c});
        if(empty.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"Không có ô trống để hồi sinh quân cờ");
        int[] target=empty.get(0);board[target[0]][target[1]]=""+colour+piece;return write(mapper,board);
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

    // Two-ply arcade search: consider the opponent's strongest reply, including king capture.
    private static int lookAhead(String[][] board, CandidateMove candidate, char colour) {
        if (candidate.captureValue() == 100) return 1_000_000;
        String[][] after = apply(board, candidate);
        int worst = evaluate(after);
        List<CandidateMove> replies = candidates(after, colour=='w'?'b':'w',false);
        if (!replies.isEmpty()) worst = colour=='b' ? replies.stream().mapToInt(reply -> evaluate(apply(after, reply))).min().orElse(worst) : replies.stream().mapToInt(reply -> evaluate(apply(after, reply))).max().orElse(worst);
        int signed=colour=='b'?worst:-worst;return signed * 100 + candidate.score() % 100;
    }

    private static String[][] apply(String[][] board, CandidateMove move) {
        String[][] copy = Arrays.stream(board).map(String[]::clone).toArray(String[][]::new);
        int[] from = square(move.from()), to = square(move.to());
        String piece = copy[from[0]][from[1]];
        copy[to[0]][to[1]] = piece.charAt(1) == 'P' && (to[0] == 0 || to[0] == 7) ? piece.charAt(0) + "Q" : piece;
        copy[from[0]][from[1]] = null;
        return copy;
    }

    private static int evaluate(String[][] board) {
        int score = 0;
        for (String[] row : board) for (String piece : row) if (piece != null) {
            int weight = piece.charAt(1) == 'K' ? 10000 : value(piece.charAt(1));
            score += piece.charAt(0) == 'b' ? weight : -weight;
        }
        return score;
    }

    private static List<CandidateMove> candidates(String[][] board, char colour) { return candidates(board,colour,false); }
    private static List<CandidateMove> candidates(String[][] board, char colour, boolean backwardPawns) {
        List<CandidateMove> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            String piece = board[row][col]; if (piece == null || piece.charAt(0) != colour) continue;
            for (int targetRow = 0; targetRow < 8; targetRow++) for (int targetCol = 0; targetCol < 8; targetCol++) {
                if (!legal(board, row, col, targetRow, targetCol, backwardPawns)) continue;
                String target = board[targetRow][targetCol]; int capture = target == null ? 0 : value(target.charAt(1));
                int centre = 6 - (Math.abs(3 - targetRow) + Math.abs(3 - targetCol));
                moves.add(new CandidateMove(coordinate(row, col), coordinate(targetRow, targetCol), capture, capture * 100 + centre));
            }
        }
        return moves;
    }

    private static int value(char piece) { return switch (piece) { case 'K' -> 100; case 'Q' -> 9; case 'R' -> 5; case 'B', 'N' -> 3; default -> 1; }; }
    private static boolean legal(String[][] board, int fromRow, int fromCol, int toRow, int toCol) { return legal(board,fromRow,fromCol,toRow,toCol,false); }
    private static boolean legal(String[][] board, int fromRow, int fromCol, int toRow, int toCol, boolean backwardPawns) {
        if (fromRow == toRow && fromCol == toCol) return false;
        String piece = board[fromRow][fromCol], target = board[toRow][toCol];
        if (target != null && target.charAt(0) == piece.charAt(0)) return false;
        int deltaRow = toRow - fromRow, deltaCol = toCol - fromCol;
        return switch (piece.charAt(1)) {
            case 'P' -> pawn(board, piece.charAt(0), fromRow, fromCol, toRow, toCol, backwardPawns);
            case 'N' -> Math.abs(deltaRow) * Math.abs(deltaCol) == 2;
            case 'K' -> Math.max(Math.abs(deltaRow), Math.abs(deltaCol)) == 1;
            case 'B' -> Math.abs(deltaRow) == Math.abs(deltaCol) && clear(board, fromRow, fromCol, toRow, toCol);
            case 'R' -> (deltaRow == 0 || deltaCol == 0) && clear(board, fromRow, fromCol, toRow, toCol);
            case 'Q' -> (deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol)) && clear(board, fromRow, fromCol, toRow, toCol);
            default -> false;
        };
    }
    private static boolean pawn(String[][] board, char colour, int fromRow, int fromCol, int toRow, int toCol, boolean backwardPawns) {
        int direction = colour == 'w' ? -1 : 1, deltaRow = toRow - fromRow, deltaCol = Math.abs(toCol - fromCol);
        if (deltaCol == 0 && board[toRow][toCol] == null) { if (deltaRow == direction || (backwardPawns && deltaRow == -direction)) return true; int start = colour == 'w' ? 6 : 1; return fromRow == start && deltaRow == 2 * direction && board[fromRow + direction][fromCol] == null; }
        return deltaCol == 1 && deltaRow == direction && board[toRow][toCol] != null && board[toRow][toCol].charAt(0) != colour;
    }
    private static boolean clear(String[][] board, int fromRow, int fromCol, int toRow, int toCol) { int rowStep = Integer.compare(toRow, fromRow), colStep = Integer.compare(toCol, fromCol), row = fromRow + rowStep, col = fromCol + colStep; while (row != toRow || col != toCol) { if (board[row][col] != null) return false; row += rowStep; col += colStep; } return true; }
    private static int[] square(String value) { return new int[] {'8' - value.charAt(1), value.charAt(0) - 'a'}; }
    private static String coordinate(int row, int col) { return "" + (char) ('a' + col) + (char) ('8' - row); }
    private static String[][] read(ObjectMapper mapper, String json) { try { return mapper.readValue(json, String[][].class); } catch (JsonProcessingException exception) { throw new IllegalStateException("Stored game board is invalid", exception); } }
    private static String write(ObjectMapper mapper, String[][] board) { try { return mapper.writeValueAsString(board); } catch (JsonProcessingException exception) { throw new IllegalStateException(exception); } }
    private record CandidateMove(String from, String to, int captureValue, int score) { }
    record BotMove(String board,String from,String to,String captured){ }
}
