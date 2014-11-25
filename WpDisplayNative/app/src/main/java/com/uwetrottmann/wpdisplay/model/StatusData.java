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

    /**
     * Temperature values, factor 10, Celsius.
     */
    public enum Temperature {

        OUTGOING(10),
        RETURN(11),
        RETURN_SHOULD(12),
        OUTDOORS(15),
        WATER(17),
        WATER_SHOULD(18),
        SOURCE_IN(19),
        SOURCE_OUT(20);

        public final int offset;

        private Temperature(int offset) {
            this.offset = offset;
        }
    }

    /**
     * Time values, factor 1, Seconds.
     */
    public enum Time {

        TIME_PUMP_ACTIVE(67),
        TIME_REST(71),
        TIME_COMPRESSOR_NOOP(73),
        TIME_RETURN_LOWER(74),
        TIME_RETURN_HIGHER(75);

        public final int offset;

        private Time(int offset) {
            this.offset = offset;
        }
    }

    private int[] rawData;

    public StatusData(int[] rawData) {
        if (rawData.length != LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "array is not size " + LENGTH_BYTES + " but was " + rawData.length);
        }
        this.rawData = rawData;
    }

    public double getTemperature(Temperature temperature) {
        int tempRaw = getValueAt(temperature.offset);
        return tempRaw / 10.0;
    }

    public String getTime(Time time) {
        int elapsedSeconds = getValueAt(time.offset);

        long hours = 0;
        long minutes = 0;
        if (elapsedSeconds >= 3600) {
            hours = elapsedSeconds / 3600;
            elapsedSeconds -= hours * 3600;
        }
        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        long seconds = elapsedSeconds;

        return hours + " h " + minutes + " min " + seconds + " sec";
    }

    private int getValueAt(int index) {
        if (index + 1 > rawData.length) {
            throw new IllegalArgumentException(
                    "offset must be from 0 to array length " + rawData.length);
        }

        return rawData[index];
    }
}
