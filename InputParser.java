import java.util.ArrayList;
import java.util.List;

/**
 * InputParser class for ASCII Chess.
 * Handles parsing of user input: move notation, commands, castling, promotion.
 */
public class InputParser {

    /**
     * Parses user input and executes the corresponding action.
     * Handles commands (help, history, undo, quit, resign), castling, and move notation.
     *
     * @param input the raw user input string
     * @param board the main board
     * @param checkBoard the working board
     * @param gameState the game state
     * @param moveHistory the move history list
     * @return true if the game should continue, false if re-prompt needed or game ended
     */
    public static boolean parseInput(String input, String[][] board, String[][] checkBoard,
                                     GameState gameState, List<Move.MoveRecord> moveHistory) {
        gameState.lastError = "";

        if (input == null || input.trim().isEmpty()) {
            gameState.lastError = "No input received. Type 'help' for instructions.";
            return false;
        }

        input = input.trim();

        // Handle commands
        if (input.equalsIgnoreCase("help")) {
            showHelp();
            return false;
        }

        if (input.equalsIgnoreCase("history")) {
            showHistory(moveHistory);
            return false;
        }

        if (input.equalsIgnoreCase("undo")) {
            if (undoMove(board, checkBoard, gameState, moveHistory)) {
                Board.syncBoards(board, checkBoard);
                Board.printBoard(board);
                System.out.println(gameState.white ? "White's turn" : "Black's turn");
                return false;
            } else {
                return false;
            }
        }

        if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
            return true;
        }

        if (input.equalsIgnoreCase("resign")) {
            gameState.drawReason = "";
            return true;
        }

        // Handle castling
        if (input.equalsIgnoreCase("O-O") || input.equalsIgnoreCase("O-O-O") ||
            input.equals("0-0") || input.equals("0-0-0")) {
            return handleCastling(input, board, checkBoard, gameState, moveHistory);
        }

        // Parse tokens
        String[] tokens;
        if (input.contains(" ")) {
            tokens = input.split("\\s+");
        } else {
            tokens = new String[]{input};
        }

