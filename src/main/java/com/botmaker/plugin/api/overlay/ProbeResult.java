package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.util.Locale;
import java.util.Optional;

/**
 * What a {@link Probe} says its call would answer now: found, missing, or cannot tell — a line for the row's
 * tooltip, and where on the screen, if anywhere. Built by {@link #found}, {@link #missing} or {@link #unknown}.
 */
public final class ProbeResult {

    private final State state;
    private final String text;
    private final Area area;

    private ProbeResult(State state, String text, Area area) {
        this.state = state;
        this.text = text == null ? "" : text;
        this.area = area;
    }

    /** It is there, at {@code area} in desktop pixels; {@code text} is one line: {@code "found 0.94 at 412,230"}. */
    public static ProbeResult found(String text, Area area) {
        return new ProbeResult(State.FOUND, text, area);
    }

    /** It is not there. */
    public static ProbeResult missing(String text) {
        return new ProbeResult(State.MISSING, text, null);
    }

    /** The probe cannot tell: no frame, an argument it cannot read. */
    public static ProbeResult unknown(String text) {
        return new ProbeResult(State.UNKNOWN, text, null);
    }

    public State state() {
        return state;
    }

    /** One line a user reads. */
    public String text() {
        return text;
    }

    /** Where, in desktop pixels; the host draws it as a {@link Marks} box. */
    public Optional<Area> area() {
        return Optional.ofNullable(area);
    }

    @Override
    public String toString() {
        return state.displayName() + (text.isEmpty() ? "" : ": " + text);
    }

    /** A probe's answer. A closed set with stable ids; an unknown id is {@link #UNKNOWN}. */
    public enum State {
        FOUND("found", "Found"),
        MISSING("missing", "Missing"),
        UNKNOWN("", "Cannot tell");

        private final String id;
        private final String displayName;

        State(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        /** The state {@code id} names; {@link #UNKNOWN} for anything else. */
        public static State fromId(String id) {
            String wanted = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            for (State state : values()) {
                if (!state.id.isEmpty() && state.id.equals(wanted)) return state;
            }
            return UNKNOWN;
        }
    }
}
