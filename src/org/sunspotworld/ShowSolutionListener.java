package org.sunspotworld;

import org.sunspotworld.helpers.LedsHelper;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.util.Utils;

/**
 *
 * @author Lukas Elmer
 */
class ShowSolutionListener {

    private ISwitch sw1 = EDemoBoard.getInstance().getSwitches()[EDemoBoard.SW1];
    private final StartApplication midlet;
    private final Thread t;
    private boolean run = true;

    public ShowSolutionListener(StartApplication m) {
        this.midlet = m;
        Runnable r = new Runnable() {

            public void run() {
                while (run) {
                    if (sw1.isClosed()) {
                        midlet.pauseApp();
                        LedsHelper.setTempColors(midlet.reference, 2500);
                        midlet.resumeApp();
                    } else {
                        Utils.sleep(100);
                    }
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
