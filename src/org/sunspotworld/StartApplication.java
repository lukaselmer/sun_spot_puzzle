package org.sunspotworld;

import org.sunspotworld.helpers.LedsHelper;
import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.sensorboard.io.IScalarInput;
import com.sun.spot.sensorboard.peripheral.IAccelerometer3D;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.*;


import java.io.IOException;
import java.util.Random;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

/**
 * A motion controlled Puzzle for the SunSPOT (Green SDK)
 * The Goal is to (re-)create a rainbow of LEDs starting with the red led on the left.
 * There are four moves : shift left and shift right by tilting the spot to the left or right.
 * The two other moves are tilt forward and backward and swap the 4 middle leds in a certain way.
 * ----------------------------------------------------------------------------------------------
 * (C) 2007 E.Hooijmeijer / www.ctrl-alt-dev.nl
 * This software is published under the GNU Public Licence v2 or better (www.gnu.org)
 * For the development blog visit Eriks SunSPOT Adventures at http://joce.nljug.org
 * @author E.Hooijmeijer
 */
public class StartApplication extends MIDlet {

    /** index of the X axis */
    private static final int X = 0;
    /** index of the Y axis */
    private static final int Y = 1;
    /** index of the Z axis */
    private static final int Z = 2;
    /** index of the red component of the led */
    private static final int RED = 0;
    /** index of the green component of the led */
    private static final int GREEN = 1;
    /** index of the blue component of the led */
    private static final int BLUE = 2;
    /** minimal value for the accelerometer */
    private static final double MINIMAL_ACCELERATION_X = 0.2;
    private static final double MINIMAL_ACCELERATION_Y = 0.3;
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
    //
    // Hardware
    //
    private IAccelerometer3D accel = EDemoBoard.getInstance().getAccelerometer();
    //private IScalarInput[] axis = new IScalarInput[]{accel.getXAxis(), accel.getYAxis(), accel.getZAxis()};
    private IScalarInput[] axis = EDemoBoard.getInstance().getScalarInputs();
    private ITriColorLED[] leds = EDemoBoard.getInstance().getLEDs();
    /** holds the current state of the puzzle */
    private LEDColor[] puzzle = new LEDColor[leds.length];
    /** holds the solved state of the puzzle */
    public final LEDColor[] reference = new LEDColor[leds.length];
    //private double[] zeroOffset = {465.5, 465.5, 465.5};     // default zero offset for raw accelerator value
    //private double[] sensitivity = {186.2, 186.2, 186.2};    // default conversion factor from raw accelerator value to G's
    //private double[] movement;
    private ExitListener exitListener;
    private Thread exitListenerThread;
    private ShowSolutionListener showSolutionListener;
    private Thread showSolutionListenerThread;
    private boolean paused = false;
    private int swapTimes = 0, cycleTimes = 0, gameTimes = 0;
    private long startTime = System.currentTimeMillis();
    private Thread resultTransmitterThread;
    private ResultTransmitter resultTransmitter;
    private static Random random = new Random();
    private ShuffleListener shuffleListener;
//    private SpotActivitySender spotActivitySender;
//    private Thread spotActivitySenderThread;

    /**
     * MIDlet call to start the application.
     */
    protected void startApp() throws MIDletStateChangeException {
        new BootloaderListener().start();   // monitor the USB (if connected) and recognize commands from host

        resultTransmitter = new ResultTransmitter();
        resultTransmitterThread = new Thread(resultTransmitter);
        resultTransmitterThread.start();
//        for (int i = 0; i < 50; i++) {
//            resultTransmitter.addStatistics(50 + random.nextInt(150), 50 + random.nextInt(150), i);
//        }

//        spotActivitySender = new SpotActivitySender();
//        spotActivitySenderThread = new Thread(spotActivitySender);
//        spotActivitySenderThread.start();


        setupPuzzle(puzzle);
        setupPuzzle(reference);

        LedsHelper.setOff();
        LedsHelper.sneake();
        LedsHelper.blink(null);

        exitListener = new ExitListener(this);
        exitListenerThread = new Thread(exitListener);
        exitListenerThread.start();

        showSolutionListener = new ShowSolutionListener(this);
        showSolutionListenerThread = new Thread(showSolutionListener);
        showSolutionListenerThread.start();

        shuffle();

        shuffleListener = new ShuffleListener(this);
        shuffleListener.start();

        startTime = System.currentTimeMillis();
        playGame();
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
        shuffle(32);
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

    private void resetGame() {
        pauseApp();
        LedsHelper.blink(null);
        long timeNeeded = System.currentTimeMillis() - startTime;
        saveGameStatistics(swapTimes, cycleTimes, timeNeeded, ++gameTimes);
        startTime = System.currentTimeMillis();
        swapTimes = 0;
        cycleTimes = 0;
        resumeApp();
        shuffle();
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
            exitListenerThread.interrupt();
            showSolutionListenerThread.interrupt();
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
