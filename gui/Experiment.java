package gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Vector;
import java.util.Date;

import javax.swing.*;

import puzzles.*;
import util.PuzzleFileParser;

/**
 * The experiment begins by asking for the subject # for the current
 * experiment.  In the training phase, the program presents the puzzles specified in
 * the puzzle file for the appropriate training condition.  In the testing phase, the
 * puzzles in the test puzzle file are presented.  Finally, pairwise comparisons between
 * puzzle states are presented.
 *   
 */

public class Experiment implements ActionListener {
	// GUI stuff
	JFrame window;
	JPanel contentPane;
	PuzzleMover puzzleMover;
	PuzzleChooser puzzleChooser1, puzzleChooser2;
	JLabel minMovesLabel, numMovesLabel, compInstLabel, compTimeLabel;
	String expertise;
	final int BORDER_WIDTH = 25;
	final int CENTER_SPACE_WIDTH = 50;
	//final Font font = new Font("SansSerif", Font.PLAIN, 14);
	final Font font = new Font("SansSerif", Font.PLAIN, 16);
	
	// Puzzles stuff
	Vector<Puzzle> puzzles, testPuzzles, compPuzzles, comp15Puzzles;
	private Puzzle currentPuzzle;
    private int index;
    boolean training = true, comp8;
    String feedback, title, puzzleInst = "";
	
	// Subject stuff
	int subjectNum;
	enum Condition { CONTROL, SUBPROBLEM, RELAXED }
	Condition cond;
	BufferedWriter dataFile, solutionFile;
	
	// Timing stuff
	long startTime;
	Timer timer, countdownTimer, delayTimer;
	static final int PUZZLE_TIME_LIMIT = 90000;
	static final int PUZZLE_DELAY = 1000;
	static final int COMP8_TIME_LIMIT = 10000;
	static final int COMP15_TIME_LIMIT = 12000;
	static final int COMP_COUNTDOWN_RES = 1000;
	static final int COMP_RESPONSE_DELAY = 1000;
	int countdown;
    
    // File formatting
    private static final String headingsFormat = "%18s %18s %18s %18s %18s\n", 
    							puzzleDataFormat = "%18d %18s %18d %18d %18d\n",
    							// puzzle #, solved?, min moves, num moves, time
    							compDataFormat = "%18d %18d %18d %18d %18d\n";
    
    public Experiment() {
	    // Ask the experimenter for the subject number, waiting until it is obtained
	    getSubjectNum();
	    while (subjectNum == 0)
	    	Thread.yield();

	    // Figure out which condition the subject will be in and the appropriate training
	    // puzzles file
		switch ((subjectNum - 1) % 3) {
			case 0: cond = Condition.CONTROL; break;
			case 1: cond = Condition.SUBPROBLEM; break;
			case 2: cond = Condition.RELAXED; break;
		}
		String condition = cond.toString().toLowerCase();
		
		String dataFileName = "subjectfiles/subject" + subjectNum + ".txt";
		String solutionFileName = "subjectfiles/solution" + subjectNum + ".txt";
		String trainPuzzleFileName = "puzzlefiles/puzzles_" + condition + ".txt";
		String testPuzzleFileName = "puzzlefiles/puzzles_test.txt";
		String comp8PuzzleFileName = "puzzlefiles/puzzles_comp8.txt";
		String comp15PuzzleFileName = "puzzlefiles/puzzles_comp15.txt";
		
		// Read the training, test, and comparison puzzles from file and store them
		puzzles = PuzzleFileParser.readPuzzles(trainPuzzleFileName);
		testPuzzles = PuzzleFileParser.readPuzzles(testPuzzleFileName);
		compPuzzles = PuzzleFileParser.readPuzzles(comp8PuzzleFileName);
		comp15Puzzles = PuzzleFileParser.readPuzzles(comp15PuzzleFileName);
		index = 0;
		currentPuzzle = puzzles.get(index);
		puzzleMover = new PuzzleMover(this, currentPuzzle);
		
		// Create the data and solution files
		try {
			dataFile = new BufferedWriter(new FileWriter(dataFileName));
			dataFile.write("Subject " + subjectNum + ", " + condition + " condition\n\n");
			dataFile.write("TRAINING PUZZLES\n");
			dataFile.write(String.format(headingsFormat,
					"Puzzle #", "Solved?", "Min. moves", "Num. moves", "Time (ms)"));

			solutionFile = new BufferedWriter(new FileWriter(solutionFileName));
			solutionFile.write("Subject " + subjectNum + ", " + condition + " condition\n\n");
			solutionFile.write("TRAINING PUZZLES\n\n");
			solutionFile.write("Puzzle 0\n");
		} catch (IOException e) {}
    }
    	
