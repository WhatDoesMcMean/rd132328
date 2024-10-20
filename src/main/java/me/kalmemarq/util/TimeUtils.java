package me.kalmemarq.util;

public class TimeUtils {
    public static long millisTime() {
        return System.nanoTime() / 1_000_000L;
    }

    public static long nanoTime() {
        return System.nanoTime();
    }
}
