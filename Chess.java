import java.util.Scanner;
/**
 * @author Jonathan Fan + Ryan Hsiu
 */
public class Chess {
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
     * Prints out chess board given 2d array of strings
     * @param board the chess board that is printed out
     */
    public void printBoard(String[][] board) {
        System.out.println();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.print(8-i);
            System.out.println();
        }
        System.out.print(" a  b  c  d  e  f  g  h");
        System.out.println();
        System.out.println();
    }

    /**
     * Translates a letter into an array index according to how a chess board is formatted
     * @param input the given letter for a move
     * @return the letter's corresponding array index
     */
    public int letterToCoordinate(String input) {
        if (input.equals("a")) {
            return 0;
        }
        else if (input.equals("b")) {
            return 1;
        }
        else if (input.equals("c")) {
            return 2;
        }
        else if (input.equals("d")) {
            return 3;
        }
        else if (input.equals("e")) {
            return 4;
        }
        else if (input.equals("f")) {
            return 5;
        }
        else if (input.equals("g")) {
            return 6;
        }
        else if (input.equals("h")) {
            return 7;
        }
        return -1;
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
                else if (jEnd == jStart && iStart-iEnd == 1 && (board[iEnd][jEnd] == "  " || board[iEnd][jEnd] == "##")) {
                }
                else if (iStart == 6 && iEnd == 4 && jStart == jEnd && (board[5][jStart].equals("  ") || board[5][jStart].equals("##")) && (board[4][jStart].equals("  ") || board[4][jStart].equals("##"))) {
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
                else if (jEnd == jStart && iEnd-iStart == 1 && (board[iEnd][jEnd] == "  " || board[iEnd][jEnd] == "##")) {
                }
                else if (iStart == 1 && iEnd == 3 && jStart == jEnd && (board[2][jStart].equals("  ") || board[2][jStart].equals("##")) && (board[3][jStart].equals("  ") || board[3][jStart].equals("##"))) {
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
                // white right castle
                if (jEnd == 6 && whiteCastle[6] && board[7][5].equals("  ") && board[7][6].equals("##")) {
                    checkBoard[7][7] = "  ";
                    checkBoard[7][5] = "wR";
                    whiteCastle[2] = false;
                    whiteCastle[6] = false;
                    return true;
                }
                // white left castle
                else if (jEnd == 2 && whiteCastle[2] && board[7][1].equals("  ") && board[7][2].equals("##") && board[7][3].equals("  ")) {
                    checkBoard[7][0] = "##";
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
                // black right castle
                if (jEnd == 6 && blackCastle[6] && board[0][5].equals("##") && board[0][6].equals("  ")) {
                    checkBoard[0][7] = "##";
                    checkBoard[0][5] = "bR";
                    blackCastle[2] = false;
                    blackCastle[6] = false;
                    return true;
                }
                // black left castle
                else if (jEnd == 2 && blackCastle[2] && board[0][1].equals("##") && board[0][2].equals("  ") && board[0][3].equals("##")) {
                    checkBoard[0][0] = "  ";
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
        int jStart = letterToCoordinate(inputs[0].substring(0,1));
        int iStart = 8-Integer.parseInt(inputs[0].substring(1,2));
        int jEnd = letterToCoordinate(inputs[1].substring(0,1));
        int iEnd = 8-Integer.parseInt(inputs[1].substring(1,2));
        if (jStart > 7 || jStart < 0 || iStart > 7 || iStart < 0 || jEnd > 7 || jEnd < 0 || iEnd > 7 || iEnd < 0) {
            return false;
        }
        if (white) {
            if (checkBoard[iStart][jStart].substring(0,1).equals("w")) {
                String start = board[iStart][jStart];
                checkBoard[iEnd][jEnd] = start;
                if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                    checkBoard[iStart][jStart] = "##";
                }
                else {
                    checkBoard[iStart][jStart] = "  ";
                }
                if (check(checkBoard)) {
                    return false;
                }
                if (moveApproval(iStart, jStart, iEnd, jEnd, board)) {
                    updateBoard();
                    updateCastle(iStart, jStart);
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
            if (checkBoard[iStart][jStart].substring(0,1).equals("b")) {
                String start = board[iStart][jStart];
                checkBoard[iEnd][jEnd] = start;
                if ((iStart % 2 == 1 && jStart % 2 == 0) || (iStart % 2 == 0 && jStart % 2 == 1)) {
                    checkBoard[iStart][jStart] = "##";
                }
                else {
                    checkBoard[iStart][jStart] = "  ";
                }
                if (check(checkBoard)) {
                    return false;
                }
                if (moveApproval(iStart, jStart, iEnd, jEnd, board)) {
                    updateCastle(iStart, jStart);
                    updateBoard();
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
    }

    /**
     * Breaks input into an array of strings and checks for edge cases
     * If a draw is offered, sets offered draw boolean to true
     * If a piece is specified for promotion, sets the promo string to the corresponding piece
     * @param input String representing the next line gotten by the scanner
     * @return true if the moves reflected by the input are ultimately valid
     */
    public boolean inputParse(String input) {
        int size = 1;
        for (int i = 0; i < input.length(); i++) {
            if (input.substring(i, i+1).equals(" ")) {
                size++;
            }
        }
        String[] inputs = new String[size];
        int j = 0;
        int index = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.substring(i, i+1).equals(" ")) {
                inputs[index] = input.substring(j, i);
                index++;
                i++;
                j = i;
            }
        }
        inputs[size-1] = input.substring(j,input.length());
        if (inputs.length > 2) {
            promo = inputs[2];
            if (inputs[2].equals("draw?")) {
                offerdraw = true;
            }
        }
        if (inputs[0].equals("resign")) {
            draw = false;
            complete = true;
            return true;
        }
        else {
            return validMove(inputs);
        }
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
        promo = "";
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
        while (!complete) {
            printBoard(board);
            if (white) {
                System.out.println("White's turn");
            }
            else {
                System.out.println("Black's turn");
            }
            if (offerdraw) {
                while (!sc.nextLine().equals("draw")) {
                    System.out.println("Illegal move, try again");
                }
                draw = true;
                complete = true;
                break;
            }
            if (check(checkBoard)) {
                System.out.println("Check");
            }
            valid = false;
            while (!valid) {
                syncBoards(board, checkBoard);
                String input = sc.nextLine();
                if (inputParse(input)) {
                    valid = true;
                }
                else {
                    System.out.println("Illegal move, try again");
                }
            }
            promo = "";
            white = !white;
            if (checkMate()) {
                printBoard(board);
                System.out.println("Checkmate");
                complete = true;
                white = !white;
            }
            if (white) {
                whitePassant = initializePassant();
            }
            else {
                blackPassant = initializePassant();
            }
        }
        if (draw) {
        }
        else if (white) {
            System.out.println("White wins!");
        }
        else {
            System.out.println("Black wins!");
        }
        sc.close();
    }
    
    /**
     * Creates a Chess instance and calls play to start the game
     * @param args Not used
     */
    public static void main(String[] args) {
        Chess game = new Chess();
        game.play();
    }
}