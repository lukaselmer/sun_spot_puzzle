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
 *
 * @author Lukas Elmer
 */
class ResultTransmitter implements Runnable {

    private static final int INACTIVE = 0;
    private static final int CONNECTING = 1;
    private static final int COMPLETED = 2;
    private static final int IOERROR = 3;
    private static final int PROTOCOLERROR = 4;
    private static int POSTstatus = INACTIVE;
    private Vector statisticsToSend = new Vector();

    ResultTransmitter() {
    }

    public void run() {
        while (true) {
            if (statisticsToSend.size() > 0) {
                Object[] currentStatistics = (Object[]) statisticsToSend.firstElement();
                if (transmit((Integer) currentStatistics[0], (Integer) currentStatistics[1], (Long) currentStatistics[2])) {
                    statisticsToSend.removeElement(currentStatistics);
                }
                Utils.sleep(300);
            } else {
                Utils.sleep(1000);
            }
        }
    }

    synchronized boolean transmit(Integer swapTimes, Integer cycleTimes, Long timeNeeded) {
        String url = "http://puzzle.elmermx.ch/puzzle_games?puzzle_game[swap_times]=" + swapTimes.intValue() + "&puzzle_game[cycle_times]=" + cycleTimes.intValue()
                + "&puzzle_game[time_in_milliseconds]=" + timeNeeded.longValue() + "";
        HttpHelper.addRequst(url);
        return true;
    }

//    synchronized boolean transmit(int swapTimes, int cycleTimes) {
//        boolean worked = false;
//        try {
//            HttpConnection conn = null;
//            long starttime = 0;
//            String resp = null;
//
//            System.out.println("Posting statistics...");
//            try {
//                POSTstatus = CONNECTING;
//                starttime = System.currentTimeMillis();
//                //conn = (HttpConnection) Connector.open("http://127.0.0.1/puzzle_games/");
//                System.out.println("a");
//                conn = (HttpConnection) Connector.open("http://puzzle.elmermx.ch/puzzle_games?puzzle_game[swap_times]=" + swapTimes + "&puzzle_game[cycle_times]=" + cycleTimes + "",
//                        Connector.READ_WRITE, true);
//                conn.setRequestMethod(HttpConnection.POST);
//                System.out.println("a");
//                resp = conn.getResponseMessage();
//                System.out.println("resp = " + resp);
//                if (resp.equals("OK") || resp.equals("Found")) {
//                    POSTstatus = COMPLETED;
//                } else {
//                    POSTstatus = PROTOCOLERROR;
//                }
//            } catch (Exception ex) {
//                POSTstatus = IOERROR;
//                System.out.println("Error transmitting results!");
//            } finally {
//                if (conn != null) {
//                    conn.close();
//                }
//            }
//
//            if (POSTstatus == COMPLETED) {
//                worked = true;
//            } else {
//                System.out.println("Posting failed. Try again later...");
//            }
//            System.out.println("Total time to post " + (System.currentTimeMillis() - starttime) + " ms");
//            System.out.flush();
//        } catch (IOException ex) {
//        }
//        return worked;
//    }
    public synchronized void addStatistics(int swapTimes, int cycleTimes, long timeNeeded, int gameTimes) {
        statisticsToSend.addElement(new Object[]{new Integer(swapTimes), new Integer(cycleTimes), new Long(timeNeeded), new Integer(gameTimes)});
    }
}
