package com.chess2.service;
import com.chess2.domain.BotDifficulty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChessBoardTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Test void botNeverMovesFrozenPiece() {
        for (BotDifficulty difficulty : BotDifficulty.values()) {
            String result = ChessBoard.botMove(mapper, ChessBoard.initial(mapper), difficulty, null, "b8");
            assertEquals("bN", ChessBoard.pieceAt(mapper, result, "b8"));
        }
    }
    @Test void hardBotAvoidsGivingAwayItsKingForAFreePawn() throws Exception {
        String[][] board = new String[8][8];
        board[0][0] = "bK"; board[1][3] = "bQ"; board[6][3] = "wP"; board[7][0] = "wR"; board[7][7] = "wK";
        String result = ChessBoard.botMove(mapper, mapper.writeValueAsString(board), BotDifficulty.HARD);
        assertNotEquals("bQ", ChessBoard.pieceAt(mapper, result, "d2"));
        assertTrue(ChessBoard.hasKing(mapper, result, 'b'));
    }
    @Test void botRespectsProtectedTarget() throws Exception {
        String[][] board = new String[8][8];
        board[0][7] = "bK"; board[7][0] = "wK"; board[3][3] = "bQ"; board[4][3] = "wQ";
        String result = ChessBoard.botMove(mapper, mapper.writeValueAsString(board), BotDifficulty.HARD, "d4");
        assertEquals("wQ", ChessBoard.pieceAt(mapper, result, "d4"));
    }
}
