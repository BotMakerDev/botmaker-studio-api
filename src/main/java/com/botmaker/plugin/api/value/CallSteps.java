package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * The last step of a declaration whose Java is a call: which factory writes it, and how a value comes apart
 * into that factory's parts.
 *
 * <pre>{@code
 * .writtenAs(LocalDate::of, LocalDate::getYear, LocalDate::getMonthValue, LocalDate::getDayOfMonth)
 * .writtenAsEach(Combo::of, Combo::keys)                  // a varargs factory, its parts one collection
 * .writtenAs(Precision::minArea, p -> p, Precision::minArea)   // an instance method: the receiver is part 0
 * .writtenAsRecord()                                      // a record: its canonical constructor
 * }</pre>
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
}
