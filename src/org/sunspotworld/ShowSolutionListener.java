package org.sunspotworld;

import org.sunspotworld.helpers.LedsHelper;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.util.Utils;

/**
 *
 * @author Lukas Elmer
 */
class ShowSolutionListener implements Runnable {

    private ISwitch sw1 = EDemoBoard.getInstance().getSwitches()[EDemoBoard.SW1];
    private final StartApplication midlet;

    public ShowSolutionListener(StartApplication midlet) {
        this.midlet = midlet;
    }

    public void run() {
        while (true) {
            if (sw1.isClosed()) {
                midlet.pauseApp();
                LedsHelper.setTempColors(midlet.reference, 2500);
                midlet.resumeApp();
            } else {
                Utils.sleep(100);
            }
        }
    }
}
