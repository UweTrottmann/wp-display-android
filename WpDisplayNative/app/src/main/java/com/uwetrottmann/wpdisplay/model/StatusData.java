/*
 * Copyright 2015 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uwetrottmann.wpdisplay.model;

import android.support.annotation.StringRes;
import com.uwetrottmann.wpdisplay.R;
import java.util.Date;

/**
 * Holder object for heat pump controller status data.
 */
public class StatusData {

    /**
     * Maximum length of data supported. Sent status data is actually 183 bytes long, but we don't
     * care about the rest, yet.
     */
    public final static int LENGTH_BYTES = 100;

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

        Temperature(int offset) {
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

        Time(int offset) {
            this.offset = offset;
        }
    }

    private int[] rawData;
    private Date timestamp;

    public StatusData(int[] rawData) {
        if (rawData.length != LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "array is not size " + LENGTH_BYTES + " but was " + rawData.length);
        }
        this.rawData = rawData;
        this.timestamp = new Date();
    }

    /**
     * Return the {@link java.util.Date} this status data was stored.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Get a Celsius temperature value.
     */
    public double getTemperature(Temperature temperature) {
        int tempRaw = getValueAt(temperature.offset);
        return tempRaw / 10.0;
    }

    /**
     * Get a time duration string, formatted like "1h 2min 3sec".
     */
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

        return hours + "h " + minutes + "min " + seconds + "sec";
    }

    private static final int FIRMWARE_VERSION_INDEX_BEGIN = 81;
    private static final int FIRMWARE_VERSION_LENGTH = 10;

    public String getFirmwareVersion() {
        String version = "";
        for (int i = FIRMWARE_VERSION_INDEX_BEGIN;
                i < FIRMWARE_VERSION_INDEX_BEGIN + FIRMWARE_VERSION_LENGTH; i++) {
            version += String.valueOf((char) getValueAt(i));
        }
        return version;
    }

    private static final int OPERATING_STATE_INDEX = 80;

    private static final int STATE_HEATING = 0;
    private static final int STATE_WATER = 1;
    private static final int STATE_NOOP = 5;

    @StringRes
    public int getOperatingState() {
        int state = getValueAt(OPERATING_STATE_INDEX);
        switch (state) {
            case STATE_HEATING:
                return R.string.state_heating;
            case STATE_WATER:
                return R.string.state_water;
            case STATE_NOOP:
                return R.string.state_noop;
            default:
                return R.string.state_unknown;
        }
    }

    private int getValueAt(int index) {
        if (index + 1 > rawData.length) {
            throw new IllegalArgumentException(
                    "offset must be from 0 to array length " + rawData.length);
        }

        return rawData[index];
    }
}
