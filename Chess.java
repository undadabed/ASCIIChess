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
    // Board representation (will be moved to GameState)
    String[][] board;
    String[][] checkBoard;

    // Game state (all state delegated here)
    GameState gameState = new GameState();



    /**
     * Initializes data structures / fields
     */
    public void init() {
        board = Board.initializeBoard();
        checkBoard = Board.initializeBoard();
        gameState.init();
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
        List<String> savedGames = SaveLoad.listSavedGames();
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
                    gameState.chess960 = true;
                    System.out.println("Chess960 mode selected. Generating random starting position...");
                    board = Board.initializeChess960Board();
                    checkBoard = Board.copyBoard(board);
                    gameState.resetCastlingRights();  // Reset castling rights based on king/rook positions
                } else {
                    gameState.chess960 = false;
                    System.out.println("Standard chess selected.");
                }
                System.out.println("Starting new game.");
            } else {
                try {
                    int choice = Integer.parseInt(response);
                    if (choice >= 1 && choice <= savedGames.size()) {
                        String gameName = savedGames.get(choice - 1);
                        if (SaveLoad.loadGame(gameName, board, gameState, gameState.moveHistory)) {
                            Board.syncBoards(board, checkBoard);
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
                        if (SaveLoad.loadGame(response, board, gameState, gameState.moveHistory)) {
                            Board.syncBoards(board, checkBoard);
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

        while (!gameState.complete) {
            Board.printBoard(board);
            if (gameState.white) {
                System.out.println("White's turn");
            } else {
                System.out.println("Black's turn");
            }
            if (gameState.offerdraw) {
                System.out.println("Draw offered. Type 'draw' to accept or make a move to decline.");
                String response = sc.nextLine();
                if (response.trim().equalsIgnoreCase("draw")) {
                    gameState.draw = true;
                    gameState.drawReason = "mutual agreement";
                    gameState.complete = true;
                    // (cleanup of autosave no longer needed with named saves)
                    break;
                }
                // If not accepting draw, continue with normal move processing
                gameState.offerdraw = false;
                if (InputParser.parseInput(response, board, checkBoard, gameState, gameState.moveHistory)) {
                    valid = true;
                    // Check if this was quit, exit, or resign
                    String lower = response.trim().toLowerCase();
                    if (lower.equals("quit") || lower.equals("exit") || lower.equals("resign")) {
                        gameState.complete = true;
                    }
                } else {
                    if (!gameState.lastError.isEmpty()) {
                        System.out.println(gameState.lastError);
                    }
                    // If gameState.lastError is empty (e.g., "help" was typed), just re-prompt
                    continue;
                }
            }
            if (Move.check(checkBoard, gameState.white)) {
                System.out.println("Check!");
            }
            valid = false;
            while (!valid) {
                Board.syncBoards(board, checkBoard);
                System.out.print("> ");
                String input = sc.nextLine();
                if (InputParser.parseInput(input, board, checkBoard, gameState, gameState.moveHistory)) {
                    valid = true;
                    // Check if this was quit, exit, or resign
                    String lower = input.trim().toLowerCase();
                    if (lower.equals("quit") || lower.equals("exit") || lower.equals("resign")) {
                        gameState.complete = true;
                    }
                } else {
                    if (!gameState.lastError.isEmpty()) {
                        System.out.println(gameState.lastError);
                    }
                    // If gameState.lastError is empty, no error message needed (e.g., "help" was typed)
                }
            }
            gameState.promo = "Q";

            // Record position for threefold repetition
            String newPosition = gameState.getPositionKey(board);
            gameState.positionHistory.put(newPosition, gameState.positionHistory.getOrDefault(newPosition, 0) + 1);

            // Check for automatic draws after move
            String autoDraw = gameState.checkAutomaticDraw();
            if (autoDraw != null) {
                Board.printBoard(board);
                System.out.println("Draw by " + autoDraw + "!");
                gameState.draw = true;
                gameState.drawReason = autoDraw;
                gameState.complete = true;
                // (cleanup of autosave no longer needed with named saves)
                break;
            }

            if (!gameState.complete) {
                gameState.white = !gameState.white;
            }

            // Check for checkmate or stalemate
            if (Move.checkMate(checkBoard, gameState.white)) {
                Board.printBoard(board);
                System.out.println("Checkmate!");
                gameState.complete = true;
                gameState.white = !gameState.white;
                // (cleanup of autosave no longer needed with named saves)
            } else if (gameState.isStalemate(checkBoard)) {
                Board.printBoard(board);
                System.out.println("Stalemate!");
                gameState.draw = true;
                gameState.drawReason = "stalemate";
                gameState.complete = true;
                // (cleanup of autosave no longer needed with named saves)
            }

            if (gameState.white) {
                gameState.whitePassant = GameState.initializePassant();
            } else {
                gameState.blackPassant = GameState.initializePassant();
            }
        }

        // Save game if exiting early (quit/resign - but not from normal game end)
        // Note: quit and resign already save in inputParse, this handles Ctrl+C case
        // by using a shutdown hook (not implemented here for simplicity)

        if (gameState.draw) {
            if (!gameState.drawReason.isEmpty()) {
                System.out.println("Game ended in a draw by " + gameState.drawReason + ".");
            } else {
                System.out.println("Game ended in a draw.");
            }
        } else if (gameState.complete && !gameState.draw) {
            // Game ended via quit or resign - no winner announcement here
            // (winner only shown for resign via resign command, quit just ends)
        } else if (gameState.white) {
            System.out.println("White wins!");
        } else {
            System.out.println("Black wins!");
        }

        // Prompt to save game name if exiting early (not checkmate/stalemate/draw)
        // This handles quit, resign, and the user can also just exit without saving
        boolean gameEndedNormally = gameState.draw || (gameState.complete && (gameState.drawReason.equals("stalemate") || gameState.drawReason.equals("50-move rule") || gameState.drawReason.equals("threefold repetition")));
        // Actually, checkmate and normal win are also "normal" but we delete save file there
        // The user wants to prompt for save name on any exit
        // So always prompt unless it's checkmate/stalemate/50-move/threefold (where save is deleted)
        boolean shouldPromptSave = !gameState.draw && !gameState.drawReason.equals("stalemate") && !gameState.drawReason.equals("50-move rule") && !gameState.drawReason.equals("threefold repetition");

        if (shouldPromptSave) {
            SaveLoad.promptSaveOnExit(sc, board, gameState, gameState.moveHistory);
        }

        sc.close();
    }

    /**
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
            if (!game.gameState.complete) {
                System.out.println("\n\nGame interrupted.");
                // Try to prompt for save name (may not work in shutdown hook)
                // Fall back to autosave
                String saveName = "autosave";
                if (SaveLoad.saveGame(saveName, game.gameState.board, game.gameState, game.gameState.moveHistory)) {
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