package analysis;

import puzzles.*;
import util.PuzzleFileParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.Vector;

import jxl.*;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.Number;


public class AutoMovesAnalyzer {
	static Vector<Puzzle> puzzlesControl = 
		PuzzleFileParser.readPuzzles("puzzlefiles/puzzles_control.txt");
	static Vector<Puzzle> puzzlesSubprob = 
		PuzzleFileParser.readPuzzles("puzzlefiles/puzzles_subproblem.txt");
	static Vector<Puzzle> puzzlesRelaxed = 
		PuzzleFileParser.readPuzzles("puzzlefiles/puzzles_relaxed.txt");
	static Vector<Puzzle> puzzlesTest = 
		PuzzleFileParser.readPuzzles("puzzlefiles/puzzles_test.txt");
	
	public static void main(String args[]) {
		int firstFile = 61;
		int lastFile = 76;
		
		for (int i = firstFile; i <= lastFile; i++) {
			String solnFileName = "../results/raw/solution" + i + ".txt";
			String excelFileName = "../results/excel/subject" + i + ".xls";
			try {
				Workbook workbook = Workbook.getWorkbook(new File(excelFileName));
				WritableWorkbook copy = Workbook.createWorkbook(
						new File("../results/excel/subject" + i + "copy.xls"), workbook);
				analyze(new BufferedReader(new FileReader(solnFileName)), copy);
			} catch (FileNotFoundException e) {
				System.err.println(solnFileName + " not found!");
				System.exit(0);
			} catch (IOException e) {
				
			} catch (BiffException e) {
				System.err.println("Error reading Excel workbook " + excelFileName);
				System.exit(0);
			}
			System.out.println("FINISHED SUBJECT " + i);
			System.out.println();
		}
	}

