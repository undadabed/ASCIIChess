import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

/**
 * ASCII Chess Game
 * @author Jonathan Fan + Ryan Hsiu
 */
public class Chess {
    // Save directory and file extension
    private static final String SAVES_DIR = "saves";
    private static final String SAVE_EXT = ".txt";

    // Board representation
    String[][] board;
    String[][] checkBoard;
    boolean[] whitePassant;
    boolean[] blackPassant;
    boolean[] whiteCastle;
    boolean[] blackCastle;
    boolean white = true;
    boolean offerdraw = false;
    boolean complete = false;
    boolean draw = false;
    String promo;
    boolean chess960 = false;  // Chess960 mode flag
    String lastError = "";

    // 50 move rule tracking
    int halfMoveClock = 0;

    // Threefold repetition tracking
    Map<String, Integer> positionHistory = new HashMap<>();

    // Draw reasons
    String drawReason = "";

    // Move history for history command and future undo support
    /**
     * Stores complete information about a move for undo functionality
     */
    private static class MoveRecord {
        String moveNotation;        // e.g., "e2e4"
        String piece;               // Piece that moved, e.g., "wp"
        String capturedPiece;       // Captured piece or null/empty
        int fromRow, fromCol;       // Starting position
        int toRow, toCol;           // Ending position
        boolean[] whiteCastleBefore;  // Castling rights before move
        boolean[] blackCastleBefore;
        boolean[] whitePassantBefore; // En passant state before move
        boolean[] blackPassantBefore;
        int halfMoveClockBefore;    // For 50 move rule
        boolean wasWhiteTurn;       // Who made this move
        boolean wasEnPassant;       // If this was an en passant capture
        int enPassantCapturedRow;   // Row of captured pawn in en passant
        int enPassantCapturedCol;   // Col of captured pawn in en passant
        boolean wasPromotion;       // If this was a pawn promotion
        String promotedTo;          // What piece was promoted to
        boolean wasCastling;        // If this was a castling move
        String castlingType;        // "K" for kingside, "Q" for queenside
        int rookFromRow, rookFromCol; // Original rook position for castling
        int rookToRow, rookToCol;   // Where rook moved to for castling

        MoveRecord(String notation, String piece, String captured,
                   int fr, int fc, int tr, int tc,
                   boolean[] wcb, boolean[] bcb,
                   boolean[] wpb, boolean[] bpb,
                   int hmc, boolean whiteTurn) {
            this.moveNotation = notation;
            this.piece = piece;
            this.capturedPiece = captured;
            this.fromRow = fr;
            this.fromCol = fc;
            this.toRow = tr;
            this.toCol = tc;
            this.whiteCastleBefore = wcb.clone();
            this.blackCastleBefore = bcb.clone();
            this.whitePassantBefore = wpb.clone();
            this.blackPassantBefore = bpb.clone();
            this.halfMoveClockBefore = hmc;
            this.wasWhiteTurn = whiteTurn;
            this.wasEnPassant = false;
            this.enPassantCapturedRow = -1;
            this.enPassantCapturedCol = -1;
            this.wasPromotion = false;
            this.promotedTo = null;
            this.wasCastling = false;
            this.castlingType = null;
            this.rookFromRow = -1;
            this.rookFromCol = -1;
            this.rookToRow = -1;
            this.rookToCol = -1;
        }
    }

    List<MoveRecord> moveHistory = new ArrayList<>();

    // Unicode chess pieces for display
    // Note: Using filled symbols for white (more visible on terminal) and hollow for black
    private static final String WHITE_KING_DISPLAY = "♚";
    private static final String WHITE_QUEEN_DISPLAY = "♛";
    private static final String WHITE_ROOK_DISPLAY = "♜";
    private static final String WHITE_BISHOP_DISPLAY = "♝";
    private static final String WHITE_KNIGHT_DISPLAY = "♞";
    private static final String WHITE_PAWN_DISPLAY = "♟";
    private static final String BLACK_KING_DISPLAY = "♔";
    private static final String BLACK_QUEEN_DISPLAY = "♕";
    private static final String BLACK_ROOK_DISPLAY = "♖";
    private static final String BLACK_BISHOP_DISPLAY = "♗";
    private static final String BLACK_KNIGHT_DISPLAY = "♘";
    private static final String BLACK_PAWN_DISPLAY = "♙";

