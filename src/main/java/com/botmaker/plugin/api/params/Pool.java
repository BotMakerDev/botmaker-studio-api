package com.botmaker.plugin.api.params;

import com.botmaker.plugin.api.value.Ref;

import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The elements several runs of a bot are dealt from, for one parameter (2026-11-15, spec
 * {@code docs/superpowers/specs/2026-11-15-run-claims-design.md} §1): a method of a {@link Pools} class returns one.
 *
 * <pre>{@code
 * Pool.of(GameAccount.MAIN, GameAccount.ALT_1, GameAccount.ALT_2).fills(Farm::account).rotate()
 * Pool.all(Hero.class).take(3).fills(Farm::team)          // a Pool<List<Hero>>: three heroes a run
 * }</pre>
 *
 * <p><b>Read by the host as values.</b> Each call is one the value grammar reads, so the host deals rows from the
 * source without running it: elements are literals, constants, or records of those. Built here, a pool only checks
 * what it is given and says it back, for tests and for the run's agent.
 *
 * <p>Immutable: every step returns a new pool. There is no public constructor.
 *
 * @param <T> what the filled parameter returns: an element, or after {@link #take} a {@code List} of them
 */
public final class Pool<T> {

    private final List<Object> elements;
    private final Class<?> all;
    private final Method fills;
    private final int take;
    private final int each;
    private final boolean rotates;

    private Pool(List<Object> elements, Class<?> all, Method fills, int take, int each, boolean rotates) {
        this.elements = elements;
        this.all = all;
        this.fills = fills;
        this.take = take;
        this.each = each;
        this.rotates = rotates;
    }

    /**
     * These elements, in this order: what the runs are dealt from.
     *
     * @throws IllegalArgumentException for no element, a {@code null} one, or one given twice
     */
    @SafeVarargs
    public static <T> Pool<T> of(T... elements) {
        Objects.requireNonNull(elements, "elements");
        if (elements.length == 0) throw new IllegalArgumentException("A pool lists at least one element");
        LinkedHashSet<Object> listed = new LinkedHashSet<>();
        for (T element : elements) {
            if (element == null) throw new IllegalArgumentException("A pool's element is null");
            if (!listed.add(element)) throw new IllegalArgumentException("A pool lists " + element + " twice");
        }
        return new Pool<>(List.copyOf(listed), null, null, 0, 1, false);
    }

    /**
     * Every constant of {@code type}, an enum or an open set, so a constant added later joins without an edit: an
     * enum's constants, or else the {@code public static final} fields {@code type} declares that hold one, each
     * value once.
     *
     * @throws IllegalArgumentException when this JVM may not read an open set's constants
     */
    public static <T> Pool<T> all(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return new Pool<>(constants(type), type, null, 0, 1, false);
    }

    /**
     * The parameter this pool fills, as its method reference: {@code Farm::account}, a method of a {@link Param}
     * group with no parameter. Its type holds every element, the elements' own or one above it (an interface they
     * implement); after {@link #take}, a list of them. Javac checks no more than that it is a method of one argument,
     * since a method reference's type cannot be bounded below: the type is checked here, as the pool is built.
     * Required: a pool that fills nothing is refused where the host reads it.
     *
     * @throws IllegalArgumentException for a lambda, a method that is not a parameter of a group, or a parameter
     *                                  whose type does not hold the elements
     * @throws IllegalStateException    when this pool already fills one
     */
    public <G> Pool<T> fills(Ref.Of1<G, ?> parameter) {
        if (fills != null) throw new IllegalStateException("A pool fills one parameter: it fills " + named(fills));
        if (!(Ref.resolve(parameter) instanceof Method method) || Modifier.isStatic(method.getModifiers())
                || method.getParameterCount() != 0 || !method.getDeclaringClass().isAnnotationPresent(Param.class)) {
            throw new IllegalArgumentException("A pool fills a parameter: a method of a @Param group with no"
                    + " parameter, as Farm::account");
        }
        Class<?> type = MethodType.methodType(method.getReturnType()).wrap().returnType();
        boolean holds = take > 0 ? type.isAssignableFrom(List.class) : elements.stream().allMatch(type::isInstance);
        if (!holds) {
            throw new IllegalArgumentException(named(method) + " is a " + method.getGenericReturnType().getTypeName()
                    + ", which does not hold " + (take > 0 ? "a list of this pool's elements" : "this pool's elements"));
        }
        return new Pool<>(elements, all, method, take, each, rotates);
    }

    /**
     * Each run takes {@code k} elements, no element in two runs: the pool becomes one of lists, which fills a
     * {@code List} parameter. Before {@link #fills}, whose type it changes.
     *
     * @throws IllegalArgumentException for {@code k < 1}, or more than the elements {@link #of} listed
     * @throws IllegalStateException    when it already takes some, or already fills a parameter
     */
    public Pool<List<T>> take(int k) {
        if (k < 1) throw new IllegalArgumentException("A run takes at least one element, not " + k);
        if (all == null && k > elements.size()) {
            throw new IllegalArgumentException("A run takes " + k + " of " + elements.size() + " elements");
        }
        if (take > 0) throw new IllegalStateException("This pool already takes " + take);
        if (fills != null) throw new IllegalStateException("take(k) comes before fills: it makes a pool of lists");
        return new Pool<>(elements, all, null, k, each, rotates);
    }

    /**
     * An element may be held by {@code n} live runs at once, not one.
     *
     * @throws IllegalArgumentException for {@code n < 1}
     */
    public Pool<T> each(int n) {
        if (n < 1) throw new IllegalArgumentException("An element is held by at least one run, not " + n);
        return new Pool<>(elements, all, fills, take, n, rotates);
    }

    /**
     * Deals the element used least recently first. Without it a run keeps what it was dealt last time (sticky), so
     * the same account goes back to the same instance.
     */
    public Pool<T> rotate() {
        return new Pool<>(elements, all, fills, take, each, true);
    }

    /**
     * The elements, in order: those {@link #of} gave, or every constant of {@link #all}'s type as this JVM saw it.
     * After {@link #take} they are still single elements, which a run takes several of.
     */
    public List<Object> elements() {
        return elements;
    }

    /** {@link #all}'s type, when the pool takes every constant of one. */
    public Optional<Class<?>> allOf() {
        return Optional.ofNullable(all);
    }

    /** The parameter's method, once {@link #fills} named it. */
    public Optional<Method> filled() {
        return Optional.ofNullable(fills);
    }

    /** How many elements a run takes: {@code 0} when it takes one and the parameter is an element, not a list. */
    public int taken() {
        return take;
    }

    /** How many live runs may hold one element at once: {@code 1} unless {@link #each} said more. */
    public int heldBy() {
        return each;
    }

    /** Whether the element used least recently is dealt first, rather than a run keeping its last. */
    public boolean rotates() {
        return rotates;
    }

    /** The pool as its calls: {@code Pool.of("main", "alt").fills(Farm::account).rotate()}. */
    @Override
    public String toString() {
        String from = all != null ? "all(" + all.getSimpleName() + ".class)"
                : elements.stream().map(Pool::said).collect(Collectors.joining(", ", "of(", ")"));
        return "Pool." + from + (take > 0 ? ".take(" + take + ")" : "")
                + (fills != null ? ".fills(" + named(fills) + ")" : "")
                + (each > 1 ? ".each(" + each + ")" : "") + (rotates ? ".rotate()" : "");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Pool<?> pool && take == pool.take && each == pool.each && rotates == pool.rotates
                && elements.equals(pool.elements) && Objects.equals(all, pool.all) && Objects.equals(fills, pool.fills);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, all, fills, take, each, rotates);
    }

    private static List<Object> constants(Class<?> type) {
        if (type.isEnum()) return List.of(type.getEnumConstants());
        LinkedHashSet<Object> constants = new LinkedHashSet<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)
                    || !type.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.trySetAccessible();
                Object value = field.get(null);
                if (value != null) constants.add(value);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot read the constants of " + type.getName()
                        + ": make the class public", e);
            }
        }
        return List.copyOf(constants);
    }

    private static String said(Object element) {
        if (element instanceof String text) return '"' + text + '"';
        if (element instanceof Enum<?> constant) return constant.getDeclaringClass().getSimpleName() + "." + constant.name();
        return String.valueOf(element);
    }

    private static String named(Method method) {
        return method.getDeclaringClass().getSimpleName() + "::" + method.getName();
    }
}
