package me.kalmemarq.util;

public class TimeUtils {
    public static long getCurrentMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
