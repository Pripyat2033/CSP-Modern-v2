package com.ben.csp.time;

/**
 * The zone's single, server-authoritative work clock.
 *
 * <p>There is exactly one of these per world and it is the same for every
 * player: it advances on its own from real elapsed time, so no player ever has
 * to wait on another to "advance the day". The rate is a compression factor
 * (how many work-minutes pass per real second), configurable by an operator.
 *
 * <p>The clock is deliberately free of any Minecraft types so it can be driven
 * deterministically in tests; the server tick handler feeds it real timestamps.
 */
public final class WorkClock {

    public static final int MINUTES_PER_DAY = 24 * 60;
    /** Default compression: one work-day per real hour. */
    public static final double DEFAULT_RATE = MINUTES_PER_DAY / 3600.0;
    public static final int DEFAULT_SHIFT_START = 8 * 60;
    public static final int DEFAULT_SHIFT_END = 20 * 60;

    private double rate;
    private long workDay;
    private double minuteOfDay;
    private long lastRealMs;
    private int shiftStart = DEFAULT_SHIFT_START;
    private int shiftEnd = DEFAULT_SHIFT_END;

    public WorkClock(long realNowMs, double rate, long startWorkDay) {
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        if (startWorkDay < 1) {
            throw new IllegalArgumentException("work day is 1-based");
        }
        this.rate = rate;
        this.workDay = startWorkDay;
        this.lastRealMs = realNowMs;
    }

    public static WorkClock fresh(long realNowMs) {
        return new WorkClock(realNowMs, DEFAULT_RATE, 1);
    }

    /** Advance the clock from real elapsed time; returns the work-days crossed. */
    public long tick(long realNowMs) {
        long deltaMs = realNowMs - lastRealMs;
        if (deltaMs <= 0) {
            return 0;
        }
        lastRealMs = realNowMs;
        return addWorkMinutes((deltaMs / 1000.0) * rate);
    }

    /** Advance by an explicit number of work-minutes; returns days crossed. */
    public long addWorkMinutes(double minutes) {
        if (minutes <= 0) {
            return 0;
        }
        minuteOfDay += minutes;
        long daysCrossed = 0;
        while (minuteOfDay >= MINUTES_PER_DAY) {
            minuteOfDay -= MINUTES_PER_DAY;
            workDay++;
            daysCrossed++;
        }
        return daysCrossed;
    }

    public long workDay() {
        return workDay;
    }

    public double minuteOfDayExact() {
        return minuteOfDay;
    }

    public int minuteOfDay() {
        return (int) Math.floor(minuteOfDay);
    }

    public int hour() {
        return minuteOfDay() / 60;
    }

    public int minute() {
        return minuteOfDay() % 60;
    }

    public String hhmm() {
        return String.format("%02d:%02d", hour(), minute());
    }

    public boolean isShift() {
        int m = minuteOfDay();
        return m >= shiftStart && m < shiftEnd;
    }

    public boolean isFirstShift() {
        return isShift();
    }

    public double rate() {
        return rate;
    }

    public void setRate(double rate) {
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        this.rate = rate;
    }

    public int shiftStart() {
        return shiftStart;
    }

    public int shiftEnd() {
        return shiftEnd;
    }

    public void setShift(int startMinute, int endMinute) {
        this.shiftStart = Math.floorMod(startMinute, MINUTES_PER_DAY);
        this.shiftEnd = Math.floorMod(endMinute, MINUTES_PER_DAY);
    }

    public long lastRealMs() {
        return lastRealMs;
    }

    /** Real-time stamp for records, in epoch millis (your "written in a book" time). */
    public static long realNow() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Day " + workDay + " " + hhmm() + (isShift() ? " (shift)" : " (off-shift)");
    }
}
