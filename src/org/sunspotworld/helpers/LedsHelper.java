package org.sunspotworld.helpers;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.Utils;

/**
 *
 * @author Lukas Elmer
 */
public final class LedsHelper {

    public static final int BLINK_TIMES = 6, BLINKING_TIME = 500, SNEAKING_TIME = 350;
    public static final LEDColor BLINK_COLOR = null, SNEAKING_COLOR = null;
    private static ITriColorLED[] leds = EDemoBoard.getInstance().getLEDs();
    public static final boolean SNEAKING_DIRECTION = true;

    public static void setTempColors(LEDColor[] colors, int millisecondsToSleep) {
        Object[][] state = getState();
        for (int i = 0; i < colors.length; i++) {
            leds[i].setOn(true);
            leds[i].setColor(colors[i]);
        }
        Utils.sleep(millisecondsToSleep);
        setState(state);
    }

    public static void setColor(int i, LEDColor c) {
        leds[i].setColor(c);
    }

    public static void setOn(int i) {
        leds[i].setOn();
    }

    public static void setOff() {
        for (int j = 0; j < leds.length; j++) {
            setOff(j);
        }
    }

    public static void setOff(int i) {
        leds[i].setOff();
    }

    private static Object[][] getState() {
        Object[][] state = new Object[leds.length][];
        for (int i = 0; i < state.length; i++) {
            state[i] = new Object[2];
            state[i][0] = new Boolean(leds[i].isOn());
            state[i][1] = leds[i].getColor();
        }
        return state;
    }

    private static void setState(Object[][] state) {
        for (int i = 0; i < state.length; i++) {
            leds[i].setOn(((Boolean) state[i][0]).booleanValue());
            leds[i].setColor((LEDColor) state[i][1]);
        }
    }

    public static void blink() {
        blink(BLINK_COLOR, BLINK_TIMES, BLINKING_TIME);
    }

    public static void blink(LEDColor color) {
        blink(color, BLINK_TIMES, BLINKING_TIME);
    }

    public static void blink(LEDColor color, int times) {
        blink(color, times, BLINKING_TIME);
    }

    public static void blink(LEDColor color, int times, int blinkingTime) {
        Object[][] state = getState();
        if (color != null) {
            for (int j = 0; j < leds.length; j++) {
                leds[j].setColor(color);
            }
        }
        for (int i = 0; i < (times * 2); i++) {
            for (int j = 0; j < leds.length; j++) {
                ITriColorLED iTriColorLED = leds[j];
                iTriColorLED.setOn(!iTriColorLED.isOn());
            }
            Utils.sleep(blinkingTime);
        }
        setState(state);
    }

    public static void sneake() {
        sneake(SNEAKING_COLOR, SNEAKING_DIRECTION, SNEAKING_TIME);
    }

    public static void sneake(LEDColor color) {
        sneake(color, SNEAKING_DIRECTION, SNEAKING_TIME);
    }

    public static void sneake(LEDColor color, boolean fromLeft) {
        sneake(color, fromLeft, SNEAKING_TIME);
    }

    public static void sneake(LEDColor color, boolean fromLeft, int sneakingTime) {
        Object[][] state = getState();
        if (fromLeft) {
            for (int i = 0; i < leds.length; i++) {
                if (color != null) {
                    leds[i].setColor(color);
                }
                leds[i].setOn(!leds[i].isOn());
                Utils.sleep(sneakingTime);
            }
        } else {
            for (int i = leds.length - 1; i >= 0; i--) {
                if (color != null) {
                    leds[i].setColor(color);
                }
                leds[i].setOn(!leds[i].isOn());
                Utils.sleep(sneakingTime);
            }
        }
        setState(state);
    }
}
