package puzzles;

import java.util.Vector;

public class NSwappableTilesPuzzle extends Puzzle {
	public int numSwappableTiles;   // includes the blank tile
	protected enum Direction {NORTH, EAST, SOUTH, WEST};
	
	public NSwappableTilesPuzzle(int numRows, int numCols, int minMoves, int numSwappableTiles) {
		super(numRows, numCols, minMoves);
		this.numSwappableTiles = numSwappableTiles;
		setGoal();
	}
	
	public NSwappableTilesPuzzle(int numRows, int numCols, int minMoves, int numSwappableTiles,
			int[][] initState) {
		super(numRows, numCols, minMoves);
		this.numSwappableTiles = numSwappableTiles;
		setGoal();
		state = new PuzzleState(initState);
	}
	
	public NSwappableTilesPuzzle(int numRows, int numCols, int minMoves, int numSwappableTiles,
			PuzzleState initState) {
		super(numRows, numCols, minMoves);
		this.numSwappableTiles = numSwappableTiles;
		setGoal();
		state = initState;
	}
	
	protected void setGoal() {
		// Create the goal state
		goal = new PuzzleState();
		int firstSwappableTile = numRows * numCols - numSwappableTiles + 1;
		
		// Number the non-swappable tiles 1, 2, ... and the swappable tiles, e.g.,
		// ..., -7, -8, etc.
		int tile = 1;
		int increment = 1;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				if (tile == firstSwappableTile) {
					tile = -tile;
					increment = -1;
				}
				goal.setTile(row, col, tile);
				tile += increment;
			}
		}
		// Number the last tile 0 (empty)
		goal.setTile(numRows - 1, numCols - 1, BLANK_TILE);
	}
	
	@Override
	public boolean isSolved() {
		return isGoal(state);
	}
	
	public boolean isGoal(PuzzleState s) {
		return s.equals(goal);
	}
	
	public boolean tryToMove(int row1, int col1, int row2, int col2) {
		// If there is only one empty tile, the user must click on a non-empty tile
		// adjacent to an empty space to move it there
		/*if (numSwappableTiles == 1) {
			if (row1 == row2 && col1 == col2)
				return slideTile(row1, col1);
			else
				return false;
		}
		
		// Otherwise, one tile must be dragged onto another, and the two tiles must
		// be exchangeable
		else*/ return canSwapTiles(row1, col1, row2, col2);
	}

	protected boolean slideTile(int row, int col) {
		for (Direction dir : Direction.values()) {
			int[] neighbor = directionToNeighbor(row, col, dir);
			int newRow = neighbor[0];
			int newCol = neighbor[1];
			if (isValid(newRow, newCol) && isSwappableTile(newRow, newCol)) {
				swapTilesInPlace(row, col, newRow, newCol);
				return true;
			}
		}
		return false;
	}
	
	protected boolean canSwapTiles(int row1, int col1, int row2, int col2) {
		// If one of the tiles is swappable, swap the two tiles if they're neighbors
		if (isSwappableTile(row1, col1) || isSwappableTile(row2, col2)) {
			for (Direction dir : Direction.values()) {
				int[] neighbor = directionToNeighbor(row1, col1, dir);
				if (row2 == neighbor[0] && col2 == neighbor[1]) {
					swapTilesInPlace(row1, col1, row2, col2);
					return true;
				}
			}
		}
		return false;
	}
	
	public Vector<PuzzleState> possibleNextStates(PuzzleState state) {
		Vector<PuzzleState> nextStates = new Vector<PuzzleState>();
		
		// Go through all tiles
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				// If this tile is swappable, try to swap it with its neighbors
				if (state.isSwappableTile(row, col)) {
					// Go through all neighbors
					for (Direction dir : Direction.values()) {
						int[] neighbor = directionToNeighbor(row, col, dir);
						int newRow = neighbor[0];
						int newCol = neighbor[1];
						
						// If the neighbor is a valid tile, make a new state with the tile and
						// its neighbor swapped and add it to the list of next states
						if (isValid(newRow, newCol)) {
							PuzzleState newState = swapTilesNewState(state, row, col, newRow, newCol);
							newState.prevState = state;
							nextStates.add(newState);
						}
					}
				}
			}
		}
		return nextStates;
	}
	
	public int heuristic(PuzzleState s) {
		// This heuristic is the sum of the Manhattan distances of all non-swappable tiles
		// to their goal locations
		int dist = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				if (!s.isSwappableTile(row, col)) {
					int tile = s.getTile(row, col);
					int[] goalLoc = goal.findTile(tile);
					dist += manhattanDistance(row, col, goalLoc[0], goalLoc[1]);
				}
			}
		}
		return dist;
	}
	
	public int numMisplacedTiles() {
		int num = 0;
		for (int row = 0; row < numRows; row++)
			for (int col = 0; col < numCols; col++) {
				int tile = getTile(row, col);
				if (tile != BLANK_TILE && tile != goal.getTile(row, col))
					num++;
			}
		return num;
	}
	
	public int totalManhattanDistance() {
		int dist = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				int tile = getTile(row, col);
				if (tile != BLANK_TILE) {
					int[] goalLoc = goal.findTile(tile);
					dist += manhattanDistance(row, col, goalLoc[0], goalLoc[1]);
				}
			}
		}
		return dist;
	}

	protected int[] directionToNeighbor(int row, int col, Direction dir) {
		int[] neighbor = new int[2];
		switch (dir) {
		case NORTH:
			neighbor[0] = row - 1;
			neighbor[1] = col;
			break;
		case EAST:
			neighbor[0] = row;
			neighbor[1] = col + 1;
			break;
		case SOUTH:
			neighbor[0] = row + 1;
			neighbor[1] = col;
			break;
		case WEST:
			neighbor[0] = row;
			neighbor[1] = col - 1;
			break;
		}
		return neighbor;
	}
}
