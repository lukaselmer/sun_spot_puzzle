package org.sunspotworld;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.IAccelerometer3D;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Random;

/**
 *
 * @author Lukas Elmer
 */
public class ShuffleListener extends Thread {

    private IAccelerometer3D accel = EDemoBoard.getInstance().getAccelerometer();
    private final StartApplication client;
    private boolean enabled = true;

    public ShuffleListener(StartApplication client) {
        this.client = client;
    }

    public void stopShuffle() {
        enabled = false;
    }

    public void run() {
        while (enabled) {
            try {
                if (Math.abs(accel.getAccel()) > 2.5) {
                    client.pauseApp();
                    client.shuffle(new Random().nextInt(20) + 40);
                    client.resumeApp();
                    Utils.sleep(200);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Utils.sleep(10);
        }
    }
}
