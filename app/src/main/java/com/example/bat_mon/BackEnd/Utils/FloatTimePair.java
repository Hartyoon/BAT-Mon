package com.example.bat_mon.BackEnd.Utils;

import java.time.LocalDateTime;

public class FloatTimePair {

    public float value;
    public LocalDateTime time;

    public FloatTimePair(LocalDateTime time, float value) {
        this.value = value;
        this.time = time;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FloatTimePair) {
            FloatTimePair otherFTP = (FloatTimePair) other;
            return otherFTP.value == this.value && otherFTP.time.equals(this.time);
        } else {
            return false;
        }
    }

}
