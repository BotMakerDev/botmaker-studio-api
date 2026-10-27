package com.botmaker.plugin.api.run;

import java.util.Objects;

/**
 * Whether the open project's bot can start now, in one line its runner reads before pressing Start: a word and
 * the reason behind it, "● Ready — game window found" (2026-10-25).
 *
 * <pre>{@code
 * StudioPlugin.declare().readiness(MyReadiness::now)
 *
 * static Readiness now() {
 *     return windowOpen() ? Readiness.ready("game window found")
 *             : Readiness.blocked("the game is not running: start it first");
 * }
 * }</pre>
 *
 * <p>Only a plugin can say it: what the bot drives, where that is and how it starts are the plugin's values in
 * the bot, which the host holds but does not read. The host asks every plugin, shows the least ready answer, and
 * asks again every few seconds while the bot's run window is open; a plugin with nothing to say answers
 * {@code null} through {@link Check}, and a host whose plugins all have nothing to say shows the bot ready.
 *
 * <p>Built by its three factories; there is no public constructor.
 */
public final class Readiness {

    /** How ready, least ready last: the host shows the last level any plugin answered. */
    public enum Level {
        /** Nothing stands in the way. */
        READY("Ready"),
        /** It can start, but something will be waited for first: a download, a launcher starting. */
        WAITING("Waiting"),
        /** It cannot start until the user does something, which the reason says. */
        BLOCKED("Not ready");

        private final String displayName;

        Level(String displayName) {
            this.displayName = displayName;
        }

        /** The word the host shows before the reason. */
        public String displayName() {
            return displayName;
        }
    }

    /**
     * What a plugin answers when the host asks. A method reference, by the contract's rule:
     * {@code .readiness(MyReadiness::now)}.
     *
     * <p>Called off the JavaFX application thread, every few seconds while a run window is open, so it may read
     * files or list windows but must return within a second; it links no JavaFX. It reads the open project from
     * the services {@code projectOpened} handed the plugin. {@code null} means nothing to say; a check that
     * throws costs its own answer only.
     */
    @FunctionalInterface
    public interface Check {
        Readiness now();
    }

    private final Level level;
    private final String reason;

    private Readiness(Level level, String reason) {
        this.level = level;
        this.reason = reason == null ? "" : reason.strip();
    }

    /** Ready, and why: {@code "game window found"}. A blank reason shows the word alone. */
    public static Readiness ready(String reason) {
        return new Readiness(Level.READY, reason);
    }

    /** It can start, after a wait the reason names: {@code "Steam is updating the game"}. */
    public static Readiness waiting(String reason) {
        return new Readiness(Level.WAITING, reason);
    }

    /** It cannot start until the user acts, as the reason says: {@code "signed out of Steam"}. */
    public static Readiness blocked(String reason) {
        return new Readiness(Level.BLOCKED, reason);
    }

    public Level level() {
        return level;
    }

    /** The reason, stripped; empty when none was given. */
    public String reason() {
        return reason;
    }

    /** The line a host shows: {@code "Ready — game window found"}, or the word alone. */
    public String line() {
        return reason.isEmpty() ? level.displayName() : level.displayName() + " — " + reason;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Readiness other && level == other.level && reason.equals(other.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, reason);
    }

    @Override
    public String toString() {
        return line();
    }
}
