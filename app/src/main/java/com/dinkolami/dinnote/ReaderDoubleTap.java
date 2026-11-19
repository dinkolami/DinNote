package com.dinkolami.dinnote;

import android.os.SystemClock;
import android.view.ViewConfiguration;

/**
 * Small, dependency-free double-tap detector for the note reader.
 * Call onTap(x, y) only for completed taps (not swipes).
 * A successful double tap resets the detector.
 */
public final class ReaderDoubleTap {
    private final long timeoutMs;
    private final float maxDistanceSquared;
    private long previousTapMs = -1L;
    private float previousX;
    private float previousY;

    public ReaderDoubleTap(float touchSlopPx) {
        timeoutMs = ViewConfiguration.getDoubleTapTimeout();
        float maxDistance = Math.max(1f, touchSlopPx * 4f);
        maxDistanceSquared = maxDistance * maxDistance;
    }

    public boolean onTap(float x, float y) {
        long now = SystemClock.uptimeMillis();
        float dx = x - previousX;
        float dy = y - previousY;
        boolean doubleTap = previousTapMs >= 0
                && now - previousTapMs <= timeoutMs
                && dx * dx + dy * dy <= maxDistanceSquared;
        if (doubleTap) {
            reset();
            return true;
        }
        previousTapMs = now;
        previousX = x;
        previousY = y;
        return false;
    }

    public void reset() {
        previousTapMs = -1L;
    }
}
