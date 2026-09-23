package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.util.List;

/**
 * A type the host has to take apart and put back together, because the Java that writes one is a call.
 *
 * <p>{@code new Point(10, 20)}, {@code java.time.Duration.ofMillis(3000L)}, {@code Flow.of(…)}: the host
 * writes the call and reads it back, and this says what goes inside the brackets. It is
 * {@code ValueContainer} renamed and merged with the half of {@code ValueCodec} that was not about stored
 * text — the same design, with the wire encoding removed.
 *
 * <h2>Nothing here is a string function</h2>
 *
 * <p>A component type takes a value apart into typed parts and puts it back together from typed parts. It
 * never sees Java source and never produces any. <b>The host owns every character of syntax</b>, written
 * once for every plugin as {@code Owner.factory(p₁, …, pₙ)} — or {@code new Owner(p₁, …, pₙ)} — and read
 * back off the parsed expression.
 *
 * <p>That is deliberate and it is the second attempt. The first gave a container {@code write(List<String>)}
 * and {@code read(String)}, which put a wire encoding back across the contract. Source text is the host's
 * <em>output</em>; it is not how values travel.
 *
 * <h2>Both halves are abstract, and that is the point</h2>
 *
 * <p>{@code ValueCodec}'s reader was a {@code default} answering empty, and eight of the seventeen
 * registered types never overrode it: the half nobody is forced to write is the half that rots. Here
 * {@code build(components(v))} equals {@code v} is a law the compiler helps keep, and
 * {@code botmaker plugin validate} checks it against {@link PluginType#fresh()}.
 *
 * <h2>It does not extend {@link PluginType}, deliberately</h2>
 *
 * <p>The two questions are independent. {@code Point} is both — a person picks one, and its Java is a call.
 * An enum constant is a {@link PluginType} and nothing else: its Java is the constant's own name, which the
 * host writes without help. The SDK's {@code Activity}, {@code Edge} and {@code Limits} are the other way
 * round — parts of a {@code Flow}, never picked on their own — and making them extend {@link PluginType}
 * would owe each a {@code fresh()} and an {@code editor()} nothing would ever call.
 *
 * @param <T> the plugin's own type, which never crosses a classloader
 */
public interface ComponentType<T> {

    /** The class this describes. Read, never loaded — see {@link PluginType#type()}. */
    Class<T> type();

    /**
     * The static type of each component, in the order they are written: {@code Point} answers
     * {@code [int.class, int.class]}, {@code Duration} answers {@code [long.class]}.
     *
     * <p>This is what keeps the recursion typed all the way down: the host walks the values and this walks
     * the types beside them, so a component's own writer is found without anything guessing from a runtime
     * class. A component whose type is itself a {@link ComponentType} nests.
     */
    List<Class<?>> componentTypes();

    /**
     * This value's components, in the order they are written, as values — never as text.
     *
     * <p>Order is this type's to decide and it must be stable: it is what a diff of the user's file shows,
     * and an order that varies per run rewrites a file for no reason. The length must match
     * {@link #componentTypes()}, except for a variable-length type — a list — where it is the size.
     */
    List<Object> components(T value);

    /** {@link #components} read backwards. Given components this type produced, the value they came from. */
    T build(List<Object> parts);

    /**
     * What writes this value: a constructor, a {@code public static} method, or — read only — an instance
     * method called on part 0.
     *
     * <ul>
     *   <li><b>A constructor</b>, the default: the one taking {@link #componentTypes()} in order, written
     *       {@code new Owner(p₁, …)}.</li>
     *   <li><b>A static method</b>, written {@code Owner.method(p₁, …)} — {@code Duration.ofMillis},
     *       {@code Map.entry}. It may be declared on another class and may return a supertype of
     *       {@link #type()}: {@code Source.current()} returns a {@code CaptureSource}.</li>
     *   <li><b>An instance method</b>, whose receiver is part 0: {@code part₀.method(p₁, …)}. That is a chain
     *       a person writes by hand — {@code Precision.TIGHT.minArea(400)},
     *       {@code CaptureSource.window("Game").region(r)}. The host reads it and <b>never writes it</b>: an
     *       edited value is written through the declaration that owns the class name. So such a type is
     *       listed in {@code StudioPlugin.componentTypes()} beside that declaration and never claims the
     *       class.</li>
     * </ul>
     *
     * <p>An {@link Executable} rather than a name, so a rename fails where the plugin builds this, not in a
     * bot's file. The host only reads its shape — declaring class, name, parameters, modifiers — and never
     * invokes it. {@code botmaker plugin validate} checks that it is public, that its parameters match
     * {@link #componentTypes()} (the receiver excluded, a varargs tail allowed), and that {@link #type()} is
     * assignable to what it returns.
     *
     * @throws IllegalStateException when the default finds no such constructor
     */
    default Executable factory() {
        List<Class<?>> parts = componentTypes();
        try {
            return type().getDeclaredConstructor(parts.toArray(new Class<?>[0]));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(type().getName() + " has no constructor taking " + parts
                    + "; override factory()", e);
        }
    }

    /**
     * {@link #components} against a value the host holds as {@code Object}.
     *
     * <p>The one unchecked cast in the design, kept here so no caller writes its own. It is sound because
     * the host only ever passes back a value this type produced, or one whose class it checked against
     * {@link #type()} first.
     */
    @SuppressWarnings("unchecked")
    default List<Object> componentsOf(Object value) {
        return components((T) value);
    }
}
