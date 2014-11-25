package com.uwetrottmann.wpdisplay.model;

/**
 * Holder object for heat pump controller status data.
 */
public class StatusData {

    /**
     * Maximum length of data supported. Sent status data is actually 183 bytes long, but we don't
     * care about the rest, yet.
     */
    public final static int LENGTH_BYTES = 80;

    interface Index {
        /**
         * All following are temparature values, factor 10, Celsius.
         */
        int TEMP_OUTGOING = 10;
        int TEMP_RETURN = 11;
        int TEMP_RETURN_SHOULD = 12;
        int TEMP_OUTDOORS = 15;
        int TEMP_WATER = 17;
        int TEMP_WATER_SHOULD = 18;
        int TEMP_SOURCE_IN = 19;
        int TEMP_SOURCE_OUT = 20;

        /**
         * All following are time values, Seconds.
         */
        int TIME_PUMP_ACTIVE = 67;
        int TIME_REST = 71;
        int TIME_COMPRESSOR_NOOP = 73;
        int TIME_RETURN_LOWER = 74;
        int TIME_RETURN_HIGHER = 75;
    }

    private int[] rawData;

    public StatusData(int[] rawData) {
        if (rawData.length != LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "array is not size " + LENGTH_BYTES + " but was " + rawData.length);
        }
        this.rawData = rawData;
    }

    public double getTemperatureOutdoors() {
        return getTemperature(Index.TEMP_OUTDOORS);
    }

    private double getTemperature(int index) {
        int tempRaw = getValueAt(index);
        return tempRaw / 10.0;
    }

    private int getValueAt(int index) {
        if (index + 1 > rawData.length) {
            throw new IllegalArgumentException(
                    "index must be from 0 to array length " + rawData.length);
        }

        return rawData[index];
    }
}
