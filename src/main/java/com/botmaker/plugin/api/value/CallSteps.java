package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The last step of a declaration whose Java is a call: which factory writes it, and how a value comes apart
 * into that factory's parts.
 *
 * <pre>{@code
 * .writtenAs(Tree::of)                                    // the plugin's own type: each part read by its name
 * .writtenAs(LocalDate::of, LocalDate::getYear, LocalDate::getMonthValue, LocalDate::getDayOfMonth)
 * .writtenAsEach(Combo::of, Combo::keys)                  // a varargs factory, its parts one collection
 * .writtenAs(Precision::minArea, p -> p, Precision::minArea)   // an instance method: the receiver is part 0
 * .writtenAs(When::atLeast, Test::when, Test::limit)      // another class's instance method: a typed chain
 * .writtenAsRecord()                                      // a record: its canonical constructor
 * }</pre>
 *
 * <p><b>A chain through another class.</b> An instance-method factory whose receiver is another class finishes a
 * chain that class starts: {@code Params.when(Progress::gold).atLeast(5)}, where {@code Params.when} is declared
 * as the call that writes a {@code When} and {@code When::atLeast} as one that writes a {@code Test}. The host
 * writes part 0, the receiver, by its own class's declaration and the link after it, and reads the chain back the
 * same way. A receiver nothing declares makes the call one the host neither writes nor reads. An instance method
 * of the value's own class is a chain of that class instead, which the host writes only where the class's other
 * factories would lose part of a value.
 *
 * <p><b>Parts read by name.</b> For a type the plugin owns, the factory alone is enough: each parameter
 * {@code p} of a static factory or constructor is read by the type's public no-argument method {@code p()}, or
 * {@code isP()} for a {@code boolean}, answering what {@code p} takes. The names are the compiled class's
 * ({@code -parameters}, which {@code botmaker-plugin-parent} turns on), and both name and type are checked when
 * the declaration is built — by the plugin's own tests, never in a bot. Renaming a parameter or its reader
 * without the other fails there. Give accessors instead for a JDK type, an instance-method factory, or a part
 * read under another name.
 *
 * <p>A part a reader could not tell from the next — a {@code boolean}, a third {@code int} — is better a named
 * link after the call than an argument in it. Every method of the type marked {@link Link} is chained after any
 * of these but {@code writtenAsEach}, and {@link DeclaredCall#with} and {@link DeclaredCall#flag} declare one
 * the mark cannot ({@link Wither}).
 *
 * <p><b>One accessor per part, in the factory's order</b>, and javac counts them: there is one
 * {@code writtenAs} per number of parts, and the accessors' types are what pick an overloaded factory
 * ({@code LocalDate.of(int, int, int)} over {@code LocalDate.of(int, Month, int)}). The value is built back
 * by invoking the factory on the parts, so {@code build(components(v))} equals {@code v} whenever the
 * factory and the accessors agree — which the plugin's own tests and {@code botmaker plugin validate} check.
 *
 * @param <T> the class the call builds
 * @param <D> what the step answers: a {@link DeclaredCall}, or a {@link DeclaredCallType} after
 *            {@link PluginType#value}
 */
public abstract class CallSteps<T, D> {

    final Class<T> type;

    CallSteps(Class<T> type) {
        this.type = type;
    }

    abstract D finish(CallShape<T> shape);

    /** A call with no argument: {@code CaptureSource.desktop()}. */
    public final <R> D writtenAs(Ref.Of0<R> factory) {
        return call(factory, List.of());
    }

    /**
     * A static factory or constructor of one part, read by name: {@code writtenAs(Tree::of)} for
     * {@code Tree.of(List<Node> nodes)} reads the part with {@code Tree.nodes()}. See the class comment for the
     * rule; the overloads up to {@link Ref.Of10} read the same way.
     *
     * @throws IllegalArgumentException for a part with no reader, or a factory compiled without
     *                                  {@code -parameters}
     */
    public final <A, R> D writtenAs(Ref.Of1<A, R> factory) {
        return inferred(factory);
    }

    public final <A, B, R> D writtenAs(Ref.Of2<A, B, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, R> D writtenAs(Ref.Of3<A, B, C, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, R> D writtenAs(Ref.Of4<A, B, C, E, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, R> D writtenAs(Ref.Of5<A, B, C, E, F, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, G, R> D writtenAs(Ref.Of6<A, B, C, E, F, G, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, G, H, R> D writtenAs(Ref.Of7<A, B, C, E, F, G, H, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, G, H, I, R> D writtenAs(Ref.Of8<A, B, C, E, F, G, H, I, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, G, H, I, J, R> D writtenAs(Ref.Of9<A, B, C, E, F, G, H, I, J, R> factory) {
        return inferred(factory);
    }

    public final <A, B, C, E, F, G, H, I, J, K, R> D writtenAs(Ref.Of10<A, B, C, E, F, G, H, I, J, K, R> factory) {
        return inferred(factory);
    }

    public final <A, R> D writtenAs(Ref.Of1<A, R> factory, Function<? super T, ? extends A> a) {
        return call(factory, List.of(a));
    }

    public final <A, B, R> D writtenAs(Ref.Of2<A, B, R> factory, Function<? super T, ? extends A> a,
                                       Function<? super T, ? extends B> b) {
        return call(factory, List.of(a, b));
    }

    public final <A, B, C, R> D writtenAs(Ref.Of3<A, B, C, R> factory, Function<? super T, ? extends A> a,
                                          Function<? super T, ? extends B> b, Function<? super T, ? extends C> c) {
        return call(factory, List.of(a, b, c));
    }

    public final <A, B, C, E, R> D writtenAs(Ref.Of4<A, B, C, E, R> factory, Function<? super T, ? extends A> a,
                                             Function<? super T, ? extends B> b, Function<? super T, ? extends C> c,
                                             Function<? super T, ? extends E> d) {
        return call(factory, List.of(a, b, c, d));
    }

    public final <A, B, C, E, F, R> D writtenAs(Ref.Of5<A, B, C, E, F, R> factory,
                                                Function<? super T, ? extends A> a,
                                                Function<? super T, ? extends B> b,
                                                Function<? super T, ? extends C> c,
                                                Function<? super T, ? extends E> d,
                                                Function<? super T, ? extends F> e) {
        return call(factory, List.of(a, b, c, d, e));
    }

    public final <A, B, C, E, F, G, R> D writtenAs(Ref.Of6<A, B, C, E, F, G, R> factory,
                                                   Function<? super T, ? extends A> a,
                                                   Function<? super T, ? extends B> b,
                                                   Function<? super T, ? extends C> c,
                                                   Function<? super T, ? extends E> d,
                                                   Function<? super T, ? extends F> e,
                                                   Function<? super T, ? extends G> f) {
        return call(factory, List.of(a, b, c, d, e, f));
    }

    public final <A, B, C, E, F, G, H, R> D writtenAs(Ref.Of7<A, B, C, E, F, G, H, R> factory,
                                                      Function<? super T, ? extends A> a,
                                                      Function<? super T, ? extends B> b,
                                                      Function<? super T, ? extends C> c,
                                                      Function<? super T, ? extends E> d,
                                                      Function<? super T, ? extends F> e,
                                                      Function<? super T, ? extends G> f,
                                                      Function<? super T, ? extends H> g) {
        return call(factory, List.of(a, b, c, d, e, f, g));
    }

    public final <A, B, C, E, F, G, H, I, R> D writtenAs(Ref.Of8<A, B, C, E, F, G, H, I, R> factory,
                                                         Function<? super T, ? extends A> a,
                                                         Function<? super T, ? extends B> b,
                                                         Function<? super T, ? extends C> c,
                                                         Function<? super T, ? extends E> d,
                                                         Function<? super T, ? extends F> e,
                                                         Function<? super T, ? extends G> f,
                                                         Function<? super T, ? extends H> g,
                                                         Function<? super T, ? extends I> h) {
        return call(factory, List.of(a, b, c, d, e, f, g, h));
    }

    public final <A, B, C, E, F, G, H, I, J, R> D writtenAs(Ref.Of9<A, B, C, E, F, G, H, I, J, R> factory,
                                                            Function<? super T, ? extends A> a,
                                                            Function<? super T, ? extends B> b,
                                                            Function<? super T, ? extends C> c,
                                                            Function<? super T, ? extends E> d,
                                                            Function<? super T, ? extends F> e,
                                                            Function<? super T, ? extends G> f,
                                                            Function<? super T, ? extends H> g,
                                                            Function<? super T, ? extends I> h,
                                                            Function<? super T, ? extends J> i) {
        return call(factory, List.of(a, b, c, d, e, f, g, h, i));
    }

    public final <A, B, C, E, F, G, H, I, J, K, R> D writtenAs(Ref.Of10<A, B, C, E, F, G, H, I, J, K, R> factory,
                                                               Function<? super T, ? extends A> a,
                                                               Function<? super T, ? extends B> b,
                                                               Function<? super T, ? extends C> c,
                                                               Function<? super T, ? extends E> d,
                                                               Function<? super T, ? extends F> e,
                                                               Function<? super T, ? extends G> f,
                                                               Function<? super T, ? extends H> g,
                                                               Function<? super T, ? extends I> h,
                                                               Function<? super T, ? extends J> i,
                                                               Function<? super T, ? extends K> j) {
        return call(factory, List.of(a, b, c, d, e, f, g, h, i, j));
    }

    /**
     * Any number of parts, for a factory past {@link Ref.Of10}: {@code factory} is a variable of a
     * serializable functional interface the plugin declares itself, extending {@link Ref}, holding a method
     * reference. There is no typing between the factory and the accessors here, so javac cannot pick an
     * overload from them; the count is still checked when the declaration is built.
     *
     * <pre>{@code
     * interface Of12<A, B, C, D, E, F, G, H, I, J, K, L, R> extends Ref { R call(A a, …, L l); }
     * static final Of12<…, Big> BIG_OF = Big::of;
     * static final DeclaredCall<Big> BIG = ComponentType.part(Big.class).writtenAs(BIG_OF, Big::a, …, Big::l);
     * }</pre>
     */
    @SafeVarargs
    public final D writtenAs(Ref factory, Function<? super T, ?>... parts) {
        return call(factory, List.of(parts));
    }

    /**
     * A varargs factory with nothing before the run: {@code Combo.of(Key...)}, taken apart as one collection
     * the host writes as that many arguments.
     */
    public final <E, R> D writtenAsEach(Ref.Of1<E[], R> factory,
                                        Function<? super T, ? extends Collection<? extends E>> elements) {
        return finish(CallShape.of(type, factory, true, List.of(elements)));
    }

    /** A record, written as its canonical constructor and taken apart by its accessors. */
    public final D writtenAsRecord() {
        return finish(CallShape.record(type));
    }

    /**
     * A factory no method reference can name — {@link Ref#member}, for a static and an instance method that
     * share a name and a number of arguments. Otherwise as {@code writtenAs}, with one accessor per part.
     */
    @SafeVarargs
    public final D writtenAsMember(Executable factory, Function<? super T, ?>... parts) {
        return finish(CallShape.of(type, factory, false, List.of(parts)));
    }

    private D call(Ref factory, List<Function<? super T, ?>> accessors) {
        return finish(CallShape.of(type, factory, false, accessors));
    }

    private D inferred(Ref factory) {
        Executable executable = Ref.resolve(factory);
        List<CallShape.Read<T>> readers = CallShape.accessorsOf(type, executable);
        List<Function<? super T, ?>> accessors = new ArrayList<>(readers.size());
        Set<String> read = new HashSet<>();
        for (CallShape.Read<T> reader : readers) {
            accessors.add(reader.get());
            read.add(reader.name());
        }
        return finish(CallShape.of(type, executable, false, accessors, read));
    }
}
