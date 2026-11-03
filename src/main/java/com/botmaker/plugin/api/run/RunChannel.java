package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.PluginDeclaration;
import com.botmaker.plugin.api.Runs;
import com.botmaker.plugin.api.TraceLine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * What a plugin's code inside a running bot hands the host: its lines, its facts of the run, its own telemetry
 * frames and its questions to the person running the bot (spec {@code
 * docs/superpowers/specs/2026-11-03-run-reports-design.md} §7). The host in the bot's process implements it and
 * hands each plugin its own by {@link PluginDeclaration#onBotRun}, before the bot's {@code main} runs.
 *
 * <p><b>Why a channel, not a library.</b> Where a run's lines go (the console, the run's log file, the host's Trace
 * tab), which line of the bot a line belongs to, and how a frame reaches the host's window are the host's. A plugin
 * that wrote to them through a library of its own would tie every bot to that library's version, and only the
 * plugin that owned it could do it. Through the channel every plugin has the same path, and the host changes its
 * own end freely.
 *
 * <p><b>Implemented by the host only</b>; a plugin calls it. Every member is {@code default}, so a member added
 * later reaches a host built before it as its default: a line printed to the console, the rest dropped, debugging
 * off and no switch kept. A run with no host (a bot started from an IDE or a plain {@code java}) gets
 * {@link #console()}, which adds the switch.
 */
public interface RunChannel {

    /**
     * What a run with no host gets: lines printed to the console (an error, and what was thrown, to standard error;
     * the rest to standard output; a repeated line with {@code (×count)}), debug lines while
     * {@linkplain #debug() debug} is on, nothing kept, frames and details dropped, and no question asked here. Debugging starts as
     * {@link Runs#DEBUG} says, on when it says nothing. A new channel each call: each holds its own switch.
     */
    static RunChannel console() {
        return new ConsoleChannel(Runs.DEBUG.read(System.getProperty(Runs.DEBUG.name())).orElse(Boolean.TRUE));
    }

    /**
     * One line of the run. The plugin gives what it knows ({@code at}, level, source, text, count, writer class and
     * method, the screen part); the host fills in the bot's own class and line when the plugin left them empty.
     *
     * @param line  the line; a {@link TraceLine.Level#DEBUG} one is dropped unless {@link #recorded()}
     * @param cause what went wrong, printed after the line; {@code null} when nothing was thrown
     */
    default void line(TraceLine line, Throwable cause) {
        if (line == null) return;
        if (line.level() == TraceLine.Level.DEBUG && !recorded()) return;
        String printed = line.source().isEmpty() ? line.text() : "[" + line.source() + "] " + line.text();
        if (line.count() > 1) printed = printed + "  (×" + line.count() + ")";
        boolean error = line.level() == TraceLine.Level.ERROR;
        (error ? System.err : System.out).println(printed);
        if (cause != null) cause.printStackTrace(System.err);
    }

    /**
     * The plugin's facts of this run in its own words ({@code flow}, {@code platform}), kept in the run's log beside
     * the facts the host knows. Opaque to the host; a later call replaces what the plugin said before.
     */
    default void details(Map<String, String> details) {
    }

    /**
     * One frame of the plugin's own telemetry, which the host's window hands back whole to the same plugin through
     * {@link Runs#onTelemetry}: the bot-side twin of it. What the bytes mean is the plugin's alone.
     */
    default void frame(byte[] frame) {
    }

    /** Whether debug lines are shown now: the run's switch, which {@link #debug(boolean)} turns. */
    default boolean debug() {
        return false;
    }

    /** Turns debug lines on or off for the rest of the run. */
    default void debug(boolean on) {
    }

    /**
     * Whether a debug line given now is kept anywhere: shown while {@link #debug()} is on, or kept by the host for
     * the lines it writes before a problem. A plugin builds no debug line when this is {@code false}.
     */
    default boolean recorded() {
        return debug();
    }

    /**
     * Asks the person running the bot and waits for the answer, as the host asks (a dialog).
     *
     * @return the raw answer, or empty when this host cannot ask (no host: the plugin asks on its own, such as on
     *         the console)
     * @throws CancellationException when the person cancelled, or the run stopped while asking
     */
    default Optional<String> ask(AskKind kind, String prompt, List<String> choices) {
        return Optional.empty();
    }
}
