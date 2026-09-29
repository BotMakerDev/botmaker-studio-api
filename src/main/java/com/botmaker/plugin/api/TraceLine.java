package com.botmaker.plugin.api;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One line of a running bot's trace, as the host read it: when, how serious, what said it, what it said, and —
 * when the bot knew — which line of its own source and which part of the screen it was about.
 *
 * <h2>Why a line crosses as a shape when telemetry crosses as bytes</h2>
 *
 * <p>{@link Runs#onTelemetry} is opaque because what a frame means (a match, a click) is the runtime's
 * vocabulary, and the contract carries none. A log line is not a vocabulary: every host that runs a program
 * shows its log, filters it by severity and finds the source line it came from, whatever the program is built
 * on. That is a capability of hosting a run, so the shape of one line is the contract's, and nothing in it names
 * a plugin concept. {@code source} is free text the bot chose ({@code "Vision"}, {@code "Game"}), never a
 * closed set this module would have to grow. See {@code docs/refactor/40-run-trace.md}.
 *
 * <p><b>The host builds it; a plugin only reads it</b> (from {@link Runs#onTrace}). So it is a record, and a
 * component added later is additive for every reader (compatibility §2).
 *
 * @param at     when the bot wrote it, by the bot's clock; never {@code null}
 * @param level  how serious; {@link Level#UNKNOWN} for a level this host does not know
 * @param source what wrote it, as the bot named it; {@code ""} when it named nothing
 * @param text   the line itself, without a level or source prefix; never {@code null}
 * @param count  how many identical lines this one stands for, at least 1 — a bot collapses a line it repeats
 *               in a loop rather than sending it every time
 * @param line   the 1-based line of the bot's source that wrote it, when the bot knew
 * @param where  the part of the screen it is about, in desktop pixels, when there is one
 */
public record TraceLine(Instant at, Level level, String source, String text, int count, OptionalInt line,
                        Optional<Region> where) {

    public TraceLine {
        if (at == null) at = Instant.EPOCH;
        if (level == null) level = Level.UNKNOWN;
        if (source == null) source = "";
        if (text == null) text = "";
        if (count < 1) count = 1;
        if (line == null) line = OptionalInt.empty();
        if (where == null) where = Optional.empty();
    }

    /** How serious a line is. A closed set with stable ids, read totally: an unknown id is {@link #UNKNOWN}. */
    public enum Level {
        /** Detail shown only while the run's debug output is on ({@link Runs#DEBUG_PROPERTY}). */
        DEBUG("debug", "Debug"),
        /** What the bot is doing, always shown. */
        INFO("info", "Info"),
        /** Something went wrong and the bot carried on. */
        WARN("warn", "Warning"),
        /** Something went wrong and the bot could not do what it was doing. */
        ERROR("error", "Error"),
        /** A level this host does not know, from a newer runtime. Shown, never dropped. */
        UNKNOWN("", "Other");

        private final String id;
        private final String displayName;

        Level(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** The stable id, what a wire or a saved filter carries. */
        public String id() {
            return id;
        }

        /** The name a filter shows. */
        public String displayName() {
            return displayName;
        }

        /** The level with this id, ignoring case, or {@link #UNKNOWN}; never throws. */
        public static Level fromId(String id) {
            if (id == null || id.isBlank()) return UNKNOWN;
            String wanted = id.trim().toLowerCase(Locale.ROOT);
            for (Level level : values()) {
                if (level != UNKNOWN && level.id.equals(wanted)) return level;
            }
            return UNKNOWN;
        }
    }

    /** A rectangle on the desktop, in pixels: where it starts and how big it is. */
    public record Region(int x, int y, int width, int height) {}
}