	private static void analyze(BufferedReader solnFile, WritableWorkbook excelFile) {
		WritableSheet movesSheet = excelFile.createSheet("moves", 1);
		WritableSheet puzzlesSheet = excelFile.getSheet("puzzles");
		
		int excelRow = 0, puzzleNum = 0, moveNum = 0;
		int initLatSum = 0, interLatSum = 0, puzzleInterLatSum = 0;
		int trainInitLatSum = 0, trainInterLatSum = 0;
		int numInitMoves = 0, numLaterMoves = 0, numInterIllMoves = 0;
		int trainNumInitMoves = 0, trainNumLaterMoves = 0;
		int lastTrainRow = -1;
		int solvedInitLatSum = 0, solvedInterLatSum = 0;
		int solvedNumInitMoves = 0, solvedNumLaterMoves = 0;
		boolean training = false, skip0 = false, lastBacktrack = false;
		boolean useLatency2 = false, solved = false;
		String condition = "";
		Puzzle prevState = null, currentState;
		Vector<Puzzle> pastStates = new Vector<Puzzle>();
		int pivot = -1, offset = 1;
		
		try {
			int initLatCol = 9, interLatCol = 10, puzzleRow = 5, solvedCol = 1;
			puzzlesSheet.addCell(new Label(initLatCol, 3, "Init. latency"));
			puzzlesSheet.addCell(new Label(interLatCol, 3, "Inter latency"));
			puzzlesSheet.addCell(new Label(initLatCol, 19, "Init. latency"));
			puzzlesSheet.addCell(new Label(interLatCol, 19, "Inter latency"));
			
			
			String line = solnFile.readLine();
			
			// Keep reading lines until we reach EOF
			while (line != null) {
				if (line.startsWith("Subject")) {
					movesSheet.addCell(new Label(0, excelRow, line));
					excelRow += 2;
					condition = line.split(" ")[2];
					
				} else if (line.startsWith("TRAINING")) {
					movesSheet.addCell(new Label(0, excelRow, line));
					excelRow += 2;
					training = true;
					
				} else if (line.startsWith("TEST")) {
					// Write the average inter-move latency for the last training puzzle
					double avgInterLat;
					if (useLatency2)
						avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 2);
					else
						avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 1);
					puzzlesSheet.addCell(new Number(interLatCol, puzzleRow, avgInterLat));
					
					trainInitLatSum = initLatSum;
					trainNumInitMoves = numInitMoves;
					trainInterLatSum = interLatSum;
					trainNumLaterMoves = numLaterMoves;
					lastTrainRow = excelRow;
					
					excelRow++;
					puzzleRow = 20;
					movesSheet.addCell(new Label(0, excelRow, line));
					excelRow += 2;
					training = false;
					puzzleNum = 0;
					
				} else if (line.equals("Puzzle 0")) {
					skip0 = true;
					
				} else if (line.startsWith("Puzzle")) {
					skip0 = false;
					puzzleNum++;
					
					// Make the 1st prevState the puzzle to be solved
					if (training) {
						if (condition.equals("control"))
							prevState = puzzlesControl.get(puzzleNum);
						else if (condition.equals("subproblem"))
							prevState = puzzlesSubprob.get(puzzleNum);
						else
							prevState = puzzlesRelaxed.get(puzzleNum);
					} else {
						// Don't skip the 0th puzzle during test
						prevState = puzzlesTest.get(puzzleNum - 1);
					}
					
					// Write the column headings for this puzzle
					movesSheet.addCell(new Label(0, excelRow, line));
					excelRow++;
					movesSheet.addCell(new Label(0, excelRow, "Move #"));
					movesSheet.addCell(new Label(1, excelRow, "Legal?"));
					movesSheet.addCell(new Label(2, excelRow, "Latency (ms)"));
					movesSheet.addCell(new Label(3, excelRow, "Dist change"));
					movesSheet.addCell(new Label(4, excelRow, "Misp change"));
					movesSheet.addCell(new Label(5, excelRow, "Manh change"));
					movesSheet.addCell(new Label(6, excelRow, "Backtracking?"));
					movesSheet.addCell(new Label(7, excelRow, "Backtrack length"));
					excelRow++;
					
					
					// If this is not the beginning of the first puzzle, write the average
					// inter-move latency of the last puzzle to the "puzzles" sheet next
					// to the corresponding puzzle
					if (puzzleNum > 1) {
						double avgInterLat;
						if (useLatency2)
							avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 2);
						else
							avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 1);
						puzzlesSheet.addCell(new Number(interLatCol, puzzleRow++, avgInterLat));
					}
					
