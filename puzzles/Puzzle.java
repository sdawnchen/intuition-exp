package puzzles;

import java.util.*;

import util.PuzzleFileParser;

public abstract class Puzzle {
	public static final int BLANK_TILE = 0;
	public int numRows, numCols, minMoves;
	public PuzzleState state, goal;
	// The solution saved when a new random puzzle is generated
	public Vector<PuzzleState> solution;
	protected Random rand = new Random();
	
	protected abstract void setGoal();
	public abstract boolean tryToMove(int row1, int col1, int row2, int col2);
	public abstract Vector<PuzzleState> possibleNextStates(PuzzleState state);
	public abstract int heuristic(PuzzleState s);
	public abstract int numMisplacedTiles();
	public abstract int totalManhattanDistance();
	public abstract boolean isSolved();
	public abstract boolean isGoal(PuzzleState state);
	
	public Puzzle(int numRows, int numCols, int minMoves) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.minMoves = minMoves;
	}
	
	public int getTile(int row, int col) {
		return state.getTile(row, col);
	}
	
	/**
	 * Swaps the tiles at row1, col1 and row2, col2 in the current state.
	 */
	protected void swapTilesInPlace(int row1, int col1, int row2, int col2) {
		int temp = getTile(row1, col1);
		state.setTile(row1, col1, getTile(row2, col2));
		state.setTile(row2, col2, temp);
	}
	
	/**
	 * Returns a new PuzzleState with the tiles at row1, col1 and row2, col2
	 * in the given state swapped.
	 */
	protected PuzzleState swapTilesNewState(PuzzleState state, int row1, int col1, int row2, int col2) {
		PuzzleState newState = state.copy();
		int temp = newState.getTile(row1, col1);
		newState.setTile(row1, col1, newState.getTile(row2, col2));
		newState.setTile(row2, col2, temp);
		return newState;
	}
	
	public boolean isValid(int row, int col) {
		return row >= 0 && row < numRows && col >= 0 && col < numCols;
	}
	
	public boolean isEmpty(int row, int col) {
		return state.isEmpty(row, col);
	}
	
	public boolean isSwappableTile(int row, int col) {
		return state.isSwappableTile(row, col);
	}
	
	/**
	 * Precondition:  The goal state has been created.
	 * Postcondition: A new random puzzle (initial state) will be created.
	 */
	public void newRandomPuzzle() {
		// Start with the goal state
		state = goal.copy();
		HashSet<PuzzleState> visitedStates = new HashSet<PuzzleState>();
		visitedStates.add(state.copy());
		System.out.println(state);
		
		Vector<PuzzleState> aStarSolution = null;
		int numMovesLeft = this.minMoves;
		while (numMovesLeft > 0) {
			for (int move = 0; move < numMovesLeft; move++) {
				// Get all the possible next states
				Vector<PuzzleState> nextStates = possibleNextStates(state);
				
				// Randomly pick a new state until it is not one we have seen before
				PuzzleState next;
				do {
					next = nextStates.get(rand.nextInt(nextStates.size()));
				} while (visitedStates.contains(next));
				
				// Update the state and add it to the set of visited states
				state = next;
				visitedStates.add(state.copy());
				System.out.println("New state:");
				System.out.println(state);
			}
			aStarSolution = aStarSearch();
			int minMovesAStar = aStarSolution.size();
			numMovesLeft = this.minMoves - minMovesAStar;
			System.out.println("num moves left: " + numMovesLeft);
		}
		solution = aStarSolution;
	} 
	
	/**
	 * Uses A* search to return a solution that contains the goal state and not the
	 * initial state.
	 */
	public Vector<PuzzleState> aStarSearch() {
		System.out.println("Starting A*");
		long startTime = (new Date()).getTime();
		Vector<PuzzleState> solution = new Vector<PuzzleState>();
		HashSet<PuzzleState> closedList = new HashSet<PuzzleState>();
		PriorityQueue<PuzzleState> openList = new PriorityQueue<PuzzleState>();
		
		PuzzleState searchState = state.copy();
		searchState.prevState = null;
		searchState.depth = 0;
		openList.add(searchState);

		while (!openList.isEmpty() && !isGoal(searchState)) {
			searchState = openList.poll();
			if (!closedList.contains(searchState)) {
				closedList.add(searchState);
				Vector<PuzzleState> nextStates = possibleNextStates(searchState);
				for (PuzzleState s : nextStates)
					s.depth = searchState.depth + 1;
				openList.addAll(nextStates);
			}
		}
		
		// If the goal has been reached, construct the solution
		if (isGoal(searchState)) {
			for (PuzzleState s = searchState; s.prevState != null; s = s.prevState) {
				solution.add(s);
			}
			Collections.reverse(solution);
		}
		
		long endTime = (new Date()).getTime();
		System.out.println("A* search ended after " + (endTime - startTime) + " ms");
		//for (PuzzleState s : solution)
		//	System.out.println(s);
		return solution;
	}
	
	/*public Vector<PuzzleState> breadthFirstSearch() {
		System.out.println("Starting bfs");
		Vector<PuzzleState> solution = new Vector<PuzzleState>();
		HashSet<PuzzleState> visitedStates = new HashSet<PuzzleState>();
		LinkedList<PuzzleState> queue = new LinkedList<PuzzleState>();
		
		PuzzleState oldState = state;
		state.prevState.prevState = null;  
		queue.add(state);
		
		while (!queue.isEmpty() && !isSolved()) {
			PuzzleState newState = queue.removeFirst();
			if (!visitedStates.contains(newState)) {
				state = newState;
				visitedStates.add(state);
				Vector<PuzzleState> nextStates = possibleNextStates();
				queue.addAll(nextStates);
			}
		}
		
		if (isSolved()) System.out.println("solved");
		System.out.println("end of bfs");
		System.out.println(state);
		
		for (PuzzleState s = state; s.prevState != null; s = s.prevState) {
			solution.add(s);
		}
		Collections.reverse(solution);
		state = oldState;
		
		System.out.println("bfs solution:");
		for (PuzzleState s : solution)
			System.out.println(s);
		return solution;
	}*/
	
	public int manhattanDistance(int row1, int col1, int row2, int col2) {
		return Math.abs(row2 - row1) + Math.abs(col2 - col1);
	}
	
	public class PuzzleState implements Comparable<PuzzleState> {
		private int tiles[][];
		PuzzleState prevState;
		private int depth, heuristic = -1;
		public int rows, cols;
		
		public PuzzleState() {
			tiles = new int[numRows][numCols];
			rows = numRows;
			cols = numCols;
		}
		
		public PuzzleState(int[][] tiles) {
			this.tiles = tiles;
			rows = tiles.length;
			cols = tiles[0].length;
		}
		
		public int hashCode() {
			int hash = 0;
			int i = 0;
			for (int row = 0; row < numRows; row++) {
				for (int col = 0; col < numCols; col++) {
					hash += i * tiles[row][col];
					i++;
				}
			}
			return hash;
		}
		
		// Used by HashSet
		public boolean equals(Object o) {
			PuzzleState otherState = (PuzzleState) o;
			return Arrays.deepEquals(tiles, otherState.tiles);
		}
		
		// Used by PriorityQueue
		public int compareTo(PuzzleState otherState) {
			// Compare first based on the f-cost, then on the heuristic
			int myHeuristic = this.heuristic();
			int otherHeuristic = otherState.heuristic();
			int myCost = depth + myHeuristic;
			int otherCost = otherState.depth + otherHeuristic;
			if (myCost < otherCost)
				return -1;
			else if (myCost == otherCost) {
				if (myHeuristic < otherHeuristic)
					return -1;
				else if (myHeuristic == otherHeuristic)
					return 0;
				else
					return 1;
			}
			else
				return 1;
		}
		
		public int heuristic() {
			// If no heuristic value has been stored, call the enclosing
			// Puzzle's heuristic function
			if (heuristic == -1)
				heuristic = Puzzle.this.heuristic(this);
			return heuristic;
		}
		
		public String toString() {
			String output = "";
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < numCols; col++)
					output += "\t" + tiles[row][col];
				output += '\n';
			}
		   return output;
		}
		
		public PuzzleState copy() {
			PuzzleState copy = new PuzzleState();
			for (int row = 0; row < rows; row++)
				for (int col = 0; col < numCols; col++)
					copy.setTile(row, col, this.getTile(row, col));
			
			return copy;
		}
		
		public int getTile(int row, int col) {
			return tiles[row][col];
		}
		
		protected void setTile(int row, int col, int tile) {
			tiles[row][col] = tile;
		}
		
		protected int[] findTile(int tile) {
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < numCols; col++) {
					if (getTile(row, col) == tile)
						return new int[] {row, col};
				}
			}
			return new int[] {-1, -1};
		}
		
		public boolean isEmpty(int row, int col) {
			return getTile(row, col) == BLANK_TILE;
		}
		
		public boolean isSwappableTile(int row, int col) {
			return getTile(row, col) <= 0;
		}
	}
	
	public static void main(String args[]) {
		Vector<Puzzle> puzzles = PuzzleFileParser.readPuzzles("puzzlefiles/puzzles_relaxed.txt");
		long startTime = (new Date()).getTime();
		for (Puzzle p : puzzles) {
			p.aStarSearch();
		}
		System.out.println("Total time: " + ((new Date()).getTime() - startTime) +
				" ms.");
	}
}
