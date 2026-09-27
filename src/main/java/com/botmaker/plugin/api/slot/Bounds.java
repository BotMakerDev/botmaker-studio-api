package com.botmaker.plugin.api.slot;

/**
 * The inclusive range a number being edited is declared to stay within — {@code @Param(min = 0, max = 1)} —
 * as {@link ValueContext#bounds()} hands it to an editor (2026-09-27).
 *
 * <p>Either end may be open: {@link Double#NEGATIVE_INFINITY} and {@link Double#POSITIVE_INFINITY} mean no
 * limit on that side, because "at most 10" is a sentence a person says. The ends are advice to a widget and a
 * clamp, never a validation that can fail: a value outside is pulled to the nearest end, since the
 * alternative is a project that refuses a value because somebody tightened a limit afterwards.
 *
 * <p>Reversed ends are read in order rather than refused, for the same reason.
 *
 * @param min the inclusive lower end, {@link Double#NEGATIVE_INFINITY} for none
 * @param max the inclusive upper end, {@link Double#POSITIVE_INFINITY} for none
 */
public record Bounds(double min, double max) {

    /** No limit on either side — what every value that declares none is edited within. */
    public static final Bounds NONE = new Bounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    public Bounds {
        if (Double.isNaN(min)) min = Double.NEGATIVE_INFINITY;
        if (Double.isNaN(max)) max = Double.POSITIVE_INFINITY;
        if (min > max) {
            double low = max;
            max = min;
            min = low;
        }
    }

    /** Whether either end is declared — what an editor asks before drawing the range. */
    public boolean isBounded() {
        return min != Double.NEGATIVE_INFINITY || max != Double.POSITIVE_INFINITY;
    }

    /** {@code value} pulled to the nearest end when it lies outside; itself otherwise. */
    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
