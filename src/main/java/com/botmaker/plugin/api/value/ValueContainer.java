package com.botmaker.plugin.api.value;

import java.util.List;

/**
 * A composite a value may be built out of — {@code List<E>}, {@code Map<K, V>}, or one a plugin contributes.
 *
 * <p>The container half of {@link ValueForm}, registered in a {@link ValueCatalog} exactly as a
 * {@link ValueType} and its {@link ValueCodec} are. A plugin may add {@code Set}, {@code Optional} or its own
 * {@code Either<L, R>} without a Studio release and without the host learning anything about it.
 *
 * <h2>Nothing here is a string function</h2>
 *
 * <p>A container takes a composite apart into typed parts and puts it back together from typed parts. It
 * never sees Java source and never produces any: <b>the host owns every character of syntax</b>, written once
 * for all containers as {@code Owner.factory(p₁, …, pₙ)} and read back by matching that prefix and splitting
 * at depth zero.
 *
 * <p>That is deliberate and it is the second attempt. The first gave a container {@code write(List<String>)}
 * and {@code read(String)}, which put a wire encoding back across the contract — the thing
 * {@code docs/refactor/33-plugin-java.md} exists to remove. Source text is the host's <em>output</em>; it is
 * not how values travel.
 *
 * <h2>Arity counts types; parts count values</h2>
 *
 * <p>{@link #arity()} is how many type arguments a form supplies: one for {@code List<E>}, two for
 * {@code Map<K, V>}. {@link #parts} returns <em>values</em>, so its length is the size of the composite and
 * has nothing to do with the arity. {@link #partForms} is what tells the host the static type of each part,
 * which is what keeps the recursion typed all the way down.
 *
 * <h2>Both halves are abstract, and that is the point</h2>
 *
 * <p>{@code ValueCodec.wireOfLiteral} was a {@code default} answering empty, and eight of the seventeen
 * registered value types never overrode it: the half nobody is forced to write is the half that rots. There
 * is no default here, so a container that cannot be rebuilt cannot be registered, and
 * {@code build(parts(v))} equals {@code v} is a law the compiler helps keep.
 *
 * @param <C> the composite's own type, which never crosses to the host except behind a wildcard capture
 */
public interface ValueContainer<C> {

    /** {@code java.util.List<E>} — {@code List.of(e₁, …)}. Registered by every catalog. */
    ValueContainer<List<?>> LIST = new HostContainers.ListContainer();

    /**
     * {@code java.util.Map<K, V>} — {@code Map.ofEntries(Map.entry(k, v), …)}, always.
     *
     * <p>Never {@code Map.of}, which takes alternating keys and values and stops at ten pairs. One spelling
     * means one rule for the writer and one for the reader, and no behaviour change at the eleventh entry.
     * Its parts are {@link java.util.Map.Entry entries}, which is why {@link #ENTRY} exists.
     */
    ValueContainer<java.util.Map<?, ?>> MAP = new HostContainers.MapContainer();

    /** {@code java.util.Map.Entry<K, V>} — {@code Map.entry(k, v)}. What a {@link #MAP}'s parts are. */
    ValueContainer<java.util.Map.Entry<?, ?>> ENTRY = new HostContainers.EntryContainer();

    /**
     * The class this container is. <b>Read, never loaded</b> — only its name is taken off the object the
     * caller already holds, exactly as {@link ValueCatalog#forJava(Class)} does for a leaf.
     */
    Class<?> type();

    /** How many type arguments a form supplies: one for {@code List<E>}, two for {@code Map<K, V>}. */
    int arity();

    /** The class the factory is declared on, which is usually the container itself — {@code Map} for an entry. */
    default Class<?> factoryOwner() {
        return type();
    }

    /**
     * The static factory's name: {@code "of"}, {@code "ofEntries"}, {@code "entry"}.
     *
     * <p>A name rather than a method handle because it is what the host writes into a file and matches when
     * reading one back, and because a handle would be behaviour crossing the contract when only a spelling
     * needs to.
     */
    String factory();

    /**
     * This composite's parts, in the order they should be written.
     *
     * <p>Order is the container's to decide and it must be stable: it is what a diff of the generated file
     * shows, and an order that varies per run rewrites a user's file for no reason.
     */
    List<Object> parts(C value);

    /** {@link #parts} read backwards. Given parts this container produced, the composite they came from. */
    C build(List<Object> parts);

    /**
     * The static type of each part, given this form's type arguments and how many parts there are.
     *
     * <p>{@code List<E>} answers {@code parts} copies of {@code E}; {@code Map<K, V>} answers {@code parts}
     * copies of {@code Entry<K, V>}; an entry answers {@code [K, V]}. The host walks the values and this walks
     * the types beside them, which is what lets a leaf codec be found for every part without anything
     * guessing from the value's runtime class.
     */
    List<ValueForm> partForms(List<ValueForm> arguments, int parts);

    // ---- derived -----------------------------------------------------------------------------------------

    /** The stable identity, which is this container's Java type name. Persisted — do not change. */
    default String id() {
        return sourceName();
    }

    /** How a generator writes the type: {@code java.util.List}, {@code java.util.Map.Entry}. */
    default String sourceName() {
        return canonical(type());
    }

    /** The class a file importing this container names, which is {@link #sourceName()}. */
    default String importName() {
        return sourceName();
    }

    /** The call a generator writes before the opening bracket: {@code java.util.Map.ofEntries}. */
    default String factorySource() {
        return canonical(factoryOwner()) + "." + factory();
    }

    /** What a menu calls wrapping a form in this container. */
    default String label() {
        return type().getSimpleName();
    }

    /**
     * {@link #parts} against a value the host holds as {@code Object}.
     *
     * <p>The one unchecked cast in the design, kept here so no caller writes its own. It is sound for the
     * same reason {@code ValueCodec}'s is: the host only ever passes back a value this container produced, or
     * one whose class it checked against {@link #type()} first.
     */
    @SuppressWarnings("unchecked")
    default List<Object> partsOf(Object value) {
        return parts((C) value);
    }

    private static String canonical(Class<?> type) {
        String name = type.getCanonicalName();
        return name != null ? name : type.getName();
    }
}
