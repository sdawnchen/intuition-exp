package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;

import puzzles.Puzzle;

public class PuzzleChooser extends JPanel implements MouseListener {
    Puzzle puzzle;
    private Experiment exp;
    PuzzleViewer puzzleViewer;
    Border border, emptyBorder;
    static final int BORDER_WIDTH = 5;
    int id;
    boolean enabled = false;
    
    public PuzzleChooser(Puzzle puzzle, Experiment exp, int id) {
        super(new BorderLayout());
        this.puzzle = puzzle;
        this.exp = exp;
        this.id = id;
        
        puzzleViewer = new PuzzleViewer(puzzle);
        add(puzzleViewer, BorderLayout.CENTER);
        border = BorderFactory.createLineBorder(Color.LIGHT_GRAY, BORDER_WIDTH);
        emptyBorder = BorderFactory.createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH,
                BORDER_WIDTH, BORDER_WIDTH);
        setBorder(emptyBorder);
        
        addMouseListener(this);
    }
    
    public void setPuzzle(Puzzle newPuzzle) {
        puzzle = newPuzzle;
        puzzleViewer.setPuzzle(newPuzzle);
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {
        setBorder(border);
    }

    public void mouseExited(MouseEvent e) {
        setBorder(emptyBorder);
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && enabled)
            exp.nextComparison(id);
    }

}
