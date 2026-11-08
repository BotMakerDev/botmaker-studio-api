package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The steps a declared call takes after its factory, and the {@link ComponentType} its shape answers — one copy
 * for {@link DeclaredCall} (a part) and {@link DeclaredCallType} (a type whose Java is a call), each step answering
 * its own class. Immutable; each step answers a copy, so a declaration stays a {@code static final} field.
 *
 * @param <T> the class the call builds
 * @param <S> the declaration a step answers
 */
abstract sealed class CallDeclaration<T, S extends CallDeclaration<T, S>> implements ComponentType<T>
        permits DeclaredCall, DeclaredCallType {

    final CallShape<T> shape;

    CallDeclaration(CallShape<T> shape) {
        this.shape = shape;
    }

    /** This declaration with {@code shape}. */
    abstract S next(CallShape<T> shape);

    /**
     * This call, plus the {@code public static final} constants of its type that a value equal to one is
     * written as: {@code .constants(FlowLayout.NONE)}. Named by value and typed, so a renamed constant or one of
     * another type is a compile error.
     */
    @SafeVarargs
    public final S constants(T... values) {
        return next(shape.withConstants(values));
    }

    /**
     * This call, then {@code .wither(part)} wherever a value's part differs from what the factory makes:
     * {@code .with(Flow.Step::described, Flow.Step::description)} writes {@code .described("Picks up ore")}.
     * See {@link Wither}. Each wither adds one part after the factory's, in the order they are declared.
     *
     * <p>A part the host holds as something the wither does not take is a {@link Held} part: {@code
     * .with(Run::home, HOME.part(Bots::method))} hands the wither {@code give} of the text it read.
     *
     * <p>A method marked {@link Link} is chained already; declaring it here replaces that link in its place —
     * for a part read under another name, or a {@link Held} one.
     *
     * @param wither an instance method of the type taking the part and answering the changed value
     * @param part   the part a value holds, as the wither takes it
     * @throws IllegalArgumentException for a method that is no wither of the type, a varargs factory, or a
     *                                  wither declared twice (a {@link Link} mark and one step is once)
     */
    public <A> S with(Ref.Of2<T, A, T> wither, Function<? super T, ? extends A> part) {
        return next(shape.withWither(Wither.with(shape.type(), wither, part)));
    }

    /**
     * {@link #with(Ref.Of2, Function)} for a varargs link, {@code .needs(Goal...)}: its part is the list {@code part}
     * reads, written with an argument per element and not at all when empty ({@link Wither#spread()}). For a link
     * whose reader is named otherwise than the method, as an overload's must be: {@code
     * .withEach(Do::needs, Do::goalNeeds)}.
     *
     * @param wither an instance method of the type taking a varargs tail and answering the changed value
     * @param part   the elements a value holds for it
     * @throws IllegalArgumentException as for {@link #with(Ref.Of2, Function)}, and for a method that is not varargs
     */
    public <A> S withEach(Ref.Of2<T, A[], T> wither, Function<? super T, ? extends List<A>> part) {
        Wither<T> each = Wither.with(shape.type(), wither, part);
        if (!each.spread()) throw new IllegalArgumentException(each + " is not varargs: declare it with .with");
        return next(shape.withWither(each));
    }

    /**
     * {@link #with(Ref.Of2, Function)} for a link whose part the host holds as something nothing turns into its
     * parameter, so the value is set by {@code apply} rather than by calling the method: a class literal of a
     * managed enum, which crosses as the plugin's value of that enum ({@code .whenOff(Collect.class)}). The host
     * checks the part it read is an {@code H} before {@code apply} sees it.
     *
     * @param wither the method the link is written as; the host reads its name and parameter, and never calls it
     * @param part   the part a value holds, as the host reads and writes it
     * @param apply  {@code value} with its part set, or {@code null} for a part this link does not take
     */
    public <A, H> S with(Ref.Of2<T, A, T> wither, Held<? super T, H> part,
                         BiFunction<? super T, ? super H, ? extends T> apply) {
        return next(shape.withWither(Wither.with(shape.type(), wither, part, apply)));
    }

    /**
     * This call, then {@code .wither()} wherever a value has {@code on} and the factory's value has not:
     * {@code .flag(Flow.Step::goesHome, Flow.Step::goHome)} writes {@code .goesHome()}. A flag only turns a
     * thing on, so a setting that is on by default is declared by its negative ({@code .off()}). See
     * {@link Wither}.
     *
     * @throws IllegalArgumentException as for {@link #with}
     */
    public S flag(Ref.Of1<T, T> wither, Predicate<? super T> on) {
        return next(shape.withWither(Wither.flag(shape.type(), wither, on)));
    }

    /**
     * This call, built from the parts the host read rather than by invoking its factory — for a factory nothing
     * the host holds can be handed to, such as one taking a class literal, which the host holds as the plugin's
     * value of that enum. Every place of the call is a {@link Held} part ({@code held.part()}); the host checks
     * each part it read against its kind first, so {@code build} reads them typed:
     * {@code .builtFrom(parts -> step(parts.get(ACTIVITY), parts.get(BODY)))}. Answer {@code null} for parts this
     * call does not build. The factory's parts only; each wither is applied after it.
     *
     * @throws IllegalArgumentException when a place of the call is a plain accessor
     */
    public S builtFrom(Function<Held.Parts, ? extends T> build) {
        return next(shape.withBuiltFrom(build));
    }

    /**
     * This call, a factory with no parts, built as {@code made} rather than by invoking the factory — whose value
     * would be what the editor's own process has: {@code Source.current()} built as the ambient source itself.
     *
     * @throws IllegalArgumentException when the factory takes parts
     */
    public S builtAs(Supplier<? extends T> made) {
        return next(shape.withBuiltAs(made));
    }

    /**
     * This call, writing only the values {@code writes} accepts. A class written by several factories is declared
     * once per factory, and the host reads a call of any of them; it writes a value with the declaration that
     * rebuilds it equal ({@link ComponentType#writer}), so most need no predicate. Give one for a choice equality
     * cannot make — a value written as a constant when one holds it, or a value holding a lambda, which no
     * declaration rebuilds equal. Without it a call writes every value of its class.
     */
    public S when(Predicate<? super T> writes) {
        if (writes == null) throw new IllegalArgumentException("when needs a predicate");
        return next(shape.withWrites(writes));
    }

    @Override
    public boolean writes(T value) {
        return shape.writes().test(value);
    }

    @Override
    public Class<T> type() {
        return shape.type();
    }

    @Override
    public Executable factory() {
        return shape.factory();
    }

    @Override
    public List<Class<?>> componentTypes() {
        return shape.allParts();
    }

    @Override
    public List<Object> components(T value) {
        return shape.taken(value);
    }

    @Override
    public T build(List<Object> parts) {
        return shape.built(parts);
    }

    @Override
    public List<Field> constants() {
        return shape.constants();
    }

    @Override
    public List<Wither<T>> withers() {
        return shape.withers();
    }
}
