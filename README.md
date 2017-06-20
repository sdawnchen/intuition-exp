# Overview

This is the code for the experiment in "Enhancing acquisition of intuition versus planning in problem solving" (Chen & Holyoak, 2010, *CogSci Proceedings*). You can read the paper [here](http://www.dawnchen.info/papers/intuition_CogSci_2010.pdf). The code was written in 2008-2009 for [Java SE 5](http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase5-419410.html).


# Screenshots

Here is the first screen in the relaxed problem condition, which shows the rules of the puzzle and an example of the puzzle:

![First screen for relaxed condition](https://github.com/sdawnchen/intuition-exp/blob/master/screenshots/relaxed_first_screen.png)

Here is a typical screen during the training phase in the control condition (screens during the test phase for all conditions look the same):

![Example training puzzle for control condition](https://github.com/sdawnchen/intuition-exp/blob/master/screenshots/control_training_puzzle.png)

Here is an example of a pairwise distance comparison in the intuition assessment phase for all conditions.

![Example pairwise comparison](https://github.com/sdawnchen/intuition-exp/blob/master/screenshots/distance_comparison.png)

More screenshots are in the `screenshots` folder.


# Source Files

The `gui`, `puzzles`, `util`, and `analysis` folders contain my code. Here are more detailed descriptions of the files within each folder:

* `gui`: This folder contains GUI-related code. `Experiment.java` contains the main GUI code and entry point for the experiment. `PuzzleViewer` is a simple component for displaying a sliding-tile puzzle state, used by `PuzzleMover` (which handles making moves on the puzzle) and `PuzzleChooser` (which handles choosing a puzzle in the intuition assessment phase).

* `puzzles`: This folder contains the logic for sliding-tile puzzles. `Puzzle` is the abstract base class for all other kinds of sliding-tile puzzles. It contains methods for performing A\* search to find the optimal solution for a puzzle and generating a new random puzzle to be solved. `NGoalsPuzzle` is the type of puzzles used in the training phase of the subproblem condition, in which only a subset of the tiles must be moved into their correct places. `NSwappableTilesPuzzle` is the type of puzzles used in the training phase of the relaxed problem condition, in which some tiles can be swapped with their neighbors.

* `util`: Utility classes for randomly generating puzzles for the training, test, and intuition assessment phases (`PuzzleFileGenerator`) and reading puzzles from a file (`PuzzleFileParser`).

* `analysis`: For analyzing the results, including gathering information about participants' backtracking behavior.

`jxl.jar` is an external Java libary for reading from and writing to Excel files.


# Puzzle Files

The folder `puzzlefiles` contains the puzzles and distance comparison problems that participants solved in the experiment. The `solution_*` files contain the solutions for puzzles in each condition's training phase and all conditions' test phase. The `paths_*` files indicate whether each distance comparison was between pairs of puzzle states on the same optimal solution path.
