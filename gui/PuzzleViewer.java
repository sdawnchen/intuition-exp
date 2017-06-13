package gui;

import puzzles.*;
import puzzles.Puzzle.PuzzleState;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public class PuzzleViewer extends JPanel {
    final int TILE_SIZE = 80; //70;
    Puzzle puzzle;
    PuzzleState puzzleState;
    
    public PuzzleViewer(Puzzle puzzle) {
        this.puzzle = puzzle;
        Dimension size = new Dimension(TILE_SIZE * puzzle.numCols + 1, 
                TILE_SIZE * puzzle.numRows + 1);
        setPreferredSize(size);
        //setMaximumSize(size);
        setBackground(Color.white);
    }
    
    public PuzzleViewer(PuzzleState ps) {
        puzzleState = ps;
        Dimension size = new Dimension(TILE_SIZE * puzzleState.cols + 1, 
                TILE_SIZE * puzzleState.rows + 1);
        setPreferredSize(size);
        //setMaximumSize(size);
        setBackground(Color.white);
    }
    
    public void setPuzzle(Puzzle newPuzzle) {
        puzzle = newPuzzle;
        Dimension size = new Dimension(TILE_SIZE * puzzle.numCols + 1, 
                TILE_SIZE * puzzle.numRows + 1);
        setPreferredSize(size);
        //setMaximumSize(size);
        revalidate();
        repaint();
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        
        if (puzzle != null)
            puzzleState = puzzle.state;
        
        // Make text look smooth
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        for (int row = 0; row < puzzleState.rows; row++) {
            for (int col = 0; col < puzzleState.cols; col++) {
                if (!puzzleState.isEmpty(row, col)) {
                    // Find the tile's coordinates
                    int x = col * TILE_SIZE;
                    int y = row * TILE_SIZE;
                    
                    // Draw the outline of the tile
                    //g.setColor(new Color(175, 195, 240));
                    g.setColor(new Color(185, 192, 255));
                    g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                    
                    // Fill in the tile with a lighter color if it is swappable
                    if (puzzleState.isSwappableTile(row, col))
                        //g.setColor(new Color(227, 240, 255));
                        g.setColor(new Color(233, 235, 255));
                    else
                        //g.setColor(new Color(205, 223, 250));  
                        g.setColor(new Color(222, 225, 255));
                    g.fillRect(x + 1, y + 1, TILE_SIZE - 1, TILE_SIZE - 1);
                    
                    // Determine the tile's label, which is the absolute value of the
                    // tile's number
                    int tile = puzzleState.getTile(row, col);
                    String label = Integer.toString(Math.abs(tile));
                    /*if (tile > 0)
                        label = Integer.toString(tile);
                    else
                        label = Character.toString((char) ('A' - tile - 1));*/
                    
                    // Compute the bounding box for the label
                    Font font = new Font("SansSerif", Font.PLAIN, (int)(TILE_SIZE/2.5));
                    FontRenderContext frc = ((Graphics2D) g).getFontRenderContext();
                    TextLayout layout = new TextLayout(label, font, frc);
                    Rectangle2D bounds = layout.getBounds();
                    
                    // Compute the position at which the label should be drawn
                    int centerX = x + (TILE_SIZE - 1)/2;
                    int centerY = y + (TILE_SIZE - 1)/2;
                    int labelPosX = centerX - (int) bounds.getWidth()/2;
                    int labelPosY = centerY + (int) bounds.getHeight()/2;
                    
                    // Draw the tile's label, using a lighter color if it is swappable
                    if (puzzleState.isSwappableTile(row, col))
                        g.setColor(new Color(145, 145, 145));
                    else
                        //g.setColor(new Color(100, 100, 100));
                        g.setColor(new Color(120, 120, 120));
                    g.setFont(font);
                    g.drawString(label,
                            labelPosX, labelPosY);
                            //x + (int)(TILE_SIZE/2.5), y + (int)(TILE_SIZE/1.5));
                }
            }
        }
    }
}
