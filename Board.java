import java.util.List;
import java.util.ArrayList;

/**
 * Board utility class for ASCII Chess.
 * Provides board initialization, display, and coordinate conversion.
 */
public class Board {

    /**
     * Initializes a 2d array of Strings with standard starting position.
     * Sets black's and white's pieces in default position and does black/white tiling.
     * @return Chess board in the form of a 2d array of strings
     */
    public static String[][] initializeBoard() {
        String[][] output = new String[8][8];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i % 2 == 0 && j % 2 == 1) || (i % 2 == 1 && j % 2 == 0)) {
                    output[i][j] = "##";
                } else {
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
     * Initializes a Chess960 (Fischer Random) board with randomized back rank.
     * Rules: bishops on opposite colors, king between rooks.
     */
    public static String[][] initializeChess960Board() {
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
    private static int[] generateChess960BackRank() {
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
     * Creates a deep copy of the board.
     */
    public static String[][] copyBoard(String[][] src) {
        String[][] dst = new String[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                dst[i][j] = src[i][j];
            }
        }
        return dst;
    }

    /**
     * Takes a 2d array "copy" and copies all its data into another 2d array "paste".
     * @param copy 2d array that will be copied from
     * @param paste 2d array that will be copied into
     */
    public static void syncBoards(String[][] copy, String[][] paste) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                paste[i][j] = copy[i][j];
            }
        }
    }

    /**
     * Prints out chess board with Unicode pieces and labels on all sides.
     * @param board the chess board that is printed out
     */
    public static void printBoard(String[][] board) {
        System.out.println();
        // Top file labels
        System.out.println("     a   b   c   d   e   f   g   h    ");
        System.out.println("   +---+---+---+---+---+---+---+---+");

        for (int i = 0; i < 8; i++) {
            // Left rank label
            System.out.print(" " + (8 - i) + " |");
            for (int j = 0; j < 8; j++) {
                String display = Piece.pieceToDisplay(board[i][j]);
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
     * Translates a letter into an array index according to how a chess board is formatted.
     * @param input the given letter for a move (case-insensitive)
     * @return the letter's corresponding array index, or -1 if invalid
     */
    public static int letterToCoordinate(String input) {
        if (input == null || input.isEmpty()) return -1;
        char c = Character.toLowerCase(input.charAt(0));
        if (c >= 'a' && c <= 'h') {
            return c - 'a';
        }
        return -1;
    }

    /**
     * Parses a square notation (e.g., "e4") into board indices.
     * @param square The square notation (letter + number)
     * @return int array [i, j] or null if invalid
     */
    public static int[] parseSquare(String square) {
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
}
