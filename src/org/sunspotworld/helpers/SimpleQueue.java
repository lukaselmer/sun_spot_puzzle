package org.sunspotworld.helpers;

import java.util.Vector;

/**
 *
 * @author Lukas Elmer
 */
class SimpleQueue {

    Vector elements = new Vector();

    public boolean isEmpty() {
        return elements.size() == 0;
    }

    public int size() {
        return elements.size();
    }

    public void enqueue(Object o) {
        elements.addElement(o);
    }

    public Object dequeue() {
        if (isEmpty()) {
            return null;
        }
        Object o = elements.firstElement();
        elements.removeElement(o);
        return o;
    }
}
