package org.sunspotworld;

import org.sunspotworld.helpers.LedsHelper;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.io.IScalarInput;
import com.sun.spot.sensorboard.peripheral.IAccelerometer3D;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.*;

import java.util.Random;

import java.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStore;

/**
 * SunSpotPuzzle game class
 * @author Lukas Elmer
 */
public class StartApplication extends MIDlet {

    // Constanst
    /** minimal value for the accelerometer */
    private static final double MINIMAL_ACCELERATION_X = 0.3; //0.2;
    private static final double MINIMAL_ACCELERATION_Y = 0.4; //0.3;
    /** indicates no player action */
    private static final int ACTION_NONE = 0;
    /** indicates the player wants to shift the leds to the left */
    private static final int ACTION_SHIFTLEFT = 1;
    /** indicates the player wants to shift the leds to the right */
    private static final int ACTION_SHIFTRIGHT = 2;
    /** indicates the player has tilted the spot up */
    private static final int ACTION_UP = 5;
    /** indicates the player has tilted the spot down */
    private static final int ACTION_DOWN = 6;
    // Hardware
    private IAccelerometer3D accel = EDemoBoard.getInstance().getAccelerometer();
    private IScalarInput[] axis = EDemoBoard.getInstance().getScalarInputs();
    private ITriColorLED[] leds = EDemoBoard.getInstance().getLEDs();
    /** holds the current state of the puzzle */
    private LEDColor[] puzzle = new LEDColor[leds.length];
    /** holds the solved state of the puzzle */
    public final LEDColor[] reference = new LEDColor[leds.length];
    // Puzzle state vars
    private boolean paused = false;
    private int swapTimes = 0, cycleTimes = 0, gameTimes = 0;
    private long startTime = System.currentTimeMillis();
    private static Random random = new Random();
    // Listeners / Background tasks
    private ShowSolutionListener showSolutionListener;
    private ExitListener exitListener;
    private ResultTransmitter resultTransmitter;
    private ShuffleListener shuffleListener;

    /**
     * MIDlet call to start the application.
     */
    protected void startApp() throws MIDletStateChangeException {
        new BootloaderListener().start();   // monitor the USB (if connected) and recognize commands from host

        setupPuzzle(puzzle);
        setupPuzzle(reference);

        // Start result transmitter
        resultTransmitter = new ResultTransmitter();
        //resultTransmitter.addStatistics(50 + random.nextInt(150), 50 + random.nextInt(150), (long) (50 + random.nextInt(150) * 500), 1);
        //        for (int i = 0; i < 50; i++) {
        //            resultTransmitter.addStatistics(50 + random.nextInt(150), 50 + random.nextInt(150), i);
        //        }
        exitListener = new ExitListener(this);
        showSolutionListener = new ShowSolutionListener(this);
        shuffleListener = new ShuffleListener(this);

        LedsHelper.setOff();
        LedsHelper.sneake();
        LedsHelper.blink();

        resetGame(false);

        playGame();


        //        spotActivitySender = new SpotActivitySender();
        //        spotActivitySenderThread = new Thread(spotActivitySender);
        //        spotActivitySenderThread.start();

    }

    /**
     * sets up the puzzle by storing the rainbow color values in the
     * given array.
     */
    private void setupPuzzle(LEDColor[] p) {
        p[0] = LEDColor.RED;
        p[1] = LEDColor.ORANGE;
        p[2] = LEDColor.YELLOW;
        p[3] = LEDColor.GREEN;
        p[4] = LEDColor.TURQUOISE;
        p[5] = LEDColor.BLUE;
        p[6] = LEDColor.MAGENTA;
        p[7] = LEDColor.WHITE;
        updateLeds(0);
    }

    public void shuffle() {
        shuffle(random.nextInt(20) + 60);
    }

    public void shuffle(int times) {
        for (int t = 0; t < times; t++) {
            swap(random.nextInt(8), random.nextInt(8));
            updateLeds();
        }
    }

    /**
     * Translates the position of the Spot into a move for the player.
     */
    private int getPlayerAction() {
        try {
            double x = accel.getTiltX();
            double y = accel.getTiltY();

            int action = ACTION_NONE;
            if (x < -MINIMAL_ACCELERATION_X) {
                action = ACTION_SHIFTLEFT;
            } else if (x > MINIMAL_ACCELERATION_X) {
                action = ACTION_SHIFTRIGHT;
            } else if (y < -MINIMAL_ACCELERATION_Y) {
                action = ACTION_UP;
            } else if (x < MINIMAL_ACCELERATION_Y) {
                //action = ACTION_DOWN;
                action = ACTION_NONE;
            }
            return action;
        } catch (IOException ex) {
            return ACTION_NONE;
        }
    }

