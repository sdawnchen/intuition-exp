package puzzles;

import puzzles.Puzzle.PuzzleState;

public class NGoalsPuzzle extends NSwappableTilesPuzzle {
    public int numGoals;

    public NGoalsPuzzle(int numRows, int numCols, int minMoves, int numGoals) {
        super(numRows, numCols, minMoves, 1);
        this.numGoals = numGoals;
    }
    
    public NGoalsPuzzle(int numRows, int numCols, int minMoves, int numGoals,
            int[][] initState) {
        super(numRows, numCols, minMoves, 1, initState);
        this.numGoals = numGoals;
    }

    public int heuristic(PuzzleState s) {
        // This heuristic is the sum of the Manhattan distances of all the tiles we care
        // about to their goal locations
        int dist = 0;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                int tile = s.getTile(row, col);
                if (tile != BLANK_TILE && tile <= numGoals) {
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
                if (tile != BLANK_TILE && tile <= numGoals &&
                        tile != goal.getTile(row, col))
                    num++;
            }
        return num;
    }
    
    public int totalManhattanDistance() {
        return heuristic(state);
    }
    
    public boolean isGoal(PuzzleState s) {
        // Check if all the tiles we care about are in their proper places
        for (int i = 0; i < numGoals; i++) {
            int row = i / numCols;
            int col = i % numCols;
            if (s.getTile(row, col) != goal.getTile(row, col)) {
                return false;
            }
        }
        return true;
    }
    
    public static void main(String args[]) {
        Boolean bool = null;
        System.out.println(bool);
        bool = true;
        System.out.println(bool);
    }
}
