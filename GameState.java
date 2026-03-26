import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * GameState class for ASCII Chess.
 * Holds all mutable game state: turn, castling rights, en passant, half-move clock, position history.
 */
public class GameState {

    // Turn
    public boolean white = true;

    // Castling rights (indices 2 and 6 are used for queenside/kingside)
    public boolean[] whiteCastle;
    public boolean[] blackCastle;

    // En passant tracking (true if a pawn on that file just did a double move)
    public boolean[] whitePassant;
    public boolean[] blackPassant;

    // 50 move rule
    public int halfMoveClock = 0;

    // Threefold repetition tracking
    public Map<String, Integer> positionHistory = new HashMap<>();

    // Promotion piece (Q/R/B/N, default Q)
    public String promo = "Q";

    // Chess960 mode
    public boolean chess960 = false;

    // Board representation
    public String[][] board;
    public String[][] checkBoard;

    // Draw reason (for end-game display)
    public String drawReason = "";

    // Last error message
    public String lastError = "";

    // UI/game flow flags
    public boolean offerdraw = false;
    public boolean complete = false;
    public boolean draw = false;

    // Move history for history command and future undo support
    public List<Move.MoveRecord> moveHistory = new ArrayList<>();

    /**
     * Initialize all state for a new game.
     */
    public void init() {
        white = true;
        whiteCastle = initializeCastle();
        blackCastle = initializeCastle();
        whitePassant = initializePassant();
        blackPassant = initializePassant();
        halfMoveClock = 0;
        positionHistory.clear();
        promo = "Q";
        lastError = "";
        drawReason = "";
        offerdraw = false;
        complete = false;
        draw = false;

        // Initialize boards
        board = Board.initializeBoard();
        checkBoard = Board.initializeBoard();

        // Initialize move history
        moveHistory.clear();

        // Record initial position
        positionHistory.put(getPositionKey(null), 1);
    }

    /**
     * Initialize castling rights array (indices 2 and 6 set to true).
     */
    public static boolean[] initializeCastle() {
        boolean[] output = new boolean[8];
        for (int i = 0; i < 8; i++) output[i] = false;
        output[2] = true;
        output[6] = true;
        return output;
    }

    /**
     * Initialize en passant array (all false).
     */
    public static boolean[] initializePassant() {
        boolean[] output = new boolean[8];
        for (int i = 0; i < 8; i++) output[i] = false;
        return output;
    }

    /**
     * Generate a unique string key for the current board position.
     * Used for threefold repetition detection.
     * @param board the current board (can be null to skip board part)
     */
    public String getPositionKey(String[][] board) {
        StringBuilder sb = new StringBuilder();

        if (board != null) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    String piece = board[i][j];
                    if (!Piece.isEmptySquare(piece)) {
                        sb.append(piece).append(i).append(j);
                    }
                }
            }
        }

        // Whose turn
        sb.append(white ? "w" : "b");

        // Castling rights
        sb.append(whiteCastle[2] ? "WCL" : "").append(whiteCastle[6] ? "WCR" : "");
        sb.append(blackCastle[2] ? "BCL" : "").append(blackCastle[6] ? "BCR" : "");

        // En passant
        for (int i = 0; i < 8; i++) {
            if (whitePassant[i]) sb.append("WP").append(i);
            if (blackPassant[i]) sb.append("BP").append(i);
        }

        return sb.toString();
    }

    /**
     * Reset en passant for the side that just moved (called at end of turn).
     */
    public void clearPassantForSide(boolean forWhite) {
        if (forWhite) {
            whitePassant = initializePassant();
        } else {
            blackPassant = initializePassant();
        }
    }

    /**
     * Check automatic draw conditions.
     * @return draw reason string, or null if no automatic draw
     */
    public String checkAutomaticDraw() {
        if (halfMoveClock >= 100) {
            return "50 move rule";
        }
        String currentPosition = getPositionKey(null);
        Integer count = positionHistory.get(currentPosition);
        if (count != null && count >= 3) {
            return "threefold repetition";
        }
        return null;
    }

    /**
     * Record current position for threefold repetition.
     * @param board current board
     */
    public void recordPosition(String[][] board) {
        String key = getPositionKey(board);
        positionHistory.put(key, positionHistory.getOrDefault(key, 0) + 1);
    }

    /**
     * Resets castling rights based on king/rook positions (for Chess960).
     */
    public void resetCastlingRights() {
        whiteCastle = initializeCastle();
        blackCastle = initializeCastle();
    }

    /**
     * Checks if the current position is stalemate.
     * Requires reference to checkBoard and Move class for check detection.
     */
    public boolean isStalemate(String[][] checkBoard) {
        return !Move.check(checkBoard, white) && !Move.hasLegalMoves(checkBoard, white);
    }
}