    /**
     * Converts internal piece representation to Unicode display character
     * @param piece The internal piece string (e.g., "wK", "bp")
     * @return Unicode chess piece character or original string for empty squares
     */
    private String pieceToDisplay(String piece) {
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
     * Displays the help menu with game instructions
     */
    private void showHelp() {
        System.out.println();
        System.out.println("+==============================================================+");
        System.out.println("|                      CHESS - HELP MENU                       |");
        System.out.println("+==============================================================+");
        System.out.println("| MOVE FORMATS:                                                |");
        System.out.println("|   e2 e4    - Move from e2 to e4 (standard format)            |");
        System.out.println("|   e2e4     - Move from e2 to e4 (no space)                   |");
        System.out.println("|   e4       - Move pawn to e4 (implicit pawn move)            |");
        System.out.println("|   e7e8Q    - Pawn promotion to Queen (Q, R, B, N)            |");
        System.out.println("|                                                              |");
        System.out.println("| CASTLING:                                                    |");
        System.out.println("|   O-O       - Kingside castle (king->g, rook->f)             |");
        System.out.println("|   O-O-O     - Queenside castle (king->c, rook->d)            |");
        System.out.println("|   e1 g1     - Castle by moving king to g file                |");
        System.out.println("|                                                              |");
        System.out.println("| COMMANDS:                                                    |");
        System.out.println("|   help      - Show this help menu                            |");
        System.out.println("|   history   - Show move history                              |");
        System.out.println("|   undo      - Undo the last move                             |");
        System.out.println("|   resign    - Forfeit the game                               |");
        System.out.println("|   draw?     - Offer a draw (opponent types 'draw' to accept) |");
        System.out.println("|   quit      - Exit the game (auto-saves)                     |");
        System.out.println("|                                                              |");
        System.out.println("| AUTOMATIC DRAWS:                                             |");
        System.out.println("|   - 50 move rule (no pawn move or capture in 50 moves)       |");
        System.out.println("|   - Threefold repetition (same position 3 times)             |");
        System.out.println("|   - Stalemate (no legal moves but not in check)              |");
        System.out.println("|                                                              |");
        System.out.println("| SAVE/LOAD:                                                   |");
        System.out.println("|   - Game auto-saves on quit or resign                        |");
        System.out.println("|   - Prompted to load saved game on start                     |");
        System.out.println("+==============================================================+");
        System.out.println();
    }

    /**
     * Checks if a square is empty (either "  " or "##")
     * @param square The square content to check
     * @return true if the square is empty
     */
    private boolean isEmptySquare(String square) {
        return square.equals("  ") || square.equals("##");
    }

    /**
     * Generates a unique string key for the current board position
     * Used for threefold repetition detection
     * @return A string representing the current position
     */
    private String getPositionKey() {
        StringBuilder sb = new StringBuilder();

        // Board position
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j];
                if (!isEmptySquare(piece)) {
                    sb.append(piece).append(i).append(j);
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
     * Checks if a square is attacked by the opponent
     * @param row The row of the square to check
     * @param col The column of the square to check
     * @param byWhite If true, check if attacked by white pieces; otherwise by black
     * @return true if the square is under attack
     */
    private boolean isSquareAttacked(int row, int col, boolean byWhite) {
        String attackerPrefix = byWhite ? "w" : "b";

        // Check for pawn attacks
        int pawnDirection = byWhite ? 1 : -1;
        int pawnRow = row + pawnDirection;
        if (pawnRow >= 0 && pawnRow < 8) {
            if (col > 0 && board[pawnRow][col - 1].equals(attackerPrefix + "p")) return true;
            if (col < 7 && board[pawnRow][col + 1].equals(attackerPrefix + "p")) return true;
        }

        // Check for knight attacks
        int[] knightMoves = {-2, -1, 1, 2};
        for (int dr : knightMoves) {
            for (int dc : knightMoves) {
                if (Math.abs(dr) == Math.abs(dc)) continue;
                int nr = row + dr, nc = col + dc;
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                    if (board[nr][nc].equals(attackerPrefix + "N")) return true;
                }
            }
        }

        // Check for king attacks (for adjacent squares)
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr, nc = col + dc;
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                    if (board[nr][nc].equals(attackerPrefix + "K")) return true;
                }
            }
        }

        // Check for rook/queen attacks (straight lines)
        int[][] rookDirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] dir : rookDirs) {
            for (int dist = 1; dist < 8; dist++) {
                int nr = row + dir[0] * dist, nc = col + dir[1] * dist;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                String piece = board[nr][nc];
                if (!isEmptySquare(piece)) {
                    if (piece.startsWith(attackerPrefix) &&
                        (piece.substring(1).equals("R") || piece.substring(1).equals("Q"))) {
                        return true;
                    }
                    break;
                }
            }
        }

        // Check for bishop/queen attacks (diagonals)
        int[][] bishopDirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] dir : bishopDirs) {
            for (int dist = 1; dist < 8; dist++) {
                int nr = row + dir[0] * dist, nc = col + dir[1] * dist;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                String piece = board[nr][nc];
                if (!isEmptySquare(piece)) {
                    if (piece.startsWith(attackerPrefix) &&
                        (piece.substring(1).equals("B") || piece.substring(1).equals("Q"))) {
                        return true;
                    }
                    break;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the current player has any legal moves
     * @return true if the current player has at least one legal move
     */
    private boolean hasLegalMoves() {
        String[][] temp = new String[8][8];
        syncBoards(board, temp);

        for (int iStart = 0; iStart < 8; iStart++) {
            for (int jStart = 0; jStart < 8; jStart++) {
                String piece = board[iStart][jStart];
                if (isEmptySquare(piece)) continue;
                if ((piece.startsWith("w") && !white) || (piece.startsWith("b") && white)) continue;

                for (int iEnd = 0; iEnd < 8; iEnd++) {
                    for (int jEnd = 0; jEnd < 8; jEnd++) {
                        if (isValidMoveOnBoard(iStart, jStart, iEnd, jEnd, temp)) {
                            // Try the move
                            String captured = temp[iEnd][jEnd];
                            temp[iEnd][jEnd] = temp[iStart][jStart];
                            temp[iStart][jStart] = ((iStart % 2 == 1 && jStart % 2 == 0) ||
                                                    (iStart % 2 == 0 && jStart % 2 == 1)) ? "##" : "  ";

                            boolean stillInCheck = check(temp);

                            // Undo the move
                            temp[iStart][jStart] = temp[iEnd][jEnd];
                            temp[iEnd][jEnd] = captured;

                            if (!stillInCheck) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a move is valid on a given board (without considering check)
     * @param iStart Starting row
     * @param jStart Starting column
     * @param iEnd Ending row
     * @param jEnd Ending column
     * @param testBoard The board to test on
     * @return true if the move is valid
     */
    private boolean isValidMoveOnBoard(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        String piece = testBoard[iStart][jStart];
        if (isEmptySquare(piece)) return false;

        String pieceType = piece.substring(1);
        String color = piece.substring(0, 1);
        boolean isWhite = color.equals("w");

        // Can't capture own piece
        if (!isEmptySquare(testBoard[iEnd][jEnd]) && testBoard[iEnd][jEnd].startsWith(color)) {
            return false;
        }

        switch (pieceType) {
            case "p":
                return isValidPawnMove(iStart, jStart, iEnd, jEnd, isWhite, testBoard);
            case "R":
                return isValidRookMove(iStart, jStart, iEnd, jEnd, testBoard);
            case "N":
                return isValidKnightMove(iStart, jStart, iEnd, jEnd);
            case "B":
                return isValidBishopMove(iStart, jStart, iEnd, jEnd, testBoard);
            case "Q":
                return isValidRookMove(iStart, jStart, iEnd, jEnd, testBoard) ||
                       isValidBishopMove(iStart, jStart, iEnd, jEnd, testBoard);
            case "K":
                return Math.abs(iStart - iEnd) <= 1 && Math.abs(jStart - jEnd) <= 1;
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(int iStart, int jStart, int iEnd, int jEnd, boolean isWhite, String[][] testBoard) {
        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;

        // Single push
        if (jStart == jEnd && iEnd - iStart == direction && isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        // Double push
        if (jStart == jEnd && iStart == startRow && iEnd - iStart == 2 * direction &&
            isEmptySquare(testBoard[iStart + direction][jStart]) && isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        // Capture
        if (Math.abs(jEnd - jStart) == 1 && iEnd - iStart == direction &&
            !isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        return false;
    }

    private boolean isValidRookMove(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        if (iStart != iEnd && jStart != jEnd) return false;

        int dr = Integer.compare(iEnd, iStart);
        int dc = Integer.compare(jEnd, jStart);

        int r = iStart + dr, c = jStart + dc;
        while (r != iEnd || c != jEnd) {
            if (!isEmptySquare(testBoard[r][c])) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    private boolean isValidKnightMove(int iStart, int jStart, int iEnd, int jEnd) {
        int dr = Math.abs(iEnd - iStart);
        int dc = Math.abs(jEnd - jStart);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    private boolean isValidBishopMove(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        if (Math.abs(iEnd - iStart) != Math.abs(jEnd - jStart)) return false;

        int dr = Integer.compare(iEnd, iStart);
        int dc = Integer.compare(jEnd, jStart);

        int r = iStart + dr, c = jStart + dc;
        while (r != iEnd || c != jEnd) {
            if (!isEmptySquare(testBoard[r][c])) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    /**
     * Checks if any valid move results in the current player not being in checkmate.
     * Current player is known based on the white field which is true if it's white's turn and false if it's black's
     * @return if the current player is in checkmate
     */
    public boolean checkMate() {
        String[][] temp = new String[8][8];
        syncBoards(checkBoard, temp);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((temp[i][j].substring(0,1).equals("w") && white) || (!white && temp[i][j].substring(0,1).equals("b"))) {
                    for (int iTest = 0; iTest < 8; iTest++) {
                        for (int jTest = 0; jTest < 8; jTest++) {
                            if (moveApproval(i, j, iTest, jTest, temp)) {
                                temp[iTest][jTest] = temp[i][j];
                                temp[i][j] = "  ";
                                if (!check(temp)) {
                                    return false;
                                }
                            }
                            syncBoards(checkBoard, temp);
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks if the current player is in check on a given board
     * @param checkBoard The board in which the program will check for a check
     * @return if the current player is in check
     */
    public boolean check(String[][] checkBoard) {
        int iKing = 0;
        int jKing = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (white && checkBoard[i][j].equals("wK")) {
                    iKing = i;
                    jKing = j;
                }
                else if (!white && checkBoard[i][j].equals("bK")) {
                    iKing = i;
                    jKing = j;
                }
            }
        }
        white = !white;
        if (!white) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (checkBoard[i][j].substring(0,1).equals("b")) {
                        if (moveApproval(i, j, iKing, jKing, checkBoard)) {
                            white = !white;
                            return true;
                        }
                    }
                }
            }
        }
        else {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (checkBoard[i][j].substring(0,1).equals("w")) {
                        if (moveApproval(i, j, iKing, jKing, checkBoard)) {
                            white = !white;
                            return true;
                        }
                    }
                }
            }
        }
        white = !white;
        return false;
    }

    /**
     * Initializes a boolean array with index 2 and 6 set to true
     * Used to initialize whiteCastle and blackCastle
     * Used to check if a castle move is legal later on
     * @return boolean array with index 2 and 6 set to true
     */
    public boolean[] initializeCastle() {
        boolean[] output = new boolean[8];
        for (int i = 0; i < 8; i++) {
            output[i] = false;
        }
        output[2] = true;
        output[6] = true;
        return output;
    }

    /**
     * Initializes a boolean array in which all values are set to false
     * Used to initialize whitePassant and blackPassant
     * Used to keep track of double moves to verify if an en passant is legal
     * @return 8 length boolean array with all values set to false
     */
    public boolean[] initializePassant() {
        boolean[] output = new boolean[8];
        for (int i = 0; i < 8; i++) {
            output[i] = false;
        }
        return output;
    }

    /**
     * Initializes a 2d array of Strings
     * Set black's and white's pieces in default position and does black/white tiling
     * @return Chess board in the form of a 2d array of strings
     */
    public String[][] initializeBoard() {
        String[][] output = new String[8][8];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i % 2 == 0 && j % 2 == 1) || (i % 2 == 1 && j % 2 == 0)) {
                    output[i][j] = "##";
                }
                else {
                    output[i][j] = "  ";
                }
            }
        }

        output[0][0] = "bR";
        output[0][1] = "bN";
        output[0][2] = "bB";
        output[0][3] = "bQ";
        output[0][4] = "bK";
        output[0][5] = "bB";
        output[0][6] = "bN";
        output[0][7] = "bR";

        output[1][0] = "bp";
        output[1][1] = "bp";
        output[1][2] = "bp";
        output[1][3] = "bp";
        output[1][4] = "bp";
        output[1][5] = "bp";
        output[1][6] = "bp";
        output[1][7] = "bp";

        output[7][0] = "wR";
        output[7][1] = "wN";
        output[7][2] = "wB";
        output[7][3] = "wQ";
        output[7][4] = "wK";
        output[7][5] = "wB";
        output[7][6] = "wN";
        output[7][7] = "wR";

        output[6][0] = "wp";
        output[6][1] = "wp";
        output[6][2] = "wp";
        output[6][3] = "wp";
        output[6][4] = "wp";
        output[6][5] = "wp";
        output[6][6] = "wp";
        output[6][7] = "wp";

        return output;
    }

    /**
     * Creates a deep copy of the board
     */
    public String[][] copyBoard(String[][] src) {
        String[][] dst = new String[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                dst[i][j] = src[i][j];
            }
        }
        return dst;
    }

    /**
     * Initializes a Chess960 (Fischer Random) board with randomized back rank.
     * Rules: bishops on opposite colors, king between rooks.
     */
    public String[][] initializeChess960Board() {
        String[][] output = new String[8][8];

        // Initialize empty squares with checkerboard pattern
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i % 2 == 0 && j % 2 == 1) || (i % 2 == 1 && j % 2 == 0)) {
                    output[i][j] = "##";
                } else {
                    output[i][j] = "  ";
                }
            }
        }

        // Generate valid back rank for white (row 7)
        int[] backRank = generateChess960BackRank();

        // Place white pieces on row 7
        String[] whitePieces = {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"};
        for (int j = 0; j < 8; j++) {
            output[7][j] = whitePieces[backRank[j]];
        }

        // Place black pieces on row 0 (same order, just black)
        String[] blackPieces = {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"};
        for (int j = 0; j < 8; j++) {
            output[0][j] = blackPieces[backRank[j]];
        }

        // Place pawns
        for (int j = 0; j < 8; j++) {
            output[1][j] = "bp";
            output[6][j] = "wp";
        }

        return output;
    }

    /**
     * Generates a valid Chess960 back rank array (0-7 indices for R, N, B, Q, K, B, N, R).
     * Returns an array where backRank[j] is the piece index (0=R,1=N,2=B,3=Q,4=K,5=B,6=N,7=R).
     * Constraints: bishops on opposite colors, king between rooks.
     * Uses the standard Chess960 algorithm: place bishops first, then queen and knights,
     * then rooks on outer remaining squares with king in the middle.
     */
    private int[] generateChess960BackRank() {
        int[] backRank = new int[8];
        boolean[] used = new boolean[8]; // which columns are filled

        // 1. Place two bishops on opposite colors (one on light, one on dark)
        // Light squares: even columns (0,2,4,6) - these are light when viewed from white's perspective
        // Dark squares: odd columns (1,3,5,7)
        int[] lightSquares = {0, 2, 4, 6};
        int[] darkSquares = {1, 3, 5, 7};
        int lightBishCol = lightSquares[(int)(Math.random() * 4)];
        int darkBishCol = darkSquares[(int)(Math.random() * 4)];
        backRank[lightBishCol] = 2; // B (first bishop)
        backRank[darkBishCol] = 5;  // B (second bishop)
        used[lightBishCol] = true;
        used[darkBishCol] = true;

        // 2. Place queen on a random empty square
        List<Integer> emptySquares = new ArrayList<>();
        for (int j = 0; j < 8; j++) if (!used[j]) emptySquares.add(j);
        int queenCol = emptySquares.remove((int)(Math.random() * emptySquares.size()));
        backRank[queenCol] = 3; // Q
        used[queenCol] = true;

        // 3. Place first knight on a random empty square
        int knight1Col = emptySquares.remove((int)(Math.random() * emptySquares.size()));
        backRank[knight1Col] = 1; // N
        used[knight1Col] = true;

        // 4. Place second knight on a random empty square
        int knight2Col = emptySquares.remove((int)(Math.random() * emptySquares.size()));
        backRank[knight2Col] = 6; // N (second knight)
        used[knight2Col] = true;

        // 5. Three squares remain. Place rooks on outer two, king in middle.
        // This guarantees king is always between rooks.
        emptySquares.clear();
        for (int j = 0; j < 8; j++) if (!used[j]) emptySquares.add(j);
        // Sort to get outer squares
        java.util.Collections.sort(emptySquares);
        int leftRookCol = emptySquares.get(0);   // Leftmost empty square
        int kingCol = emptySquares.get(1);        // Middle empty square
        int rightRookCol = emptySquares.get(2);   // Rightmost empty square

        backRank[leftRookCol] = 0;  // R
        backRank[kingCol] = 4;      // K
        backRank[rightRookCol] = 7; // R (second rook)

        return backRank;
    }

    /**
     * Resets castling rights based on current king and rook positions.
     * For Chess960, castling rights are set if king and rooks are in their starting positions.
     */
    public void resetCastlingRights() {
        whiteCastle = new boolean[8];
        blackCastle = new boolean[8];

        // Find white king and rooks on row 7
        int wkCol = -1, wr1Col = -1, wr2Col = -1;
        for (int j = 0; j < 8; j++) {
            if (board[7][j].equals("wK")) wkCol = j;
            else if (board[7][j].equals("wR")) {
                if (wr1Col == -1) wr1Col = j;
                else wr2Col = j;
            }
        }
        // White castling: if king exists and rooks exist, set rights
        // White kingside: rook on right of king (higher col)
        // White queenside: rook on left of king (lower col)
        if (wkCol >= 0 && wr1Col >= 0 && wr2Col >= 0) {
            int leftRook = Math.min(wr1Col, wr2Col);
            int rightRook = Math.max(wr1Col, wr2Col);
            // Kingside: right rook exists (col 6 or 7 is typical, but any is ok if king to its left)
            if (rightRook > wkCol) {
                whiteCastle[6] = true; // Kingside available
            }
            // Queenside: left rook exists (col 0 or 2 is typical)
            if (leftRook < wkCol) {
                whiteCastle[2] = true; // Queenside available
            }
        }

        // Find black king and rooks on row 0
        int bkCol = -1, br1Col = -1, br2Col = -1;
        for (int j = 0; j < 8; j++) {
            if (board[0][j].equals("bK")) bkCol = j;
            else if (board[0][j].equals("bR")) {
                if (br1Col == -1) br1Col = j;
                else br2Col = j;
            }
        }
        if (bkCol >= 0 && br1Col >= 0 && br2Col >= 0) {
            int leftRook = Math.min(br1Col, br2Col);
            int rightRook = Math.max(br1Col, br2Col);
            if (rightRook > bkCol) blackCastle[6] = true;
            if (leftRook < bkCol) blackCastle[2] = true;
        }
    }

    /**
     * Prints out chess board with Unicode pieces and labels on all sides
     * @param board the chess board that is printed out
     */
    public void printBoard(String[][] board) {
        System.out.println();
        // Top file labels
        System.out.println("     a   b   c   d   e   f   g   h    ");
        System.out.println("   +---+---+---+---+---+---+---+---+");

        for (int i = 0; i < 8; i++) {
            // Left rank label
            System.out.print(" " + (8 - i) + " |");
            for (int j = 0; j < 8; j++) {
                String display = pieceToDisplay(board[i][j]);
                // Use proper spacing for display characters
                if (display.equals("##") || display.equals("  ")) {
                    System.out.print(" " + display + "|");
                } else {
                    System.out.print(" " + display + " |");
                }
            }
            // Right rank label
            System.out.println(" " + (8 - i));

            // Print row separator or bottom border
            if (i < 7) {
                System.out.println("   +---+---+---+---+---+---+---+---+");
            } else {
                System.out.println("   +---+---+---+---+---+---+---+---+");
            }
        }

        // Bottom file labels
        System.out.println("     a   b   c   d   e   f   g   h    ");
        System.out.println();
    }

    /**
     * Translates a letter into an array index according to how a chess board is formatted
     * @param input the given letter for a move (case-insensitive)
     * @return the letter's corresponding array index, or -1 if invalid
     */
    public int letterToCoordinate(String input) {
        if (input == null || input.isEmpty()) return -1;
        char c = Character.toLowerCase(input.charAt(0));
        if (c >= 'a' && c <= 'h') {
            return c - 'a';
        }
        return -1;
    }

    /**
     * Parses a square notation (e.g., "e4") into board indices
     * @param square The square notation (letter + number)
     * @return int array [i, j] or null if invalid
     */
    private int[] parseSquare(String square) {
        if (square == null || square.length() < 2) return null;

        int j = letterToCoordinate(square.substring(0, 1));
        if (j < 0) return null;

        try {
            int rank = Integer.parseInt(square.substring(1, 2));
            if (rank < 1 || rank > 8) return null;
            int i = 8 - rank;
            return new int[]{i, j};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Updates the board 2d array to be the same as the 2d array checkBoard
     */
    public void updateBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = checkBoard[i][j];
            }
        }
    }

    /**
     * Takes a 2d array "copy" and copies all its data into another 2d array "paste"
     * @param copy 2d array that will be copied from
     * @param paste 2d array that will be copied into
     */
    public void syncBoards(String[][] copy, String[][] paste) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                paste[i][j] = copy[i][j];
            }
        }
    }

    /**
     * Checks if a given move is from one of the 4 corners (rook) and updates the blackCastle/whiteCastle arrays to reflect the move
     * Prevents castling on a side if the rook has been moved
     * @param iStart i index of 2d array ex. board[i][j]
     * @param jStart j index of 2d array ex. board[i][j]
     */
    public void updateCastle(int iStart, int jStart) {
        // black left castle
        if (iStart == 0 && jStart == 0) {
            blackCastle[2] = false;
        }
        // black right castle
        else if (iStart == 0 && jStart == 7) {
            blackCastle[6] = false;
        }
        // white left castle
        else if (iStart == 7 && jStart == 0) {
            whiteCastle[2] = false;
        }
        // white right castle
        else if (iStart == 7 && jStart == 7) {
            whiteCastle[6] = false;
        }
    }

    /**
     * Given a 2d array representing the board and the start and end indices, return true if a move is valid and false otherwise
     * Does calculations based on the piece that is at the starting area to see it can move to the end block legally
     * Does not check if the move leaves the king in check, that is done elsewhere
     * @param iStart i index of piece ex. board[i][j]
     * @param jStart j index of piece ex. board[i][j]
     * @param iEnd i index of requested ending spot ex. board[i][j]
     * @param jEnd j index of requested ending spot ex. board[i][j]
     * @param board 2d string array in which all calculations will be done on
     * @return true if the move is valid false otherwise
     */
    public boolean moveApproval(int iStart, int jStart, int iEnd, int jEnd, String[][] board) {
        String piece = board[iStart][jStart].substring(1,2);
        if (piece.equals("p")) {
            if (white) {
                // Taking piece
                if ((jEnd-jStart == 1 || jStart-jEnd == 1) && (iStart-iEnd == 1) && board[iEnd][jEnd].substring(0,1).equals("b")) {
                }
                // en passant
                else if ((jEnd-jStart == 1 || jStart-jEnd == 1) && (iStart-iEnd == 1) && (iEnd == 2 && blackPassant[jEnd])) {
                    if (jEnd % 2 == 0) {
                        checkBoard[3][jEnd] = "##";
                    }
                    else {
                        checkBoard[3][jEnd] = "  ";
                    }
                }
                // Moving forward no piece blocking
                else if (jEnd == jStart && iStart-iEnd == 1 && isEmptySquare(board[iEnd][jEnd])) {
                }
                else if (iStart == 6 && iEnd == 4 && jStart == jEnd && isEmptySquare(board[5][jStart]) && isEmptySquare(board[4][jStart])) {
                    whitePassant[jStart] = true;
                }
                else {
                    return false;
                }
            }
            else {
                // Taking piece
                if ((jEnd-jStart == 1 || jStart-jEnd == 1) && (iEnd-iStart == 1) && (board[iEnd][jEnd].substring(0,1).equals("w"))) {
                }
                // en passant
                else if ((jEnd-jStart == 1 || jStart-jEnd == 1) && (iEnd-iStart == 1) && (iEnd == 5 && whitePassant[jEnd])) {
                    if (jEnd % 2 == 0) {
                        checkBoard[4][jEnd] = "  ";
                    }
                    else {
                        checkBoard[4][jEnd] = "##";
                    }
                }
                // Moving forward no piece blocking
                else if (jEnd == jStart && iEnd-iStart == 1 && isEmptySquare(board[iEnd][jEnd])) {
                }
                else if (iStart == 1 && iEnd == 3 && jStart == jEnd && isEmptySquare(board[2][jStart]) && isEmptySquare(board[3][jStart])) {
                    blackPassant[jStart] = true;
                }
                else {
                    return false;
                }
            }
            if (white && iEnd == 0) {
                if (promo.equals("B")) {
                    checkBoard[iEnd][jEnd] = "wB";
                }
                else if (promo.equals("N")) {
                    checkBoard[iEnd][jEnd] = "wN";
                }
                else if (promo.equals("R")) {
                    checkBoard[iEnd][jEnd] = "wR";
                }
                else {
                    checkBoard[iEnd][jEnd] = "wQ";
                }
            }
            if (!white && iEnd == 7) {
                if (promo.equals("B")) {
                    checkBoard[iEnd][jEnd] = "bB";
                }
                else if (promo.equals("N")) {
                    checkBoard[iEnd][jEnd] = "bN";
                }
                else if (promo.equals("R")) {
                    checkBoard[iEnd][jEnd] = "bR";
                }
                else {
                    checkBoard[iEnd][jEnd] = "bQ";
                }
            }
            return true;
        }
        else if (piece.equals("R")) {
            if (iStart == iEnd && jStart == jEnd) {
                return false;
            }
            else if (iStart == iEnd) {
                boolean val = true;
                if (jStart > jEnd) {
                    for (int i = 1; i < jStart - jEnd; i++) {
                        if (!board[iStart][jStart - i].equals("  ") && !board[iStart][jStart - i].equals("##")) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (white) {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                val = false;
                        }
                    }
                    if (val) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    for (int i = 1; i < jEnd - jStart; i++) {
                        if (!board[iStart][jStart + i].equals("  ") && !board[iStart][jStart + i].equals("##")) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (white) {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                val = false;
                            }
                    }
                    if (val) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            else if (jStart == jEnd) {
                boolean val = true;
                if (iStart > iEnd) {
                    for (int i = 1; i < iStart - iEnd; i++) {
                        if (!board[iStart - i][jStart].equals("  ") && !board[iStart - i][jStart].equals("##")) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (white) {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                val = false;
                            }
                    }
                    if (val) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    for (int i = 1; i < iEnd - iStart; i++) {
                        if (!board[iStart + i][jStart].equals("  ") && !board[iStart + i][jStart].equals("##")) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (white) {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                val = false;
                            }
                    }
                    if (val) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            else {
                return false;
            }
        }
        else if (piece.equals("N")) {
            if ((Math.abs(iEnd-iStart) == 2 && Math.abs(jEnd-jStart) == 1) || (Math.abs(jEnd-jStart) == 2 && Math.abs(iEnd-iStart) == 1)) {
                if (white && (board[iEnd][jEnd].substring(0,1).equals("b") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].equals("  "))) {
                    return true;
                }
                else if (!white && (board[iEnd][jEnd].substring(0,1).equals("w") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].equals("  "))) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (piece.equals("B")) {
            if(Math.abs(iStart - iEnd) != Math.abs(jStart -jEnd)) {
                return false;
            }
            else if (iStart < iEnd && jStart < jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!board[iStart + i][jStart + i].equals("  ") && !board[iStart + i][jStart + i].equals("##")) {
                        return false;
                    }
                }
                if (white) {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart < iEnd && jStart > jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!board[iStart + i][jStart - i].equals("  ") && !board[iStart + i][jStart - i].equals("##")) {
                        return false;
                    }
                }
                if (white) {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart > iEnd && jStart < jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!board[iStart - i][jStart + i].equals("  ") && !board[iStart - i][jStart + i].equals("##")) {
                        return false;
                    }
                }
                if (white) {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart > iEnd && jStart > jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!board[iStart - i][jStart - i].equals("  ") && !board[iStart - i][jStart - i].equals("##")) {
                        return false;
                    }
                }
                if (white) {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }
        else if (piece.equals("Q")) {
            if (iStart == iEnd || jStart == jEnd) {
                if (iStart == iEnd) {
                    boolean val = true;
                    if (jStart > jEnd) {
                        for (int i = 1; i < jStart - jEnd; i++) {
                            if (!board[iStart][jStart - i].equals("  ") && !board[iStart][jStart - i].equals("##")) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (white) {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                    val = false;
                            }
                        }
                        if (val) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                    else {
                        for (int i = 1; i < jEnd - jStart; i++) {
                            if (!board[iStart][jStart + i].equals("  ") && !board[iStart][jStart + i].equals("##")) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (white) {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                    val = false;
                                }
                        }
                        if (val) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }
                else if (jStart == jEnd) {
                    boolean val = true;
                    if (iStart > iEnd) {
                        for (int i = 1; i < iStart - iEnd; i++) {
                            if (!board[iStart - i][jStart].equals("  ") && !board[iStart - i][jStart].equals("##")) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (white) {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                    val = false;
                                }
                        }
                        if (val) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                    else {
                        for (int i = 1; i < iEnd - iStart; i++) {
                            if (!board[iStart + i][jStart].equals("  ") && !board[iStart + i][jStart].equals("##")) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (white) {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##") || board[iEnd][jEnd].substring(0,1).equals("w"))) {
                                    val = false;
                                }
                        }
                        if (val) {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }
                else {
                    return false;
                }
            }
            else if (Math.abs(iStart - iEnd) == Math.abs(jStart - jEnd)) {
                if (iStart < iEnd && jStart < jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!board[iStart + i][jStart + i].equals("  ") && !board[iStart + i][jStart + i].equals("##")) {
                            return false;
                        }
                    }
                    if (white) {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart < iEnd && jStart > jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!board[iStart + i][jStart - i].equals("  ") && !board[iStart + i][jStart - i].equals("##")) {
                            return false;
                        }
                    }
                    if (white) {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart > iEnd && jStart < jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!board[iStart - i][jStart + i].equals("  ") && !board[iStart - i][jStart + i].equals("##")) {
                            return false;
                        }
                    }
                    if (white) {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart > iEnd && jStart > jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!board[iStart - i][jStart - i].equals("  ") && !board[iStart - i][jStart - i].equals("##")) {
                            return false;
                        }
                    }
                    if (white) {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!board[iEnd][jEnd].equals("  ") && !board[iEnd][jEnd].equals("##") && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else if (piece.equals("K")) {
            if (Math.abs(iStart-iEnd) <= 1 && Math.abs(jStart-jEnd) <= 1) {
                if ((board[iEnd][jEnd].substring(0,1).equals("w") || board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##")) && !white) {
                    blackCastle[2] = false;
                    blackCastle[6] = false;
                    return true;
                }
                else if ((board[iEnd][jEnd].substring(0,1).equals("b") || board[iEnd][jEnd].equals("  ") || board[iEnd][jEnd].equals("##")) && white) {
                    whiteCastle[2] = false;
                    whiteCastle[6] = false;
                    return true;
                }
                else {
                    return false;
                }
            }
            else if (white && iEnd == 7) {
                // White castling - must not be in check
                // Find king position dynamically (works for Chess960)
                int kingCol = -1;
                for (int j = 0; j < 8; j++) {
                    if (board[7][j].equals("wK")) { kingCol = j; break; }
                }
                if (kingCol < 0) return false;

                if (isSquareAttacked(7, kingCol, false)) {
                    return false; // King is in check, cannot castle
                }

                // Kingside castling (king to g, col 6)
                if (jEnd == 6 && whiteCastle[6]) {
                    // Find the rook to the right of the king (for kingside)
                    int rookCol = -1;
                    for (int j = kingCol + 1; j < 8; j++) {
                        if (board[7][j].equals("wR")) { rookCol = j; break; }
                    }
                    if (rookCol < 0) return false; // No rook found for castling

                    // Chess960 castling: all squares king and rook pass over must be empty
                    // (except their starting squares which they vacate)
                    // King destination: col 6, Rook destination: col 5
                    // King path: from kingCol to 6 (exclusive of kingCol, inclusive of 6, but allow rookCol)
                    // Rook path: from rookCol to 5 (exclusive of rookCol, inclusive of 5, but allow kingCol)

                    // Check king's path (from min(kingCol,6) to max(kingCol,6), excluding kingCol, allowing rookCol)
                    int kingPathStart = Math.min(kingCol, 6);
                    int kingPathEnd = Math.max(kingCol, 6);
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (j == kingCol) continue; // King's start is OK
                        if (j == rookCol) continue; // Rook's start is OK (rook moves out)
                        if (!isEmptySquare(board[7][j])) return false;
                    }

                    // Check rook's path (from min(rookCol,5) to max(rookCol,5), excluding rookCol, allowing kingCol)
                    int rookPathStart = Math.min(rookCol, 5);
                    int rookPathEnd = Math.max(rookCol, 5);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue; // Rook's start is OK
                        if (j == kingCol) continue; // King's start is OK (king moves out)
                        if (!isEmptySquare(board[7][j])) return false;
                    }

                    // Check if king passes through or lands on attacked square
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(7, j, false)) return false;
                    }

                    // Execute: move rook to f (col 5)
                    // Don't clear rook's old position if it's the king's destination (adjacent case)
                    if (rookCol != 6) {
                        checkBoard[7][rookCol] = (rookCol % 2 == 1) ? "##" : "  ";
                    }
                    checkBoard[7][5] = "wR";
                    whiteCastle[2] = false;
                    whiteCastle[6] = false;
                    return true;
                }
                // Queenside castling (king to c, col 2)
                else if (jEnd == 2 && whiteCastle[2]) {
                    // Find the rook to the left of the king (for queenside)
                    int rookCol = -1;
                    for (int j = kingCol - 1; j >= 0; j--) {
                        if (board[7][j].equals("wR")) { rookCol = j; break; }
                    }
                    if (rookCol < 0) return false;

                    // King destination: col 2, Rook destination: col 3
                    int kingPathStart = Math.min(kingCol, 2);
                    int kingPathEnd = Math.max(kingCol, 2);
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (j == kingCol) continue;
                        if (j == rookCol) continue;
                        if (!isEmptySquare(board[7][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 3);
                    int rookPathEnd = Math.max(rookCol, 3);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!isEmptySquare(board[7][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(7, j, false)) return false;
                    }

                    // Don't clear rook's old position if it's the king's destination (adjacent case)
                    if (rookCol != 2) {
                        checkBoard[7][rookCol] = (rookCol % 2 == 1) ? "##" : "  ";
                    }
                    checkBoard[7][3] = "wR";
                    whiteCastle[2] = false;
                    whiteCastle[6] = false;
                    return true;
                }
                else {
                    return false;
                }
            }
            else if (!white && iEnd == 0) {
                // Black castling - must not be in check
                int kingCol = -1;
                for (int j = 0; j < 8; j++) {
                    if (board[0][j].equals("bK")) { kingCol = j; break; }
                }
                if (kingCol < 0) return false;

                if (isSquareAttacked(0, kingCol, true)) {
                    return false; // King is in check, cannot castle
                }

                // Kingside castling (king to g, col 6)
                if (jEnd == 6 && blackCastle[6]) {
                    int rookCol = -1;
                    for (int j = kingCol + 1; j < 8; j++) {
                        if (board[0][j].equals("bR")) { rookCol = j; break; }
                    }
                    if (rookCol < 0) return false;

                    int kingPathStart = Math.min(kingCol, 6);
                    int kingPathEnd = Math.max(kingCol, 6);
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (j == kingCol) continue;
                        if (j == rookCol) continue;
                        if (!isEmptySquare(board[0][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 5);
                    int rookPathEnd = Math.max(rookCol, 5);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!isEmptySquare(board[0][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(0, j, true)) return false;
                    }

                    // Don't clear rook's old position if it's the king's destination (adjacent case)
                    if (rookCol != 6) {
                        checkBoard[0][rookCol] = (rookCol % 2 == 1) ? "##" : "  ";
                    }
                    checkBoard[0][5] = "bR";
                    blackCastle[2] = false;
                    blackCastle[6] = false;
                    return true;
                }
                // Queenside castling (king to c, col 2)
                else if (jEnd == 2 && blackCastle[2]) {
                    int rookCol = -1;
                    for (int j = kingCol - 1; j >= 0; j--) {
                        if (board[0][j].equals("bR")) { rookCol = j; break; }
                    }
                    if (rookCol < 0) return false;

                    int kingPathStart = Math.min(kingCol, 2);
                    int kingPathEnd = Math.max(kingCol, 2);
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (j == kingCol) continue;
                        if (j == rookCol) continue;
                        if (!isEmptySquare(board[0][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 3);
                    int rookPathEnd = Math.max(rookCol, 3);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!isEmptySquare(board[0][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(0, j, true)) return false;
                    }

                    // Don't clear rook's old position if it's the king's destination (adjacent case)
                    if (rookCol != 2) {
                        checkBoard[0][rookCol] = (rookCol % 2 == 1) ? "##" : "  ";
                    }
                    checkBoard[0][3] = "bR";
                    blackCastle[2] = false;
                    blackCastle[6] = false;
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    /**
     * Given an array representing the inputs gotten from the scanner, returns true if a the move is valid
     * First has letters translated into array indices and also translates the numbers into their corresponding array indices
     * Then updates checkboard to reflect the desired move
     * Then checks if the desired move is valid
     * @param inputs String array representing the inputs given ex. e2 e4
     * @return whether or not the move is valid
     */
    public boolean validMove(String[] inputs) {
        if (inputs == null || inputs.length < 2) {
            lastError = "Invalid move format. Use: e2 e4, e2e4, or e4";
            return false;
        }

        int jStart, iStart, jEnd, iEnd;

        try {
            jStart = letterToCoordinate(inputs[0].substring(0, 1));
            iStart = 8 - Integer.parseInt(inputs[0].substring(1, 2));
            jEnd = letterToCoordinate(inputs[1].substring(0, 1));
            iEnd = 8 - Integer.parseInt(inputs[1].substring(1, 2));
        } catch (Exception e) {
            lastError = "Invalid move format. Use: e2 e4, e2e4, or e4";
            return false;
        }

        if (jStart > 7 || jStart < 0 || iStart > 7 || iStart < 0 || jEnd > 7 || jEnd < 0 || iEnd > 7 || iEnd < 0) {
            lastError = "Square out of bounds. Use a-h for files and 1-8 for ranks.";
            return false;
        }

        if (white) {
            if (!checkBoard[iStart][jStart].substring(0, 1).equals("w")) {
                lastError = "No white piece on " + inputs[0] + ". Select one of your pieces.";
                return false;
            }
            String start = board[iStart][jStart];
            String captured = board[iEnd][jEnd];
            boolean isPawnMove = start.substring(1).equals("p");
            boolean isCapture = !isEmptySquare(captured);

            // Store state before move for history/undo
            boolean[] oldWhiteCastle = whiteCastle.clone();
            boolean[] oldBlackCastle = blackCastle.clone();
            boolean[] oldWhitePassant = whitePassant.clone();
            boolean[] oldBlackPassant = blackPassant.clone();
            int oldHalfMoveClock = halfMoveClock;

            checkBoard[iEnd][jEnd] = start;
            if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                checkBoard[iStart][jStart] = "##";
            } else {
                checkBoard[iStart][jStart] = "  ";
            }
            if (check(checkBoard)) {
                lastError = "Move would leave king in check.";
                return false;
            }
            if (moveApproval(iStart, jStart, iEnd, jEnd, board)) {
                // Create move record for history
                String moveNotation = inputs[0] + inputs[1];
                MoveRecord record = new MoveRecord(moveNotation, start, captured,
                        iStart, jStart, iEnd, jEnd,
                        oldWhiteCastle, oldBlackCastle,
                        oldWhitePassant, oldBlackPassant,
                        oldHalfMoveClock, true);

                // Check for special moves (castling)
                // Detect castling: king moves to g (col 6) or c (col 2) on back rank with castling rights
                boolean isCastlingMove = false;
                if (start.substring(1).equals("K")) {
                    if ((jEnd == 6 && ((white && whiteCastle[6]) || (!white && blackCastle[6]))) ||
                        (jEnd == 2 && ((white && whiteCastle[2]) || (!white && blackCastle[2])))) {
                        isCastlingMove = true;
                    } else if (Math.abs(jEnd - jStart) == 2) {
                        // Also catch 2-square moves (standard chess castling or some Chess960 positions)
                        isCastlingMove = true;
                    }
                }
                if (isCastlingMove) {
                    record.wasCastling = true;
                    record.castlingType = (jEnd > jStart) ? "K" : "Q";
                    // Set rook positions for undo - find rook dynamically
                    int rookFromCol = -1;
                    if (record.castlingType.equals("K")) {
                        // Kingside: find rook to the right of starting king position
                        for (int j = jStart + 1; j < 8; j++) {
                            if (board[iStart][j].equals(white ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 7;
                        record.rookToRow = iStart;
                        record.rookToCol = 5;
                    } else {
                        // Queenside: find rook to the left of starting king position
                        for (int j = jStart - 1; j >= 0; j--) {
                            if (board[iStart][j].equals(white ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 0;
                        record.rookToRow = iStart;
                        record.rookToCol = 3;
                    }
                }
                // Detect en passant capture (pawn diagonal move, target square was empty, captured a pawn)
                if (isPawnMove && Math.abs(jEnd - jStart) == 1 && isEmptySquare(captured)) {
                    // Check if this was en passant by looking at passant arrays
                    boolean enPassantPossible = (white && blackPassant[jEnd] && iEnd == 2) ||
                                                 (!white && whitePassant[jEnd] && iEnd == 5);
                    if (enPassantPossible) {
                        record.wasEnPassant = true;
                        // Captured pawn is on the row it moved to (one row behind the capturing pawn's destination)
                        // For white capturing en passant: captured pawn is at row iEnd-1 (black moved forward)
                        // For black capturing en passant: captured pawn is at row iEnd+1 (white moved forward)
                        record.enPassantCapturedRow = white ? (iEnd - 1) : (iEnd + 1);
                        record.enPassantCapturedCol = jEnd;
                        record.capturedPiece = white ? "bp" : "wp";
                    }
                }
                if (isPawnMove && (iEnd == 0 || iEnd == 7)) {
                    record.wasPromotion = true;
                    record.promotedTo = promo;
                }

                moveHistory.add(record);

                updateBoard();
                updateCastle(iStart, jStart);

                // Update halfmove clock for 50 move rule
                if (isPawnMove || isCapture) {
                    halfMoveClock = 0;
                } else {
                    halfMoveClock++;
                }

                return true;
            } else {
                lastError = "Invalid move for this piece.";
                return false;
            }
        } else {
            if (!checkBoard[iStart][jStart].substring(0, 1).equals("b")) {
                lastError = "No black piece on " + inputs[0] + ". Select one of your pieces.";
                return false;
            }
            String start = board[iStart][jStart];
            String captured = board[iEnd][jEnd];
            boolean isPawnMove = start.substring(1).equals("p");
            boolean isCapture = !isEmptySquare(captured);

            // Store state before move for history/undo
            boolean[] oldWhiteCastle = whiteCastle.clone();
            boolean[] oldBlackCastle = blackCastle.clone();
            boolean[] oldWhitePassant = whitePassant.clone();
            boolean[] oldBlackPassant = blackPassant.clone();
            int oldHalfMoveClock = halfMoveClock;

            checkBoard[iEnd][jEnd] = start;
            if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                checkBoard[iStart][jStart] = "##";
            } else {
                checkBoard[iStart][jStart] = "  ";
            }
            if (check(checkBoard)) {
                lastError = "Move would leave king in check.";
                return false;
            }
            if (moveApproval(iStart, jStart, iEnd, jEnd, board)) {
                // Create move record for history
                String moveNotation = inputs[0] + inputs[1];
                MoveRecord record = new MoveRecord(moveNotation, start, captured,
                        iStart, jStart, iEnd, jEnd,
                        oldWhiteCastle, oldBlackCastle,
                        oldWhitePassant, oldBlackPassant,
                        oldHalfMoveClock, false);

                // Check for special moves (castling)
                // Detect castling: king moves to g (col 6) or c (col 2) on back rank with castling rights
                boolean isCastlingMoveBlack = false;
                if (start.substring(1).equals("K")) {
                    if ((jEnd == 6 && ((white && whiteCastle[6]) || (!white && blackCastle[6]))) ||
                        (jEnd == 2 && ((white && whiteCastle[2]) || (!white && blackCastle[2])))) {
                        isCastlingMoveBlack = true;
                    } else if (Math.abs(jEnd - jStart) == 2) {
                        isCastlingMoveBlack = true;
                    }
                }
                if (isCastlingMoveBlack) {
                    record.wasCastling = true;
                    record.castlingType = (jEnd > jStart) ? "K" : "Q";
                    // Set rook positions for undo - find rook dynamically
                    int rookFromCol = -1;
                    if (record.castlingType.equals("K")) {
                        for (int j = jStart + 1; j < 8; j++) {
                            if (board[iStart][j].equals(white ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 7;
                        record.rookToRow = iStart;
                        record.rookToCol = 5;
                    } else {
                        for (int j = jStart - 1; j >= 0; j--) {
                            if (board[iStart][j].equals(white ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 0;
                        record.rookToRow = iStart;
                        record.rookToCol = 3;
                    }
                }
                // Detect en passant
                if (isPawnMove && Math.abs(jEnd - jStart) == 1 && isEmptySquare(captured)) {
                    boolean enPassantPossible = (white && blackPassant[jEnd] && iEnd == 2) ||
                                                 (!white && whitePassant[jEnd] && iEnd == 5);
                    if (enPassantPossible) {
                        record.wasEnPassant = true;
                        // Captured pawn is on the row it moved to (one row behind the capturing pawn's destination)
                        // For white capturing en passant: captured pawn is at row iEnd-1 (black moved forward)
                        // For black capturing en passant: captured pawn is at row iEnd+1 (white moved forward)
                        record.enPassantCapturedRow = white ? (iEnd - 1) : (iEnd + 1);
                        record.enPassantCapturedCol = jEnd;
                        record.capturedPiece = white ? "bp" : "wp";
                    }
                }
                if (isPawnMove && (iEnd == 0 || iEnd == 7)) {
                    record.wasPromotion = true;
                    record.promotedTo = promo;
                }

                moveHistory.add(record);

                updateCastle(iStart, jStart);
                updateBoard();

                // Update halfmove clock for 50 move rule
                if (isPawnMove || isCapture) {
                    halfMoveClock = 0;
                } else {
                    halfMoveClock++;
                }

                return true;
            } else {
                lastError = "Invalid move for this piece.";
                return false;
            }
        }
    }

    /**
     * Attempts to find a pawn that can move to the target square
     * Used for implicit pawn moves like "e4"
     * @param targetSquare The target square (e.g., "e4")
     * @return String array [from, to] or null if no valid pawn move found
     */
    private String[] findImplicitPawnMove(String targetSquare) {
        int[] target = parseSquare(targetSquare);
        if (target == null) return null;

        int iEnd = target[0];
        int jEnd = target[1];
        String colorPrefix = white ? "w" : "b";
        String pawnPiece = colorPrefix + "p";

        // Check for pawns that can move to this square
        // For white: pawns move up (decreasing i)
        // For black: pawns move down (increasing i)

        if (white) {
            // Single push from i+1
            if (iEnd + 1 < 8 && board[iEnd + 1][jEnd].equals(pawnPiece)) {
                String fromSquare = "" + (char)('a' + jEnd) + (8 - (iEnd + 1));
                return new String[]{fromSquare, targetSquare};
            }
            // Double push from i+2 (only from starting position)
            if (iEnd == 4 && iEnd + 2 < 8 && board[iEnd + 2][jEnd].equals(pawnPiece) &&
                isEmptySquare(board[iEnd + 1][jEnd]) && isEmptySquare(board[iEnd][jEnd])) {
                String fromSquare = "" + (char)('a' + jEnd) + (8 - (iEnd + 2));
                return new String[]{fromSquare, targetSquare};
            }
            // Capture from diagonal
            for (int dj = -1; dj <= 1; dj += 2) {
                int jStart = jEnd + dj;
                int iStart = iEnd + 1;
                if (jStart >= 0 && jStart < 8 && iStart < 8) {
                    if (board[iStart][jStart].equals(pawnPiece)) {
                        String fromSquare = "" + (char)('a' + jStart) + (8 - iStart);
                        // Check if there's an enemy piece to capture or en passant
                        if (!isEmptySquare(board[iEnd][jEnd]) && board[iEnd][jEnd].startsWith("b")) {
                            return new String[]{fromSquare, targetSquare};
                        }
                        // En passant
                        if (iEnd == 2 && blackPassant[jEnd]) {
                            return new String[]{fromSquare, targetSquare};
                        }
                    }
                }
            }
        } else {
            // Single push from i-1
            if (iEnd - 1 >= 0 && board[iEnd - 1][jEnd].equals(pawnPiece)) {
                String fromSquare = "" + (char)('a' + jEnd) + (8 - (iEnd - 1));
                return new String[]{fromSquare, targetSquare};
            }
            // Double push from i-2 (only from starting position)
            if (iEnd == 3 && iEnd - 2 >= 0 && board[iEnd - 2][jEnd].equals(pawnPiece) &&
                isEmptySquare(board[iEnd - 1][jEnd]) && isEmptySquare(board[iEnd][jEnd])) {
                String fromSquare = "" + (char)('a' + jEnd) + (8 - (iEnd - 2));
                return new String[]{fromSquare, targetSquare};
            }
            // Capture from diagonal
            for (int dj = -1; dj <= 1; dj += 2) {
                int jStart = jEnd + dj;
                int iStart = iEnd - 1;
                if (jStart >= 0 && jStart < 8 && iStart >= 0) {
                    if (board[iStart][jStart].equals(pawnPiece)) {
                        String fromSquare = "" + (char)('a' + jStart) + (8 - iStart);
                        // Check if there's an enemy piece to capture or en passant
                        if (!isEmptySquare(board[iEnd][jEnd]) && board[iEnd][jEnd].startsWith("w")) {
                            return new String[]{fromSquare, targetSquare};
                        }
                        // En passant
                        if (iEnd == 5 && whitePassant[jEnd]) {
                            return new String[]{fromSquare, targetSquare};
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Handles castling input (O-O or O-O-O)
     * For Chess960, finds the king dynamically and moves it to standard castling destination.
     * @param castleInput The castle notation
     * @return true if castling was successful
     */
    private boolean handleCastling(String castleInput) {
        // Find king position dynamically (works for both standard and Chess960)
        int kingRow = white ? 7 : 0;
        int kingCol = -1;
        for (int j = 0; j < 8; j++) {
            if (board[kingRow][j].equals(white ? "wK" : "bK")) {
                kingCol = j;
                break;
            }
        }
        if (kingCol < 0) return false; // No king found

        String kingSquare = "" + (char)('a' + kingCol) + (8 - kingRow);
        String targetSquare;

        if (castleInput.equalsIgnoreCase("O-O") || castleInput.equals("0-0")) {
            targetSquare = white ? "g1" : "g8";
        } else if (castleInput.equalsIgnoreCase("O-O-O") || castleInput.equals("0-0-0")) {
            targetSquare = white ? "c1" : "c8";
        } else {
            return false;
        }

        return validMove(new String[]{kingSquare, targetSquare});
    }

    /**
     * Parses and validates user input with robust error handling
     * Supports multiple formats: e2 e4, e2e4, e4, O-O, O-O-O
     * @param input String representing the next line gotten by the scanner
     * @return true if the moves reflected by the input are ultimately valid
     */
    public boolean inputParse(String input) {
        lastError = "";

        // Handle null or empty input
        if (input == null || input.trim().isEmpty()) {
            lastError = "No input received. Type 'help' for instructions.";
            return false;
        }

        // Normalize input
        input = input.trim();

        // Handle commands
        if (input.equalsIgnoreCase("help")) {
            showHelp();
            return false; // Return false to re-prompt for move
        }

        if (input.equalsIgnoreCase("history")) {
            showHistory();
            return false; // Return false to re-prompt for move
        }

        if (input.equalsIgnoreCase("undo")) {
            if (undoMove()) {
                // Sync checkBoard with board
                syncBoards(board, checkBoard);
                // Print board so user can see the state after undo
                printBoard(board);
                System.out.println(white ? "White's turn" : "Black's turn");
                return false; // Re-prompt for move
            } else {
                return false; // Error already set in lastError
            }
        }

        if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
            // Save with name prompt happens at end of play()
            complete = true;
            draw = false;
            return true;
        }

        if (input.equalsIgnoreCase("resign")) {
            // Save with name prompt happens at end of play()
            draw = false;
            complete = true;
            return true;
        }

        // Handle castling
        if (input.equalsIgnoreCase("O-O") || input.equalsIgnoreCase("O-O-O") ||
            input.equals("0-0") || input.equals("0-0-0")) {
            return handleCastling(input);
        }

        // Parse the input into tokens
        String[] tokens;
        if (input.contains(" ")) {
            tokens = input.split("\\s+");
        } else {
            // Check for formats like "e2e4" or "e2e4Q"
            tokens = new String[]{input};
        }

        // Handle draw offer
        boolean drawOffer = false;
        String promotionPiece = null;

        // Check for draw offer or promotion in tokens
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("draw?")) {
                drawOffer = true;
                tokens[i] = null;
            }
        }

        // Filter out null tokens and check for promotion
        List<String> moveTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) continue;

            // Check if last character is promotion piece (e.g., "e7e8Q")
            if (token.length() >= 4) {
                char lastChar = Character.toUpperCase(token.charAt(token.length() - 1));
                if (lastChar == 'Q' || lastChar == 'R' || lastChar == 'B' || lastChar == 'N') {
                    promotionPiece = String.valueOf(lastChar);
                    token = token.substring(0, token.length() - 1);
                }
            }
            moveTokens.add(token);
        }

        if (drawOffer) {
            offerdraw = true;
        }

        // Set promotion piece
        if (promotionPiece != null) {
            promo = promotionPiece;
        } else {
            promo = "Q"; // Default to Queen
        }

        // Handle different input formats
        String[] moveArgs;

        if (moveTokens.size() == 1) {
            String singleToken = moveTokens.get(0);

            // Check if it's a combined move like "e2e4"
            if (singleToken.length() >= 4) {
                String from = singleToken.substring(0, 2);
                String to = singleToken.substring(2, 4);
                moveArgs = new String[]{from, to};
            }
            // Check if it's an implicit pawn move like "e4"
            else if (singleToken.length() == 2) {
                moveArgs = findImplicitPawnMove(singleToken);
                if (moveArgs == null) {
                    lastError = "No valid pawn move to " + singleToken + " found.";
                    return false;
                }
            } else {
                lastError = "Invalid move format. Use: e2 e4, e2e4, or e4";
                return false;
            }
        } else if (moveTokens.size() >= 2) {
            moveArgs = new String[]{moveTokens.get(0), moveTokens.get(1)};
        } else {
            lastError = "Invalid move format. Type 'help' for instructions.";
            return false;
        }

        return validMove(moveArgs);
    }

    /**
     * Initializes data structures / fields
     */
    public void init() {
        board = initializeBoard();
        checkBoard = initializeBoard();
        whitePassant = initializePassant();
        blackPassant = initializePassant();
        whiteCastle = initializeCastle();
        blackCastle = initializeCastle();
        promo = "Q";
        lastError = "";
        halfMoveClock = 0;
        positionHistory.clear();
        drawReason = "";

        // Record initial position
        String initialPosition = getPositionKey();
        positionHistory.put(initialPosition, 1);
    }

    /**
     * Checks for automatic draw conditions (50 move rule, threefold repetition)
     * @return true if an automatic draw condition is met
     */
    private boolean checkAutomaticDraws() {
        // 50 move rule (100 half-moves = 50 full moves)
        if (halfMoveClock >= 100) {
            drawReason = "50 move rule";
            return true;
        }

        // Threefold repetition
        String currentPosition = getPositionKey();
        Integer count = positionHistory.get(currentPosition);
        if (count != null && count >= 3) {
            drawReason = "threefold repetition";
            return true;
        }

        return false;
    }

    /**
     * Checks for stalemate (no legal moves but not in check)
     * @return true if the current player is in stalemate
     */
    private boolean isStalemate() {
        // If in check, it's not stalemate (could be checkmate)
        if (check(checkBoard)) {
            return false;
        }
        // If has legal moves, not stalemate
        return !hasLegalMoves();
    }

    /**
     * Displays the move history
     */
    private void showHistory() {
        System.out.println();
        System.out.println("+================ MOVE HISTORY ================+");
        if (moveHistory.isEmpty()) {
            System.out.println("| No moves have been made yet.                 |");
        } else {
            System.out.println("| Move  White    Black                        |");
            System.out.println("| ----  ------   ------                       |");
            for (int i = 0; i < moveHistory.size(); i += 2) {
                int moveNum = (i / 2) + 1;
                String whiteMove = formatMoveForHistory(moveHistory.get(i));
                String blackMove = (i + 1 < moveHistory.size()) ?
                        formatMoveForHistory(moveHistory.get(i + 1)) : "";
                System.out.printf("| %4d  %-8s %-8s                     |%n", moveNum, whiteMove, blackMove);
            }
        }
        System.out.println("+==============================================+");
        System.out.println();
    }

    /**
     * Formats a move record for display in history
     */
    private String formatMoveForHistory(MoveRecord record) {
        String notation = record.moveNotation;
        if (record.wasCastling) {
            if ("K".equals(record.castlingType)) {
                return "O-O";
            } else {
                return "O-O-O";
            }
        }
        if (record.wasPromotion && record.promotedTo != null) {
            notation += "=" + record.promotedTo;
        }
        if (!isEmptySquare(record.capturedPiece)) {
            // Add 'x' for captures
            String from = notation.substring(0, 2);
            String to = notation.substring(2, 4);
            notation = from + "x" + to;
            if (record.wasPromotion && record.promotedTo != null) {
                notation += "=" + record.promotedTo;
            }
        }
        return notation;
    }

    /**
     * Undoes the last move in the history
     * @return true if a move was undone, false if no moves to undo
     */
    private boolean undoMove() {
        if (moveHistory.isEmpty()) {
            lastError = "No moves to undo.";
            return false;
        }

        MoveRecord lastMove = moveHistory.remove(moveHistory.size() - 1);

        // Restore the piece to its original position
        String pieceToRestore = lastMove.piece;

        // For promotion, restore the original pawn
        if (lastMove.wasPromotion) {
            pieceToRestore = lastMove.wasWhiteTurn ? "wp" : "bp";
        }

        // Restore board
        board[lastMove.fromRow][lastMove.fromCol] = pieceToRestore;

        // Restore captured piece or empty square
        if (lastMove.wasEnPassant) {
            // En passant: captured pawn is on a different square
            board[lastMove.toRow][lastMove.toCol] = (lastMove.toRow + lastMove.toCol) % 2 == 1 ? "##" : "  ";
            board[lastMove.enPassantCapturedRow][lastMove.enPassantCapturedCol] = lastMove.capturedPiece;
        } else if (lastMove.wasCastling) {
            // Castling: also move the rook back
            board[lastMove.toRow][lastMove.toCol] = (lastMove.toRow + lastMove.toCol) % 2 == 1 ? "##" : "  ";
            board[lastMove.fromRow][lastMove.fromCol] = pieceToRestore;
            // Rook back
            String rook = lastMove.wasWhiteTurn ? "wR" : "bR";
            board[lastMove.rookFromRow][lastMove.rookFromCol] = rook;
            board[lastMove.rookToRow][lastMove.rookToCol] = (lastMove.rookToRow + lastMove.rookToCol) % 2 == 1 ? "##" : "  ";
        } else {
            // Normal move
            if (!isEmptySquare(lastMove.capturedPiece)) {
                board[lastMove.toRow][lastMove.toCol] = lastMove.capturedPiece;
            } else {
                // Restore empty square with proper checkerboard
                board[lastMove.toRow][lastMove.toCol] = (lastMove.toRow + lastMove.toCol) % 2 == 1 ? "##" : "  ";
            }
        }

        // Restore game state
        whiteCastle = lastMove.whiteCastleBefore.clone();
        blackCastle = lastMove.blackCastleBefore.clone();
        whitePassant = lastMove.whitePassantBefore.clone();
        blackPassant = lastMove.blackPassantBefore.clone();
        halfMoveClock = lastMove.halfMoveClockBefore;

        // Decrement position history for the position we undid (the position after the move)
        String undonePosition = getPositionKey();
        if (positionHistory.containsKey(undonePosition)) {
            int count = positionHistory.get(undonePosition);
            if (count <= 1) {
                positionHistory.remove(undonePosition);
            } else {
                positionHistory.put(undonePosition, count - 1);
            }
        }

        // Switch turn back
        white = !white;

        // Clear error
        lastError = "";

        return true;
    }

    /**
     * Saves the current game state to a named file
     * @param gameName The name for the saved game
     * @return true if saved successfully, false otherwise
     */
    private boolean saveGame(String gameName) {
        // Ensure saves directory exists
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String fileName = SAVES_DIR + File.separator + gameName + SAVE_EXT;
        File file = new File(fileName);

        if (file.exists()) {
            return false; // Duplicate name
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("turn:" + (white ? "white" : "black"));
            writer.println("board:");
            for (int i = 0; i < 8; i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < 8; j++) {
                    row.append(board[i][j]);
                    if (j < 7) row.append(",");
                }
                writer.println(row);
            }
            writer.println("whiteCastle:" + whiteCastle[2] + "," + whiteCastle[6]);
            writer.println("blackCastle:" + blackCastle[2] + "," + blackCastle[6]);
            StringBuilder wp = new StringBuilder("whitePassant:");
            StringBuilder bp = new StringBuilder("blackPassant:");
            for (int i = 0; i < 8; i++) {
                wp.append(whitePassant[i]).append(i < 7 ? "," : "");
                bp.append(blackPassant[i]).append(i < 7 ? "," : "");
            }
            writer.println(wp);
            writer.println(bp);
            writer.println("halfMoveClock:" + halfMoveClock);
            writer.println("moves:" + moveHistory.size());
            for (MoveRecord record : moveHistory) {
                writer.println("move:" + record.moveNotation + "|" + record.piece + "|" +
                        (record.capturedPiece == null ? "" : record.capturedPiece) + "|" +
                        record.fromRow + "," + record.fromCol + "|" + record.toRow + "," + record.toCol + "|" +
                        record.wasWhiteTurn + "|" + record.wasCastling + "|" +
                        (record.castlingType == null ? "" : record.castlingType) + "|" +
                        record.wasPromotion + "|" + (record.promotedTo == null ? "" : record.promotedTo) + "|" +
                        record.wasEnPassant + "|" + record.enPassantCapturedRow + "," + record.enPassantCapturedCol + "|" +
                        record.rookFromRow + "," + record.rookFromCol + "|" + record.rookToRow + "," + record.rookToCol);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Saves to default file (legacy support)
     */
    private void saveGame() {
        saveGame("autosave");
    }

    /**
     * Lists all saved game names
     */
    private List<String> listSavedGames() {
        List<String> games = new ArrayList<>();
        File dir = new File(SAVES_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(SAVE_EXT));
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    games.add(name.substring(0, name.length() - SAVE_EXT.length()));
                }
            }
        }
        games.sort(String::compareToIgnoreCase);
        return games;
    }

    /**
     * Loads a game from a named save file
     * @param gameName The name of the saved game
     * @return true if game was loaded successfully
     */
    private boolean loadGame(String gameName) {
        String fileName = SAVES_DIR + File.separator + gameName + SAVE_EXT;
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int moveCount = 0;
            List<String> moveLines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("turn:")) {
                    white = "white".equals(line.substring(5));
                } else if (line.startsWith("board:")) {
                    for (int i = 0; i < 8; i++) {
                        line = reader.readLine();
                        String[] cells = line.split(",");
                        for (int j = 0; j < 8; j++) {
                            board[i][j] = cells[j];
                            checkBoard[i][j] = cells[j];
                        }
                    }
                } else if (line.startsWith("whiteCastle:")) {
                    String[] parts = line.substring(12).split(",");
                    whiteCastle[2] = Boolean.parseBoolean(parts[0]);
                    whiteCastle[6] = Boolean.parseBoolean(parts[1]);
                } else if (line.startsWith("blackCastle:")) {
                    String[] parts = line.substring(12).split(",");
                    blackCastle[2] = Boolean.parseBoolean(parts[0]);
                    blackCastle[6] = Boolean.parseBoolean(parts[1]);
                } else if (line.startsWith("whitePassant:")) {
                    String[] parts = line.substring(13).split(",");
                    for (int i = 0; i < 8; i++) {
                        whitePassant[i] = Boolean.parseBoolean(parts[i]);
                    }
                } else if (line.startsWith("blackPassant:")) {
                    String[] parts = line.substring(13).split(",");
                    for (int i = 0; i < 8; i++) {
                        blackPassant[i] = Boolean.parseBoolean(parts[i]);
                    }
                } else if (line.startsWith("halfMoveClock:")) {
                    halfMoveClock = Integer.parseInt(line.substring(14));
                } else if (line.startsWith("moves:")) {
                    moveCount = Integer.parseInt(line.substring(6));
                } else if (line.startsWith("move:")) {
                    moveLines.add(line.substring(5));
                }
            }

            // Reconstruct move history
            moveHistory.clear();
            for (String moveData : moveLines) {
                String[] parts = moveData.split("\\|");
                MoveRecord record = new MoveRecord(
                        parts[0], parts[1], parts[2],
                        Integer.parseInt(parts[3].split(",")[0]),
                        Integer.parseInt(parts[3].split(",")[1]),
                        Integer.parseInt(parts[4].split(",")[0]),
                        Integer.parseInt(parts[4].split(",")[1]),
                        whiteCastle.clone(), blackCastle.clone(),
                        whitePassant.clone(), blackPassant.clone(),
                        halfMoveClock, Boolean.parseBoolean(parts[5])
                );
                if (parts.length > 6) record.wasCastling = Boolean.parseBoolean(parts[6]);
                if (parts.length > 7) record.castlingType = parts[7].isEmpty() ? null : parts[7];
                if (parts.length > 8) record.wasPromotion = Boolean.parseBoolean(parts[8]);
                if (parts.length > 9) record.promotedTo = parts[9].isEmpty() ? null : parts[9];
                moveHistory.add(record);
            }

            // Rebuild position history
            positionHistory.clear();
            String pos = getPositionKey();
            positionHistory.put(pos, 1);

            // Verify turn is correct based on last move (if any)
            // If last move was by white, next turn is black, and vice versa
            if (!moveHistory.isEmpty()) {
                MoveRecord lastMove = moveHistory.get(moveHistory.size() - 1);
                // wasWhiteTurn=true means white made that move -> black's turn next
                // wasWhiteTurn=false means black made that move -> white's turn next
                boolean expectedWhite = !lastMove.wasWhiteTurn;
                if (white != expectedWhite) {
                    white = expectedWhite;
                }
            }

            System.out.println("Game loaded.");
            System.out.println("Moves played: " + moveHistory.size());
            return true;
        } catch (Exception e) {
            System.out.println("Error loading game: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the save file (legacy - no-op with named saves)
     */
    private void deleteSaveFile() {
        // With named saves, we don't auto-delete on game end
        // User can manually delete saves from the saves/ directory
    }

    /**
     * Checks if a save file exists (legacy - always false with named saves)
     */
    private boolean hasSaveFile() {
        return false;
    }

    /**
     * Logic for playing the game
     * Starts scanner and call init to initialize data structures / fields
     * Repeatedly prompts players for inputs until the game is ended
     * Prints out ending message
     */
    public void play() {
        init();
        Scanner sc = new Scanner(System.in);
        boolean valid = false;
        boolean loadedFromSave = false;

        // Show welcome message
        System.out.println();
        System.out.println("+========================================+");
        System.out.println("|          WELCOME TO ASCII CHESS        |");
        System.out.println("+========================================+");
        System.out.println();

        // Check for saved games
        List<String> savedGames = listSavedGames();
        if (!savedGames.isEmpty()) {
            System.out.println("Saved games:");
            for (int i = 0; i < savedGames.size(); i++) {
                System.out.println("  " + (i + 1) + ") " + savedGames.get(i));
            }
            System.out.println("Enter a number to load, or 'new' to start a new game:");
            System.out.print("> ");
            String response = sc.nextLine().trim();
            if (response.equalsIgnoreCase("new")) {
                // Prompt for game mode
                System.out.println("Select game mode:");
                System.out.println("  1) Standard Chess");
                System.out.println("  2) Chess960 (Fischer Random)");
                System.out.print("> ");
                String modeChoice = sc.nextLine().trim();
                if (modeChoice.equals("2")) {
                    chess960 = true;
                    System.out.println("Chess960 mode selected. Generating random starting position...");
                    board = initializeChess960Board();
                    checkBoard = copyBoard(board);
                    resetCastlingRights();  // Reset castling rights based on king/rook positions
                } else {
                    chess960 = false;
                    System.out.println("Standard chess selected.");
                }
                System.out.println("Starting new game.");
            } else {
                try {
                    int choice = Integer.parseInt(response);
                    if (choice >= 1 && choice <= savedGames.size()) {
                        String gameName = savedGames.get(choice - 1);
                        if (loadGame(gameName)) {
                            loadedFromSave = true;
                        } else {
                            System.out.println("Failed to load game. Starting new game.");
                        }
                    } else {
                        System.out.println("Invalid choice. Starting new game.");
                    }
                } catch (NumberFormatException e) {
                    // Try as a name
                    if (savedGames.contains(response)) {
                        if (loadGame(response)) {
                            loadedFromSave = true;
                        } else {
                            System.out.println("Failed to load game. Starting new game.");
                        }
                    } else {
                        System.out.println("Starting new game.");
                    }
                }
            }
        } else {
            System.out.println("Type 'help' at any time for instructions.");
        }
        System.out.println();

        if (!loadedFromSave) {
            System.out.println("Type 'help' at any time for instructions.");
            System.out.println();
        }

        while (!complete) {
            printBoard(board);
            if (white) {
                System.out.println("White's turn");
            } else {
                System.out.println("Black's turn");
            }
            if (offerdraw) {
                System.out.println("Draw offered. Type 'draw' to accept or make a move to decline.");
                String response = sc.nextLine();
                if (response.trim().equalsIgnoreCase("draw")) {
                    draw = true;
                    drawReason = "mutual agreement";
                    complete = true;
                    deleteSaveFile(); // Game finished normally
                    break;
                }
                // If not accepting draw, continue with normal move processing
                offerdraw = false;
                if (inputParse(response)) {
                    valid = true;
                } else {
                    if (!lastError.isEmpty()) {
                        System.out.println(lastError);
                    }
                    // If lastError is empty (e.g., "help" was typed), just re-prompt
                    continue;
                }
            }
            if (check(checkBoard)) {
                System.out.println("Check!");
            }
            valid = false;
            while (!valid) {
                syncBoards(board, checkBoard);
                System.out.print("> ");
                String input = sc.nextLine();
                if (inputParse(input)) {
                    valid = true;
                } else {
                    if (!lastError.isEmpty()) {
                        System.out.println(lastError);
                    }
                    // If lastError is empty, no error message needed (e.g., "help" was typed)
                }
            }
            promo = "Q";

            // Record position for threefold repetition
            String newPosition = getPositionKey();
            positionHistory.put(newPosition, positionHistory.getOrDefault(newPosition, 0) + 1);

            // Check for automatic draws after move
            if (checkAutomaticDraws()) {
                printBoard(board);
                System.out.println("Draw by " + drawReason + "!");
                draw = true;
                complete = true;
                deleteSaveFile(); // Game finished normally
                break;
            }

            white = !white;

            // Check for checkmate or stalemate
            if (checkMate()) {
                printBoard(board);
                System.out.println("Checkmate!");
                complete = true;
                white = !white;
                deleteSaveFile(); // Game finished normally
            } else if (isStalemate()) {
                printBoard(board);
                System.out.println("Stalemate!");
                draw = true;
                drawReason = "stalemate";
                complete = true;
                deleteSaveFile(); // Game finished normally
            }

            if (white) {
                whitePassant = initializePassant();
            } else {
                blackPassant = initializePassant();
            }
        }

        // Save game if exiting early (quit/resign - but not from normal game end)
        // Note: quit and resign already save in inputParse, this handles Ctrl+C case
        // by using a shutdown hook (not implemented here for simplicity)

        if (draw) {
            if (!drawReason.isEmpty()) {
                System.out.println("Game ended in a draw by " + drawReason + ".");
            } else {
                System.out.println("Game ended in a draw.");
            }
        } else if (complete && !draw) {
            // Game finished normally - winner already announced
        } else if (white) {
            System.out.println("White wins!");
        } else {
            System.out.println("Black wins!");
        }

        // Prompt to save game name if exiting early (not checkmate/stalemate/draw)
        // This handles quit, resign, and the user can also just exit without saving
        boolean gameEndedNormally = draw || (complete && (drawReason.equals("stalemate") || drawReason.equals("50-move rule") || drawReason.equals("threefold repetition")));
        // Actually, checkmate and normal win are also "normal" but we delete save file there
        // The user wants to prompt for save name on any exit
        // So always prompt unless it's checkmate/stalemate/50-move/threefold (where save is deleted)
        boolean shouldPromptSave = !draw && !drawReason.equals("stalemate") && !drawReason.equals("50-move rule") && !drawReason.equals("threefold repetition");

        if (shouldPromptSave) {
            promptSaveOnExit(sc);
        }

        sc.close();
    }

    /**
     * Prompts the user for a name to save the game, handling duplicates and skip
     */
    private void promptSaveOnExit(Scanner sc) {
        System.out.println();
        System.out.println("Enter a name to save this game (or type 'quit'/'exit' to skip saving):");
        while (true) {
            System.out.print("> ");
            String name;
            try {
                name = sc.nextLine();
            } catch (Exception e) {
                // Ctrl+C or other input error - skip saving
                System.out.println("Not saving.");
                return;
            }
            if (name == null) {
                System.out.println("Not saving.");
                return;
            }
            name = name.trim();
            if (name.equalsIgnoreCase("quit") || name.equalsIgnoreCase("exit") || name.isEmpty()) {
                System.out.println("Not saving.");
                return;
            }
            // Check for duplicate name
            File saveFile = new File(SAVES_DIR + File.separator + name + SAVE_EXT);
            if (saveFile.exists()) {
                System.out.println("That name is already taken. Please choose another or type 'quit' to skip:");
                continue;
            }
            // Valid unique name - save
            if (saveGame(name)) {
                System.out.println("Game saved as '" + name + "'.");
            } else {
                System.out.println("Failed to save game.");
            }
            return;
        }
    }
    
    /**
     * Creates a Chess instance and calls play to start the game
     * @param args Not used
     */
    public static void main(String[] args) {
        final Chess game = new Chess();

        // Handle Ctrl+C (SIGINT) - prompt to save like quit/exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // If game is not complete, try to save
            if (!game.complete) {
                System.out.println("\n\nGame interrupted.");
                // Try to prompt for save name (may not work in shutdown hook)
                // Fall back to autosave
                String saveName = "autosave";
                if (game.saveGame(saveName)) {
                    System.out.println("Game auto-saved as '" + saveName + "'.");
                    System.out.println("You can load it next time by selecting it from the saved games list.");
                } else {
                    // Maybe duplicate name - try with timestamp or just print
                    System.out.println("Could not auto-save (file may exist). Use 'quit' or 'exit' to save properly.");
                }
            }
        }));

        game.play();
    }
}