package com.botmaker.plugin.api.value;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One named link a call is written with after its factory: an instance method of the type answering a copy
 * with one thing changed. Declared with {@link DeclaredCall#with} or {@link DeclaredCall#flag}, read by the
 * host, never built by hand.
 *
 * <pre>{@code
 * Flow.activity(COLLECT, Collect::body).described("Picks up ore").goesHome()
 * }</pre>
 *
 * <p><b>Why a chain and not more arguments.</b> {@code Flow.activity(COLLECT, Collect::body, "Picks up ore",
 * true, false, true)} is a call nobody can read without the declaration open: which {@code true} goes home?
 * A chain names every part it sets, and leaves out every part that is as the factory makes it, so a developer
 * reading the bot with no BotMaker tools reads what it says.
 *
 * <p><b>Two kinds.</b>
 *
 * <ul>
 *   <li>A <b>with</b> takes one argument, the part it sets: {@code .described("Picks up ore")}.</li>
 *   <li>A <b>flag</b> takes none and turns one thing on: {@code .goesHome()}. It is written only when it is
 *       on and the factory's value has it off, so <b>a setting that is on by default gets a negative name</b>
 *       ({@code .off()}, {@code .staysPut()}): a flag cannot be written to turn something off.</li>
 * </ul>
 *
 * <p><b>Its part is one of the call's components.</b> A declaration with withers takes a value apart into the
 * factory's parts followed by one part per wither, in declaration order — a flag's is a {@code Boolean} — and
 * {@link ComponentType#componentTypes()} lists them the same way, so {@code build(components(v))} equals
 * {@code v} as for any call, and a generic editor shows a field for each. Given only the factory's parts,
 * {@code build} answers the value the factory alone makes: that is how the host knows which links a value
 * needs, writing each whose part differs from it.
 *
 * @param <T> the class the call builds, and each link answers
 */
public final class Wither<T> {

    private final Class<T> type;
    private final Method method;
    private final Function<? super T, ?> part;
    private final boolean flag;

    private Wither(Class<T> type, Method method, Function<? super T, ?> part, boolean flag) {
        this.type = type;
        this.method = method;
        this.part = part;
        this.flag = flag;
    }

    static <T> Wither<T> with(Class<T> type, Ref wither, Function<? super T, ?> part) {
        return new Wither<>(type, checked(type, wither, 1), required(part), false);
    }

    static <T> Wither<T> flag(Class<T> type, Ref wither, Predicate<? super T> on) {
        Predicate<? super T> test = required(on);
        return new Wither<>(type, checked(type, wither, 0), value -> test.test(value), true);
    }

    private static <F> F required(F given) {
        if (given == null) throw new IllegalArgumentException("No accessor given for a wither's part");
        return given;
    }

    private static Method checked(Class<?> type, Ref wither, int arguments) {
        Executable executable = Ref.resolve(wither);
        String name = executable.getDeclaringClass().getName() + "." + executable.getName();
        if (!(executable instanceof Method method) || Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(name + " is not an instance method; a wither is called on the"
                    + " value it changes");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(name + " is not public; a bot calls it");
        }
        if (!method.getDeclaringClass().isAssignableFrom(type)) {
            throw new IllegalArgumentException(name + " is not a method of " + type.getName());
        }
        if (!type.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException(name + " returns " + method.getReturnType().getName() + ", not a "
                    + type.getName() + "; a wither answers the changed value");
        }
        if (method.getParameterCount() != arguments || method.isVarArgs()) {
            throw new IllegalArgumentException(name + " takes " + method.getParameterCount() + " arguments, where "
                    + (arguments == 0 ? "a flag takes none" : "a wither takes the one part it sets"));
        }
        return method;
    }

    /** The method the link is written as. The host reads its name and parameter, and never calls it. */
    public Method method() {
        return method;
    }

    /** Whether this is a flag, written with no argument and only to turn its part on. */
    public boolean flag() {
        return flag;
    }

    /** The class of the part this link sets: its parameter's, or {@code boolean} for a flag. */
    public Class<?> partType() {
        return flag ? boolean.class : method.getParameterTypes()[0];
    }

    /** The part {@code value} holds for this link; a flag's is a {@code Boolean}. */
    public Object part(T value) {
        return part.apply(value);
    }

    /**
     * {@code value} with this link's part set to {@code part} — or {@code null} when it cannot be: a part of
     * the wrong kind, a flag asked to turn off what {@code value} has on, or a method that refuses it. A null
     * is the host's "not this shape", so the source stays as written.
     */
    public T apply(T value, Object part) {
        if (value == null) return null;
        try {
            Object changed;
            if (flag) {
                if (!(part instanceof Boolean on)) return null;
                if (!on) return Boolean.TRUE.equals(this.part.apply(value)) ? null : value;
                changed = method.invoke(value);
            } else {
                Object argument = CallShape.coerce(part, method.getParameterTypes()[0]);
                if (argument == CallShape.REFUSED) return null;
                changed = method.invoke(value, argument);
            }
            return type.isInstance(changed) ? type.cast(changed) : null;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException refused) {
            return null;
        }
    }

    /**
     * {@link #part} against a value the host holds as {@code Object}. Unchecked, and sound for the same reason
     * as {@link ComponentType#componentsOf}: only a value of the declared class is ever passed.
     */
    @SuppressWarnings("unchecked")
    public Object partOf(Object value) {
        return part((T) value);
    }

    /** {@link #apply} against a value the host holds as {@code Object}; see {@link #partOf}. */
    @SuppressWarnings("unchecked")
    public Object applyTo(Object value, Object part) {
        return type.isInstance(value) ? apply((T) value, part) : null;
    }

    @Override
    public String toString() {
        return "Wither[" + method.getDeclaringClass().getName() + "." + method.getName() + (flag ? "()" : "(…)") + "]";
    }
}
