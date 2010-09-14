package org.sunspotworld.helpers;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

/**
 *
 * @author Lukas Elmer
 */
class HostFinder implements Runnable {

    private String host;

    public void run() {
        while (true) {
            if (!connected()) {
                try {
                    RadiogramConnection conn = (RadiogramConnection) Connector.open("radiogram://:41");
                    Datagram dg = conn.newDatagram(conn.getMaximumLength());
                    try {
                        conn.receive(dg);
                        if (dg.readUTF().equals("host")) {
                            host = dg.getAddress();
                            System.out.println("Connected to host " + host);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        conn.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                Utils.sleep(300);
            }
        }
    }

    public boolean connected() {
        return host != null;
    }

    public String getHost() {
        return host;
    }

    public void resetHost() {
        System.out.println("connection lost!");
        host = null;
    }
}
