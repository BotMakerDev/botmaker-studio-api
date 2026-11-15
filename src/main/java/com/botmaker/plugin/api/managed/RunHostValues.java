package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.source.ManagedValue;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Your plugin's values of this PC ({@link HostHolder}) as a run reads and writes them: what a run learns of this
 * computer while it plays, kept for the next run, such as when each action was last done on an instance.
 *
 * <pre>{@code
 * Optional<Done> done = RunHostValues.read(MyHostValues.DONE);
 * RunHostValues.update(MyHostValues.DONE, kept -> kept.with(mine)).ifPresent(why -> log("not kept: " + why));
 * }</pre>
 *
 * <p>Only your plugin's run-time half reaches its own values: a call from any other jar, the bot's own code
 * included, is refused. A write lands in the value's class at once, under the folder's lock, and is not checked by a
 * compile; the host commits what a run changed once, when the run ends, so one run is one step of the values' undo.
 * Several runs at once share one folder: {@link #update} reads and writes under the lock, so what another run wrote
 * in between is never lost.
 *
 * <p>With no host serving them (a run BotMaker did not start, a test), every read is empty and every write is
 * refused with why: a run keeps what it learned in memory then.
 *
 * <p><b>Not for a secret</b>, as every value of this PC.
 */
public final class RunHostValues {

    /**
     * What the host installs before a run's {@code main}; never a plugin's to implement.
     */
    public interface Service {

        /** {@code value} as this PC keeps it, for {@code caller}; empty when not kept, unreadable or refused. */
        <T> Optional<T> read(Class<?> caller, ManagedValue<T> value);

        /**
         * {@code value} set, for {@code caller}, to what {@code change} makes of it as kept now (its default when not
         * kept yet), read and written under the folder's lock: empty when written, else why not.
         */
        <T> Optional<String> update(Class<?> caller, ManagedValue<T> value, UnaryOperator<T> change);
    }

    private static final StackWalker CALLERS = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static volatile Service service;

    private RunHostValues() {}

    /** {@code value} as this PC keeps it; empty when it is not kept yet, cannot be read, or is not your plugin's. */
    public static <T> Optional<T> read(ManagedValue<T> value) {
        Objects.requireNonNull(value, "value");
        Service serving = service;
        return serving == null ? Optional.empty() : serving.read(CALLERS.getCallerClass(), value);
    }

    /** Sets {@code value} to {@code newValue} on this PC: empty when written, else why not. */
    public static <T> Optional<String> write(ManagedValue<T> value, T newValue) {
        return change(CALLERS.getCallerClass(), value, kept -> newValue);
    }

    /**
     * Sets {@code value} on this PC to what {@code change} makes of it as kept now, its default when not kept yet; read
     * and written under the folder's lock, so a change another run made in between is kept. Empty when written, else
     * why not. {@code change} may run more than once: give it no side effect.
     */
    public static <T> Optional<String> update(ManagedValue<T> value, UnaryOperator<T> change) {
        Objects.requireNonNull(change, "change");
        return change(CALLERS.getCallerClass(), value, change);
    }

    private static <T> Optional<String> change(Class<?> caller, ManagedValue<T> value, UnaryOperator<T> change) {
        Objects.requireNonNull(value, "value");
        Service serving = service;
        if (serving == null) return Optional.of("BotMaker keeps no values of this PC for this run.");
        return serving.update(caller, value, change);
    }

    /** Installs the host's service for this run; {@code null} takes it away. Called by the host only. */
    public static void serve(Service serving) {
        service = serving;
    }
}
