package org.sunspotworld.helpers;

import com.sun.spot.util.Queue;
import java.util.Vector;

/**
 *
 * @author Lukas Elmer
 */
public class HttpHelper {

    private static HttpRequstSender sender;
    private static Thread senderThread;

    static {
        sender = new HttpRequstSender();
        senderThread = new Thread(sender);
        senderThread.start();
    }

    public static void addRequst(String url) {
        addRequst(url, true);
    }
    public static void addRequst(String url, boolean important) {
        sender.addRequest(url, important);
    }
}
