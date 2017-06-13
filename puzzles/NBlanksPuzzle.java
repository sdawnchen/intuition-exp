package puzzles;

import java.util.*;

public class NBlanksPuzzle extends NSwappableTilesPuzzle {
	
	public NBlanksPuzzle(int numRows, int numCols, int minMoves,
			int numSwappableTiles) {
		super(numRows, numCols, minMoves, numSwappableTiles);
	}
	
	public NBlanksPuzzle(int numRows, int numCols, int minMoves,
			int numSwappableTiles, int[][] initState) {
		super(numRows, numCols, minMoves, numSwappableTiles, initState);
	}

	protected void setGoal() {
		super.setGoal();
		// Set all the blank tiles in the goal state to 0
		int firstBlankTile = numRows * numCols - numSwappableTiles + 1;
		for (int tile = numRows * numCols; tile >= firstBlankTile; tile--) {
			int row = (tile - 1) / numCols;
			int col = (tile - 1) % numCols;
			goal.setTile(row, col, BLANK_TILE);
		}
	}
	
	protected boolean canSwapTiles(int row1, int col1, int row2, int col2) {
		// Cannot swap two empty tiles
		if (isEmpty(row1, col1) && isEmpty(row2, col2))
			return false;
		return super.canSwapTiles(row1, col1, row2, col2);
	}
	
	public boolean isSwappableTile(int row, int col) {
		return isEmpty(row, col);
	}
	
	public Vector<PuzzleState> possibleNextStates(PuzzleState state) {
		Vector<PuzzleState> nextStates = super.possibleNextStates(state);
		
		// Remove the states that are equal to the current state (since this class
		// doesn't distinguish between different swappable tiles)
		for (int i = 0; i < nextStates.size(); i++) {
			if (state.equals(nextStates.get(i))) {
				nextStates.remove(i);
				i--;
			}
		}
		return nextStates;
	}
}
