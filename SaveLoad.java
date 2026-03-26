import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SaveLoad class for ASCII Chess.
 * Handles game saving and loading to/from files.
 */
public class SaveLoad {

    public static final String SAVES_DIR = "saves";
    public static final String SAVE_EXT = ".txt";

    /**
     * Saves the game to a named file.
     * @param gameName the name of the save file
     * @param board the current board
     * @param gameState the game state
     * @param moveHistory the move history
     * @return true if saved successfully
     */
    public static boolean saveGame(String gameName, String[][] board, GameState gameState,
                                    List<Move.MoveRecord> moveHistory) {
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
            // Save whose turn it is NEXT (after the last move). On load, that player should move.
            // gameState.white is already the next player after the move loop switched it.
            writer.println("turn:" + (gameState.white ? "white" : "black"));
            writer.println("board:");
            for (int i = 0; i < 8; i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < 8; j++) {
                    row.append(board[i][j]);
                    if (j < 7) row.append(",");
                }
                writer.println(row);
            }
            writer.println("whiteCastle:" + gameState.whiteCastle[2] + "," + gameState.whiteCastle[6]);
            writer.println("blackCastle:" + gameState.blackCastle[2] + "," + gameState.blackCastle[6]);
            StringBuilder wp = new StringBuilder("whitePassant:");
            StringBuilder bp = new StringBuilder("blackPassant:");
            for (int i = 0; i < 8; i++) {
                wp.append(gameState.whitePassant[i]).append(i < 7 ? "," : "");
                bp.append(gameState.blackPassant[i]).append(i < 7 ? "," : "");
            }
            writer.println(wp);
            writer.println(bp);
            writer.println("halfMoveClock:" + gameState.halfMoveClock);
            writer.println("moves:" + moveHistory.size());
            for (Move.MoveRecord record : moveHistory) {
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
     * Loads a game from a named save file.
     * @param gameName the name of the saved game
     * @param board the board to load into
     * @param gameState the game state to load into
     * @param moveHistory the move history list to load into
     * @return true if loaded successfully
     */
    public static boolean loadGame(String gameName, String[][] board, GameState gameState,
                                    List<Move.MoveRecord> moveHistory) {
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
                    gameState.white = "white".equals(line.substring(5));
                } else if (line.startsWith("board:")) {
                    for (int i = 0; i < 8; i++) {
                        String boardLine = reader.readLine();
                        String[] cells = boardLine.split(",");
                        for (int j = 0; j < 8; j++) {
                            board[i][j] = cells[j];
                        }
                    }
                } else if (line.startsWith("whiteCastle:")) {
                    String[] vals = line.substring(12).split(",");
                    gameState.whiteCastle[2] = Boolean.parseBoolean(vals[0]);
                    gameState.whiteCastle[6] = Boolean.parseBoolean(vals[1]);
                } else if (line.startsWith("blackCastle:")) {
                    String[] vals = line.substring(12).split(",");
                    gameState.blackCastle[2] = Boolean.parseBoolean(vals[0]);
                    gameState.blackCastle[6] = Boolean.parseBoolean(vals[1]);
                } else if (line.startsWith("whitePassant:")) {
                    String[] vals = line.substring(13).split(",");
                    for (int i = 0; i < 8; i++) {
                        gameState.whitePassant[i] = Boolean.parseBoolean(vals[i]);
                    }
                } else if (line.startsWith("blackPassant:")) {
                    String[] vals = line.substring(13).split(",");
                    for (int i = 0; i < 8; i++) {
                        gameState.blackPassant[i] = Boolean.parseBoolean(vals[i]);
                    }
                } else if (line.startsWith("halfMoveClock:")) {
                    gameState.halfMoveClock = Integer.parseInt(line.substring(14));
                } else if (line.startsWith("moves:")) {
                    moveCount = Integer.parseInt(line.substring(6));
                } else if (line.startsWith("move:")) {
                    moveLines.add(line.substring(5));
                }
            }

            moveHistory.clear();
            for (String moveLine : moveLines) {
                String[] parts = moveLine.split("\\|");
                String[] fromCoords = parts[3].split(",");
                String[] toCoords = parts[4].split(",");
                Move.MoveRecord record = new Move.MoveRecord(
                        parts[0], parts[1], parts[2].isEmpty() ? null : parts[2],
                        Integer.parseInt(fromCoords[0]), Integer.parseInt(fromCoords[1]),
                        Integer.parseInt(toCoords[0]), Integer.parseInt(toCoords[1]),
                        gameState.whiteCastle.clone(), gameState.blackCastle.clone(),
                        gameState.whitePassant.clone(), gameState.blackPassant.clone(),
                        gameState.halfMoveClock, Boolean.parseBoolean(parts[5]));
                record.wasCastling = Boolean.parseBoolean(parts[6]);
                record.castlingType = parts[7].isEmpty() ? null : parts[7];
                record.wasPromotion = Boolean.parseBoolean(parts[8]);
                record.promotedTo = parts[9].isEmpty() ? null : parts[9];
                record.wasEnPassant = Boolean.parseBoolean(parts[10]);
                if (parts.length > 11 && !parts[11].isEmpty()) {
                    String[] epCoords = parts[11].split(",");
                    record.enPassantCapturedRow = Integer.parseInt(epCoords[0]);
                    record.enPassantCapturedCol = Integer.parseInt(epCoords[1]);
                }
                if (parts.length > 12 && !parts[12].isEmpty()) {
                    String[] rookFrom = parts[12].split(",");
                    record.rookFromRow = Integer.parseInt(rookFrom[0]);
                    record.rookFromCol = Integer.parseInt(rookFrom[1]);
                }
                if (parts.length > 13 && !parts[13].isEmpty()) {
                    String[] rookTo = parts[13].split(",");
                    record.rookToRow = Integer.parseInt(rookTo[0]);
                    record.rookToCol = Integer.parseInt(rookTo[1]);
                }
                moveHistory.add(record);
            }

            return true;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Lists all saved game names.
     */
    public static List<String> listSavedGames() {
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
     * Deletes a save file.
     */
    public static boolean deleteSaveFile(String gameName) {
        String fileName = SAVES_DIR + File.separator + gameName + SAVE_EXT;
        File file = new File(fileName);
        return file.delete();
    }

    /**
     * Checks if a save file exists.
     */
    public static boolean hasSaveFile(String gameName) {
        String fileName = SAVES_DIR + File.separator + gameName + SAVE_EXT;
        return new File(fileName).exists();
    }

    /**
     * Prompts user to save on exit.
     */
    public static void promptSaveOnExit(java.util.Scanner sc, String[][] board, GameState gameState,
                                         List<Move.MoveRecord> moveHistory) {
        System.out.print("Enter a name to save this game (or type 'quit'/'exit' to skip saving): ");
        String saveName = sc.nextLine().trim();

        if (saveName.equalsIgnoreCase("quit") || saveName.equalsIgnoreCase("exit") || saveName.isEmpty()) {
            System.out.println("Not saving.");
            return;
        }

        if (hasSaveFile(saveName)) {
            System.out.print("A game with this name already exists. Overwrite? (y/n): ");
            String confirm = sc.nextLine().trim();
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.println("Not saving.");
                return;
            }
            deleteSaveFile(saveName);
        }

        if (saveGame(saveName, board, gameState, moveHistory)) {
            System.out.println("Game saved as '" + saveName + "'.");
        } else {
            System.out.println("Failed to save game.");
        }
    }
}
