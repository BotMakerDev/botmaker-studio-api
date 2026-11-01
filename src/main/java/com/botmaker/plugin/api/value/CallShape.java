package com.botmaker.plugin.api.value;

import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What a declared call is made of — the factory, how a value comes apart, how it is built back, the constants
 * a value equal to one is written as, the withers chained after it — and the derivations that let a
 * declaration state only the factory and the parts. Immutable; every {@code with} answers a copy.
 *
 * <p>{@code parts}, {@code components} and {@code build} are the factory's alone; {@link #allParts},
 * {@link #taken} and {@link #built} add one part per wither after them ({@link Wither}). {@code held} has one
 * entry per accessor, the {@link Held} part standing there or {@code null} for a plain accessor.
 */
record CallShape<T>(Class<T> type, Executable factory, List<Class<?>> parts,
                    Function<T, List<Object>> components, Function<List<Object>, T> build, List<Field> constants,
                    List<Wither<T>> withers, Predicate<? super T> writes, List<Held<?, ?>> held) {

    /** A call to {@code factory}, taken apart by {@code accessors} in its part order and built back by invoking it. */
    static <T> CallShape<T> of(Class<T> type, Ref factory, boolean spreadLast, List<Function<? super T, ?>> accessors) {
        return of(type, Ref.resolve(factory), spreadLast, accessors);
    }

    static <T> CallShape<T> of(Class<T> type, Executable factory, boolean spreadLast,
                               List<Function<? super T, ?>> accessors) {
        return of(type, factory, spreadLast, accessors, Set.of());
    }

    /**
     * As {@link #of(Class, Executable, boolean, List)}, with every {@link Link} of the type chained after the
     * factory but those read by a method named in {@code readByFactory}: a part the factory already takes.
     */
    static <T> CallShape<T> of(Class<T> type, Executable factory, boolean spreadLast,
                               List<Function<? super T, ?>> accessors, Set<String> readByFactory) {
        List<Class<?>> parts = partsOf(factory);
        if (accessors.size() != parts.size()) {
            throw new IllegalArgumentException(name(factory) + " takes " + parts.size() + " parts and "
                    + accessors.size() + " were given");
        }
        if (spreadLast && !factory.isVarArgs()) {
            throw new IllegalArgumentException(name(factory) + " is not varargs, so its last part is not spread");
        }
        checkResult(type, factory);
        List<Held<?, ?>> held = new ArrayList<>(accessors.size());
        for (Function<? super T, ?> accessor : accessors) held.add(Held.at(accessor));
        if (factory.isVarArgs() && held.stream().anyMatch(Objects::nonNull)) {
            throw new IllegalArgumentException(name(factory) + " is varargs, so its parts have no fixed places for a"
                    + " Held part");
        }
        List<Wither<T>> links = links(type, readByFactory);
        if (factory.isVarArgs() && !links.isEmpty()) {
            throw new IllegalArgumentException(name(factory) + " is varargs, so the @Link methods of "
                    + type.getName() + " have no place after its parts");
        }
        return new CallShape<>(type, factory, parts, taking(accessors, spreadLast),
                handing(accessors, invoking(type, factory)), List.of(), links, value -> true,
                Collections.unmodifiableList(held));
    }

    /**
     * The accessors of {@code factory}'s parts, found by name and type: parameter {@code p} is read by
     * {@code type}'s public no-argument method {@code p()}, or {@code isP()} for a {@code boolean}, answering
     * what {@code p} takes. Refused for an instance method, whose receiver has no name; a varargs factory; an
     * inner class's constructor, whose outer instance is no part; and a class compiled without
     * {@code -parameters}, whose names are {@code arg0}.
     */
    static <T> List<Read<T>> accessorsOf(Class<T> type, Executable factory) {
        if (factory instanceof Method m && !Modifier.isStatic(m.getModifiers())) {
            throw new IllegalArgumentException(name(factory) + " is an instance method, whose receiver has no name"
                    + " to read it by: give an accessor per part");
        }
        if (factory.isVarArgs()) {
            throw new IllegalArgumentException(name(factory) + " is varargs: declare it with writtenAsEach");
        }
        List<Read<T>> out = new ArrayList<>();
        for (Parameter parameter : factory.getParameters()) {
            if (parameter.isImplicit() || parameter.isSynthetic()) {
                throw new IllegalArgumentException(name(factory) + " takes an outer instance no value holds: make "
                        + type.getName() + " a static class, or give an accessor per part");
            }
            if (!parameter.isNamePresent()) {
                throw new IllegalArgumentException(name(factory) + " was compiled without -parameters, so its parts"
                        + " cannot be read by name: compile with -parameters (botmaker-plugin-parent does), or give"
                        + " an accessor per part");
            }
            Read<T> reader = reader(type, parameter.getName(), parameter.getType());
            if (reader == null) {
                throw new IllegalArgumentException(name(factory) + "'s part " + parameter.getName() + " has no reader:"
                        + " " + type.getName() + " needs a public " + parameter.getName() + "()"
                        + (parameter.getType() == boolean.class ? " or is" + capital(parameter.getName()) + "()" : "")
                        + " answering a " + parameter.getType().getName() + ", or give an accessor per part");
            }
            out.add(reader);
        }
        return out;
    }

    /** A part's reader: the method's name, and the method as a function of the value. */
    record Read<T>(String name, Function<? super T, ?> get) {
    }

    /**
     * Every method of {@code type} marked {@link Link}, on itself or on a method it overrides, as a link read by
     * its pair: {@code x(A)} by {@code x()} (or {@code isX()} for a {@code boolean}), the flag {@code x()} by
     * {@code isX()} — but those whose reader is named in {@code readByFactory}, a part the factory takes already.
     * In the order of their readers among a record's components, then by name.
     *
     * @throws IllegalArgumentException for a marked method with no reader, or that is no link of the type
     */
    static <T> List<Wither<T>> links(Class<T> type, Set<String> readByFactory) {
        List<Method> marks = marks(type);
        if (marks.isEmpty()) return List.of();
        record Found<T>(Wither<T> link, String reader) {
        }
        List<Found<T>> found = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()
                    || !marked(method, marks)) {
                continue;
            }
            String what = type.getName() + "." + method.getName();
            if (method.getParameterCount() > 1) {
                throw new IllegalArgumentException(what + " is marked @Link and takes " + method.getParameterCount()
                        + " arguments, where a link takes the one part it sets, or none for a flag");
            }
            if (!type.isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException(what + " is marked @Link and answers a "
                        + method.getReturnType().getName() + ": override it in " + type.getName() + " answering a "
                        + type.getSimpleName() + ", so a bot's chain keeps its type");
            }
            boolean flag = method.getParameterCount() == 0;
            Read<T> reader = flag ? reader(type, "is" + capital(method.getName()), boolean.class)
                    : reader(type, method.getName(), method.getParameterTypes()[0]);
            if (reader == null) {
                throw new IllegalArgumentException(what + " is marked @Link but " + type.getName() + " has no public "
                        + (flag ? "boolean is" + capital(method.getName()) + "()"
                        : method.getName() + "() answering a " + method.getParameterTypes()[0].getName())
                        + " to read its part by: rename the reader, or declare the link with .with/.flag");
            }
            if (readByFactory.contains(reader.name())) continue;
            found.add(new Found<>(Wither.linked(type, method, reader.get()), reader.name()));
        }
        List<String> components = type.isRecord()
                ? Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList() : List.of();
        return found.stream()
                .sorted(Comparator.<Found<T>>comparingInt(each -> {
                    int at = components.indexOf(each.reader());
                    return at < 0 ? Integer.MAX_VALUE : at;
                }).thenComparing(each -> each.link().method().getName()))
                .map(Found::link)
                .toList();
    }

    /** Every method marked {@link Link} declared by {@code type} or any of its supertypes. */
    private static List<Method> marks(Class<?> type) {
        List<Method> out = new ArrayList<>();
        List<Class<?>> pending = new ArrayList<>(List.of(type));
        Set<Class<?>> seen = new HashSet<>();
        while (!pending.isEmpty()) {
            Class<?> at = pending.removeLast();
            if (at == null || !seen.add(at)) continue;
            for (Method method : at.getDeclaredMethods()) {
                if (!method.isBridge() && method.isAnnotationPresent(Link.class)) out.add(method);
            }
            pending.add(at.getSuperclass());
            pending.addAll(List.of(at.getInterfaces()));
        }
        return out;
    }

    /**
     * Whether {@code method} is one of {@code marks} or overrides one: same name and count, each parameter taking
     * what the mark's does — so a mark on a generic supertype's {@code scaled(P)} covers {@code scaled(Size)}.
     */
    private static boolean marked(Method method, List<Method> marks) {
        for (Method mark : marks) {
            if (!mark.getName().equals(method.getName()) || mark.getParameterCount() != method.getParameterCount()) {
                continue;
            }
            if (!mark.getDeclaringClass().isAssignableFrom(method.getDeclaringClass())) continue;
            boolean fits = true;
            for (int i = 0; i < mark.getParameterCount(); i++) {
                fits &= mark.getParameterTypes()[i].isAssignableFrom(method.getParameterTypes()[i]);
            }
            if (fits) return true;
        }
        return false;
    }

    /** The reader of a part named {@code name} taking a {@code part}: {@code name()}, or {@code isName()} for a boolean. */
    private static <T> Read<T> reader(Class<T> type, String name, Class<?> part) {
        Read<T> plain = method(type, name, part);
        return plain != null || box(part) != Boolean.class ? plain : method(type, "is" + capital(name), part);
    }

    /**
     * The public no-argument instance method {@code name} of {@code type} answering what {@code part} takes, as a
     * reader — or {@code null} when there is none.
     */
    private static <T> Read<T> method(Class<T> type, String name, Class<?> part) {
        Method method;
        try {
            method = type.getMethod(name);
        } catch (NoSuchMethodException absent) {
            return null;
        }
        if (Modifier.isStatic(method.getModifiers())) return null;
        if (!box(part).isAssignableFrom(box(method.getReturnType()))) return null;
        method.trySetAccessible();
        return new Read<>(name, value -> {
            try {
                return method.invoke(value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(type.getName() + "." + name + "() failed", e);
            }
        });
    }

    private static String capital(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /** A record written as its canonical constructor, taken apart by its accessors. */
    static <T> CallShape<T> record(Class<T> type) {
        if (!type.isRecord()) throw new IllegalArgumentException(type.getName() + " is not a record");
        RecordComponent[] fields = type.getRecordComponents();
        Class<?>[] parameters = new Class<?>[fields.length];
        List<Function<? super T, ?>> accessors = new ArrayList<>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            RecordComponent field = fields[i];
            parameters[i] = field.getType();
            accessors.add(value -> {
                try {
                    return field.getAccessor().invoke(value);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(type.getName() + "." + field.getName() + "() failed", e);
                }
            });
        }
        Set<String> read = new HashSet<>();
        for (RecordComponent field : fields) read.add(field.getName());
        try {
            return of(type, type.getConstructor(parameters), false, accessors, read);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getName() + " is not a public record", e);
        }
    }

    /** This shape with the four parts a declaration's steps change; the factory and how it comes apart stay. */
    private CallShape<T> copy(Function<List<Object>, T> build, List<Field> constants, List<Wither<T>> withers,
                              Predicate<? super T> writes) {
        return new CallShape<>(type, factory, parts, components, build, constants, withers, writes, held);
    }

    CallShape<T> withBuild(Function<List<Object>, T> build) {
        return copy(build, constants, withers, writes);
    }

    /**
     * This shape built by {@code build} from its held parts, each checked first: a part read as anything but its
     * held kind is not this call. Refused when an accessor is plain, whose part {@code build} could not read.
     */
    CallShape<T> withBuiltFrom(Function<Held.Parts, ? extends T> build) {
        if (build == null) throw new IllegalArgumentException("builtFrom needs a function");
        if (held.isEmpty() || held.contains(null)) {
            throw new IllegalArgumentException(name(factory) + " is built from its parts only when each is a Held"
                    + " part: write held.part() at every place");
        }
        if (new HashSet<>(held).size() != held.size()) {
            throw new IllegalArgumentException(name(factory) + " names one Held part at two places; declare one per"
                    + " place, so each is read by its own name");
        }
        List<Held<?, ?>> at = held;
        return withBuild(given -> {
            List<Object> read = each(at, given, (place, part) -> place.read(part));
            if (read == null) return null;
            try {
                T value = build.apply(new Held.Parts(at, read));
                return type.isInstance(value) ? value : null;
            } catch (RuntimeException refused) {
                return null;
            }
        });
    }

    /**
     * {@code read} of each part at its place, one place per part — or {@code null} for another count, or a part
     * {@code read} answers {@link #REFUSED} for.
     */
    private static <P> List<Object> each(List<P> places, List<Object> given, BiFunction<P, Object, Object> read) {
        if (given == null || given.size() != places.size()) return null;
        List<Object> out = new ArrayList<>(given.size());
        for (int i = 0; i < given.size(); i++) {
            Object part = read.apply(places.get(i), given.get(i));
            if (part == REFUSED) return null;
            out.add(part);
        }
        return Collections.unmodifiableList(out);
    }

    /** This shape, a call with no parts, built as {@code made} rather than by calling its factory. */
    CallShape<T> withBuiltAs(Supplier<? extends T> made) {
        if (made == null) throw new IllegalArgumentException("builtAs needs a supplier");
        if (!parts.isEmpty()) {
            throw new IllegalArgumentException(name(factory) + " takes " + parts.size() + " parts; builtAs is for a"
                    + " call with none, builtFrom for one with parts");
        }
        return withBuild(given -> given != null && given.isEmpty() ? made.get() : null);
    }

    /** The {@code public static final} fields of {@link #type} holding exactly these values. */
    CallShape<T> withConstants(Object... values) {
        List<Field> found = new ArrayList<>(values.length);
        for (Object value : values) found.add(constant(value));
        return copy(build, List.copyOf(found), withers, writes);
    }

    /** This shape writing only the values {@code writes} accepts; see {@link DeclaredCall#when}. */
    CallShape<T> withWrites(Predicate<? super T> writes) {
        return copy(build, constants, withers, writes);
    }

    /**
     * This shape with {@code wither} chained after the ones it has, or in the place of the link its method's
     * {@link Link} mark gave. Refused on a varargs factory, whose parts are any number, so the wither's part
     * would have no place; and for a method already declared, which a value would set twice.
     */
    CallShape<T> withWither(Wither<T> wither) {
        if (factory.isVarArgs()) {
            throw new IllegalArgumentException(name(factory) + " is varargs, so no wither can follow its parts");
        }
        List<Wither<T>> more = new ArrayList<>(withers);
        for (int i = 0; i < more.size(); i++) {
            Method chained = more.get(i).method();
            if (!chained.getName().equals(wither.method().getName())
                    || !Arrays.equals(chained.getParameterTypes(), wither.method().getParameterTypes())) {
                continue;
            }
            if (!more.get(i).inferred()) {
                throw new IllegalArgumentException(wither.method().getName() + " is chained twice");
            }
            more.set(i, wither);
            return copy(build, constants, List.copyOf(more), writes);
        }
        more.add(wither);
        return copy(build, constants, List.copyOf(more), writes);
    }

    /** Every part: the factory's, then one per wither. */
    List<Class<?>> allParts() {
        if (withers.isEmpty()) return parts;
        List<Class<?>> out = new ArrayList<>(parts);
        for (Wither<T> wither : withers) out.add(wither.partType());
        return List.copyOf(out);
    }

    /** {@code value} taken apart: the factory's parts, then each wither's. */
    List<Object> taken(T value) {
        List<Object> base = components.apply(value);
        if (withers.isEmpty() || base == null) return base;
        List<Object> out = new ArrayList<>(base);
        for (Wither<T> wither : withers) out.add(wither.part(value));
        return Collections.unmodifiableList(out);
    }

    /**
     * The value back: the factory's parts built, then each wither applied whose part differs from the one the
     * factory made — so a wither that would refuse the factory's own default is never asked to take it. Given
     * the factory's parts alone, the value the factory makes: what the host compares a value with to know
     * which links it needs. {@code null} for any other count.
     */
    T built(List<Object> given) {
        if (withers.isEmpty() || given == null) return build.apply(given);
        int fixed = parts.size();
        if (given.size() != fixed && given.size() != fixed + withers.size()) return null;
        T made = build.apply(given.subList(0, fixed));
        T value = made;
        for (int i = 0; value != null && i < given.size() - fixed; i++) {
            Wither<T> wither = withers.get(i);
            Object part = given.get(fixed + i);
            if (Objects.equals(part, wither.part(made))) continue;
            value = wither.apply(value, part);
        }
        return value;
    }

    private Field constant(Object value) {
        for (Field field : type.getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) continue;
            try {
                if (field.get(null) == value) return field;
            } catch (IllegalAccessException ignored) {
                // Not readable, so not a constant the host could write either.
            }
        }
        throw new IllegalArgumentException(value + " is not a public static final constant of " + type.getName());
    }

    /**
     * The parts a factory is written with: its parameters, after the receiver when it is an instance method.
     * A varargs factory's last part is the element type, which the host repeats for every further argument.
     */
    static List<Class<?>> partsOf(Executable factory) {
        List<Class<?>> out = new ArrayList<>();
        if (factory instanceof Method m && !Modifier.isStatic(m.getModifiers())) out.add(m.getDeclaringClass());
        Class<?>[] parameters = factory.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            boolean repeated = factory.isVarArgs() && i == parameters.length - 1;
            out.add(repeated ? parameters[i].getComponentType() : parameters[i]);
        }
        return List.copyOf(out);
    }

    private static void checkResult(Class<?> type, Executable factory) {
        Class<?> result = factory instanceof Method m ? m.getReturnType() : factory.getDeclaringClass();
        if (!box(result).isAssignableFrom(box(type)) && !box(type).isAssignableFrom(box(result))) {
            throw new IllegalArgumentException(name(factory) + " returns " + result.getName() + ", not a "
                    + type.getName());
        }
    }

    private static <T> Function<T, List<Object>> taking(List<Function<? super T, ?>> accessors, boolean spreadLast) {
        List<Function<? super T, ?>> fixed = List.copyOf(accessors);
        return value -> {
            List<Object> out = new ArrayList<>(fixed.size());
            for (int i = 0; i < fixed.size(); i++) {
                Object part = Held.taken(fixed.get(i), value);
                if (spreadLast && i == fixed.size() - 1 && part instanceof Collection<?> each) out.addAll(each);
                else out.add(part);
            }
            return Collections.unmodifiableList(out);
        };
    }

    /**
     * {@code invoke} handed each held part as its factory parameter takes it ({@link Held#part(Function)}); a part
     * read as anything but its held kind is not this call. {@code invoke} itself when no accessor is held.
     */
    private static <T> Function<List<Object>, T> handing(List<Function<? super T, ?>> accessors,
                                                         Function<List<Object>, T> invoke) {
        List<Function<? super T, ?>> fixed = List.copyOf(accessors);
        if (fixed.stream().allMatch(accessor -> Held.at(accessor) == null)) return invoke;
        return parts -> {
            List<Object> handed = each(fixed, parts, Held::handed);
            return handed == null ? null : invoke.apply(handed);
        };
    }

    /**
     * The value back by invoking {@code factory} on the parts, each coerced to the parameter it fills — or
     * {@code null} for parts this call does not build: too few or too many, one of the wrong kind, or a
     * factory that refuses them. A null is the host's "not this shape", so the source stays as written.
     */
    private static <T> Function<List<Object>, T> invoking(Class<T> type, Executable factory) {
        boolean receiver = factory instanceof Method m && !Modifier.isStatic(m.getModifiers());
        Class<?>[] parameters = factory.getParameterTypes();
        int fixed = parameters.length - (factory.isVarArgs() ? 1 : 0);
        return parts -> {
            if (parts == null) return null;
            int offset = receiver ? 1 : 0;
            int count = parts.size() - offset;
            if (count < fixed || (!factory.isVarArgs() && count != parameters.length)) return null;
            Object on = null;
            if (receiver) {
                on = parts.getFirst();
                if (!factory.getDeclaringClass().isInstance(on)) return null;
            }
            Object[] arguments = new Object[parameters.length];
            for (int i = 0; i < fixed; i++) {
                Object argument = coerce(parts.get(offset + i), parameters[i]);
                if (argument == REFUSED) return null;
                arguments[i] = argument;
            }
            if (factory.isVarArgs()) {
                Class<?> element = parameters[fixed].getComponentType();
                Object tail = Array.newInstance(element, count - fixed);
                for (int i = 0; i < count - fixed; i++) {
                    Object argument = coerce(parts.get(offset + fixed + i), element);
                    if (argument == REFUSED || (argument == null && element.isPrimitive())) return null;
                    Array.set(tail, i, argument);
                }
                arguments[fixed] = tail;
            }
            try {
                Object built = factory instanceof Constructor<?> c ? c.newInstance(arguments)
                        : ((Method) factory).invoke(on, arguments);
                return type.isInstance(built) ? type.cast(built) : null;
            } catch (ReflectiveOperationException | IllegalArgumentException | ClassCastException refused) {
                return null;
            }
        };
    }

    static final Object REFUSED = new Object();

    /** {@code part} as a {@code to} argument, or {@link #REFUSED}. Numbers widen and round; nothing else converts. */
    static Object coerce(Object part, Class<?> to) {
        Class<?> boxed = box(to);
        if (part == null) return to.isPrimitive() ? REFUSED : null;
        if (boxed.isInstance(part)) return part;
        if (part instanceof Number n) {
            boolean integral = n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
            long whole = integral ? n.longValue() : Math.round(n.doubleValue());
            if (boxed == Integer.class) return (int) whole;
            if (boxed == Long.class) return whole;
            if (boxed == Short.class) return (short) whole;
            if (boxed == Byte.class) return (byte) whole;
            if (boxed == Double.class) return n.doubleValue();
            if (boxed == Float.class) return n.floatValue();
        }
        return REFUSED;
    }

    private static Class<?> box(Class<?> type) {
        return type.isPrimitive() ? MethodType.methodType(type).wrap().returnType() : type;
    }

    private static String name(Executable factory) {
        return factory.getDeclaringClass().getName() + (factory instanceof Constructor<?> ? ".<init>" : "." + factory.getName());
    }
}
