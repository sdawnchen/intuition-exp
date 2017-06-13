package util;

import puzzles.*;
import puzzles.Puzzle.PuzzleState;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PuzzleFileGenerator {
    private static void writePuzzleToFile(Puzzle puzzle, BufferedWriter file) {
        try {
            if (puzzle.getClass() == NGoalsPuzzle.class)
                file.write(((NGoalsPuzzle) puzzle).numGoals + "\n");
            
            for (int row = 0; row < puzzle.numRows; row++) {
                for (int col = 0; col < puzzle.numCols; col++) {
                    String line = String.valueOf(puzzle.getTile(row, col));
                    if (col > 0) line = "\t" + line;
                    file.write(line);
                }
                file.write("\n");
            }
            file.write(puzzle.minMoves + "\n\n");
        } catch (IOException e) {}
    }
    
    private static void writeSolnToFile(Puzzle puzzle, BufferedWriter file) {
        Vector<PuzzleState> solution = puzzle.solution;
        try {
            for (int i = 1; i < solution.size(); i++) {
                file.write("Step " + i + "\n");
                file.write(solution.get(i).toString() + "\n");
            }
            file.write("\n");
        } catch (IOException e) {}
    }
    
    public static void main(String args[]) {
        // Set parameters
        // Training puzzles
        int numTrainPuzzles = 13;
        int[] trainMoves = {3, 4, 5, 6, 7, 8, 8, 9, 9, 9, 10, 10, 10};
        int[] trainKs = {4, 4, 3, 3, 2, 2, 2, 1, 1, 1, 1, 0, 0};
        int[] trainSizes = new int[numTrainPuzzles];
        Arrays.fill(trainSizes, 3);
        
        // Test puzzles
        int numTestPuzzles = 12;
        int[] testMoves = new int[numTestPuzzles];
        int[] testSizes = new int[numTestPuzzles];
        Arrays.fill(testMoves, 12);
        // The first half of the test puzzles will be 8-puzzles and the second half
        // will be 15-puzzles
        Arrays.fill(testSizes, 0, numTestPuzzles/2, 3);
        Arrays.fill(testSizes, numTestPuzzles/2, numTestPuzzles, 4);
        
        // 8-puzzle comparisons
        int num8Comps = 20;
        boolean[] comp8FirstPuzzleCloser = new boolean[num8Comps];
        boolean[] comp8SamePath = new boolean[num8Comps];
        Random rand = new Random();
        // Choose a random answer for each comparison
        for (int i = 0; i < num8Comps; i++)
            comp8FirstPuzzleCloser[i] = rand.nextBoolean();
        
        // The first half of the pairs will be on the same solution path with 0.5
        // probability, whereas the second half will be the opposite of the choices
        // in the first half
        for (int i = 0; i < num8Comps/2; i++)
            comp8SamePath[i] = rand.nextBoolean();
        for (int i = num8Comps/2; i < num8Comps; i++)
            comp8SamePath[i] = !comp8SamePath[i - num8Comps/2];
        
        // 15-puzzle comparisons
        int num15Comps = 20;
        boolean[] comp15FirstPuzzleCloser = new boolean[num15Comps];
        boolean[] comp15SamePath = new boolean[num15Comps];
        // Choose a random answer for each comparison
        for (int i = 0; i < num15Comps; i++)
            comp15FirstPuzzleCloser[i] = rand.nextBoolean();
        
        // The first half of the pairs will be on the same solution path with 0.5
        // probability, whereas the second half will be the opposite of the choices
        // in the first half
        for (int i = 0; i < num15Comps/2; i++)
            comp15SamePath[i] = rand.nextBoolean();
        for (int i = num15Comps/2; i < num15Comps; i++)
            comp15SamePath[i] = !comp15SamePath[i - num15Comps/2];
        
        // Create files
        BufferedWriter cPuzzleFile = null, sPuzzleFile = null,
                       rPuzzleFile = null, tPuzzleFile = null,
                       cSolnFile = null, sSolnFile = null,
                       rSolnFile = null, tSolnFile = null,
                       comp8File = null, comp15File = null,
                       paths8File = null, paths15File = null;
        String puzzleFileName = "puzzlefiles/puzzles_";
        String solnFileName = "puzzlefiles/solution_";
        String compFileName = "puzzlefiles/puzzles_comp";
        String pathsFileName = "puzzlefiles/paths_comp";
        
        try {
            cPuzzleFile = new BufferedWriter(new FileWriter(puzzleFileName + "control.txt"));
            sPuzzleFile = new BufferedWriter(new FileWriter(puzzleFileName + "subproblem.txt"));
            rPuzzleFile = new BufferedWriter(new FileWriter(puzzleFileName + "relaxed.txt"));
            tPuzzleFile = new BufferedWriter(new FileWriter(puzzleFileName + "test.txt"));
            
            cSolnFile = new BufferedWriter(new FileWriter(solnFileName + "control.txt"));
            sSolnFile = new BufferedWriter(new FileWriter(solnFileName + "subproblem.txt"));
            rSolnFile = new BufferedWriter(new FileWriter(solnFileName + "relaxed.txt"));
            tSolnFile = new BufferedWriter(new FileWriter(solnFileName + "test.txt"));
            
            comp8File = new BufferedWriter(new FileWriter(compFileName + "8.txt"));
            comp15File = new BufferedWriter(new FileWriter(compFileName + "15.txt"));
            paths8File = new BufferedWriter(new FileWriter(pathsFileName + "8.txt"));
            paths15File = new BufferedWriter(new FileWriter(pathsFileName + "15.txt"));
        } catch (IOException e) {}
        
        // Generate training puzzles
        for (int i = 0; i < numTrainPuzzles; i++) {
            // Get the parameters for this set of training puzzles
            int size = trainSizes[i], moves = trainMoves[i], k = trainKs[i];
            int numGoals = size * size - 1;
            
            // Create the training puzzles in each condition
            Puzzle controlPuzzle = new NSwappableTilesPuzzle(size, size, moves, 1);
            Puzzle subproblemPuzzle = new NGoalsPuzzle(size, size, moves, numGoals - k);
            Puzzle relaxedPuzzle = new NSwappableTilesPuzzle(size, size, moves, k + 1);
            controlPuzzle.newRandomPuzzle();
            subproblemPuzzle.newRandomPuzzle();
            relaxedPuzzle.newRandomPuzzle();
            
            // Write the puzzles to file
            writePuzzleToFile(controlPuzzle, cPuzzleFile);
            writePuzzleToFile(subproblemPuzzle, sPuzzleFile);
            writePuzzleToFile(relaxedPuzzle, rPuzzleFile);
            
            // Write the puzzles' solutions to file
            try {
                cSolnFile.write("Puzzle " + i + "\n");
                sSolnFile.write("Puzzle " + i + "\n");
                rSolnFile.write("Puzzle " + i + "\n");
            } catch (IOException e) {}
            
            writeSolnToFile(controlPuzzle, cSolnFile);
            writeSolnToFile(subproblemPuzzle, sSolnFile);
            writeSolnToFile(relaxedPuzzle, rSolnFile);
        }
        
        // Generate test puzzles
        for (int i = 0; i < numTestPuzzles; i++) {
            int size = testSizes[i], moves = testMoves[i];
            Puzzle testPuzzle = new NSwappableTilesPuzzle(size, size, moves, 1);
            testPuzzle.newRandomPuzzle();
            writePuzzleToFile(testPuzzle, tPuzzleFile);
            try {
                tSolnFile.write("Puzzle " + (i + 1) + "\n");
            } catch (IOException e) {}
            writeSolnToFile(testPuzzle, tSolnFile);
        }
        
        // Generate 8-puzzle comparisons
        for (int i = 0; i < num8Comps; i++) {
            boolean firstPuzzleCloser = comp8FirstPuzzleCloser[i];
            boolean samePath = comp8SamePath[i];
            
            // Randomly choose fartherDist and closerDist
            int fartherDist = 2 + rand.nextInt(27);     // 2 through 28
            int diffRatioNum = 1 + rand.nextInt(5);     // 1 through 5
            int diffRatioDenom = diffRatioNum + 1 + rand.nextInt(15 - diffRatioNum);  // diffRatioNum+1 through 15
            
            // Bias closerDist to be closer to fartherDist (harder comparisons)
            int closerDist = (int) Math.ceil(fartherDist * (1 - ((float) diffRatioNum) / diffRatioDenom));
            if (closerDist == fartherDist)
                closerDist--;
            
            // Create the closer and farther puzzles
            // Randomly generate the farther puzzle
            Puzzle nearPuzzle, farPuzzle = new NSwappableTilesPuzzle(3, 3, fartherDist, 1);
            farPuzzle.newRandomPuzzle();
            // If the puzzles are on the same path, use a puzzle state from the solution of
            // the farther puzzle as the closer puzzle
            if (samePath) {
                PuzzleState nearPuzzleState = farPuzzle.solution.get(fartherDist - closerDist);
                nearPuzzle = new NSwappableTilesPuzzle(3, 3, closerDist, 1, nearPuzzleState);
            // Otherwise, create a new puzzle as the closer puzzle
            } else {
                nearPuzzle = new NSwappableTilesPuzzle(3, 3, closerDist, 1);
                nearPuzzle.newRandomPuzzle();
            }
            
            Puzzle puzzle1, puzzle2;
            if (firstPuzzleCloser) {
                puzzle1 = nearPuzzle;
                puzzle2 = farPuzzle;
            } else {
                puzzle1 = farPuzzle;
                puzzle2 = nearPuzzle;
            }
            
            // Write the data to file
            writePuzzleToFile(puzzle1, comp8File);
            writePuzzleToFile(puzzle2, comp8File);
            try {
                if (samePath)
                    paths8File.write("Same\n");
                else
                    paths8File.write("Different\n");
            } catch (IOException e) {}
        }
        
        // Generate 15-puzzle comparisons
        for (int i = 0; i < num15Comps; i++) {
            boolean firstPuzzleCloser = comp15FirstPuzzleCloser[i];
            boolean samePath = comp15SamePath[i];
            
            // Randomly choose fartherDist and closerDist
            int fartherDist = 2 + rand.nextInt(27);     // 2 through 28
            int diffRatioNum = 1 + rand.nextInt(5);     // 1 through 5
            int diffRatioDenom = diffRatioNum + 1 + rand.nextInt(15 - diffRatioNum);  // diffRatioNum+1 through 15
            
            // Bias closerDist to be closer to fartherDist (harder comparisons)
            int closerDist = (int) Math.ceil(fartherDist * (1 - ((float) diffRatioNum) / diffRatioDenom));
            if (closerDist == fartherDist)
                closerDist--;
            
            // Create the closer and farther puzzles
            // Randomly generate the farther puzzle
            Puzzle nearPuzzle, farPuzzle = new NSwappableTilesPuzzle(4, 4, fartherDist, 1);
            farPuzzle.newRandomPuzzle();
            // If the puzzles are on the same path, use a puzzle state from the solution of
            // the farther puzzle as the closer puzzle
            if (samePath) {
                PuzzleState nearPuzzleState = farPuzzle.solution.get(fartherDist - closerDist);
                nearPuzzle = new NSwappableTilesPuzzle(4, 4, closerDist, 1, nearPuzzleState);
            // Otherwise, create a new puzzle as the closer puzzle
            } else {
                nearPuzzle = new NSwappableTilesPuzzle(4, 4, closerDist, 1);
                nearPuzzle.newRandomPuzzle();
            }
            
            Puzzle puzzle1, puzzle2;
            if (firstPuzzleCloser) {
                puzzle1 = nearPuzzle;
                puzzle2 = farPuzzle;
            } else {
                puzzle1 = farPuzzle;
                puzzle2 = nearPuzzle;
            }
            
            // Write the data to file
            writePuzzleToFile(puzzle1, comp15File);
            writePuzzleToFile(puzzle2, comp15File);
            try {
                if (samePath)
                    paths15File.write("Same\n");
                else
                    paths15File.write("Different\n");
            } catch (IOException e) {}
        }
        
        // Close all files
        try {
            cPuzzleFile.close();
            sPuzzleFile.close();
            rPuzzleFile.close();
            tPuzzleFile.close();
            
            cSolnFile.close();
            sSolnFile.close();
            rSolnFile.close();
            tSolnFile.close();
            
            comp8File.close();
            comp15File.close();
            paths8File.close();
            paths15File.close();
        } catch (IOException e) {}
    }
}
