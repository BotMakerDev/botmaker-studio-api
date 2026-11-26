// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.source.ManagedParam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What several runs at once are dealt for one of your plugin's own parameters (2026-11-15): the values this PC has
 * for it, so no two live runs get the same one, but where {@link #sharing} lets several hold it. Named by a
 * {@link com.botmaker.plugin.api.PoolHolder}.
 *
 * <pre>{@code
 * public static final PluginPool<Instance> INSTANCES = PluginPool.fills(SdkValues.INSTANCE)
 *         .from(SdkPools::instances)            // this PC's instances, from hostValues()
 *         .narrowedBy(SdkPools::linked)         // those the row's earlier picks leave
 *         .offering(SdkPools::offers);          // "Create 2 instances"
 * }</pre>
 *
 * <p><b>The bot writes nothing.</b> A bot's own pool is Java in its {@code @Pools} class
 * ({@link com.botmaker.plugin.api.params.Pool}); yours is this declaration, and the parameter stays the single
 * run's value the bot already has. With one run nothing is dealt and that value is used.
 *
 * <p><b>Every candidate is a value the host can write</b>: of the parameter's type, written by the types your
 * plugin declares, as the parameter's own value is. The host deals each run one, holds it for every bot on this PC
 * while the run lives, and hands it to the run as that parameter's value.
 *
 * <p><b>The host calls these when it deals</b>, with or without a bot open and with no JavaFX, on a thread of its
 * own: read your data of this PC through the {@link StudioServices#hostValues()} handed in, start nothing, and
 * answer fast. A call that throws fails the dealing with your plugin named, and no run starts.
 *
 * <p>Immutable: every step returns a new pool. Keep each in a {@code static final} field; there is no public
 * constructor.
 *
 * @param <T> the parameter's class
 */
public final class PluginPool<T> {

    /** Gives a pool's candidates: every value this PC has for the parameter, free or not, in the order to deal. */
    @FunctionalInterface
    public interface Candidates<T> {

        /**
         * The candidates now. {@code null} reads as none; a {@code null} element, one given twice and one that is
         * not of the parameter's type are dropped.
         *
         * @param services the host's services; {@link StudioServices#hostValues()} is your data of this PC
         */
        List<T> candidates(StudioServices services);
    }

    /** Leaves, of a pool's candidates, those that go with what the row already holds. */
    @FunctionalInterface
    public interface Narrowing<T> {

        /**
         * The candidates that can join a row holding {@code picks}, in the order to try them. Anything answered
         * that is not one of {@code candidates} is dropped; {@code null} reads as none, which sends the dealing
         * back to try another pick before this pool.
         *
         * @param candidates what {@link Candidates} gave, less what other live runs hold
         * @param picks      what the row was dealt before this pool
         * @param services   the host's services
         */
        List<T> left(List<T> candidates, Picks picks, StudioServices services);
    }

    /** Gives the ways to have more candidates, when fewer runs can start than were asked for. */
    @FunctionalInterface
    public interface Offers {

        /**
         * The offers for {@code shortfall}, the likeliest first; none when nothing more fits on this PC. Asking
         * changes nothing: an offer acts only when the user picks it.
         */
        List<PoolOffer> offers(Shortfall shortfall, StudioServices services);
    }

    /**
     * What one row was dealt before a pool: the picks of the pools ahead of it. Plugins' pools are dealt before the
     * bot's, a plugin's after those of the plugins it depends on, and a plugin's own in the order it names them.
     * Implemented by the host.
     */
    public interface Picks {

        /** A row with no pick yet. */
        Picks NONE = new Picks() {
            @Override
            public <V> Optional<V> of(ManagedParam<V> parameter) {
                return Optional.empty();
            }
        };

        /**
         * The row's pick for {@code parameter}, yours or one of a plugin yours depends on; empty when no pool
         * ahead of this one fills it.
         */
        <V> Optional<V> of(ManagedParam<V> parameter);
    }

    /** Says how many live runs may hold one candidate at once (2026-11-27). */
    @FunctionalInterface
    public interface Sharing<T> {

        /** As many runs as ask: a candidate with no limit, such as a desktop several games play on. */
        int ANY = Integer.MAX_VALUE;

        /** How many runs may hold {@code candidate}; under {@code 1} reads as {@code 1}, {@link #ANY} as no limit. */
        int runs(T candidate);
    }

    /** How far a pool falls short of the runs asked for. Implemented by the host. */
    public interface Shortfall {

        /** How many runs were asked for. */
        int asked();

        /** How many more of this pool's candidates those runs need: at least one. */
        int missing();
    }

    private final ManagedParam<T> parameter;
    private final Candidates<T> candidates;
    private final Narrowing<T> narrowing;
    private final Offers offers;
    private final Sharing<T> sharing;

    private PluginPool(ManagedParam<T> parameter, Candidates<T> candidates, Narrowing<T> narrowing, Offers offers) {
        this(parameter, candidates, narrowing, offers, null);
    }

    private PluginPool(ManagedParam<T> parameter, Candidates<T> candidates, Narrowing<T> narrowing, Offers offers,
                       Sharing<T> sharing) {
        this.sharing = sharing;
        this.parameter = parameter;
        this.candidates = candidates;
        this.narrowing = narrowing;
        this.offers = offers;
    }

    /**
     * A pool for {@code parameter}, one your plugin declares among its values. A plugin has one pool a parameter.
     *
     * @throws NullPointerException for no parameter
     */
    public static <T> Steps<T> fills(ManagedParam<T> parameter) {
        return new Steps<>(Objects.requireNonNull(parameter, "parameter"));
    }

    /** After {@link #fills}: where the candidates come from. */
    public static final class Steps<T> {

        private final ManagedParam<T> parameter;

        private Steps(ManagedParam<T> parameter) {
            this.parameter = parameter;
        }

        /**
         * The candidates, asked each time the host deals: {@code .from(SdkPools::instances)}.
         *
         * @throws NullPointerException for none
         */
        public PluginPool<T> from(Candidates<T> candidates) {
            return new PluginPool<>(parameter, Objects.requireNonNull(candidates, "candidates"), null, null);
        }
    }

    /**
     * This pool, its candidates narrowed by what the row already holds. Without it every free candidate goes with
     * any row.
     *
     * @throws NullPointerException for none
     */
    public PluginPool<T> narrowedBy(Narrowing<T> narrowing) {
        return new PluginPool<>(parameter, candidates, Objects.requireNonNull(narrowing, "narrowing"), offers, sharing);
    }

    /**
     * This pool, with ways to have more candidates. Without it the host says how many are missing and offers
     * nothing.
     *
     * @throws NullPointerException for none
     */
    public PluginPool<T> offering(Offers offers) {
        return new PluginPool<>(parameter, candidates, narrowing, Objects.requireNonNull(offers, "offers"), sharing);
    }

    /**
     * This pool, some of its candidates held by several live runs at once: {@code .sharing(place -> place.isDesktop()
     * ? Sharing.ANY : 1)} for a desktop many runs play on. Without it each candidate goes to one run.
     *
     * @throws NullPointerException for none
     */
    public PluginPool<T> sharing(Sharing<T> sharing) {
        return new PluginPool<>(parameter, candidates, narrowing, offers, Objects.requireNonNull(sharing, "sharing"));
    }

    /**
     * How many live runs may hold {@code candidate} at once, as the host reads it: {@code 1} without a
     * {@link #sharing}; else what it answers, {@code 1} for an answer under one or for a sharing that throws.
     */
    public int sharedBy(T candidate) {
        if (sharing == null) return 1;
        try {
            return Math.max(1, sharing.runs(candidate));
        } catch (RuntimeException | LinkageError e) {
            return 1;
        }
    }

    /** The parameter the pool fills. */
    public ManagedParam<T> parameter() {
        return parameter;
    }

    /** What {@link Candidates} gives now, as the host reads it: of the parameter's type, each once, none null. */
    public List<T> candidates(StudioServices services) {
        return sound(candidates.candidates(services), null);
    }

    /**
     * Those of {@code candidates} that go with {@code picks}, as the host reads them: all of them, in their order,
     * without a {@link #narrowedBy narrowing}; else what it answers, less anything that is no candidate.
     */
    public List<T> left(List<T> candidates, Picks picks, StudioServices services) {
        List<T> given = sound(candidates, null);
        if (narrowing == null) return given;
        return sound(narrowing.left(given, picks == null ? Picks.NONE : picks, services), given);
    }

    /** The offers for {@code shortfall}, none null: empty without {@link #offering}. */
    public List<PoolOffer> offers(Shortfall shortfall, StudioServices services) {
        if (offers == null) return List.of();
        List<PoolOffer> offered = offers.offers(Objects.requireNonNull(shortfall, "shortfall"), services);
        return offered == null ? List.of() : offered.stream().filter(Objects::nonNull).toList();
    }

    /** {@code answered} as values of the parameter's type, each once, and only those {@code among} holds, if any. */
    private List<T> sound(List<T> answered, List<T> among) {
        if (answered == null) return List.of();
        Set<T> allowed = among == null ? null : new HashSet<>(among);
        LinkedHashSet<T> kept = new LinkedHashSet<>();
        for (Object one : new ArrayList<Object>(answered)) {
            T typed = parameter.cast(one);
            if (typed != null && (allowed == null || allowed.contains(typed))) kept.add(typed);
        }
        return List.copyOf(kept);
    }

    @Override
    public String toString() {
        return "PluginPool[" + parameter.id() + "]";
    }
}