					// Restore the per-puzzle information
					useLatency2 = false;
					moveNum = 0;
					puzzleInterLatSum = 0;
					solved = ((LabelCell) puzzlesSheet.getCell(solvedCol, puzzleRow)).getString().equals("Y");
					
				} else if (line.startsWith("Illegal") && !skip0) {
					moveNum++;
					movesSheet.addCell(new Number(0, excelRow, moveNum));
					movesSheet.addCell(new Number(1, excelRow, 0));
					
					String[] tokens = line.split(" ");
					int latency = Integer.valueOf(tokens[tokens.length - 2].substring(1));
					movesSheet.addCell(new Number(2, excelRow, latency));
					excelRow++;
					
					// If 1st latency < 1000 ms, use 2nd latency as initial latency
					if (moveNum == 1) {
						if (latency < 1000)
							useLatency2 = true;
						else {
							initLatSum += latency;
							numInitMoves++;
							// Write the initial latency in the "puzzles" sheet next to
							// the corresponding puzzle
							puzzlesSheet.addCell(new Number(initLatCol, puzzleRow, latency));
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' initial latencies
							if (solved) {
								solvedInitLatSum += latency;
								solvedNumInitMoves++;
							}
						}
					} else {
						if (useLatency2 && moveNum == 2) {
							initLatSum += latency;
							numInitMoves++;
							// Write the initial latency in the "puzzles" sheet next to
							// the corresponding puzzle
							puzzlesSheet.addCell(new Number(initLatCol, puzzleRow, latency));
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' initial latencies
							if (solved) {
								solvedInitLatSum += latency;
								solvedNumInitMoves++;
							}
						} else {
							interLatSum += latency;
							puzzleInterLatSum += latency;
							numLaterMoves++;
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' inter-move latencies
							if (solved) {
								solvedInterLatSum += latency;
								solvedNumLaterMoves++;
							}
						}
					}
					
					numInterIllMoves++;
					
				} else if (line.startsWith("Step") && !skip0) {
					moveNum++;
					movesSheet.addCell(new Number(0, excelRow, moveNum));
					movesSheet.addCell(new Number(1, excelRow, 1));
					
					String[] tokens = line.split(" ");
					int latency = Integer.valueOf(tokens[tokens.length - 2].substring(1));
					movesSheet.addCell(new Number(2, excelRow, latency));
					
					// If 1st latency < 1000 ms, use 2nd latency as initial latency
					if (moveNum == 1) {
						if (latency < 1000)
							useLatency2 = true;
						else {
							initLatSum += latency;
							numInitMoves++;
							// Write the initial latency in the "puzzles" sheet next to
							// the corresponding puzzle
							puzzlesSheet.addCell(new Number(initLatCol, puzzleRow, latency));
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' initial latencies
							if (solved) {
								solvedInitLatSum += latency;
								solvedNumInitMoves++;
							}
						}
					} else {
						if (useLatency2 && moveNum == 2) {
							initLatSum += latency;
							numInitMoves++;
							// Write the initial latency in the "puzzles" sheet next to
							// the corresponding puzzle
							puzzlesSheet.addCell(new Number(initLatCol, puzzleRow, latency));
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' initial latencies
							if (solved) {
								solvedInitLatSum += latency;
								solvedNumInitMoves++;
							}
						} else {
							interLatSum += latency;
							puzzleInterLatSum += latency;
							numLaterMoves++;
							
							// If the current puzzle was solved, add the latency to the sum of solved
							// puzzles' inter-move latencies
							if (solved) {
								solvedInterLatSum += latency;
								solvedNumLaterMoves++;
							}
						}
					}
					
				} else if (line.startsWith("\t") && !skip0) {
					// Parse the puzzle
					String[] rowTiles = line.split("\t");
					int size = rowTiles.length - 1;
					int[][] tiles = new int[size][size];
					int numSwappableTiles = 0;
					
					for (int r = 0; r < size; r++) {
						for (int c = 0; c < size; c++) {
							int tileNum = Integer.valueOf(rowTiles[c + 1]);
							tiles[r][c] = tileNum;
							if (tileNum <= 0)
								numSwappableTiles++;
						}
						line = solnFile.readLine();
						rowTiles = line.split("\t");
					}
					
					// Create the puzzle
					if (training && condition.equals("subproblem")) {
						// Get numGoals for this subproblem puzzle from the appropriate puzzle
						// in the subproblem condition puzzle file
						int numGoals = ((NGoalsPuzzle) puzzlesSubprob.get(puzzleNum)).numGoals;
						currentState = new NGoalsPuzzle(size, size, 0, numGoals, tiles);
					} else
						currentState = new NSwappableTilesPuzzle(size, size, 0,
								numSwappableTiles, tiles);
					currentState.minMoves = currentState.aStarSearch().size();
					
					// Compare the current state to the previous one on various measures of distance 
					movesSheet.addCell(new Number(3, excelRow,
							currentState.minMoves - prevState.minMoves));
					movesSheet.addCell(new Number(4, excelRow,
							currentState.numMisplacedTiles() - prevState.numMisplacedTiles()));
					movesSheet.addCell(new Number(5, excelRow,
							currentState.totalManhattanDistance() - prevState.totalManhattanDistance()));
					
					/* Backtracking stuff */
					// First, check if the current state continues along the current backtracking path
					int backIndex = pivot - offset;
					if (backIndex >= 0 &&
							currentState.state.equals(pastStates.get(backIndex).state)) {
						lastBacktrack = true;
						// Record this as a backtracking move
						movesSheet.addCell(new Number(6, excelRow, 1));
						// Increment offset to reflect the next state along the path
						offset++;
						
					} else {
						// Record this as a non-backtracking move
						movesSheet.addCell(new Number(6, excelRow, 0));
			
						if (lastBacktrack) {  // If the last move was a backtracking move
							// Record the length of the last backtracking path on the row
							// with the last legal move
							movesSheet.addCell(new Number(7, excelRow - numInterIllMoves - 1, offset - 1));
							lastBacktrack = false;
							
							// Check if this move backtracks one step from the previous move
							pivot = pastStates.size() - 1;
							offset = 1;
							Puzzle backState = pastStates.get(pivot - offset);
							if (currentState.state.equals(backState.state)) {
								lastBacktrack = true;
								// Record this as a backtracking move
								movesSheet.addCell(new Number(6, excelRow, 1));
								// Increment offset to reflect the next state along the path
								offset++;
							} else
								// If not, make the current state the pivot for the next move
								pivot++;
						} else
							// If neither the current nor the last move was a backtracking
							// move, simply move the pivot up--it will be the current state
							pivot++;
					}
					
					pastStates.add(currentState);
					prevState = currentState;
					excelRow++;
					numInterIllMoves = 0;
				}
				line = solnFile.readLine();
			}
			
			// Write the average inter-move latency for the last test puzzle
			double avgInterLat;
			if (useLatency2)
				avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 2);
			else
				avgInterLat = ((double) puzzleInterLatSum) / (moveNum - 1);
			puzzlesSheet.addCell(new Number(interLatCol, puzzleRow, avgInterLat));
			
			/* Write summary statistics */
			// Training puzzles
			movesSheet.addCell(new Label(10, 5, "TRAINING"));
			movesSheet.addCell(new Label(10, 6, "Avg. initial latency (ms)"));
			movesSheet.addCell(new Number(11, 6, ((double) trainInitLatSum) / trainNumInitMoves));
			
			movesSheet.addCell(new Label(10, 7, "Avg. inter-move latency (ms)"));
			movesSheet.addCell(new Number(11, 7, ((double) trainInterLatSum) / trainNumLaterMoves));
			
			movesSheet.addCell(new Label(10, 8, "% of illegal moves"));
			movesSheet.addCell(new Formula(11, 8, "(1-SUM(B1:B" + lastTrainRow + 
					")/COUNT(B1:B" + lastTrainRow + "))*100"));
			
			movesSheet.addCell(new Label(10, 9, "% of legal moves dec. dist"));
			movesSheet.addCell(new Formula(11, 9, "COUNTIF(D1:D" + lastTrainRow +
					",\"<0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 10, "% of legal moves inc. dist"));
			movesSheet.addCell(new Formula(11, 10, "COUNTIF(D1:D" + lastTrainRow +
					",\">0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 11, "% of legal moves dec. misp"));
			movesSheet.addCell(new Formula(11, 11, "COUNTIF(E1:E" + lastTrainRow + 
					",\"<0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 12, "% of legal moves inc. misp"));
			movesSheet.addCell(new Formula(11, 12, "COUNTIF(E1:E" + lastTrainRow +
					",\">0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 13, "% of legal moves dec. Manh"));
			movesSheet.addCell(new Formula(11, 13, "COUNTIF(F1:F" + lastTrainRow +
					",\"<0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 14, "% of legal moves inc. Manh"));
			movesSheet.addCell(new Formula(11, 14, "COUNTIF(F1:F" + lastTrainRow +
					",\">0\")/COUNTIF(B1:B" + lastTrainRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 15, "Num. of backtrack sequences"));
			movesSheet.addCell(new Formula(11, 15, "COUNT(H1:H" + lastTrainRow + ")"));
			
			movesSheet.addCell(new Label(10, 16, "% of backtracking moves"));
			movesSheet.addCell(new Formula(11, 16, "SUM(G1:G" + lastTrainRow +
					")/COUNT(G1:G" + lastTrainRow + ")*100"));
			
			movesSheet.addCell(new Label(10, 17, "Avg. backtrack length"));
			movesSheet.addCell(new Formula(11, 17, "AVERAGE(H1:H" + lastTrainRow + ")"));
			
			movesSheet.addCell(new Label(10, 18, "Max backtrack length"));
			movesSheet.addCell(new Formula(11, 18, "MAX(H1:H" + lastTrainRow + ")"));
			
			
			// Test puzzles
			movesSheet.addCell(new Label(10, 20, "TEST"));
			movesSheet.addCell(new Label(10, 21, "Avg. initial latency (ms)"));
			movesSheet.addCell(new Number(11, 21, 
					((double) (initLatSum - trainInitLatSum)) / (numInitMoves - trainNumInitMoves)));
			
			movesSheet.addCell(new Label(10, 22, "Avg. inter-move latency (ms)"));
			movesSheet.addCell(new Number(11, 22, 
					((double) (interLatSum - trainInterLatSum)) / (numLaterMoves - trainNumLaterMoves)));
			
			movesSheet.addCell(new Label(10, 23, "% of illegal moves"));
			movesSheet.addCell(new Formula(11, 23, "(1-SUM(B" + (lastTrainRow + 1) +
					":B" + excelRow + ")/COUNT(B" + (lastTrainRow + 1) + ":B" + excelRow + "))*100"));
			
			movesSheet.addCell(new Label(10, 24, "% of legal moves dec. dist"));
			movesSheet.addCell(new Formula(11, 24, "COUNTIF(D" + (lastTrainRow + 1) +
					":D" + excelRow + ",\"<0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 25, "% of legal moves inc. dist"));
			movesSheet.addCell(new Formula(11, 25, "COUNTIF(D" + (lastTrainRow + 1) +
					":D" + excelRow + ",\">0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 26, "% of legal moves dec. misp"));
			movesSheet.addCell(new Formula(11, 26, "COUNTIF(E" + (lastTrainRow + 1) +
					":E" + excelRow + ",\"<0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 27, "% of legal moves inc. misp"));
			movesSheet.addCell(new Formula(11, 27, "COUNTIF(E" + (lastTrainRow + 1) +
					":E" + excelRow + ",\">0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 28, "% of legal moves dec. Manh"));
			movesSheet.addCell(new Formula(11, 28, "COUNTIF(F" + (lastTrainRow + 1) +
					":F" + excelRow + ",\"<0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 29, "% of legal moves inc. Manh"));
			movesSheet.addCell(new Formula(11, 29, "COUNTIF(F" + (lastTrainRow + 1) +
					":F" + excelRow + ",\">0\")/COUNTIF(B" + (lastTrainRow + 1) + ":B" + excelRow + ",1)*100"));
			
			movesSheet.addCell(new Label(10, 30, "Num. of backtrack sequences"));
			movesSheet.addCell(new Formula(11, 30, "COUNT(H" + (lastTrainRow + 1) + ":H" + excelRow + ")"));
			
			movesSheet.addCell(new Label(10, 31, "% of backtracking moves"));
			movesSheet.addCell(new Formula(11, 31, "SUM(G" + (lastTrainRow + 1) +
					":G" + excelRow + ")/COUNT(G" + (lastTrainRow + 1) + ":G" + excelRow + ")*100"));
			
			movesSheet.addCell(new Label(10, 32, "Avg. backtrack length"));
			movesSheet.addCell(new Formula(11, 32, "AVERAGE(H" + (lastTrainRow + 1) + ":H" + excelRow + ")"));
			
			movesSheet.addCell(new Label(10, 33, "Max backtrack length"));
			movesSheet.addCell(new Formula(11, 33, "MAX(H" + (lastTrainRow + 1) + ":H" + excelRow + ")"));
			
			
			// Training and test puzzles combined
			movesSheet.addCell(new Label(10, 35, "OVERALL"));
			movesSheet.addCell(new Label(10, 36, "Avg. initial latency (ms)"));
			movesSheet.addCell(new Number(11, 36, ((double) initLatSum) / numInitMoves));
			
			movesSheet.addCell(new Label(10, 37, "Avg. inter-move latency (ms)"));
			movesSheet.addCell(new Number(11, 37, ((double) interLatSum) / numLaterMoves));
			
			movesSheet.addCell(new Label(10, 38, "Avg. solved init latency"));
			movesSheet.addCell(new Number(11, 38, ((double) solvedInitLatSum) / solvedNumInitMoves));
			
			movesSheet.addCell(new Label(10, 39, "Avg. solved inter latency"));
			movesSheet.addCell(new Number(11, 39, ((double) solvedInterLatSum) / solvedNumLaterMoves));
			
			movesSheet.addCell(new Label(10, 40, "Avg. unsolved init latency"));
			movesSheet.addCell(new Number(11, 40, 
					((double) (initLatSum - solvedInitLatSum)) / (numInitMoves - solvedNumInitMoves)));
			
			movesSheet.addCell(new Label(10, 41, "Avg. unsolved inter latency"));
			movesSheet.addCell(new Number(11, 41, 
					((double) (interLatSum - solvedInterLatSum)) / (numLaterMoves - solvedNumLaterMoves)));
			
			movesSheet.addCell(new Label(10, 42, "% of illegal moves"));
			movesSheet.addCell(new Formula(11, 42, "(1-SUM(B:B)/COUNT(B:B))*100"));
			
			movesSheet.addCell(new Label(10, 43, "% of legal moves dec. dist"));
			movesSheet.addCell(new Formula(11, 43, "COUNTIF(D:D,\"<0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 44, "% of legal moves inc. dist"));
			movesSheet.addCell(new Formula(11, 44, "COUNTIF(D:D,\">0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 45, "% of legal moves dec. misp"));
			movesSheet.addCell(new Formula(11, 45, "COUNTIF(E:E,\"<0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 46, "% of legal moves inc. misp"));
			movesSheet.addCell(new Formula(11, 46, "COUNTIF(E:E,\">0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 47, "% of legal moves dec. Manh"));
			movesSheet.addCell(new Formula(11, 47, "COUNTIF(F:F,\"<0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 48, "% of legal moves inc. Manh"));
			movesSheet.addCell(new Formula(11, 48, "COUNTIF(F:F,\">0\")/COUNTIF(B:B,1)*100"));
			
			movesSheet.addCell(new Label(10, 49, "Num. of backtrack sequences"));
			movesSheet.addCell(new Formula(11, 49, "COUNT(H:H)"));
			
			movesSheet.addCell(new Label(10, 50, "% of backtracking moves"));
			movesSheet.addCell(new Formula(11, 50, "SUM(G:G)/COUNT(G:G)*100"));
			
			movesSheet.addCell(new Label(10, 51, "Avg. backtrack length"));
			movesSheet.addCell(new Formula(11, 51, "AVERAGE(H:H)"));
			
			movesSheet.addCell(new Label(10, 52, "Max backtrack length"));
			movesSheet.addCell(new Formula(11, 52, "MAX(H:H)"));
			
			
			
		} catch (IOException e) {
			
		} catch (WriteException e) {
			System.err.println("Error writing to Excel workbook");
			System.exit(0);
		} finally {
			try {
				solnFile.close();
				excelFile.write();
				excelFile.close();
			} catch (IOException e) {
				
			} catch (WriteException e) {}
		}
		
	}
}
