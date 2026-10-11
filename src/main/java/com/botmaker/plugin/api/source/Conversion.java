package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.value.Ref;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How a release of this plugin carries a bot's managed values from a shape it no longer writes to the one it
 * does — declared by the plugin, run by the host when the bot upgrades the plugin (2026-10-11).
 *
 * <p>The host never invents a migration: what an old value means in the new shape is something only the plugin
 * knows. So a release that renames a managed value's id, moves its holder or reshapes an open set keeps the old
 * declaration {@link ManagedValue#retired() retired} and declares the step from it:
 *
 * <pre>{@code
 * public static final Conversion ACTIVITIES_FROM_OUTCOMES = Conversion.from(SdkValues.OUTCOMES, SdkValues.FLOW_V1)
 *         .to(SdkValues.ACTIVITIES, SdkValues.FLOW)
 *         .by(SdkConversions::activities)          // Converted activities(Converting old)
 *         .because("Each activity is now its own enum of outcomes.");
 * }</pre>
 *
 * <p><b>When it runs.</b> During a plugin upgrade, after the snapshot and before the bot's calls are repaired: the
 * host reads each {@code from} value the bot holds through the new jar's declarations, calls {@code by}, writes
 * each {@code to} value it answers, and removes the retired holders and marked methods nothing else names. A
 * conversion that throws, or answers a value the host cannot write, refuses the whole upgrade with nothing
 * written. Many to many: one conversion may read several old values and write several new ones. Conversions run
 * in the order the plugin lists them, so a later one may read what an earlier one wrote.
 *
 * <p><b>What enforces it.</b> {@code botmaker plugin validate}: every retired value is the {@code from} of a
 * conversion, and, {@code --against} the previous release, every managed value whose holder, shape or type
 * changed or vanished is one.
 *
 * <p>A class, not a record, for the reason {@link ManagedValue} is one: only these steps build it.
 */
public final class Conversion {

    private final List<ManagedValue<?>> sources;
    private final List<ManagedValue<?>> targets;
    private final Ref.Of1<Converting, Converted> by;
    private final String reason;

    private Conversion(List<ManagedValue<?>> sources, List<ManagedValue<?>> targets,
                       Ref.Of1<Converting, Converted> by, String reason) {
        this.sources = sources;
        this.targets = targets;
        this.by = by;
        this.reason = reason;
    }

    /**
     * <b>The way to declare a conversion</b>, from the {@link ManagedValue#retired() retired} values it reads.
     *
     * @throws IllegalArgumentException when none is given, one is not retired, or one is given twice
     */
    public static ToStep from(ManagedValue<?>... retired) {
        List<ManagedValue<?>> sources = distinct(retired, "from");
        for (ManagedValue<?> value : sources) {
            if (!value.isRetired()) {
                throw new IllegalArgumentException(value + " is not retired(): a conversion reads only a shape the "
                        + "plugin no longer writes");
            }
        }
        return new ToStep(sources);
    }

    /** After {@link #from}: the values it writes. */
    public static final class ToStep {

        private final List<ManagedValue<?>> sources;

        private ToStep(List<ManagedValue<?>> sources) {
            this.sources = sources;
        }

        /**
         * Writing these values, each one the plugin declares now. A value read and a value written never share an
         * id: a retired shape keeps a marker constant of its own ({@code SdkValue.Id.FLOW_V1}), so a bot's old
         * holder and its new one are never the same name to the host.
         *
         * @throws IllegalArgumentException when none is given, one is retired, one is given twice, or one has the
         *                                  id of a value it reads
         */
        public ByStep to(ManagedValue<?>... values) {
            List<ManagedValue<?>> targets = distinct(values, "to");
            Set<String> read = new HashSet<>();
            sources.forEach(value -> read.add(value.id()));
            for (ManagedValue<?> value : targets) {
                if (value.isRetired()) {
                    throw new IllegalArgumentException(value + " is retired(): a conversion writes only a shape "
                            + "the plugin writes now");
                }
                if (read.contains(value.id())) {
                    throw new IllegalArgumentException(value.id() + " is both read and written: give the retired "
                            + "shape a marker constant of its own");
                }
            }
            return new ByStep(sources, targets);
        }
    }

    /** After {@link ToStep#to}: what does the converting. */
    public static final class ByStep {

        private final List<ManagedValue<?>> sources;
        private final List<ManagedValue<?>> targets;

        private ByStep(List<ManagedValue<?>> sources, List<ManagedValue<?>> targets) {
            this.sources = sources;
            this.targets = targets;
        }

        /**
         * Converted by {@code convert}, a method reference — {@code SdkConversions::activities} — from the old values
         * to the new ones.
         *
         * @throws IllegalArgumentException when {@code convert} is a lambda rather than a method reference
         */
        public Reason by(Ref.Of1<Converting, Converted> convert) {
            Ref.resolve(convert);
            return new Reason(sources, targets, convert);
        }
    }

    /** The last step: the sentence the upgrade report shows for it. */
    public static final class Reason {

        private final List<ManagedValue<?>> sources;
        private final List<ManagedValue<?>> targets;
        private final Ref.Of1<Converting, Converted> by;

        private Reason(List<ManagedValue<?>> sources, List<ManagedValue<?>> targets,
                       Ref.Of1<Converting, Converted> by) {
            this.sources = sources;
            this.targets = targets;
            this.by = by;
        }

        /** What changed, for the person upgrading: {@code "Each activity is now its own enum of outcomes."} */
        public Conversion because(String reason) {
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A conversion needs a reason");
            return new Conversion(sources, targets, by, reason);
        }
    }

    /** The retired values it reads, in the order given. */
    public List<ManagedValue<?>> sources() {
        return sources;
    }

    /** The values it writes, in the order given. */
    public List<ManagedValue<?>> targets() {
        return targets;
    }

    /** The sentence the upgrade report shows for it. */
    public String reason() {
        return reason;
    }

    /**
     * Runs the plugin's conversion over {@code old} — what the host does during an upgrade.
     *
     * @throws IllegalStateException when the conversion threw, answered nothing, or wrote a value its
     *                               {@link #targets()} do not declare in that shape and type; the host refuses
     *                               the upgrade with this sentence
     */
    public Converted convert(Converting old) {
        Converted written;
        try {
            written = by.call(old == null ? Converting.of(null) : old);
        } catch (RuntimeException | LinkageError e) {
            throw new IllegalStateException(this + " failed: " + e, e);
        }
        if (written == null) throw new IllegalStateException(this + " answered nothing; answer Converted.empty()");
        Map<String, ManagedValue<?>> declared = new HashMap<>();
        targets.forEach(value -> declared.put(value.id(), value));
        written.written().forEach((id, value) -> {
            ManagedValue<?> target = declared.get(id);
            if (target == null) {
                throw new IllegalStateException(this + " wrote " + id + ", which its to(…) does not name");
            }
            if (!fits(target, value)) {
                throw new IllegalStateException(this + " wrote " + id + " as " + value + ", which is not "
                        + (target.isOpenSet() ? "a set of " : "a ") + target.type().getName());
            }
        });
        return written;
    }

    /** Whether {@code value} is what {@code target} holds: one of its type, or for an open set a map of them. */
    private static boolean fits(ManagedValue<?> target, Object value) {
        if (!target.isOpenSet()) return target.cast(value) != null;
        return value instanceof Map<?, ?> members
                && members.values().stream().allMatch(member -> target.cast(member) != null);
    }

    private static List<ManagedValue<?>> distinct(ManagedValue<?>[] values, String step) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("A conversion's " + step + "(…) needs at least one value");
        }
        List<ManagedValue<?>> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (ManagedValue<?> value : values) {
            if (value == null) throw new IllegalArgumentException("A conversion's " + step + "(…) holds a null");
            if (!ids.add(value.id())) {
                throw new IllegalArgumentException(value.id() + " is given twice to " + step + "(…)");
            }
            list.add(value);
        }
        return List.copyOf(list);
    }

    @Override
    public String toString() {
        return "Conversion" + sources.stream().map(ManagedValue::id).toList() + " -> "
                + targets.stream().map(ManagedValue::id).toList();
    }
}
