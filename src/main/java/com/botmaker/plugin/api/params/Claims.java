package com.botmaker.plugin.api.params;

import java.util.Objects;

/**
 * A value only a running bot knows, held so no other live run takes it (2026-11-15, spec
 * {@code docs/superpowers/specs/2026-11-15-run-claims-design.md} §6): a random name, an id read from the game.
 *
 * <pre>{@code
 * String name = Names.random();
 * while (!Claims.claim("guild:" + name)) name = Names.random();
 * ...
 * Claims.release("guild:" + name);            // or left held until this run's process ends
 * }</pre>
 *
 * <p>What a run can know before it starts is a {@link Pool}'s, dealt by the host ahead; a claim is for what it cannot,
 * so the host never counts claims into how many runs fit. Every claim of a run is freed when its process ends, a crash
 * included.
 *
 * <p><b>Served by the run's agent.</b> Before the bot's {@code main}, the agent of a run the host started
 * {@linkplain #serve installs} the service that checks this computer's live runs. A run with none (a test, a bot
 * started by a plain {@code java}) has no other run to meet, so every claim is granted there.
 */
public final class Claims {

    /**
     * What answers the claims: the host's, installed by the run's agent. Implemented by the host only, so a member
     * added later is abstract and a host that forgets one does not compile, rather than granting every claim.
     */
    public interface Service {

        /** Holds {@code key} for this run; {@code false} when another live run holds it. */
        boolean claim(String key);

        /** Frees {@code key} before this run ends; nothing when this run does not hold it. */
        void release(String key);
    }

    /** A run with no agent: no other run to meet. */
    private static final Service GRANTED = new Service() {
        @Override
        public boolean claim(String key) {
            return true;
        }

        @Override
        public void release(String key) {
        }
    };

    private static volatile Service service;

    private Claims() {}

    /**
     * Holds {@code key} for this run, so no other live run on this computer holds it at the same time. A key this run
     * already holds is granted again.
     *
     * @return {@code false} when another live run holds it
     * @throws IllegalArgumentException for a blank key
     */
    public static boolean claim(String key) {
        return current().claim(checked(key));
    }

    /**
     * Frees {@code key} before this run ends, for another run to claim. A key this run does not hold is left as it is.
     *
     * @throws IllegalArgumentException for a blank key
     */
    public static void release(String key) {
        current().release(checked(key));
    }

    /**
     * Installs the service that answers this run's claims. For the run's agent, before the bot's {@code main}: the
     * first service installed stays for the whole run.
     *
     * @throws IllegalStateException when a service is already installed
     */
    public static synchronized void serve(Service served) {
        Objects.requireNonNull(served, "service");
        if (service != null) throw new IllegalStateException("This run's claims are already served");
        service = served;
    }

    private static Service current() {
        Service served = service;
        return served == null ? GRANTED : served;
    }

    private static String checked(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("A claim's key is blank");
        return key;
    }
}
