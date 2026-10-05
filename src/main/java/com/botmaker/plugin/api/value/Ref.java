package com.botmaker.plugin.api.value;

import java.io.Serializable;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * A method reference a declaration names a factory by, {@code Combo::of}, resolved to the {@link Executable}
 * the host writes and reads.
 *
 * <p><b>Why not a method name.</b> A factory used to be looked up as {@code method(Combo.class, "of",
 * Key[].class)}: a string javac never checks, beside a class list that had to match the overload by hand. A
 * reference is checked by javac, follows a rename in the IDE, and picks the overload from the types it is
 * used with. Nobody implements these interfaces or calls them: a plugin writes {@code Combo::of} where a
 * declaration step asks for one, and the step reads which method it names.
 *
 * <p><b>How it is read.</b> A {@link Serializable} method reference carries a {@link SerializedLambda}, which
 * names the class, the method and its descriptor; {@link #resolve} turns that into the {@link Method} or
 * {@link java.lang.reflect.Constructor}. It runs once, where a declaration is built — a {@code static final}
 * field — so a reference that cannot be read fails the plugin's own class initialisation and tests, never a
 * bot's file.
 *
 * <p><b>A lambda is refused.</b> {@code (a, b) -> Combo.of(a, b)} compiles to a method of the declaring class
 * with no name the host can write, so the step throws, naming the class. Write the reference itself.
 *
 * <p><b>The one case Java cannot write.</b> A class with a static and an instance method of one name and the
 * same number of arguments counting the receiver — {@code CaptureSource.region(source, rect)} and
 * {@code source.region(rect)} — makes {@code CaptureSource::region} ambiguous in javac. {@link #member} names
 * such a factory by its parts instead, and is for that case only.
 */
public interface Ref extends Serializable {

    /** A factory with no argument: {@code CaptureSource::desktop}, {@code Vision::lastMatch}. */
    @FunctionalInterface
    interface Of0<R> extends Ref {
        R call();
    }

    /** One argument, or a receiver: {@code Duration::ofMillis}, {@code ImageTemplate::new}. */
    @FunctionalInterface
    interface Of1<A, R> extends Ref {
        R call(A a);
    }

    /** Two arguments, or a receiver and one: {@code ZoneOffset::ofHoursMinutes}, {@code Precision::minArea}. */
    @FunctionalInterface
    interface Of2<A, B, R> extends Ref {
        R call(A a, B b);
    }

    /** Three: {@code LocalDate::of}, {@code Color::new}. */
    @FunctionalInterface
    interface Of3<A, B, C, R> extends Ref {
        R call(A a, B b, C c);
    }

    /** Four. */
    @FunctionalInterface
    interface Of4<A, B, C, D, R> extends Ref {
        R call(A a, B b, C c, D d);
    }

    /** Five: {@code OffsetTime::of}. */
    @FunctionalInterface
    interface Of5<A, B, C, D, E, R> extends Ref {
        R call(A a, B b, C c, D d, E e);
    }

    /** Six: {@code BotSettings::of}. */
    @FunctionalInterface
    interface Of6<A, B, C, D, E, F, R> extends Ref {
        R call(A a, B b, C c, D d, E e, F f);
    }

    /** Seven: {@code Flow::activity}. */
    @FunctionalInterface
    interface Of7<A, B, C, D, E, F, G, R> extends Ref {
        R call(A a, B b, C c, D d, E e, F f, G g);
    }

    /** Eight. */
    @FunctionalInterface
    interface Of8<A, B, C, D, E, F, G, H, R> extends Ref {
        R call(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    /** Nine. */
    @FunctionalInterface
    interface Of9<A, B, C, D, E, F, G, H, I, R> extends Ref {
        R call(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    /**
     * Ten, the most a typed {@code writtenAs} takes. A factory with more is named through a serializable
     * functional interface of the plugin's own that extends {@code Ref} — {@code interface Of12<…> extends
     * Ref { R call(…); }} — assigned to a variable of that type and passed to
     * {@link CallSteps#writtenAs(Ref, java.util.function.Function[])}. A record of any size needs none of
     * this: {@code writtenAsRecord()}.
     */
    @FunctionalInterface
    interface Of10<A, B, C, D, E, F, G, H, I, J, R> extends Ref {
        R call(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    /**
     * A method that returns nothing, with no argument — for a step that names a {@code void} call, which an
     * {@code Of} cannot: {@code Mouse::click} is not an {@code Of1<Point, R>} for any {@code R}. A step taking
     * these has a name of its own rather than an {@code Of} overload, because javac cannot choose between the
     * two for an overloaded method.
     */
    @FunctionalInterface
    interface Void0 extends Ref {
        void call();
    }

    /** A {@code void} method of one argument, or a receiver: {@code Mouse::click}. See {@link Void0}. */
    @FunctionalInterface
    interface Void1<A> extends Ref {
        void call(A a);
    }

    /** A {@code void} method of two. */
    @FunctionalInterface
    interface Void2<A, B> extends Ref {
        void call(A a, B b);
    }

    /** A {@code void} method of three. */
    @FunctionalInterface
    interface Void3<A, B, C> extends Ref {
        void call(A a, B b, C c);
    }

    /** A {@code void} method of four. */
    @FunctionalInterface
    interface Void4<A, B, C, D> extends Ref {
        void call(A a, B b, C c, D d);
    }

    /**
     * The method or constructor {@code ref} names.
     *
     * @throws IllegalArgumentException when {@code ref} is a lambda rather than a method reference, or its
     *                                  target cannot be found
     */
    static Executable resolve(Ref ref) {
        if (ref == null) throw new IllegalArgumentException("No method reference given");
        SerializedLambda form = serialized(ref);
        String name = form.getImplMethodName();
        String owner = form.getImplClass().replace('/', '.');
        if (name.startsWith("lambda$")) {
            throw new IllegalArgumentException("A lambda in " + owner + " was given where a method reference"
                    + " (Owner::method) was expected: the host can only write a call it can name");
        }
        ClassLoader loader = ref.getClass().getClassLoader();
        try {
            Class<?> declaring = Class.forName(owner, false, loader);
            MethodType signature = MethodType.fromMethodDescriptorString(form.getImplMethodSignature(), loader);
            Class<?>[] parameters = signature.parameterArray();
            if (name.equals("<init>")) return declaring.getDeclaredConstructor(parameters);
            return declaring.getDeclaredMethod(name, parameters);
        } catch (ReflectiveOperationException | TypeNotPresentException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot find " + owner + "." + name + " named by a method reference", e);
        }
    }

    /**
     * {@code owner.name(parameters)}, public — for the one factory a reference cannot name (see the class
     * comment). Throws when it is gone, so a rename fails the plugin's class initialisation.
     */
    static Method member(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getMethod(name, parameters);
            if (!Modifier.isPublic(method.getModifiers())) throw new NoSuchMethodException(name);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(owner.getName() + "." + name + Arrays.toString(parameters) + " is gone", e);
        }
    }

    private static SerializedLambda serialized(Ref ref) {
        try {
            Method writeReplace = ref.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            if (writeReplace.invoke(ref) instanceof SerializedLambda form) return form;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalArgumentException(ref.getClass().getName() + " is not a method reference", e);
        }
        throw new IllegalArgumentException(ref.getClass().getName() + " is not a method reference");
    }
}
