package org.sunspotworld;

import com.sun.spot.util.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import org.sunspotworld.helpers.HttpHelper;

/**
 * The result transmitter
 * @author Lukas Elmer
 */
class ResultTransmitter {

    private Vector statisticsToSend = new Vector();
    private final Thread t;
    private boolean run = true;
    private String address;

    ResultTransmitter(String adr) {
        address = adr;
        Runnable r = new Runnable() {

            public void run() {
                while (run) {
                    if (statisticsToSend.size() > 0) {
                        Object[] currentStatistics = (Object[]) statisticsToSend.firstElement();
                        if (transmit((Integer) currentStatistics[0], (Integer) currentStatistics[1], (Long) currentStatistics[2], address)) {
                            statisticsToSend.removeElement(currentStatistics);
                        }
                        Utils.sleep(300);
                    } else {
                        Utils.sleep(1000);
                    }
                }
            }
        };
        t = new Thread(r, "ResultTransmitterThread");
        t.start();
    }

    public void stopService() {
        run = false;
    }

    public synchronized boolean transmit(Integer swapTimes, Integer cycleTimes, Long timeNeeded, String address) {
        String url = "http://puzzle.elmermx.ch/puzzle_games?puzzle_game[swap_times]=" + swapTimes.intValue() + "&puzzle_game[cycle_times]=" + cycleTimes.intValue()
                + "&puzzle_game[address]=" + address
                + "&puzzle_game[time_in_milliseconds]=" + timeNeeded.longValue() + "";
        HttpHelper.addRequst(url);
        return true;
    }

    public synchronized void addStatistics(int swapTimes, int cycleTimes, long timeNeeded, int gameTimes) {
        statisticsToSend.addElement(new Object[]{new Integer(swapTimes), new Integer(cycleTimes), new Long(timeNeeded), new Integer(gameTimes)});
    }
}
