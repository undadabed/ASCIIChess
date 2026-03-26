/**
 * Piece utility class for ASCII Chess.
 * Contains piece display mappings (Unicode symbols).
 */
public class Piece {

    // Unicode chess pieces for display
    // Note: Using filled symbols for white (more visible on terminal) and hollow for black
    public static final String WHITE_KING_DISPLAY   = "♚";
    public static final String WHITE_QUEEN_DISPLAY  = "♛";
    public static final String WHITE_ROOK_DISPLAY   = "♜";
    public static final String WHITE_BISHOP_DISPLAY = "♝";
    public static final String WHITE_KNIGHT_DISPLAY = "♞";
    public static final String WHITE_PAWN_DISPLAY   = "♟";
    public static final String BLACK_KING_DISPLAY   = "♔";
    public static final String BLACK_QUEEN_DISPLAY  = "♕";
    public static final String BLACK_ROOK_DISPLAY   = "♖";
    public static final String BLACK_BISHOP_DISPLAY = "♗";
    public static final String BLACK_KNIGHT_DISPLAY = "♘";
    public static final String BLACK_PAWN_DISPLAY   = "♙";

    /**
     * Converts internal piece representation to Unicode display character.
     * @param piece The internal piece string (e.g., "wK", "bp")
     * @return Unicode chess piece character or original string for empty squares
     */
    public static String pieceToDisplay(String piece) {
        if (piece.equals("wK")) return WHITE_KING_DISPLAY;
        if (piece.equals("wQ")) return WHITE_QUEEN_DISPLAY;
        if (piece.equals("wR")) return WHITE_ROOK_DISPLAY;
        if (piece.equals("wB")) return WHITE_BISHOP_DISPLAY;
        if (piece.equals("wN")) return WHITE_KNIGHT_DISPLAY;
        if (piece.equals("wp")) return WHITE_PAWN_DISPLAY;
        if (piece.equals("bK")) return BLACK_KING_DISPLAY;
        if (piece.equals("bQ")) return BLACK_QUEEN_DISPLAY;
        if (piece.equals("bR")) return BLACK_ROOK_DISPLAY;
        if (piece.equals("bB")) return BLACK_BISHOP_DISPLAY;
        if (piece.equals("bN")) return BLACK_KNIGHT_DISPLAY;
        if (piece.equals("bp")) return BLACK_PAWN_DISPLAY;
        if (piece.equals("##")) return "##";
        return "  ";
    }

    /**
     * Checks if a square is empty (either "  " or "##").
     * @param square The square content to check
     * @return true if the square is empty
     */
    public static boolean isEmptySquare(String square) {
        return square.equals("  ") || square.equals("##");
    }
}
