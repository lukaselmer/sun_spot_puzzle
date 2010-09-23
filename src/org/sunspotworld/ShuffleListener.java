package org.sunspotworld;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.IAccelerometer3D;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Random;
import org.sunspotworld.helpers.LedsHelper;

/**
 *
 * @author Lukas Elmer
 */
public class ShuffleListener extends Thread {

    public static final int SHUFFLE_TIMES_LIMIT = 2;
    private IAccelerometer3D accel = EDemoBoard.getInstance().getAccelerometer();
    private final StartApplication midlet;
    private boolean run = true;
    private final Thread t;
    private int shuffleTimes = 0;

    public ShuffleListener(StartApplication m) {
        this.midlet = m;
        Runnable r = new Runnable() {

            public void run() {
                while (run) {
                    try {
                        if (Math.abs(accel.getAccel()) > 2.5) {
                            if (shuffleTimes >= SHUFFLE_TIMES_LIMIT) {
                                midlet.pauseApp();
                                LedsHelper.blink();
                                midlet.resetGame(false);
                            } else {
                                shuffleTimes += 2;
                            }
                        } else {
                            if (shuffleTimes > 0) {
                                shuffleTimes--;
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    Utils.sleep(10);
                }
            }
        };
        t = new Thread(r, "ShowSolutionListener");
        t.start();
    }

    public void stopService() {
        run = false;
    }
}
