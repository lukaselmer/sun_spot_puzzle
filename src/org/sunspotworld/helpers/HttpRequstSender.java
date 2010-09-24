package org.sunspotworld.helpers;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.HttpConnection;

/**
 *
 * @author Lukas Elmer
 */
public class HttpRequstSender implements Runnable {

    private static final int INACTIVE = 0;
    private static final int CONNECTING = 1;
    private static final int COMPLETED = 2;
    private static final int IOERROR = 3;
    private static final int PROTOCOLERROR = 4;
    private static int POSTstatus = INACTIVE;
    private SimpleQueue requests = new SimpleQueue();
    private boolean enabled = true;
    // private static String host = null;//"0014.4F01.0000.5B1B";
    private static HostFinder hostFinder = null;//"0014.4F01.0000.5B1B";
    private static Thread hostFinderThread = null;//"0014.4F01.0000.5B1B";

    public void run() {
        while (enabled) {
            while (!requests.isEmpty()) {
                Vector urls = collect20Urls();
                //String url = (String) requests.dequeue();
                doHttpRequst(urls);
            }
            Utils.sleep(100);
        }
    }

    public void stop() {
        enabled = false;
    }

    public void addRequest(String url, boolean important) {
        if (important || requests.size() < 3) {
            requests.enqueue(url);
        }
    }

    private void doHttpRequst(Vector urls) {
        if (hostFinder == null) {
            hostFinder = new HostFinder();
            hostFinderThread = new Thread(hostFinder);
            hostFinderThread.start();
        }
        while (!hostFinder.connected()) {
            System.out.println("Not connected to host.");
            Utils.sleep(3000);
        }
        System.out.print("Queue = " + requests.size() + "; Transmitting URL '" + urls.size() + "'...");
        int connectionFailedCounter = 0;
        while (!transmit(urls)) {
            System.out.println(" failed! Trying again...");
            if (connectionFailedCounter > 5) {
                hostFinder.resetHost();
            }
            connectionFailedCounter++;
        }
        System.out.println("ok!");
    }

    synchronized boolean transmit(Vector urls) {
        boolean success = false;
        if (!hostFinder.connected()) {
            Utils.sleep(2000);
            return false;
        }
        try {
            RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://" + hostFinder.getHost() + ":40", Connector.READ_WRITE, true);
            Datagram dg = conn.newDatagram(conn.getMaximumLength());
            try {
                dg.writeInt(urls.size());
                for (int i = 0; i < urls.size(); i++) {
                    dg.writeUTF((String) urls.elementAt(i));
                }
                conn.send(dg);
                conn.receive(dg);
                String answer = dg.readUTF();
                success = answer.equals("ok");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                conn.close();
            }
        } catch (Exception ex) {
            hostFinder.resetHost();
        }
        return success;
    }

    synchronized boolean transmit2(String url) {
        boolean worked = false;
        try {
            HttpConnection conn = null;
            //long starttime = 0;
            String resp = null;

            try {
                POSTstatus = CONNECTING;
                //starttime = System.currentTimeMillis();
                conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
                conn.setRequestMethod(HttpConnection.POST);
                resp = conn.getResponseMessage();
                //System.out.println("resp = " + resp);
                if (resp != null && resp.toLowerCase().equals("ok") || resp.toLowerCase().equals("found")) {
                    POSTstatus = COMPLETED;
                } else {
                    POSTstatus = PROTOCOLERROR;
                }
            } catch (Exception ex) {
                POSTstatus = IOERROR;
                //System.out.println("Error transmitting results!");
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
            if (POSTstatus == COMPLETED) {
                worked = true;
            }
            // else {
            //    System.out.println("Posting failed. Try again later...");
            //}
            //System.out.println("Total time to post " + (System.currentTimeMillis() - starttime) + " ms");
            //System.out.flush();
        } catch (IOException ex) {
        }
        return worked;
    }

    private Vector collect20Urls() {
        Vector urls = new Vector();
        for (int i = 0; i < 2; i++) {
            if (!requests.isEmpty()) {
                urls.addElement((String) requests.dequeue());
            }
        }
        return urls;
    }
}
