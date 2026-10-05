package com.botmaker.plugin.api;

import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * The open project's bot, as a running process — start it, stop it, and hear about it.
 *
 * <p>Every member here passes the host-only test on {@link StudioServices} for the same reason: the host
 * compiled the project, holds its resolved classpath and owns the process it launched. A plugin cannot start
 * a bot, cannot learn that one exited, and cannot discover a pid it never spawned. It is a
 * <b>capability</b> — nothing in this interface names a concept belonging to any plugin's API.
 *
 * <h2>Telemetry crosses as its own wire bytes, and that is the whole reason it can cross at all</h2>
 *
 * <p>{@link #onTelemetry} hands over <em>one encoded frame</em>, and this module has no idea what is in it.
 * The format belongs to whichever runtime the bot is built on, and that runtime decodes its own wire: a
 * decoded shape crossing here would be a vocabulary, which is exactly what the contract refuses to carry. A host
 * that relays a frame it cannot read is doing its job.
 *
 * <p>Bytes rather than text because the wire already <em>is</em> bytes and has exactly one definition. A
 * text rendering would be a second encoding of the same thing, owned by neither end and drifting from
 * both.
 *
 * <h2>Nothing here is guaranteed to do anything</h2>
 *
 * <p>{@link #NONE} is what a host that does not run bots answers, and it is total: no run ever starts, no
 * listener ever fires, and unregistering is a no-op. A plugin therefore never null-checks and never asks
 * whether the host supports running — it registers, and a host with nothing to say says nothing.
 */
public interface Runs {

    /**
     * Requests a run of the open project, exactly as the host's own Run action does — the same compile, the
     * same classpath, the same process.
     *
     * <p>Asynchronous and advisory. It returns before the bot has started, may be refused (a project that
     * does not compile, a run already in flight), and the only honest way to learn what happened is
     * {@link #onStateChanged}.
     */
    void start();

    /** Requests that a running bot stop. Advisory and asynchronous, exactly like {@link #start()}. */
    void stop();

    /** Whether a bot is running right now. A moment later it may not be. */
    boolean isRunning();

    /**
     * The running bot's process id, or empty when none is running.
     *
     * <p>For the things only a pid can do — a signal, a priority, a process-level probe. A plugin that acts
     * on it owns what that does to somebody's bot: {@code SIGSTOP} freezes it wherever it happens to be, and
     * any wall-clock timing the bot keeps skews across the pause.
     */
    OptionalLong pid();

    /**
     * Registers {@code listener} for run state — {@code true} when a bot starts, {@code false} when one
     * stops — and returns the way to unregister it.
     *
     * <p>Closing the returned handle is required rather than tidy: a plugin outlives every project bound to
     * it, so a listener left registered accumulates one copy per project opened. Close it in
     * {@link StudioPlugin#projectClosing()}, which exists for exactly this kind of thing.
     *
     * <p>The thread is the host's and is not promised. A listener that touches UI must hop to the JavaFX
     * application thread itself.
     */
    AutoCloseable onStateChanged(Consumer<Boolean> listener);

    /**
     * Registers {@code listener} for the running bot's telemetry, one encoded frame at a time, and returns
     * the way to unregister it.
     *
     * <p>The frame is opaque here — see the note above on why it is bytes. A host with no telemetry to relay
     * simply never calls the listener, which is indistinguishable from a bot that writes none, and is meant
     * to be.
     *
     * <p><b>The array is not the listener's to modify.</b> One frame is delivered to every registered
     * listener, so a listener that writes into it corrupts what the next one reads. Read it, decode it, and
     * do not keep it: nothing promises it stays valid after the call returns.
     *
     * <p>Same two rules as {@link #onStateChanged}: close the handle in
     * {@link StudioPlugin#projectClosing()}, and assume nothing about the thread.
     */
    AutoCloseable onTelemetry(Consumer<byte[]> listener);

    /** A host that runs no bots: nothing starts, nothing fires, and unregistering does nothing. */
    Runs NONE = new Runs() {

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public OptionalLong pid() {
            return OptionalLong.empty();
        }

        @Override
        public AutoCloseable onStateChanged(Consumer<Boolean> listener) {
            return () -> {
            };
        }

        @Override
        public AutoCloseable onTelemetry(Consumer<byte[]> listener) {
            return () -> {
            };
        }
    };

    /**
     * The value of the system property every run of this bot <b>on this machine</b> is started with, or
     * {@code null} when none is set.
     *
     * <p>For a fact about running the bot here rather than about the bot — which game this computer launches,
     * say — so it must never be committed with the project, never be published with it, and still reach the
     * bot's process. The host keeps it beside the project in its own git-excluded state and passes it to every
     * run and debug session as {@code -D<name>=<value>}; a bot run any other way passes its own {@code -D}.
     * Host-only because the host owns both halves: the checkout's local state and the process it launches.
     *
     * <p>The name is a system property's, and a plugin names only its own: {@code botmaker.launch.target}
     * belongs to the plugin whose runtime reads it. A host that runs no bots keeps nothing and answers
     * {@code null}.
     */
    default String property(String name) {
        return null;
    }

    /**
     * Sets {@link #property(String)} for every later run on this machine; {@code null} or blank clears it. A run
     * already in flight keeps what it started with. A host that runs no bots ignores it.
     */
    default void setProperty(String name, String value) {
    }

    /**
     * The run property that turns a bot's debug output on: every run on this machine is started with
     * {@code -Dbotmaker.debug=true} while the host's own toggle is on, and {@code false} while it is off.
     * Unset means the bot decides for itself.
     *
     * <p>The one property the host names rather than a plugin, because the toggle is the host's: showing a run's
     * log is what hosting a run is. A plugin reads and sets it through {@link #property}/{@link #setProperty}
     * like any other; a runtime that has no debug output ignores it.
     */
    String DEBUG_PROPERTY = "botmaker.debug";

    /**
     * Registers {@code listener} for the running bot's trace, one {@link TraceLine} at a time, and returns the
     * way to unregister it.
     *
     * <p>The host reads each line off the bot's own output channel and hands over what it read; a line this
     * host could not read is not delivered. Unlike {@link #onTelemetry}, the value is a shape — see
     * {@link TraceLine} on why a log line is a capability rather than a vocabulary. A host that shows no trace
     * never calls the listener, which is what this default does.
     *
     * <p>Same two rules as {@link #onStateChanged}: close the handle in {@link StudioPlugin#projectClosing()},
     * and assume nothing about the thread.
     */
    default AutoCloseable onTrace(Consumer<TraceLine> listener) {
        return () -> {
        };
    }
}
