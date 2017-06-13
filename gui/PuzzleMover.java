package gui;

import puzzles.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.swing.*;

public class PuzzleMover extends JPanel implements MouseListener {
    private Experiment exp;
    PuzzleViewer puzzleViewer;
    
    private int startRow, startCol, endRow, endCol;
    private Puzzle puzzle;
    int stepNum;
    long thisMoveStartTime, lastMoveEndTime;
    boolean enabled = true;
    
    public PuzzleMover(Experiment exp, Puzzle puzzle) {
        super(new BorderLayout());
        this.exp = exp;
        this.puzzle = puzzle;
        
        puzzleViewer = new PuzzleViewer(puzzle);
        this.add(puzzleViewer, BorderLayout.CENTER);
        //setMaximumSize(puzzleViewer.getMaximumSize());
        
        addMouseListener(this);
    }
    
    public void setPuzzle(Puzzle newPuzzle) {
        stepNum = 0;
        lastMoveEndTime = 0;
        thisMoveStartTime = 0;
        puzzle = newPuzzle;
        puzzleViewer.setPuzzle(newPuzzle);
        enabled = false;
        
        // The dimensions of the new puzzle might be different,
        // so resize and re-center the window
        exp.window.pack();
        exp.window.setLocationRelativeTo(null);
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && enabled) {
            thisMoveStartTime = (new Date()).getTime();
            startRow = e.getY() / puzzleViewer.TILE_SIZE;
            startCol = e.getX() / puzzleViewer.TILE_SIZE;
            
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            long currentTime = (new Date()).getTime(), latency;
            
            // Continue processing this move only if it was not started either
            // during the last puzzle or during the first 1s interval of this one
            if (thisMoveStartTime != 0) {
                // Calculate the inter-move latency
                if (lastMoveEndTime == 0)
                    latency = thisMoveStartTime - exp.startTime;
                else
                    latency = thisMoveStartTime - lastMoveEndTime;
                
                lastMoveEndTime = currentTime;
                endRow = e.getY() / puzzleViewer.TILE_SIZE;
                endCol = e.getX() / puzzleViewer.TILE_SIZE;
                /*System.out.println("Drag started in " + startRow + ", " + startCol);
                System.out.println("Drag ended in " + endRow + ", " + endCol);
                System.out.println();*/
                
                boolean legalMove = false;
                if (puzzle.isValid(endRow, endCol)) {
                    if (puzzle.tryToMove(startRow, startCol, endRow, endCol)) {
                        //paintImmediately(this.getVisibleRect());
                        repaint();
                        stepNum++;
                        if (exp.numMovesLabel != null) {
                            exp.numMovesLabel.setText("You have made " + stepNum + " move(s) so far.");
                            //exp.contentPane.paintImmediately(exp.numMovesLabel.getBounds());
                        }
                        legalMove = true;
                    }
                }
                
                try {
                    // If the move is legal, write the step # and the puzzle state
                    if (legalMove) {
                        exp.solutionFile.write("Step " + stepNum);
                        exp.solutionFile.write(" (" + latency + " ms)\n");
                        exp.solutionFile.write(puzzle.state.toString() + "\n");
                    
                    // Otherwise, write that it was an illegal move
                    } else {
                        exp.solutionFile.write("Illegal move from (" +
                                (startRow + 1) + ", " + (startCol + 1) + ") to (" +
                                (endRow + 1) + ", " + (endCol + 1) + ")");
                        exp.solutionFile.write(" (" + latency + " ms)\n\n");
                    }
                } catch (IOException e1) {}
            
                // If the puzzle is solved after a legal move, show the next puzzle
                if (legalMove && puzzle.isSolved())
                    exp.nextPuzzle(currentTime);
            }
        }
    }
}
