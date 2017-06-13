package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import puzzles.*;

/**
 * This class parses puzzle files.  A puzzle file contains a list of puzzles, each of
 * which is separated by an empty line (two newlines) and has the following format:
 * 
 * [number of goals to satisfy]
 * <row 1 of puzzle's initial state, consisting of integers separated by spaces/tabs>
 * ...
 * <row n of puzzle's initial state>
 * <minimum number of moves required to solve the puzzle>
 * 
 * The first line, the number of goals to satisfy, is optional and is used in the
 * subproblem condition only.
 * 
 */

public class PuzzleFileParser {
	
	/**
	 * Reads the puzzles from the specified file and returns them.
	 * @param puzzleFileName The name of the puzzle file
	 * @return A Vector of Puzzle objects
	 */
	public static Vector<Puzzle> readPuzzles(String puzzleFileName) {
		Vector<Puzzle> puzzles = new Vector<Puzzle>();
		
		try {
			BufferedReader puzzleFile =  new BufferedReader(new FileReader(puzzleFileName));
			try {
				String line = puzzleFile.readLine();
				
				// Keep reading lines until we reach EOF
				while (line != null) {
					String puzzleStr = "";
					// Keep reading lines for the current puzzle until we reach an empty line
					while (line != null && !line.trim().equals("")) {
						puzzleStr += line + '\n';
						line = puzzleFile.readLine();
					}
					puzzles.add(makePuzzle(puzzleStr));
					line = puzzleFile.readLine();
				}
				
			} catch (IOException e) {
				
			} finally {
				puzzleFile.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println(puzzleFileName + " not found!");
			System.exit(0);
		} catch (IOException e) {}
		
		return puzzles;
	}
	
	/**
	 * Creates a Puzzle object from the specified string containing the puzzle
	 * @param puzzleStr A string specifying the puzzle's initial state, number of moves and
	 * possibly the number of goals to satisfy
	 * @return A Puzzle
	 */
	public static Puzzle makePuzzle(String puzzleStr) {
		Puzzle puzzle;
		String[] lines = puzzleStr.split("\n");
		int numGoals = 0;
		boolean nGoals;
		int numRows, start, minMoves;
		
		// If the puzzle specifies the number of goals to satisfy, it must be on the first
		// line by itself
		try {
			numGoals = Integer.valueOf(lines[0].trim());
			nGoals = true;
			numRows = lines.length - 2;
			start = 1;
		} catch (NumberFormatException e) {
			nGoals = false;
			numRows = lines.length - 1;
			start = 0;
		}
		int numCols = lines[start].split("[\t ]").length;
		minMoves = Integer.valueOf(lines[lines.length - 1].trim());
		
		int numSwappableTiles = 0;
		int numBlanks = 0;
		int[][] tiles = new int[numRows][numCols];
		
		for (int row = 0; row < numRows; row++) {
			String[] rowTiles = lines[row + start].split("[\t ]");
			//for (int i = 0; i < rowTiles.length; i++)
			//	System.out.print(rowTiles[i] + ' ');
			//System.out.println();
			for (int col = 0; col < numCols; col++) {
				int tileNum = Integer.valueOf(rowTiles[col]);
				tiles[row][col] = tileNum;
				if (tileNum <= 0)
					numSwappableTiles++;
				if (tileNum == 0)
					numBlanks++;
			}
		}
		//System.out.println();
		
		if (nGoals)
			puzzle = new NGoalsPuzzle(numRows, numCols, minMoves, numGoals, tiles);
		else if (numBlanks > 1)
			puzzle = new NBlanksPuzzle(numRows, numCols, minMoves, numBlanks, tiles);
		else
			puzzle = new NSwappableTilesPuzzle(numRows, numCols, minMoves, numSwappableTiles,
					tiles);
		
		return puzzle;
	}
	
	public static void main(String args[]) {
		System.out.println("8-puzzles");
		System.out.println("Misplaced\tManhattan");
		Vector<Puzzle> puzzles = readPuzzles("puzzlefiles/puzzles_comp8.txt");
		for (Puzzle p : puzzles) {
			System.out.print(p.numMisplacedTiles() + "\t");
			System.out.println(p.totalManhattanDistance());
		}
		System.out.println();
		
		System.out.println("15-puzzles");
		System.out.println("Misplaced\tManhattan");
		puzzles = readPuzzles("puzzlefiles/puzzles_comp15.txt");
		for (Puzzle p : puzzles) {
			System.out.print(p.numMisplacedTiles() + "\t");
			System.out.println(p.totalManhattanDistance());
		}
	}
}