	public static void main(String[] args) {
		// Set the look-and-feel to be the system's native L&F
		try {
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
	    } catch (Exception e) {}
	    
	    // Obtain the necessary information for the experiment
	    final Experiment exp = new Experiment();
		
		// Begin the experiment
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                exp.beginExperiment();
            }
        });
	}
	
	/**
	 * Prompts the user for the subject number using a dialog box.
	 */
	private void getSubjectNum() {
		// Set up the option pane, which contains the contents of the dialog box
		final JTextField textField = new JTextField(5);
		Object[] components = {"Enter subject number:", textField};
		final JOptionPane optionPane = new JOptionPane(
				components,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[] {"OK"});
		
		// Make pressing Enter register as entering a new value into the text field
		textField.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						optionPane.setValue(JOptionPane.OK_OPTION);
					}
				});

		// Set up the dialog box
		final JDialog dialog = new JDialog();
		dialog.setContentPane(optionPane);
		// End the experiment if the experimenter closes the dialog box
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.addWindowListener(new WindowAdapter() {
			    public void windowClosing(WindowEvent we) {
			        System.exit(0);
			    }
			});

		// Add a listener to handle when a value is entered into the text field
		optionPane.addPropertyChangeListener(
		    new PropertyChangeListener() {
		        public void propertyChange(PropertyChangeEvent ev) {
		            String prop = ev.getPropertyName();
		
		            if (dialog.isVisible()
		            	&& (ev.getSource() == optionPane)
	                    && (JOptionPane.VALUE_PROPERTY.equals(prop))) {

		            	String input = textField.getText();
						
						// Ignore reset
						if (optionPane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
						    return;
						}
						
						// Reset the JOptionPane's value so that a PropertyChangeEvent is
						// fired off if the user presses OK again without entering a different
						// value in the text field
						optionPane.setValue(
						        JOptionPane.UNINITIALIZED_VALUE);
						
						// Show an error message if the input is not a positive integer
						try {
							int subjNum = Integer.valueOf(input);
							if (subjNum <= 0) {
								JOptionPane.showMessageDialog(optionPane,
					                   "Please enter a positive integer.",
					                   "Invalid subject number",
						                JOptionPane.ERROR_MESSAGE);
								textField.selectAll();
								textField.requestFocusInWindow();
							} else {
								// If the subject number is valid, hide the dialog box
								dialog.setVisible(false);
								subjectNum = subjNum;
							}
						} catch (NumberFormatException ex) {
						    JOptionPane.showMessageDialog(optionPane,
						                    "Please enter a positive integer.",
						                    "Invalid subject number",
						                    JOptionPane.ERROR_MESSAGE);
						    textField.selectAll();
						    textField.requestFocusInWindow();
						}
		            }
		        }
		    });
		
		// Show the dialog box
		dialog.pack();
		dialog.setLocationRelativeTo(null);  // put it in the center
		dialog.setVisible(true);
	}
        
    private void beginExperiment() {
		// Create and set up the window
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // TODO: Temporary code to close files correctly!  Subjects should not be
        // able to close the window and end the experiment.
        /*window.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent we) {
		    	try {
		    		dataFile.close();
		    		solutionFile.close();
				} catch (IOException e) {}
		        System.exit(0);
		    }
		});*/
        
        // Figure out instructions for the first puzzle
        NSwappableTilesPuzzle firstPuzzle = (NSwappableTilesPuzzle) puzzles.firstElement();
        String goalTiles = "", swapMove = "";
		if (cond == Condition.CONTROL)
			goalTiles = "all the numbered tiles";
		else if (cond == Condition.SUBPROBLEM)
			goalTiles = "tiles 1 through " + ((NGoalsPuzzle) firstPuzzle).numGoals;
		else if (cond == Condition.RELAXED) {
			goalTiles = "all the numbered tiles";
			/*int firstSwappableTile = firstPuzzle.numRows * firstPuzzle.numCols - 
				firstPuzzle.numSwappableTiles + 1;
			int lastSwappableTile = firstPuzzle.numRows * firstPuzzle.numCols - 1;
			swapMove = " or swap each of tiles " + firstSwappableTile + " through " +
				lastSwappableTile + " with any of its adjacent tiles";*/
			
			swapMove = " or swap each of the lighter-colored tiles with any of its adjacent tiles";
		}
		
		// Create instructions label
		String expInst = "<html>In this experiment, you will solve sliding puzzles" +
						 " like the one on the lower left. &nbsp;Try to solve this puzzle" +
						 " by putting " + goalTiles + " in the order shown on the" +
						 " lower right (increasing across the rows then down the" +
						 " columns). &nbsp;You can slide any tile into the empty square" +
						 swapMove + " by dragging with the mouse.</html>";
		
		JLabel instLabel = new JLabel(expInst);
		instLabel.setFont(font);
		instLabel.setVerticalAlignment(JLabel.TOP);
		instLabel.setOpaque(true);
		
		// Adjust the size of the label according to the first puzzle's size
		Dimension oldSize = instLabel.getPreferredSize();
		int area = oldSize.width * oldSize.height;
		int newWidth = puzzleMover.getPreferredSize().width*2 + CENTER_SPACE_WIDTH;
		Dimension newSize = new Dimension(newWidth, area / newWidth + 50);
		instLabel.setPreferredSize(newSize);
		
		// Put everything in the content pane
		/*contentPane = new JPanel(new BorderLayout());
		contentPane.add(instLabel, BorderLayout.PAGE_START);
		contentPane.add(puzzleMover, BorderLayout.LINE_START);
		contentPane.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)),
        		BorderLayout.CENTER);
		contentPane.add(new PuzzleViewer(firstPuzzle.goal), BorderLayout.LINE_END);*/
		
		contentPane = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		
		// Add the instruction label at the top
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		contentPane.add(instLabel, constraints);

		// Add the puzzle on the lower left
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		contentPane.add(puzzleMover, constraints);

		// Add empty space in the lower center
		constraints.gridx = 1;
		constraints.gridy = 1;
		contentPane.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)), constraints);

		// Add the puzzle goal on the lower right
		constraints.gridx = 2;
		constraints.gridy = 1;
		contentPane.add(new PuzzleViewer(firstPuzzle.goal), constraints);
		
        contentPane.setBorder(BorderFactory.createEmptyBorder(BORDER_WIDTH, BORDER_WIDTH,
        		BORDER_WIDTH, BORDER_WIDTH));
        window.setContentPane(contentPane);

        // Display the window
        window.pack();
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        
        // Create the puzzle switching task
        ActionListener PuzzleSwitcher = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nextPuzzle((new Date()).getTime());
            }
        };
        timer = new Timer(PUZZLE_TIME_LIMIT, PuzzleSwitcher);
        
        // Create the task to enable making a move when the puzzle delay is over
        ActionListener MoveEnabler = new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		delayTimer.stop();
        		puzzleMover.enabled = true;
        	}
        };
        delayTimer = new Timer(PUZZLE_DELAY, MoveEnabler);
        // Start stopwatch
        startTime = (new Date()).getTime();
	}
    
	void nextPuzzle(long currentTime) {
		// Stop timer and stopwatch
		timer.stop();
		long timeTaken = currentTime - startTime;
		if (timeTaken > PUZZLE_TIME_LIMIT)
			timeTaken = PUZZLE_TIME_LIMIT;
		
		// Repaint before switching to next puzzle
		contentPane.paintImmediately(contentPane.getVisibleRect());
		
		// Pause for 500 ms
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		// Determine the feedback message based on whether the puzzle was solved
		String solved;
		if (currentPuzzle.isSolved()) {
			solved = "Y";
			title = "You solved it!";
			feedback = "You solved the puzzle using " +
				(puzzleMover.stepNum - currentPuzzle.minMoves) + " extra move(s).";
		} else {
			solved = "N";
			title = "Time is up!";
			feedback = "Time is up!";
		}
		
		int puzzleNum;
		if (training)	// If training, count the initial puzzle as number 0
			puzzleNum = index;
		else
			puzzleNum = index + 1;
		
		// Write the data for the current puzzle to the data file
		try {
			dataFile.write(String.format(puzzleDataFormat, 
					puzzleNum, solved, currentPuzzle.minMoves,
					puzzleMover.stepNum, timeTaken));
		} catch (IOException ex) {}
		
		index++;
		if (index < puzzles.size()) {   // there are more puzzles to solve
			// Go on to the next puzzle
			currentPuzzle = puzzles.get(index);
			try {
				solutionFile.write("\nPuzzle " + (puzzleNum + 1) + "\n");
			} catch (IOException e1) {}
			
			if (training) {
				// Determine the instructions for this puzzle based on the experimental condition
				String puzzleInst2 = "";
				if (cond == Experiment.Condition.CONTROL || cond == Experiment.Condition.RELAXED) {
					puzzleInst2 = "  Go on to the next puzzle.";
				} else if (cond == Experiment.Condition.SUBPROBLEM) {
					String goalTiles;
					int numGoals = ((NGoalsPuzzle) currentPuzzle).numGoals;
					if (numGoals < currentPuzzle.numRows * currentPuzzle.numCols - 1)
						goalTiles = "tiles 1 through " + numGoals;
					else
						goalTiles = "all the tiles";
					
					puzzleInst = "  In the next puzzle, try to slide " + goalTiles
							+ " into their correct places.";
					if (puzzles.get(index - 1).isSolved())
						puzzleInst2 = "  In the next puzzle,\ntry to slide " + goalTiles
							+ " into their correct places.";
					else
						puzzleInst2 = "  In the next puzzle, try to slide " + goalTiles
							+ " into\ntheir correct places.";
					
				} /*else if (cond == Experiment.Condition.RELAXED) {
					int numSwappableTiles = ((NSwappableTilesPuzzle) currentPuzzle).numSwappableTiles;
					int firstSwappableTile = currentPuzzle.numRows
							* currentPuzzle.numCols - numSwappableTiles + 1;
					int lastSwappableTile = currentPuzzle.numRows
							* currentPuzzle.numCols - 1;

					String swappableTiles;
					if (numSwappableTiles == 1) // only the blank tile is swappable
						swappableTiles = "no numbered tiles";
					else if (numSwappableTiles == 2)
						swappableTiles = "tile " + firstSwappableTile;
					else
						swappableTiles = "tiles " + firstSwappableTile
								+ " through " + lastSwappableTile;

					puzzleInst = "  In the\nnext puzzle, " + swappableTiles
							+ " may be swapped with neighboring tiles.";
					puzzleInst2 = "  In the next puzzle, " + swappableTiles
					+ " may be swapped\nwith neighboring tiles.";
				}*/
				
				// If the puzzle just solved was the initial one, begin training phase
				if (index == 1)
					beginTraining();

				// Otherwise, show the inter-puzzle message (feedback on current puzzle
				// + instructions for next puzzle)
				else {
					JOptionPane.showOptionDialog(
							window,
							feedback + puzzleInst2, // message
							title, // title
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.PLAIN_MESSAGE, null,
							new Object[] { "Start next puzzle" }, null);
				}
			}
			// Show the next puzzle
			minMovesLabel.setText("This puzzle can be solved in " + currentPuzzle.minMoves + " moves.");
			numMovesLabel.setText("You have made 0 move(s) so far.");
			puzzleMover.setPuzzle(currentPuzzle);
			
			// Restart the timer and stopwatch
	        timer.start();
	        startTime = (new Date()).getTime(); 
	        delayTimer.start();
		} 
		
		else if (training)   // end of training phase
			beginTest();
		else   // end of testing phase
			beginComparisons8();
	}

	private void beginTraining() {
		String trainingInst = "Well done!  Now you will be given some more puzzles to practice on, with 1 minute\n" +
							  "and 30 seconds for each.  Try to solve each in as few moves as you can.  There will be\n" +
							  "a penalty for every move you make past the minimum number of moves needed to\n" +
							  "solve the puzzle (which will be shown).  You may find it easier to plan your moves\n" +
							  "first.";
		if (cond == Experiment.Condition.SUBPROBLEM)
			trainingInst += puzzleInst;

		JOptionPane.showOptionDialog(
				window,
				trainingInst, // message
				"Well done!", // title
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null,
				new Object[] { "Begin practice" }, null);

		minMovesLabel = new JLabel("This puzzle can be solved in " + currentPuzzle.minMoves + " moves.");
		minMovesLabel.setFont(font);
		minMovesLabel.setVerticalAlignment(JLabel.TOP);
		minMovesLabel.setHorizontalAlignment(JLabel.CENTER);
		minMovesLabel.setOpaque(true);
		
		numMovesLabel = new JLabel("You have made 0 move(s) so far.");
		numMovesLabel.setFont(font);
		numMovesLabel.setVerticalAlignment(JLabel.TOP);
		numMovesLabel.setHorizontalAlignment(JLabel.CENTER);
		numMovesLabel.setOpaque(true);
		
		// Adjust the size of the label according to the first puzzle's size
		/*contentPane.setLayout(new BorderLayout());
		Dimension oldSize = movesLabel.getPreferredSize();
		int area = oldSize.width * oldSize.height;
		int newWidth = puzzleMover.getPreferredSize().width;
		Dimension newSize = new Dimension(newWidth, area / newWidth + 30);
		movesLabel.setPreferredSize(newSize);*/
		/*Dimension oldSize = movesLabel.getPreferredSize();
		Dimension newSize = new Dimension(oldSize.width, oldSize.height + 30);
		movesLabel.setPreferredSize(newSize);*/
		
		contentPane.removeAll();
		
		/*contentPane.add(movesLabel, BorderLayout.PAGE_START);
		contentPane.add(puzzleMover, BorderLayout.CENTER);*/
		
		// Add the label that indicates the number of moves for this puzzle at the top
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		//constraints.gridheight = 2;
		//constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.CENTER;
		contentPane.add(minMovesLabel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.insets = new Insets(0, 0, 30, 0);
		contentPane.add(numMovesLabel, constraints);
		
		// Add the puzzle in the center bottom
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 2;
		contentPane.add(puzzleMover, constraints);
		
		contentPane.validate();
	}
	
	private void beginTest() {
		training = false;
		String testInst;
		if (puzzles.get(index - 1).isSolved()) {
			testInst = "  That's the end of the practice\n" + 
			   "session.  Ready for a test?  Once again, you will have 1 minute and 30\n" +
			   "seconds for each puzzle, and there will be a penalty for every extra move\n" +
			   "you make.";
			
			if (cond == Experiment.Condition.SUBPROBLEM)
				testInst += "  For every puzzle in the test, try to slide all tiles into their\n" + 
							"correct places.";
			else if (cond == Experiment.Condition.RELAXED)
				testInst += "  For every puzzle in the test, no tiles may be swapped with\n" +
							"other tiles.";
		} else {
			testInst = "  That's the end of the practice session.  Ready for a test?\n" +
	  		   "Once again, you will have 1 minute and 30 seconds for each puzzle,\n" + 
	  		   "and there will be a penalty for every extra move you make.";
	
			if (cond == Experiment.Condition.SUBPROBLEM)
				testInst += "  For\nevery puzzle in the test," + 
							" try to slide all tiles into their correct places.";
			else if (cond == Experiment.Condition.RELAXED)
				testInst += "  For\nevery puzzle in the test, no tiles may" +
							" be swapped with other tiles.";
		}
		
		// Show a dialog box containing instructions for the testing phase
		JOptionPane.showOptionDialog(
				window,
				feedback + testInst,    // message
				title,				// title
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				new Object[] {"Begin test"},
				null);
		
		// Start section for test puzzles in data and solution files
		try {
			dataFile.write("\nTEST PUZZLES\n");
			dataFile.write(String.format(headingsFormat,
					"Puzzle #", "Solved?", "Min. moves", "Num. moves", "Time (ms)"));
			
			solutionFile.write("\nTEST PUZZLES\n\n");
			solutionFile.write("Puzzle 1\n");
		} catch (IOException e) {}
		
		// Set the puzzles to be the test puzzles and restart index
		puzzles = testPuzzles;
		index = 0;
		currentPuzzle = puzzles.get(index);
		
		// Show the next puzzle
		minMovesLabel.setText("This puzzle can be solved in " + currentPuzzle.minMoves + " moves.");
		numMovesLabel.setText("You have made 0 move(s) so far.");
		puzzleMover.setPuzzle(currentPuzzle);
		
		// Restart the timer and stopwatch
		timer.start();
		startTime = (new Date()).getTime();
		delayTimer.start();
	}
	
	private void beginComparisons8() {
		comp8 = true;
		String comp8Inst = "That's the end of the test.  Now you will see a series of pairs of puzzles.\n" +
						  "Click on the one in each pair that you think will take fewer moves to solve.\n" +
						  "You will have 10 seconds for each pair.  Clicking within the 1st second\n" +
						  "will not count as a response.";
		
		if (cond == Experiment.Condition.SUBPROBLEM)
			comp8Inst += "  Solving a puzzle requires putting all tiles\n" +
						 "into their correct places.";
		/*else if (cond == Experiment.Condition.RELAXED)
			compInst += "  No numbered tiles may be swapped\n" +
						"with other tiles.";*/
		
		// Show a dialog box containing instructions for the comparison phase
		JOptionPane.showOptionDialog(
				window,
				comp8Inst,    // message
				"",			 // title
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				new Object[] {"Begin"},
				null);
		
		// Start the 8-puzzle comparisons section in the data file
		try {
			dataFile.write("\n8-PUZZLE COMPARISONS\n");
			dataFile.write(String.format(headingsFormat,
					"Comp. #", "Puzzle chosen", "Puzzle 1 moves", "Puzzle 2 moves", "Time (ms)"));
		} catch (IOException e) {}
		
		// Restart the index and get the first two puzzles
		index = 0;
        puzzleChooser1 = new PuzzleChooser(compPuzzles.get(index), this, 1);
        puzzleChooser2 = new PuzzleChooser(compPuzzles.get(index + 1), this, 2);
		
        // Create the instruction label
		compInstLabel = new JLabel("<html>Which puzzle takes <b>fewer</b> moves to solve?</html>");
		compInstLabel.setFont(font);
		compInstLabel.setVerticalAlignment(JLabel.TOP);
		compInstLabel.setHorizontalAlignment(JLabel.CENTER);
		compInstLabel.setOpaque(true);
		
		// Create the time count-down label
		countdown = COMP8_TIME_LIMIT;
		compTimeLabel = new JLabel("You have " + countdown/COMP_COUNTDOWN_RES + " second(s) left.");
		compTimeLabel.setFont(font);
		compTimeLabel.setVerticalAlignment(JLabel.TOP);
		compTimeLabel.setHorizontalAlignment(JLabel.CENTER);
		compTimeLabel.setOpaque(true);
		
		// Change the sizes of the labels
		/*Dimension oldSize = compInstLabel.getPreferredSize();
		int area = oldSize.width * oldSize.height;
		int newWidth = puzzleChooser1.getPreferredSize().width*2 + CENTER_SPACE_WIDTH;
		Dimension newSize = new Dimension(newWidth, area / newWidth + 8);
		compInstLabel.setPreferredSize(newSize);
		compTimeLabel.setPreferredSize(newSize);*/
        
        // Remove everything from the window
		contentPane.removeAll();
		
		/*JPanel puzzlePanel = new JPanel();
		puzzlePanel.setLayout(new BoxLayout(puzzlePanel, BoxLayout.LINE_AXIS));
		puzzlePanel.add(puzzleChooser1);
		puzzlePanel.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)));
		puzzlePanel.add(puzzleChooser2);
		contentPane.add(compInstLabel, BorderLayout.PAGE_START);
		contentPane.add(puzzlePanel, BorderLayout.CENTER);*/
		
		/*contentPane.setLayout(new BorderLayout());
 		contentPane.add(compInstLabel, BorderLayout.PAGE_START);     
 		contentPane.add(puzzleChooser1, BorderLayout.LINE_START);
        contentPane.add(puzzleChooser2, BorderLayout.LINE_END);
        contentPane.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)),
        		BorderLayout.CENTER);*/
        
        GridBagConstraints constraints = new GridBagConstraints();
        // Add the instructions and time labels at the top
        constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.CENTER;
		contentPane.add(compInstLabel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(0, 0, 30, 0);
		contentPane.add(compTimeLabel, constraints);
		
		// Add the first puzzle state on the bottom left
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 2;
		contentPane.add(puzzleChooser1, constraints);

		// Add the second puzzle state on the bottom right
		constraints.gridx = 1;
		constraints.gridy = 2;
		constraints.insets = new Insets(0, CENTER_SPACE_WIDTH, 0, 0);
		contentPane.add(puzzleChooser2, constraints);
        
        // Show everything
        contentPane.validate();
        window.pack();
        window.setLocationRelativeTo(null);
        
        // Create the comparison switching task
        ActionListener CompSwitcher = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nextComparison(0);
            }
        };
        timer = new Timer(COMP8_TIME_LIMIT, CompSwitcher);
        
        // Create the countdown updating task
        ActionListener Countdown = new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		countdown -= COMP_COUNTDOWN_RES;
        		compTimeLabel.setText("You have " + countdown/COMP_COUNTDOWN_RES + " second(s) left.");
        	}
        };
        countdownTimer = new Timer(COMP_COUNTDOWN_RES, Countdown);
        
        // Create the task to enable choosing a puzzle state when the response delay is over
        ActionListener ResponseEnabler = new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		delayTimer.stop();
        		puzzleChooser1.enabled = true;
        		puzzleChooser2.enabled = true;
        	}
        };
        delayTimer = new Timer(COMP_RESPONSE_DELAY, ResponseEnabler);
        
        // Start timer, stopwatch, and countdown
        timer.start();
        startTime = (new Date()).getTime();
        countdownTimer.start();
        delayTimer.start();
	}
	
	private void beginComparisons15() {
		comp8 = false;
		String comp15Inst = "You will now have 12 seconds for each pair.";

		// Show a dialog box containing instructions for the comparison phase
		JOptionPane.showMessageDialog(
				window,
				comp15Inst,    // message
				"",			   // title
				JOptionPane.PLAIN_MESSAGE);
		
		// Start the 15-puzzle comparisons section in the data file
		try {
			dataFile.write("\n15-PUZZLE COMPARISONS\n");
			dataFile.write(String.format(headingsFormat,
					"Comp. #", "Puzzle chosen", "Puzzle 1 moves", "Puzzle 2 moves", "Time (ms)"));
		} catch (IOException e) {}
		
		countdown = COMP15_TIME_LIMIT;
        compTimeLabel.setText("You have " + countdown/COMP_COUNTDOWN_RES + " second(s) left.");
		compPuzzles = comp15Puzzles;
		index = 0;
		puzzleChooser1.setPuzzle(compPuzzles.get(index));
		puzzleChooser2.setPuzzle(compPuzzles.get(index + 1));
		window.pack();
        window.setLocationRelativeTo(null);
		
		// Create the comparison switching task and start timer, stopwatch and countdown timer
        ActionListener CompSwitcher = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nextComparison(0);
            }
        };
        timer = new Timer(COMP15_TIME_LIMIT, CompSwitcher);
        
        timer.start();
        startTime = (new Date()).getTime();
        countdownTimer.start();
        delayTimer.start();
        System.out.println("Puzzle began at " + startTime);
	}
	
	void nextComparison(int pChooserID) {
		// Stop stopwatch, timer, and countdown
		long timeTaken = (new Date()).getTime() - startTime;
		timer.stop();
		countdownTimer.stop();
		
		puzzleChooser1.enabled = false;
		puzzleChooser2.enabled = false;
		
		if (comp8 && timeTaken > COMP8_TIME_LIMIT)
			timeTaken = COMP8_TIME_LIMIT;
		else if (!comp8 && timeTaken > COMP15_TIME_LIMIT)
			timeTaken = COMP15_TIME_LIMIT;
		
		// Write the data for the current comparison to the data file
		try {
			dataFile.write(String.format(compDataFormat, 
					index/2 + 1, pChooserID, puzzleChooser1.puzzle.minMoves,
					puzzleChooser2.puzzle.minMoves, timeTaken));
		} catch (IOException ex) {}
		
		index += 2;
		if (index < compPuzzles.size()) {   // there are more comparisons
			// Show the next comparison
			puzzleChooser1.setPuzzle(compPuzzles.get(index));
			puzzleChooser2.setPuzzle(compPuzzles.get(index + 1));
			
			/*JPanel puzzlePanel = new JPanel();
			puzzlePanel.setLayout(new BoxLayout(puzzlePanel, BoxLayout.LINE_AXIS));
			puzzlePanel.add(puzzleChooser1);
			puzzlePanel.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)));
			puzzlePanel.add(puzzleChooser2);
			contentPane.removeAll();
			//contentPane.add(compInstLabel, BorderLayout.PAGE_START);
			contentPane.add(puzzlePanel, BorderLayout.CENTER);*/
			
			/*contentPane.removeAll();
			//contentPane.add(compInstLabel, BorderLayout.PAGE_START);
			contentPane.add(puzzleChooser1, BorderLayout.LINE_START);
	        contentPane.add(puzzleChooser2, BorderLayout.LINE_END);
	        contentPane.add(Box.createRigidArea(new Dimension(CENTER_SPACE_WIDTH, 0)),
	        		BorderLayout.CENTER);*/
			
			/*GridBagConstraints constraints = new GridBagConstraints();
	        // Add the instructions label at the top
	        constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridwidth = 2;
			contentPane.add(compInstLabel, constraints);
			
			// Add the first puzzle state on the bottom left
			constraints.gridx = 0;
			constraints.gridy = 1;
			constraints.gridwidth = 1;
			contentPane.add(puzzleChooser1, constraints);

			// Add the second puzzle state on the bottom right
			constraints.gridx = 1;
			constraints.gridy = 1;
			constraints.insets = new Insets(0, CENTER_SPACE_WIDTH, 0, 0);
			contentPane.add(puzzleChooser2, constraints);*/
	        
	        // Show everything
	        /*contentPane.validate();
	        window.pack();
	        window.setLocationRelativeTo(null);*/
			
			// Restart the timer, stopwatch, and countdown timer
			if (comp8)
				countdown = COMP8_TIME_LIMIT;
			else
				countdown = COMP15_TIME_LIMIT;
			compTimeLabel.setText("You have " + countdown/COMP_COUNTDOWN_RES + " second(s) left.");
			
	        timer.start();
	        startTime = (new Date()).getTime(); 
	        countdownTimer.start();
	        delayTimer.start();
		} else if (comp8)   // end of 8-puzzle comparisons
			beginComparisons15();
		else   // there are no more comparisons, so ask about the subject's prior expertise
			askExpertise();
	}
	
	private void askExpertise() {
		// Create the text label with the question
		String lastQuestion = "<html>We're almost done! &nbsp;Just one last question (take your time):" +
							  "&nbsp;Before this experiment, what was your level of expertise on" +
							  " the standard sliding puzzle?</html>";
		JLabel questionLabel = new JLabel(lastQuestion);
		questionLabel.setFont(font);
		questionLabel.setVerticalAlignment(JLabel.TOP);
		questionLabel.setOpaque(true);
		
		// Create the choices (radio buttons)
		String lowStr = "Low", medStr = "Medium", highStr = "High";
		JRadioButton lowButton = new JRadioButton(lowStr);
		lowButton.setFont(font);
	    lowButton.setActionCommand(lowStr);
	    JRadioButton medButton = new JRadioButton(medStr);
	    medButton.setFont(font);
	    medButton.setActionCommand(medStr);
	    JRadioButton highButton = new JRadioButton(highStr);
	    highButton.setFont(font);
	    highButton.setActionCommand(highStr);

	    // Group the choices together to ensure mutual exclusivity
	    ButtonGroup group = new ButtonGroup();
	    group.add(lowButton);
	    group.add(medButton);
	    group.add(highButton);
	    
	    // Register a listener for each of the radio buttons
	    lowButton.addActionListener(this);
	    medButton.addActionListener(this);
	    highButton.addActionListener(this);
	    
	    // Put the choices in a row in a panel
        JPanel choicePanel = new JPanel(new GridLayout(1, 0));
        choicePanel.add(lowButton);
        choicePanel.add(medButton);
        choicePanel.add(highButton);
        choicePanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
	    
	    // Adjust the size of the label
		Dimension oldSize = questionLabel.getPreferredSize();
		int area = oldSize.width * oldSize.height;
		int newWidth = choicePanel.getPreferredSize().width + 100;
		Dimension newSize = new Dimension(newWidth, area / newWidth + 30);
		questionLabel.setPreferredSize(newSize);
		
		// Create the "done" button
		JButton doneButton = new JButton("Done");
		doneButton.setFont(font);
		//doneButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		doneButton.setActionCommand("done");
		doneButton.addActionListener(this);
		
		// Create a panel for the button so it will be in the center
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		int labelWidth = questionLabel.getPreferredSize().width;
		int buttonWidth = doneButton.getPreferredSize().width;
		Component rigidArea = Box.createRigidArea(new Dimension((labelWidth - buttonWidth)/2, 0));
		buttonPanel.add(rigidArea);
		buttonPanel.add(doneButton);

		// Add everything to the content pane and show them
		contentPane.removeAll();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(questionLabel, BorderLayout.PAGE_START);
		contentPane.add(choicePanel, BorderLayout.CENTER);
		contentPane.add(buttonPanel, BorderLayout.PAGE_END);

		contentPane.validate();
		window.pack();
        window.setLocationRelativeTo(null);
	}

	/**
	 * Responds to the subject selecting a level of expertise or clicking the "done" button.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("done")) {
			if (expertise == null) {
				JOptionPane.showMessageDialog(window,
		                   "Please select your previous level of expertise.",
		                   "",
			                JOptionPane.ERROR_MESSAGE);
			} else {
				try {
					dataFile.write("\nPrior expertise is " + expertise.toLowerCase() + "\n");
					endExperiment();
				} catch (IOException e1) {}
			}
			
		// If the subject selected a level of expertise, store it.
		} else {
			expertise = e.getActionCommand();
		}
	}
	
	private void endExperiment() {
		// Show the final dialog box
		String finalMsg = "That's the end of the experiment.  Thank you for partipating!";
		JOptionPane.showMessageDialog(
				window, 
				finalMsg, 
				"That's it!", 
				JOptionPane.PLAIN_MESSAGE);
		
		// Close the data and solution files and exit the program
		try {
			dataFile.close();
			solutionFile.close();
		} catch (IOException e1) {}
		System.exit(0);
	}
}