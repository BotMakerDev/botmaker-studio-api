package com.botmaker.plugin.api.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A part of a call the host holds as something other than what the factory takes: a class literal of a managed
 * enum, which crosses as the plugin's value of that enum; a method reference, which crosses as the text it is
 * written as; a map whose stray entries are dropped rather than refusing the whole value.
 *
 * <pre>{@code
 * static final Held<Flow.Step, Activity> ACTIVITY = Held.as(Activity.class, Flow.Step::activity);
 * static final Held<Flow.Step, String> BODY = Held.as(String.class, step -> sourceOf(step.body()));
 *
 * static final DeclaredCall<Flow.Step> STEP = ComponentType.part(Flow.Step.class)
 *         .<Class<Outcome>, ActivityBody<Outcome>, Flow.Step>writtenAs(Flow::activity, ACTIVITY.part(), BODY.part())
 *         .builtFrom(parts -> step(parts.get(ACTIVITY), parts.get(BODY)));
 * }</pre>
 *
 * <p><b>Why a declaration and not a list.</b> A call whose parts the factory cannot take used to be taken apart by
 * a {@code components} function answering {@code List<Object>} and built back by unpacking one with
 * {@code parts.get(2) instanceof Activity …}: positions counted by hand, and a part of the wrong kind found only by
 * the unpacking that forgot to check it. A held part names its kind once; the host checks every part read against
 * it before anything is built, and {@link Parts#get} hands each back as itself.
 *
 * <p><b>Three ways the value is built back.</b>
 *
 * <ul>
 *   <li>{@link #part()}: the factory takes what the host holds, a map or a list it checked.</li>
 *   <li>{@link #part(Function)}: the factory takes {@code give} of it, {@code Gamebot::goHome} from its text.</li>
 *   <li>{@link DeclaredCall#builtFrom}: the factory cannot be called on anything the host holds (it takes a
 *       class literal), so the plugin builds the value from the parts.</li>
 * </ul>
 *
 * <p>A part read as anything else is not this call: the source stays as written. Identity matters: a held part is
 * a constant, declared once and named both where the call is declared and where {@link Parts#get} reads it.
 *
 * <p>New in 0.8.2: a plugin declaring one needs a host on 0.8.2 or later, whose contract has this class.
 *
 * @param <T> the class the call builds
 * @param <H> what the host holds this part as
 */
public final class Held<T, H> {

    private final String kind;
    private final Function<? super T, ? extends H> take;
    /** The host's part as an {@code H}, or {@link CallShape#REFUSED} when it is none. */
    private final Function<Object, Object> read;

    private Held(String kind, Function<? super T, ? extends H> take, Function<Object, Object> read) {
        this.kind = kind;
        this.take = Objects.requireNonNull(take, "take");
        this.read = read;
    }

    /**
     * A part the host holds as an {@code H}, which {@code take} reads off a value. A part is read as a plain
     * accessor's is: a number widens or rounds to {@code H}, and {@code null} is an {@code H} unless {@code H} is
     * primitive.
     */
    public static <T, H> Held<T, H> as(Class<H> held, Function<? super T, ? extends H> take) {
        Objects.requireNonNull(held, "held");
        return new Held<>(held.getName(), take, part -> CallShape.coerce(part, held));
    }

    /**
     * A list the host holds every element of as an {@code E}, each read as {@link #as} reads a part. One element
     * of another kind refuses the part: a list's elements are its meaning in order, and building it without one
     * would drop it from the bot.
     */
    public static <T, E> Held<T, List<E>> list(Class<E> element, Function<? super T, ? extends List<E>> take) {
        Objects.requireNonNull(element, "element");
        return new Held<>("List<" + element.getName() + ">", take, part -> {
            if (!(part instanceof List<?> given)) return CallShape.REFUSED;
            List<Object> out = new ArrayList<>(given.size());
            for (Object each : given) {
                Object read = CallShape.coerce(each, element);
                if (read == CallShape.REFUSED) return CallShape.REFUSED;
                out.add(read);
            }
            return Collections.unmodifiableList(out);
        });
    }

    /**
     * A map the host holds keyed by {@code K} to {@code V}, each key and value read as {@link #as} reads a part,
     * keeping the entries of those kinds and dropping a stray one: each entry stands alone, so one the host could
     * not read costs that entry and not the value.
     */
    public static <T, K, V> Held<T, Map<K, V>> map(Class<K> key, Class<V> value,
                                                    Function<? super T, ? extends Map<K, V>> take) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return new Held<>("Map<" + key.getName() + ", " + value.getName() + ">", take, part -> {
            if (!(part instanceof Map<?, ?> given)) return CallShape.REFUSED;
            Map<Object, Object> kept = new LinkedHashMap<>();
            given.forEach((k, v) -> {
                Object readKey = CallShape.coerce(k, key);
                Object readValue = CallShape.coerce(v, value);
                if (readKey != CallShape.REFUSED && readValue != CallShape.REFUSED) kept.put(readKey, readValue);
            });
            return Collections.unmodifiableMap(kept);
        });
    }

    /**
     * This part where a {@code writtenAs} or {@code with} takes an accessor: the host reads it as an {@code H}, and
     * the factory or wither is handed it as it is. {@code A} is the parameter it stands at; when it is not what the
     * host holds, the call is built by {@link DeclaredCall#builtFrom} instead.
     */
    public <A> Function<T, A> part() {
        return new Part<>(this, null);
    }

    /** This part, handed to the factory or wither as {@code give} of what the host holds: its text to a method. */
    public <A> Function<T, A> part(Function<? super H, ? extends A> give) {
        return new Part<>(this, Objects.requireNonNull(give, "give"));
    }

    /** What {@code value} holds here, as the host writes it. */
    public H take(T value) {
        return take.apply(value);
    }

    /** The host's part as an {@code H}, or {@link CallShape#REFUSED}. */
    Object read(Object part) {
        return read.apply(part);
    }

    @SuppressWarnings("unchecked")
    Object takeAny(Object value) {
        return take((T) value);
    }

    /** What {@code accessor} reads off {@code value}: a held part's {@link #take}, else the accessor's answer. */
    @SuppressWarnings("unchecked")
    static Object taken(Function<?, ?> accessor, Object value) {
        if (accessor instanceof Part<?, ?, ?> held) return held.held.takeAny(value);
        return ((Function<Object, ?>) accessor).apply(value);
    }

    /**
     * What a factory or wither is handed for the host's {@code part} at {@code accessor}'s place: the part itself
     * for a plain accessor, a held part's read and {@code give} for a held one, or {@link CallShape#REFUSED}.
     */
    static Object handed(Function<?, ?> accessor, Object part) {
        return accessor instanceof Part<?, ?, ?> held ? held.handed(part) : part;
    }

    /** The held part at {@code accessor}'s place, or {@code null} for a plain accessor. */
    static Held<?, ?> at(Function<?, ?> accessor) {
        return accessor instanceof Part<?, ?, ?> held ? held.held : null;
    }

    @Override
    public String toString() {
        return "Held[" + kind + "]";
    }

    /** A held part standing at one place of a call: {@link #part()}. Read by the host, never called. */
    static final class Part<T, H, A> implements Function<T, A> {

        final Held<T, H> held;
        private final Function<? super H, ? extends A> give;

        Part(Held<T, H> held, Function<? super H, ? extends A> give) {
            this.held = held;
            this.give = give;
        }

        /**
         * Refused: a held part is read by the host, which takes {@link Held#take} of a value. Called directly it
         * could only answer the factory's argument, which is not what the host compares or writes.
         */
        @Override
        public A apply(T value) {
            throw new UnsupportedOperationException(held + " is a held part: read it with take(value)");
        }

        @SuppressWarnings("unchecked")
        Object handed(Object part) {
            Object read = held.read(part);
            if (read == CallShape.REFUSED || give == null) return read;
            try {
                return give.apply((H) read);
            } catch (RuntimeException refused) {
                return CallShape.REFUSED;
            }
        }
    }

    /**
     * The parts of a call the host read, each checked against its held part: what {@link DeclaredCall#builtFrom}
     * builds a value from. Built by the host, never by hand.
     */
    public static final class Parts {

        private final List<Held<?, ?>> at;
        private final List<Object> values;

        Parts(List<Held<?, ?>> at, List<Object> values) {
            this.at = at;
            this.values = values;
        }

        /**
         * The part {@code part} stands for, as the host holds it.
         *
         * @throws IllegalArgumentException for a held part this call is not written with
         */
        @SuppressWarnings("unchecked")
        public <H> H get(Held<?, H> part) {
            for (int i = 0; i < at.size(); i++) {
                if (at.get(i) == part) return (H) values.get(i);
            }
            throw new IllegalArgumentException(part + " is not a part of this call");
        }
    }
}
