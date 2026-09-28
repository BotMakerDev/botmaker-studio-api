package com.botmaker.plugin.api.value;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * What a declared call is made of — the factory, how a value comes apart, how it is built back, the constants
 * a value equal to one is written as — and the derivations that let a declaration state only the factory and
 * the parts. Immutable; every {@code with} answers a copy.
 */
record CallShape<T>(Class<T> type, Executable factory, List<Class<?>> parts,
                    Function<T, List<Object>> components, Function<List<Object>, T> build, List<Field> constants) {

    /** A call to {@code factory}, taken apart by {@code accessors} in its part order and built back by invoking it. */
    static <T> CallShape<T> of(Class<T> type, Ref factory, boolean spreadLast, List<Function<? super T, ?>> accessors) {
        return of(type, Ref.resolve(factory), spreadLast, accessors);
    }

    static <T> CallShape<T> of(Class<T> type, Executable factory, boolean spreadLast,
                               List<Function<? super T, ?>> accessors) {
        List<Class<?>> parts = partsOf(factory);
        if (accessors.size() != parts.size()) {
            throw new IllegalArgumentException(name(factory) + " takes " + parts.size() + " parts and "
                    + accessors.size() + " were given");
        }
        if (spreadLast && !factory.isVarArgs()) {
            throw new IllegalArgumentException(name(factory) + " is not varargs, so its last part is not spread");
        }
        checkResult(type, factory);
        return new CallShape<>(type, factory, parts, taking(accessors, spreadLast), invoking(type, factory),
                List.of());
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
        try {
            return of(type, type.getConstructor(parameters), false, accessors);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getName() + " is not a public record", e);
        }
    }

    CallShape<T> withComponents(Function<T, List<Object>> components) {
        return new CallShape<>(type, factory, parts, components, build, constants);
    }

    CallShape<T> withBuild(Function<List<Object>, T> build) {
        return new CallShape<>(type, factory, parts, components, build, constants);
    }

    /** The {@code public static final} fields of {@link #type} holding exactly these values. */
    CallShape<T> withConstants(Object... values) {
        List<Field> found = new ArrayList<>(values.length);
        for (Object value : values) found.add(constant(value));
        return new CallShape<>(type, factory, parts, components, build, List.copyOf(found));
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
                Object part = fixed.get(i).apply(value);
                if (spreadLast && i == fixed.size() - 1 && part instanceof Collection<?> each) out.addAll(each);
                else out.add(part);
            }
            return Collections.unmodifiableList(out);
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

    private static final Object REFUSED = new Object();

    /** {@code part} as a {@code to} argument, or {@link #REFUSED}. Numbers widen and round; nothing else converts. */
    private static Object coerce(Object part, Class<?> to) {
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
        return type.isPrimitive() ? java.lang.invoke.MethodType.methodType(type).wrap().returnType() : type;
    }

    private static String name(Executable factory) {
        return factory.getDeclaringClass().getName() + (factory instanceof Constructor<?> ? ".<init>" : "." + factory.getName());
    }
}
