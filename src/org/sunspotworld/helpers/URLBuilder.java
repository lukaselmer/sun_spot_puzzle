package org.sunspotworld.helpers;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * @author Lukas Elmer
 */
public class URLBuilder {

    private String model, controller;
    private Hashtable attrs = new Hashtable();

    public static URLBuilder build(String model, String controller) {
        return new URLBuilder(model, controller);
    }

    private URLBuilder(String model, String controller) {
        this.model = model;
        this.controller = controller;
    }

    public URLBuilder add(String attribute, String value) {
        attrs.put(attribute, value);
        return this;
    }

    public URLBuilder add(String attribute, int value) {
        return add(attribute, "" + value);
    }

    public URLBuilder add(String attribute, double value) {
        return add(attribute, "" + value);
    }

    public URLBuilder add(String attribute, long value) {
        return add(attribute, "" + value);
    }

    public URLBuilder add(String attribute, boolean value) {
        return add(attribute, value ? 1 : 0);
    }

    public String toString() {
        String ret = "http://puzzle.elmermx.ch/" + controller + "?";
        attrs.keys();
        for (Enumeration e = attrs.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = (String) attrs.get(key);
            ret += model + "[" + key + "]=" + value;
            if (e.hasMoreElements()) {
                ret += "&";
            }
        }
        return ret;
    }
}
