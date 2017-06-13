package gui;

import puzzles.*;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Vector;

public class PuzzleTester {
	private static Puzzle puzzle;
	public static long startTime;

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame window = new JFrame("");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contentPane = new JPanel(new BorderLayout());
        Vector<Puzzle> puzzles = new Vector<Puzzle>();
        contentPane.add(new PuzzleViewer(puzzle), BorderLayout.CENTER);
        window.add(contentPane);

        //Display the window.
        window.pack();
        //window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    public static void main(String[] args) {
    	puzzle = new NSwappableTilesPuzzle(3, 3, 3, 3);
    	puzzle.newRandomPuzzle();
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
