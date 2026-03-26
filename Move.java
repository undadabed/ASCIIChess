/**
 * Move class for ASCII Chess.
 * Represents a move and contains move execution mechanics (excluding validation).
 * Handles standard moves, castling, en passant, and promotion.
 */
public class Move {

    // Move data
    public int fromRow, fromCol, toRow, toCol;
    public String piece;           // e.g., "wK", "bp"
    public String capturedPiece;   // captured piece or null/empty
    public boolean wasCastling;
    public String castlingType;    // "K" or "Q"
    public boolean wasEnPassant;
    public int enPassantCapturedRow, enPassantCapturedCol;
    public boolean wasPromotion;
    public String promotedTo;      // "Q", "R", "B", "N"

    public Move(int fromRow, int fromCol, int toRow, int toCol, String piece) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.capturedPiece = null;
        this.wasCastling = false;
        this.castlingType = null;
        this.wasEnPassant = false;
        this.enPassantCapturedRow = -1;
        this.enPassantCapturedCol = -1;
        this.wasPromotion = false;
        this.promotedTo = null;
    }

    /**
     * Execute a standard piece move on the board (no special handling).
     */
    public static void executeStandardMove(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        String piece = board[fromRow][fromCol];
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = ((fromRow % 2 == 1 && fromCol % 2 == 0) ||
                                   (fromRow % 2 == 0 && fromCol % 2 == 1)) ? "##" : "  ";
    }

    /**
     * Execute castling on the board.
     * @param board the board
     * @param kingRow the row of the king (7 for white, 0 for black)
     * @param kingFromCol starting king column
     * @param kingToCol destination king column (6 for K-side, 2 for Q-side)
     * @param rookFromCol starting rook column
     * @param rookToCol destination rook column (5 for K-side, 3 for Q-side)
     */
    public static void executeCastling(String[][] board, int kingRow, int kingFromCol, int kingToCol,
                                       int rookFromCol, int rookToCol) {
        String king = board[kingRow][kingFromCol];
        String rook = board[kingRow][rookFromCol];

        // Move king
        board[kingRow][kingToCol] = king;
        board[kingRow][kingFromCol] = ((kingRow % 2 == 1 && kingFromCol % 2 == 0) ||
                                       (kingRow % 2 == 0 && kingFromCol % 2 == 1)) ? "##" : "  ";

        // Move rook (if not already at destination, e.g., Chess960 adjacent case)
        if (rookFromCol != kingToCol) {
            board[kingRow][rookToCol] = rook;
        }
        if (rookFromCol != rookToCol) {
            board[kingRow][rookFromCol] = ((kingRow % 2 == 1 && rookFromCol % 2 == 0) ||
                                           (kingRow % 2 == 0 && rookFromCol % 2 == 1)) ? "##" : "  ";
        }
    }

    /**
     * Execute en passant capture on the board.
     * @param board the board
     * @param fromRow pawn's starting row
     * @param fromCol pawn's starting col
     * @param toRow pawn's destination row
     * @param toCol pawn's destination col
     * @param capturedPawnRow row of the captured pawn
     * @param capturedPawnCol col of the captured pawn
     */
    public static void executeEnPassant(String[][] board, int fromRow, int fromCol, int toRow, int toCol,
                                        int capturedPawnRow, int capturedPawnCol) {
        String pawn = board[fromRow][fromCol];
        board[toRow][toCol] = pawn;
        board[fromRow][fromCol] = ((fromRow % 2 == 1 && fromCol % 2 == 0) ||
                                   (fromRow % 2 == 0 && fromCol % 2 == 1)) ? "##" : "  ";
        // Remove captured pawn
        board[capturedPawnRow][capturedPawnCol] = ((capturedPawnRow % 2 == 1 && capturedPawnCol % 2 == 0) ||
                                                    (capturedPawnRow % 2 == 0 && capturedPawnCol % 2 == 1)) ? "##" : "  ";
    }

    /**
     * Execute pawn promotion on the board.
     * @param board the board
     * @param row destination row (0 for white promote, 7 for black promote)
     * @param col destination col
     * @param promotedTo piece to promote to ("Q", "R", "B", "N")
     * @param isWhite true if white's pawn
     */
    public static void executePromotion(String[][] board, int row, int col, String promotedTo, boolean isWhite) {
        String color = isWhite ? "w" : "b";
        board[row][col] = color + promotedTo;
    }

    /**
     * Apply this move to the given board (executes the move mechanics).
     * Note: This assumes the move has been validated; it does not check legality.
     * @param board the board to apply the move to
     */
    public void execute(String[][] board) {
        if (wasCastling) {
            // Determine rook positions based on castling type
            int kingRow = fromRow;
            int rookFromCol = (castlingType.equals("K")) ? 7 : 0;
            int rookToCol = (castlingType.equals("K")) ? 5 : 3;
            // Find actual rook position (for Chess960)
            for (int j = 0; j < 8; j++) {
                if (board[fromRow][j].equals(piece.substring(0, 1) + "R")) {
                    rookFromCol = j;
                    break;
                }
            }
            executeCastling(board, fromRow, fromCol, toCol, rookFromCol, rookToCol);
        } else if (wasEnPassant) {
            executeEnPassant(board, fromRow, fromCol, toRow, toCol,
                             enPassantCapturedRow, enPassantCapturedCol);
        } else {
            executeStandardMove(board, fromRow, fromCol, toRow, toCol);
        }

        // Handle promotion
        if (wasPromotion && promotedTo != null) {
            boolean isWhite = piece.startsWith("w");
            executePromotion(board, toRow, toCol, promotedTo, isWhite);
        }
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Validates a pawn move (without checking check).
     */
    public static boolean isValidPawnMove(int iStart, int jStart, int iEnd, int jEnd, boolean isWhite, String[][] testBoard) {
        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;

        // Single push
        if (jStart == jEnd && iEnd - iStart == direction && Piece.isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        // Double push
        if (jStart == jEnd && iStart == startRow && iEnd - iStart == 2 * direction &&
            Piece.isEmptySquare(testBoard[iStart + direction][jStart]) && Piece.isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        // Capture
        if (Math.abs(jEnd - jStart) == 1 && iEnd - iStart == direction &&
            !Piece.isEmptySquare(testBoard[iEnd][jEnd])) {
            return true;
        }
        return false;
    }

    /**
     * Validates a rook move (without checking check).
     */
    public static boolean isValidRookMove(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        if (iStart != iEnd && jStart != jEnd) return false;

        int dr = Integer.compare(iEnd, iStart);
        int dc = Integer.compare(jEnd, jStart);

        int r = iStart + dr, c = jStart + dc;
        while (r != iEnd || c != jEnd) {
            if (!Piece.isEmptySquare(testBoard[r][c])) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    /**
     * Validates a knight move (without checking check).
     */
    public static boolean isValidKnightMove(int iStart, int jStart, int iEnd, int jEnd) {
        int dr = Math.abs(iEnd - iStart);
        int dc = Math.abs(jEnd - jStart);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    /**
     * Validates a bishop move (without checking check).
     */
    public static boolean isValidBishopMove(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        if (Math.abs(iEnd - iStart) != Math.abs(jEnd - jStart)) return false;

        int dr = Integer.compare(iEnd, iStart);
        int dc = Integer.compare(jEnd, jStart);

        int r = iStart + dr, c = jStart + dc;
        while (r != iEnd || c != jEnd) {
            if (!Piece.isEmptySquare(testBoard[r][c])) return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    /**
     * Validates a queen move (without checking check).
     */
    public static boolean isValidQueenMove(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        return isValidRookMove(iStart, jStart, iEnd, jEnd, testBoard) ||
               isValidBishopMove(iStart, jStart, iEnd, jEnd, testBoard);
    }

    /**
     * Validates a king move (without checking check).
     */
    public static boolean isValidKingMove(int iStart, int jStart, int iEnd, int jEnd) {
        return Math.abs(iStart - iEnd) <= 1 && Math.abs(jStart - jEnd) <= 1;
    }

    /**
     * Validates a move for a piece type (without checking check).
     */
    public static boolean isValidMoveOnBoard(int iStart, int jStart, int iEnd, int jEnd, String[][] testBoard) {
        String piece = testBoard[iStart][jStart];
        if (Piece.isEmptySquare(piece)) return false;

        String pieceType = piece.substring(1);
        String color = piece.substring(0, 1);
        boolean isWhite = color.equals("w");

        // Can't capture own piece
        if (!Piece.isEmptySquare(testBoard[iEnd][jEnd]) && testBoard[iEnd][jEnd].startsWith(color)) {
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
                return isValidQueenMove(iStart, jStart, iEnd, jEnd, testBoard);
            case "K":
                return isValidKingMove(iStart, jStart, iEnd, jEnd);
            default:
                return false;
        }
    }

    // ==================== CHECK DETECTION ====================

    /**
     * Checks if a square is attacked by the opponent.
     */
    public static boolean isSquareAttacked(String[][] board, int row, int col, boolean byWhite) {
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
                if (!Piece.isEmptySquare(piece)) {
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
                if (!Piece.isEmptySquare(piece)) {
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
     * Checks if the current player is in check.
     * @param board the board
     * @param isWhiteTurn true if it's white's turn
     */
    public static boolean check(String[][] board, boolean isWhiteTurn) {
        // Find king
        int iKing = 0, jKing = 0;
        String kingPiece = isWhiteTurn ? "wK" : "bK";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j].equals(kingPiece)) {
                    iKing = i; jKing = j;
                    break;
                }
            }
        }
        // Check if attacked by opponent
        return isSquareAttacked(board, iKing, jKing, !isWhiteTurn);
    }

    /**
     * Checks if current player has any legal moves.
     */
    public static boolean hasLegalMoves(String[][] board, boolean isWhiteTurn) {
        String[][] temp = new String[8][8];
        Board.syncBoards(board, temp);

        for (int iStart = 0; iStart < 8; iStart++) {
            for (int jStart = 0; jStart < 8; jStart++) {
                String piece = board[iStart][jStart];
                if (Piece.isEmptySquare(piece)) continue;
                if ((piece.startsWith("w") && !isWhiteTurn) || (piece.startsWith("b") && isWhiteTurn)) continue;

                for (int iEnd = 0; iEnd < 8; iEnd++) {
                    for (int jEnd = 0; jEnd < 8; jEnd++) {
                        if (isValidMoveOnBoard(iStart, jStart, iEnd, jEnd, temp)) {
                            // Try the move
                            String captured = temp[iEnd][jEnd];
                            temp[iEnd][jEnd] = temp[iStart][jStart];
                            temp[iStart][jStart] = ((iStart % 2 == 1 && jStart % 2 == 0) ||
                                                    (iStart % 2 == 0 && jStart % 2 == 1)) ? "##" : "  ";

                            boolean stillInCheck = check(temp, isWhiteTurn);

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
     * Checks if current player is in checkmate.
     */
    public static boolean checkMate(String[][] board, boolean isWhiteTurn) {
        if (!check(board, isWhiteTurn)) return false;
        return !hasLegalMoves(board, isWhiteTurn);
    }

    /**
     * Validates a move and applies it to checkBoard (for en passant/promotion/castling).
     * This is the central move approval method that handles all piece types and special moves.
     * @param iStart starting row
     * @param jStart starting col
     * @param iEnd destination row
     * @param jEnd destination col
     * @param board the main board (for validation)
     * @param checkBoard the board to modify (for en passant/promotion/castling execution)
     * @param isWhite whose turn it is
     * @param whiteCastle white castling rights
     * @param blackCastle black castling rights
     * @param whitePassant white en passant tracking
     * @param blackPassant black en passant tracking
     * @param promo promotion piece (Q/R/B/N)
     * @return true if move is valid
     */
    public static boolean approveMove(int iStart, int jStart, int iEnd, int jEnd, String[][] board, String[][] checkBoard, boolean isWhite, boolean[] whiteCastle, boolean[] blackCastle, boolean[] whitePassant, boolean[] blackPassant, String promo) {
        String piece = board[iStart][jStart].substring(1,2);
        if (piece.equals("p")) {
            if (isWhite) {
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
                else if (jEnd == jStart && iStart-iEnd == 1 && Piece.isEmptySquare(board[iEnd][jEnd])) {
                }
                else if (iStart == 6 && iEnd == 4 && jStart == jEnd && Piece.isEmptySquare(board[5][jStart]) && Piece.isEmptySquare(board[4][jStart])) {
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
                else if (jEnd == jStart && iEnd-iStart == 1 && Piece.isEmptySquare(board[iEnd][jEnd])) {
                }
                else if (iStart == 1 && iEnd == 3 && jStart == jEnd && Piece.isEmptySquare(board[2][jStart]) && Piece.isEmptySquare(board[3][jStart])) {
                    blackPassant[jStart] = true;
                }
                else {
                    return false;
                }
            }
            if (isWhite && iEnd == 0) {
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
            if (!isWhite && iEnd == 7) {
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
                        if (!Piece.isEmptySquare(board[iStart][jStart - i])) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (isWhite) {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                        if (!Piece.isEmptySquare(board[iStart][jStart + i])) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (isWhite) {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                        if (!Piece.isEmptySquare(board[iStart - i][jStart])) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (isWhite) {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                        if (!Piece.isEmptySquare(board[iStart + i][jStart])) {
                            val = false;
                        }
                    }
                    // Square moving to is either empty or has a piece of opposite color
                    if (isWhite) {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                            val = false;
                        }
                    }
                    else {
                        if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                if (isWhite && (board[iEnd][jEnd].substring(0,1).equals("b") || Piece.isEmptySquare(board[iEnd][jEnd]))) {
                    return true;
                }
                else if (!isWhite && (board[iEnd][jEnd].substring(0,1).equals("w") || Piece.isEmptySquare(board[iEnd][jEnd]))) {
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
                    if (!Piece.isEmptySquare(board[iStart + i][jStart + i])) {
                        return false;
                    }
                }
                if (isWhite) {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart < iEnd && jStart > jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!Piece.isEmptySquare(board[iStart + i][jStart - i])) {
                        return false;
                    }
                }
                if (isWhite) {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart > iEnd && jStart < jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!Piece.isEmptySquare(board[iStart - i][jStart + i])) {
                        return false;
                    }
                }
                if (isWhite) {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                        return false;
                    }
                }
                return true;
            }
            else if (iStart > iEnd && jStart > jEnd) {
                for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                    if (!Piece.isEmptySquare(board[iStart - i][jStart - i])) {
                        return false;
                    }
                }
                if (isWhite) {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                        return false;
                    }
                }
                else {
                    if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
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
                            if (!Piece.isEmptySquare(board[iStart][jStart - i])) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (isWhite) {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                            if (!Piece.isEmptySquare(board[iStart][jStart + i])) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (isWhite) {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                            if (!Piece.isEmptySquare(board[iStart - i][jStart])) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (isWhite) {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                            if (!Piece.isEmptySquare(board[iStart + i][jStart])) {
                                val = false;
                            }
                        }
                        // Square moving to is either empty or has a piece of opposite color
                        if (isWhite) {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("b"))) {
                                val = false;
                            }
                        }
                        else {
                            if (!(Piece.isEmptySquare(board[iEnd][jEnd]) || board[iEnd][jEnd].substring(0,1).equals("w"))) {
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
                        if (!Piece.isEmptySquare(board[iStart + i][jStart + i])) {
                            return false;
                        }
                    }
                    if (isWhite) {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart < iEnd && jStart > jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!Piece.isEmptySquare(board[iStart + i][jStart - i])) {
                            return false;
                        }
                    }
                    if (isWhite) {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart > iEnd && jStart < jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!Piece.isEmptySquare(board[iStart - i][jStart + i])) {
                            return false;
                        }
                    }
                    if (isWhite) {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
                            return false;
                        }
                    }
                    return true;
                }
                else if (iStart > iEnd && jStart > jEnd) {
                    for (int i = 1; i < Math.abs(iEnd-iStart); i++) {
                        if (!Piece.isEmptySquare(board[iStart - i][jStart - i])) {
                            return false;
                        }
                    }
                    if (isWhite) {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("b")) {
                            return false;
                        }
                    }
                    else {
                        if (!Piece.isEmptySquare(board[iEnd][jEnd]) && !board[iEnd][jEnd].substring(0,1).equals("w")) {
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
                if ((board[iEnd][jEnd].substring(0,1).equals("w") || Piece.isEmptySquare(board[iEnd][jEnd])) && !isWhite) {
                    blackCastle[2] = false;
                    blackCastle[6] = false;
                    return true;
                }
                else if ((board[iEnd][jEnd].substring(0,1).equals("b") || Piece.isEmptySquare(board[iEnd][jEnd])) && isWhite) {
                    whiteCastle[2] = false;
                    whiteCastle[6] = false;
                    return true;
                }
                else {
                    return false;
                }
            }
            else if (isWhite && iEnd == 7) {
                // White castling - must not be in check
                // Find king position dynamically (works for Chess960)
                int kingCol = -1;
                for (int j = 0; j < 8; j++) {
                    if (board[7][j].equals("wK")) { kingCol = j; break; }
                }
                if (kingCol < 0) return false;

                if (isSquareAttacked(board, 7, kingCol, false)) {
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
                        if (!Piece.isEmptySquare(board[7][j])) return false;
                    }

                    // Check rook's path (from min(rookCol,5) to max(rookCol,5), excluding rookCol, allowing kingCol)
                    int rookPathStart = Math.min(rookCol, 5);
                    int rookPathEnd = Math.max(rookCol, 5);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue; // Rook's start is OK
                        if (j == kingCol) continue; // King's start is OK (king moves out)
                        if (!Piece.isEmptySquare(board[7][j])) return false;
                    }

                    // Check if king passes through or lands on attacked square
                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(board, 7, j, false)) return false;
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
                        if (!Piece.isEmptySquare(board[7][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 3);
                    int rookPathEnd = Math.max(rookCol, 3);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!Piece.isEmptySquare(board[7][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(board, 7, j, false)) return false;
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
            else if (!isWhite && iEnd == 0) {
                // Black castling - must not be in check
                int kingCol = -1;
                for (int j = 0; j < 8; j++) {
                    if (board[0][j].equals("bK")) { kingCol = j; break; }
                }
                if (kingCol < 0) return false;

                if (isSquareAttacked(board, 0, kingCol, true)) {
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
                        if (!Piece.isEmptySquare(board[0][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 5);
                    int rookPathEnd = Math.max(rookCol, 5);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!Piece.isEmptySquare(board[0][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(board, 0, j, true)) return false;
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
                        if (!Piece.isEmptySquare(board[0][j])) return false;
                    }

                    int rookPathStart = Math.min(rookCol, 3);
                    int rookPathEnd = Math.max(rookCol, 3);
                    for (int j = rookPathStart; j <= rookPathEnd; j++) {
                        if (j == rookCol) continue;
                        if (j == kingCol) continue;
                        if (!Piece.isEmptySquare(board[0][j])) return false;
                    }

                    for (int j = kingPathStart; j <= kingPathEnd; j++) {
                        if (isSquareAttacked(board, 0, j, true)) return false;
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
     * Updates castling rights when a rook or king moves from a corner square.
     * @param iStart starting row of the piece
     * @param jStart starting column of the piece
     * @param whiteCastle white castling rights array
     * @param blackCastle black castling rights array
     */
    public static void updateCastle(int iStart, int jStart, boolean[] whiteCastle, boolean[] blackCastle) {
        if (iStart == 0 && jStart == 0) {
            blackCastle[2] = false;
        } else if (iStart == 0 && jStart == 7) {
            blackCastle[6] = false;
        } else if (iStart == 7 && jStart == 0) {
            whiteCastle[2] = false;
        } else if (iStart == 7 && jStart == 7) {
            whiteCastle[6] = false;
        }
    }

    /**
     * Validates and executes a move given input notation.
     * Handles piece ownership, check detection, castling, en passant, promotion,
     * history recording, and state updates.
     *
     * @param inputs parsed move input [from, to] e.g. ["e2", "e4"]
     * @param board the main board
     * @param checkBoard the working board for validation
     * @param isWhite current player's turn
     * @param whiteCastle white castling rights
     * @param blackCastle black castling rights
     * @param whitePassant white en passant tracking
     * @param blackPassant black en passant tracking
     * @param promo promotion piece (Q/R/B/N)
     * @param halfMoveClockRef single-element array to update half-move clock
     * @param lastErrorRef single-element array to set error message on failure
     * @param moveHistory list to add MoveRecord to
     * @return true if move was valid and executed
     */
    public static boolean validMove(String[] inputs, String[][] board, String[][] checkBoard,
                                     boolean isWhite, boolean[] whiteCastle, boolean[] blackCastle,
                                     boolean[] whitePassant, boolean[] blackPassant, String promo,
                                     int[] halfMoveClockRef, String[] lastErrorRef,
                                     java.util.List<MoveRecord> moveHistory) {
        if (inputs == null || inputs.length < 2) {
            lastErrorRef[0] = "Invalid move format. Use: e2 e4, e2e4, or e4";
            return false;
        }

        int jStart, iStart, jEnd, iEnd;

        try {
            jStart = Board.letterToCoordinate(inputs[0].substring(0, 1));
            iStart = 8 - Integer.parseInt(inputs[0].substring(1, 2));
            jEnd = Board.letterToCoordinate(inputs[1].substring(0, 1));
            iEnd = 8 - Integer.parseInt(inputs[1].substring(1, 2));
        } catch (Exception e) {
            lastErrorRef[0] = "Invalid move format. Use: e2 e4, e2e4, or e4";
            return false;
        }

        if (jStart > 7 || jStart < 0 || iStart > 7 || iStart < 0 || jEnd > 7 || jEnd < 0 || iEnd > 7 || iEnd < 0) {
            lastErrorRef[0] = "Square out of bounds. Use a-h for files and 1-8 for ranks.";
            return false;
        }

        if (isWhite) {
            if (!checkBoard[iStart][jStart].substring(0, 1).equals("w")) {
                lastErrorRef[0] = "No white piece on " + inputs[0] + ". Select one of your pieces.";
                return false;
            }
            String start = board[iStart][jStart];
            String captured = board[iEnd][jEnd];
            boolean isPawnMove = start.substring(1).equals("p");
            boolean isCapture = !Piece.isEmptySquare(captured);

            boolean[] oldWhiteCastle = whiteCastle.clone();
            boolean[] oldBlackCastle = blackCastle.clone();
            boolean[] oldWhitePassant = whitePassant.clone();
            boolean[] oldBlackPassant = blackPassant.clone();
            int oldHalfMoveClock = halfMoveClockRef[0];

            checkBoard[iEnd][jEnd] = start;
            if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                checkBoard[iStart][jStart] = "##";
            } else {
                checkBoard[iStart][jStart] = "  ";
            }
            if (check(checkBoard, isWhite)) {
                lastErrorRef[0] = "Move would leave king in check.";
                return false;
            }
            if (approveMove(iStart, jStart, iEnd, jEnd, board, checkBoard, isWhite, whiteCastle, blackCastle, whitePassant, blackPassant, promo)) {
                String moveNotation = inputs[0] + inputs[1];
                MoveRecord record = new MoveRecord(moveNotation, start, captured,
                        iStart, jStart, iEnd, jEnd,
                        oldWhiteCastle, oldBlackCastle,
                        oldWhitePassant, oldBlackPassant,
                        oldHalfMoveClock, true);

                boolean isCastlingMove = false;
                if (start.substring(1).equals("K")) {
                    if ((jEnd == 6 && ((isWhite && whiteCastle[6]) || (!isWhite && blackCastle[6]))) ||
                        (jEnd == 2 && ((isWhite && whiteCastle[2]) || (!isWhite && blackCastle[2])))) {
                        isCastlingMove = true;
                    } else if (Math.abs(jEnd - jStart) == 2) {
                        isCastlingMove = true;
                    }
                }
                if (isCastlingMove) {
                    record.wasCastling = true;
                    record.castlingType = (jEnd > jStart) ? "K" : "Q";
                    int rookFromCol = -1;
                    if (record.castlingType.equals("K")) {
                        for (int j = jStart + 1; j < 8; j++) {
                            if (board[iStart][j].equals(isWhite ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 7;
                        record.rookToRow = iStart;
                        record.rookToCol = 5;
                    } else {
                        for (int j = jStart - 1; j >= 0; j--) {
                            if (board[iStart][j].equals(isWhite ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 0;
                        record.rookToRow = iStart;
                        record.rookToCol = 3;
                    }
                }
                if (isPawnMove && Math.abs(jEnd - jStart) == 1 && Piece.isEmptySquare(captured)) {
                    boolean enPassantPossible = (isWhite && blackPassant[jEnd] && iEnd == 2) ||
                                                 (!isWhite && whitePassant[jEnd] && iEnd == 5);
                    if (enPassantPossible) {
                        record.wasEnPassant = true;
                        record.enPassantCapturedRow = isWhite ? (iEnd - 1) : (iEnd + 1);
                        record.enPassantCapturedCol = jEnd;
                        record.capturedPiece = isWhite ? "bp" : "wp";
                    }
                }
                if (isPawnMove && (iEnd == 0 || iEnd == 7)) {
                    record.wasPromotion = true;
                    record.promotedTo = promo;
                }

                moveHistory.add(record);
                Board.syncBoards(checkBoard, board);
                updateCastle(iStart, jStart, whiteCastle, blackCastle);

                if (isPawnMove || isCapture) {
                    halfMoveClockRef[0] = 0;
                } else {
                    halfMoveClockRef[0]++;
                }

                return true;
            } else {
                lastErrorRef[0] = "Invalid move for this piece.";
                return false;
            }
        } else {
            if (!checkBoard[iStart][jStart].substring(0, 1).equals("b")) {
                lastErrorRef[0] = "No black piece on " + inputs[0] + ". Select one of your pieces.";
                return false;
            }
            String start = board[iStart][jStart];
            String captured = board[iEnd][jEnd];
            boolean isPawnMove = start.substring(1).equals("p");
            boolean isCapture = !Piece.isEmptySquare(captured);

            boolean[] oldWhiteCastle = whiteCastle.clone();
            boolean[] oldBlackCastle = blackCastle.clone();
            boolean[] oldWhitePassant = whitePassant.clone();
            boolean[] oldBlackPassant = blackPassant.clone();
            int oldHalfMoveClock = halfMoveClockRef[0];

            checkBoard[iEnd][jEnd] = start;
            if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                checkBoard[iStart][jStart] = "##";
            } else {
                checkBoard[iStart][jStart] = "  ";
            }
            if (check(checkBoard, isWhite)) {
                lastErrorRef[0] = "Move would leave king in check.";
                return false;
            }
            if (approveMove(iStart, jStart, iEnd, jEnd, board, checkBoard, isWhite, whiteCastle, blackCastle, whitePassant, blackPassant, promo)) {
                String moveNotation = inputs[0] + inputs[1];
                MoveRecord record = new MoveRecord(moveNotation, start, captured,
                        iStart, jStart, iEnd, jEnd,
                        oldWhiteCastle, oldBlackCastle,
                        oldWhitePassant, oldBlackPassant,
                        oldHalfMoveClock, false);

                boolean isCastlingMoveBlack = false;
                if (start.substring(1).equals("K")) {
                    if ((jEnd == 6 && ((isWhite && whiteCastle[6]) || (!isWhite && blackCastle[6]))) ||
                        (jEnd == 2 && ((isWhite && whiteCastle[2]) || (!isWhite && blackCastle[2])))) {
                        isCastlingMoveBlack = true;
                    } else if (Math.abs(jEnd - jStart) == 2) {
                        isCastlingMoveBlack = true;
                    }
                }
                if (isCastlingMoveBlack) {
                    record.wasCastling = true;
                    record.castlingType = (jEnd > jStart) ? "K" : "Q";
                    int rookFromCol = -1;
                    if (record.castlingType.equals("K")) {
                        for (int j = jStart + 1; j < 8; j++) {
                            if (board[iStart][j].equals(isWhite ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 7;
                        record.rookToRow = iStart;
                        record.rookToCol = 5;
                    } else {
                        for (int j = jStart - 1; j >= 0; j--) {
                            if (board[iStart][j].equals(isWhite ? "wR" : "bR")) { rookFromCol = j; break; }
                        }
                        record.rookFromRow = iStart;
                        record.rookFromCol = (rookFromCol >= 0) ? rookFromCol : 0;
                        record.rookToRow = iStart;
                        record.rookToCol = 3;
                    }
                }
                if (isPawnMove && Math.abs(jEnd - jStart) == 1 && Piece.isEmptySquare(captured)) {
                    boolean enPassantPossible = (isWhite && blackPassant[jEnd] && iEnd == 2) ||
                                                 (!isWhite && whitePassant[jEnd] && iEnd == 5);
                    if (enPassantPossible) {
                        record.wasEnPassant = true;
                        record.enPassantCapturedRow = isWhite ? (iEnd - 1) : (iEnd + 1);
                        record.enPassantCapturedCol = jEnd;
                        record.capturedPiece = isWhite ? "bp" : "wp";
                    }
                }
                if (isPawnMove && (iEnd == 0 || iEnd == 7)) {
                    record.wasPromotion = true;
                    record.promotedTo = promo;
                }

                moveHistory.add(record);
                updateCastle(iStart, jStart, whiteCastle, blackCastle);
                Board.syncBoards(checkBoard, board);

                if (isPawnMove || isCapture) {
                    halfMoveClockRef[0] = 0;
                } else {
                    halfMoveClockRef[0]++;
                }

                return true;
            } else {
                lastErrorRef[0] = "Invalid move for this piece.";
                return false;
            }
        }
    }

    /**
     * Stores complete information about a move for undo functionality.
     */
    public static class MoveRecord {
        public String moveNotation;        // e.g., "e2e4"
        public String piece;               // Piece that moved, e.g., "wp"
        public String capturedPiece;       // Captured piece or null/empty
        public int fromRow, fromCol;       // Starting position
        public int toRow, toCol;           // Ending position
        public boolean[] whiteCastleBefore;  // Castling rights before move
        public boolean[] blackCastleBefore;
        public boolean[] whitePassantBefore; // En passant state before move
        public boolean[] blackPassantBefore;
        public int halfMoveClockBefore;    // For 50 move rule
        public boolean wasWhiteTurn;       // Who made this move
        public boolean wasEnPassant;       // If this was an en passant capture
        public int enPassantCapturedRow;   // Row of captured pawn in en passant
        public int enPassantCapturedCol;   // Col of captured pawn in en passant
        public boolean wasPromotion;       // If this was a pawn promotion
        public String promotedTo;          // What piece was promoted to
        public boolean wasCastling;        // If this was a castling move
        public String castlingType;        // "K" for kingside, "Q" for queenside
        public int rookFromRow, rookFromCol; // Original rook position for castling
        public int rookToRow, rookToCol;   // Where rook moved to for castling

        public MoveRecord(String notation, String piece, String captured,
                          int fr, int fc, int tr, int tc,
                          boolean[] wcb, boolean[] bcb,
                          boolean[] wpb, boolean[] bpb,
                          int hmc, boolean wasWhiteTurn) {
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
            this.wasWhiteTurn = wasWhiteTurn;
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
}
