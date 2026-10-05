package com.botmaker.plugin.api.catalog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The identity of one member: its declaring class, its name, and its JVM descriptor.
 *
 * <p>The descriptor is what makes an overload set tractable — {@code click(Point)} and {@code click(Rect)}
 * differ in it — and it is the spelling every consumer of the catalog has used to tell one overload from
 * another, via {@link #signature()}.
 *
 * <p>A catalog is built by reflecting the classes named in {@link PaletteCatalog#of(Class[])}, so members
 * are discovered rather than named and there is nothing for javac to check about them.
 *
 * <p><b>Constructors</b> report {@link #name()} as {@code <init>}; {@link #isConstructor()} says so.
 */
public record MemberId(Class<?> declaringClass, String name, String descriptor) {

    /** The name the JVM gives a constructor. */
    public static final String CONSTRUCTOR = "<init>";

    public MemberId {
        Objects.requireNonNull(declaringClass, "declaringClass");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(descriptor, "descriptor");
    }

    /**
     * The identity of a method already in hand — the one route in, since a catalog is built by reflection.
     *
     * <p>The descriptor is computed rather than read, and it is the JVM's own spelling: ids meet each other
     * in {@link PaletteCatalog#mergedWith}, in a Studio menu lookup and in every equality test, and an id
     * that differs only in how it was built is an entry that silently matches nothing.
     */
    public static MemberId of(Method method) {
        Objects.requireNonNull(method, "method");
        return new MemberId(method.getDeclaringClass(), method.getName(),
                descriptor(method.getParameterTypes(), method.getReturnType()));
    }

    /** As {@link #of(Method)}, for a constructor; the name is {@link #CONSTRUCTOR} and the return type void. */
    public static MemberId of(Constructor<?> constructor) {
        Objects.requireNonNull(constructor, "constructor");
        return new MemberId(constructor.getDeclaringClass(), CONSTRUCTOR,
                descriptor(constructor.getParameterTypes(), void.class));
    }

    private static String descriptor(Class<?>[] parameterTypes, Class<?> returnType) {
        StringBuilder out = new StringBuilder("(");
        for (Class<?> parameterType : parameterTypes) {
            appendDescriptor(out, parameterType);
        }
        return appendDescriptor(out.append(')'), returnType).toString();
    }

    private static StringBuilder appendDescriptor(StringBuilder out, Class<?> type) {
        Class<?> element = type;
        while (element.isArray()) {
            out.append('[');
            element = element.getComponentType();
        }
        if (!element.isPrimitive()) {
            return out.append('L').append(element.getName().replace('.', '/')).append(';');
        }
        return out.append(switch (element.getName()) {
            case "boolean" -> 'Z';
            case "byte" -> 'B';
            case "char" -> 'C';
            case "short" -> 'S';
            case "int" -> 'I';
            case "long" -> 'J';
            case "float" -> 'F';
            case "double" -> 'D';
            default -> 'V';
        });
    }

    public boolean isConstructor() {
        return CONSTRUCTOR.equals(name);
    }

    /** The fully-qualified name of the class declaring the member. */
    public String declaringClassName() {
        return declaringClass.getName();
    }

    /**
     * The declared parameter types, as source-style names ({@code com.botmaker.sdk.api.geometry.Point},
     * {@code int}, {@code java.lang.String[]}), in declaration order.
     *
     * <p>For an instance-method reference the receiver is <em>not</em> among them: the descriptor is the
     * method's own, so {@code ImageTemplate::width} reports no parameters even though as a
     * function it takes one argument.
     */
    public List<String> parameterTypeNames() {
        List<String> names = new ArrayList<>();
        int i = descriptor.indexOf('(') + 1;
        int end = descriptor.indexOf(')');
        while (i < end) {
            int dimensions = 0;
            while (descriptor.charAt(i) == '[') {
                dimensions++;
                i++;
            }
            String name;
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int semi = descriptor.indexOf(';', i);
                name = descriptor.substring(i + 1, semi).replace('/', '.');
                i = semi + 1;
            } else {
                name = primitive(c);
                i++;
            }
            names.add(name + "[]".repeat(dimensions));
        }
        return List.copyOf(names);
    }

    private String primitive(char c) {
        return switch (c) {
            case 'Z' -> "boolean";
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'S' -> "short";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'F' -> "float";
            case 'D' -> "double";
            case 'V' -> "void";
            default -> throw new IllegalStateException("unreadable descriptor " + descriptor);
        };
    }

    /**
     * A human-readable signature — {@code click(com.botmaker.sdk.api.geometry.Point)} — the spelling Studio's
     * surface queries have always used to tell one overload from another.
     */
    public String signature() {
        return name + "(" + String.join(",", parameterTypeNames()) + ")";
    }

    @Override
    public String toString() {
        return declaringClass.getName() + "#" + signature();
    }
}