        boolean drawOffer = false;
        String promotionPiece = null;

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("draw?")) {
                drawOffer = true;
                tokens[i] = null;
            }
        }

        List<String> moveTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) continue;
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
            gameState.drawReason = "draw offered";
        }

        if (promotionPiece != null) {
            gameState.promo = promotionPiece;
        } else {
            gameState.promo = "Q";
        }

        String[] moveArgs;
        if (moveTokens.size() == 1) {
            String singleToken = moveTokens.get(0);
            if (singleToken.length() >= 4) {
                String from = singleToken.substring(0, 2);
                String to = singleToken.substring(2, 4);
                moveArgs = new String[]{from, to};
            } else if (singleToken.length() == 2) {
                moveArgs = findImplicitPawnMove(singleToken, board, gameState.white);
                if (moveArgs == null) {
                    gameState.lastError = "No valid pawn move to " + singleToken + " found.";
                    return false;
                }
            } else {
                gameState.lastError = "Invalid move format. Use: e2 e4, e2e4, or e4";
                return false;
            }
        } else if (moveTokens.size() >= 2) {
            moveArgs = new String[]{moveTokens.get(0), moveTokens.get(1)};
        } else {
            gameState.lastError = "Invalid move format. Type 'help' for instructions.";
            return false;
        }

        int[] halfMoveRef = new int[]{gameState.halfMoveClock};
        String[] errorRef = new String[]{gameState.lastError};
        boolean result = Move.validMove(moveArgs, board, checkBoard, gameState.white, gameState.whiteCastle, gameState.blackCastle, gameState.whitePassant, gameState.blackPassant, gameState.promo, halfMoveRef, errorRef, moveHistory);
        gameState.halfMoveClock = halfMoveRef[0];
        gameState.lastError = errorRef[0];
        return result;
    }

    /**
     * Attempts to find a pawn that can move to the target square.
     * Used for implicit pawn moves like "e4".
     */
    public static String[] findImplicitPawnMove(String targetSquare, String[][] board, boolean isWhite) {
        int[] target = Board.parseSquare(targetSquare);
        if (target == null) return null;

        int iEnd = target[0];
        int jEnd = target[1];
        String colorPrefix = isWhite ? "w" : "b";
        String pawnPiece = colorPrefix + "p";

        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;

        // Check single push
        int iStart = iEnd - direction;
        if (iStart >= 0 && iStart < 8 && board[iStart][jEnd].equals(pawnPiece)) {
            return new String[]{((char)('a' + jEnd)) + "" + (8 - iStart), targetSquare};
        }

        // Check double push (pawn must be on its starting row: row 6 for white, row 1 for black)
        int pawnStartRow = isWhite ? 6 : 1;
        int iDoubleStart = iEnd - 2 * direction; // Two squares toward the pawn from target
        if (iDoubleStart == pawnStartRow && iDoubleStart >= 0 && iDoubleStart < 8) {
            int iMiddle = iEnd - direction; // One square toward the pawn from target
            if (board[iDoubleStart][jEnd].equals(pawnPiece) &&
                Piece.isEmptySquare(board[iMiddle][jEnd])) {
                return new String[]{((char)('a' + jEnd)) + "" + (8 - iDoubleStart), targetSquare};
            }
        }

        // Check captures (pawn captures diagonally one square forward)
        int iCaptureRow = iEnd - direction;
        if (iCaptureRow >= 0 && iCaptureRow < 8) {
            for (int dj = -1; dj <= 1; dj += 2) {
                int jStart = jEnd + dj;
                if (jStart >= 0 && jStart < 8 && board[iCaptureRow][jStart].equals(pawnPiece)) {
                    return new String[]{((char)('a' + jStart)) + "" + (8 - iCaptureRow), targetSquare};
                }
            }
        }

        return null;
    }

    /**
     * Handles castling input (O-O, O-O-O, 0-0, 0-0-0).
     */
    public static boolean handleCastling(String castleInput, String[][] board, String[][] checkBoard,
                                         GameState gameState, List<Move.MoveRecord> moveHistory) {
        String kingSquare = gameState.white ? "e1" : "e8";
        String targetSquare;

        if (castleInput.equalsIgnoreCase("O-O") || castleInput.equals("0-0")) {
            targetSquare = gameState.white ? "g1" : "g8";
        } else if (castleInput.equalsIgnoreCase("O-O-O") || castleInput.equals("0-0-0")) {
            targetSquare = gameState.white ? "c1" : "c8";
        } else {
            return false;
        }

        int[] halfMoveRef = new int[]{gameState.halfMoveClock};
        String[] errorRef = new String[]{gameState.lastError};
        boolean result = Move.validMove(new String[]{kingSquare, targetSquare}, board, checkBoard, gameState.white, gameState.whiteCastle, gameState.blackCastle, gameState.whitePassant, gameState.blackPassant, gameState.promo, halfMoveRef, errorRef, moveHistory);
        gameState.halfMoveClock = halfMoveRef[0];
        gameState.lastError = errorRef[0];
        return result;
    }

    private static void showHelp() {
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

    private static void showHistory(List<Move.MoveRecord> moveHistory) {
        System.out.println();
        System.out.println("Move History:");
        System.out.println("-------------");
        if (moveHistory.isEmpty()) {
            System.out.println("No moves yet.");
        } else {
            for (int i = 0; i < moveHistory.size(); i++) {
                Move.MoveRecord record = moveHistory.get(i);
                String turn = record.wasWhiteTurn ? "White" : "Black";
                System.out.println((i + 1) + ". " + turn + ": " + record.moveNotation);
            }
        }
        System.out.println();
    }

    private static boolean undoMove(String[][] board, String[][] checkBoard, GameState gameState,
                                    List<Move.MoveRecord> moveHistory) {
        if (moveHistory.isEmpty()) {
            gameState.lastError = "No moves to undo.";
            return false;
        }

        Move.MoveRecord lastMove = moveHistory.remove(moveHistory.size() - 1);

        // Restore piece to original position
        board[lastMove.fromRow][lastMove.fromCol] = lastMove.piece;

        // Restore captured piece or empty square
        if (lastMove.capturedPiece != null && !lastMove.capturedPiece.isEmpty()) {
            board[lastMove.toRow][lastMove.toCol] = lastMove.capturedPiece;
        } else {
            board[lastMove.toRow][lastMove.toCol] = ((lastMove.toRow % 2 == 1 && lastMove.toCol % 2 == 0) ||
                                                       (lastMove.toRow % 2 == 0 && lastMove.toCol % 2 == 1)) ? "##" : "  ";
        }

        // Handle en passant restore
        if (lastMove.wasEnPassant) {
            String capturedPawn = lastMove.wasWhiteTurn ? "bp" : "wp";
            board[lastMove.enPassantCapturedRow][lastMove.enPassantCapturedCol] = capturedPawn;
        }

        // Handle castling restore
        if (lastMove.wasCastling) {
            String rookPiece = lastMove.wasWhiteTurn ? "wR" : "bR";
            board[lastMove.rookFromRow][lastMove.rookFromCol] = rookPiece;
            board[lastMove.rookToRow][lastMove.rookToCol] = ((lastMove.rookToRow % 2 == 1 && lastMove.rookToCol % 2 == 0) ||
                                                              (lastMove.rookToRow % 2 == 0 && lastMove.rookToCol % 2 == 1)) ? "##" : "  ";
        }

        // Handle promotion restore
        if (lastMove.wasPromotion) {
            board[lastMove.fromRow][lastMove.fromCol] = lastMove.wasWhiteTurn ? "wp" : "bp";
        }

        // Restore castling rights
        gameState.whiteCastle = lastMove.whiteCastleBefore.clone();
        gameState.blackCastle = lastMove.blackCastleBefore.clone();

        // Restore en passant
        gameState.whitePassant = lastMove.whitePassantBefore.clone();
        gameState.blackPassant = lastMove.blackPassantBefore.clone();

        // Restore half-move clock
        gameState.halfMoveClock = lastMove.halfMoveClockBefore;

        // Switch turn back
        gameState.white = lastMove.wasWhiteTurn;

        Board.syncBoards(board, checkBoard);
        return true;
    }
}
