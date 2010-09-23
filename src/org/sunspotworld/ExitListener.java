package org.sunspotworld;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.util.*;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 *
 * @author Lukas Elmer
 */
public class ExitListener {

    private ISwitch sw2 = EDemoBoard.getInstance().getSwitches()[EDemoBoard.SW2];
    private final StartApplication midlet;
    private final Thread t;
    private boolean run = true;

    public ExitListener(StartApplication m) {
        this.midlet = m;
        Runnable r = new Runnable() {

            public void run() {
                try {
                    while (run) {
                        if (sw2.isClosed()) {
                            midlet.exit();
                        }
                        Utils.sleep(100);
                    }
                } catch (Exception ex) {
                }
            }
        };
        t = new Thread(r, "ExitListener");
        t.start();
    }

    public void stopService() {
        run = false;
    }
}