    /**
     * Main game loop, get the player action, perform it, update the screen,
     * check if the player has solved the puzzle. Repeat until infinity.
     */
    private void playGame() {
        int lastAction = ACTION_NONE;
        while (true) {
            if (paused) {
                Utils.sleep(50);
            } else {
                int action = getPlayerAction();
                switch (action) {
                    case ACTION_NONE:
                        break;
                    case ACTION_SHIFTLEFT:
                        doShiftLeft();
                        break;
                    case ACTION_SHIFTRIGHT:
                        doShiftRight();
                        break;
                }
                if (action != lastAction) {
                    switch (action) {
                        case ACTION_UP:
                            doUp();
                            break;
                        case ACTION_DOWN:
                            doDown();
                            break;
                    }
                }
                if (action != ACTION_NONE) {
                    updateLeds();
                }
                if (isSolved()) {
                    resetGame();
                }
                Utils.sleep(250);
                lastAction = action;
                cycleTimes++;
            }
        }
    }

    public void resetGame() {
        resetGame(true);
    }

    public void resetGame(boolean saveGameStatistics) {
        pauseApp();
        if (saveGameStatistics) {
            LedsHelper.blink();
            long timeNeeded = System.currentTimeMillis() - startTime;
            saveGameStatistics(swapTimes, cycleTimes, timeNeeded, ++gameTimes);
        }
        startTime = System.currentTimeMillis();
        swapTimes = 0;
        cycleTimes = 0;
        shuffle();
        LedsHelper.blink();
        resumeApp();
    }

    /**
     * Compares the current state of the puzzle with the refence state.
     * @return true if the puzzle is solved.
     */
    private boolean isSolved() {
        boolean solved = true;
        for (int t = 0; t < puzzle.length; t++) {
            if (!puzzle[t].equals(reference[t])) {
                solved = false;
                break;
            }
        }
        if (solved) {
            return true;
        }
        for (int t = 0; t < puzzle.length; t++) {
            if (!puzzle[t].equals(reference[puzzle.length - t - 1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * performs the up move, swaps the middle for leds.
     */
    public void doUp() {
//        swap(2, 4);
//        swap(3, 5);
        swap(3, 4);
        swapTimes++;
    }

    /**
     * performs the down move, swaps the middle for leds in a slightly
     * different way.
     */
    public void doDown() {
//        swap(3, 4);
//        swap(2, 5);
        swap(4, 3);
        swapTimes++;
    }

    /**
     * swaps two puzzle positions.
     */
    protected void swap(int s, int d) {
        LEDColor tmp = puzzle[s];
        puzzle[s] = puzzle[d];
        puzzle[d] = tmp;
    }

    /**
     * shifts the leds to the left
     */
    private void doShiftLeft() {
        LEDColor tmp = puzzle[0];
        for (int t = 0; t < puzzle.length - 1; t++) {
            puzzle[t] = puzzle[t + 1];
        }
        puzzle[puzzle.length - 1] = tmp;
    }

    /**
     * shifts the leds to the right
     */
    private void doShiftRight() {
        LEDColor tmp = puzzle[puzzle.length - 1];
        for (int t = puzzle.length - 1; t > 0; t--) {
            puzzle[t] = puzzle[t - 1];
        }
        puzzle[0] = tmp;
    }

    private void updateLeds() {
        updateLeds(50);
    }

    private void updateLeds(int millisecondsToWait) {
        for (int t = 0; t < puzzle.length; t++) {
            leds[t].setOn();
            leds[t].setColor(puzzle[t]);
        }
        Utils.sleep(millisecondsToWait);
    }

    public void exit() {
        pauseApp();
        try {
            //exitListenerThread.interrupt();
            exitListener.stopService();
            resultTransmitter.stopService();
            showSolutionListener.stopService();
            shuffleListener.stopService();
        } catch (Exception ex) {
        }
        LedsHelper.sneake(LEDColor.CYAN);
        LedsHelper.blink(null);
        LedsHelper.sneake();
        notifyDestroyed();
    }

    protected void pauseApp() {
        paused = true;
    }

    protected void resumeApp() {
        paused = false;
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        try {
            for (int i = 0; i < 8; i++) {
                leds[i].setOff();
            }
        } catch (Exception ex) {
        }
    }

    private void saveGameStatistics(int swapTimes, int cycleTimes, long timeNeeded, int gameTimes) {
        try {
            RecordStore.deleteRecordStore("gametimes");
            RecordStore rms = RecordStore.openRecordStore("gametimes", true);
            byte[] inputData = {new Integer(gameTimes).byteValue()};
            int recordId = rms.addRecord(inputData, 0, inputData.length);
            rms.closeRecordStore();
        } catch (Exception ex) {
        }
        resultTransmitter.addStatistics(swapTimes, cycleTimes, timeNeeded, gameTimes);
    }
}
