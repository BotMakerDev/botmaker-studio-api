package com.botmaker.plugin.api.managed;

import com.botmaker.plugin.api.source.ManagedValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Hands a bot's {@code @Managed} values to the plugins that own them, at run time, so no bot writes an
 * {@code install()}.
 *
 * <pre>{@code
 * // the plugin's runtime half, once
 * ManagedValues.claim(MyValues.GREETING, Greeter::use);
 *
 * // the bot's main, through whatever entry point the plugin offers
 * ManagedValues.install(Values.class);
 * }</pre>
 *
 * <h2>Why it is here, the one class in this module that is not an interface or a record</h2>
 *
 * <p>It is the other half of {@link Managed}: the annotation says which method holds a value, and this says
 * who takes it when the bot starts. Every plugin that puts {@code @Managed} in a bot already brings this
 * module at {@code compile}, so here the runtime costs nobody a dependency.
 *
 * <p>The bot still <b>names</b> each values class, because that is a fact only the bot has and one javac can
 * check. A {@code META-INF/services} entry or a package walked by convention would be a second statement of
 * a fact the file already carries.
 *
 * <h2>Claim ordering</h2>
 *
 * <p>A plugin claims before {@link #install} runs — from its entry point, or from the static initialiser of
 * a type its {@code @Managed} methods return. The last claim on an id wins, so claiming twice is harmless.
 */
public final class ManagedValues {

    /** Who takes each id's value. Concurrent: a static initialiser may still be claiming on another thread. */
    private static final Map<String, Consumer<Object>> SINKS = new ConcurrentHashMap<>();

    private ManagedValues() {}

    /**
     * Says that {@code sink} takes the value of every method a bot marks with {@code value}'s id —
     * {@code @SdkValue(SdkValue.Id.FLOW)} for a typed one, {@code @Managed("<id>")} otherwise.
     *
     * <p>Typed by the declaration: a value that is not a {@code T} never reaches the sink and is reported by
     * {@link #install} instead. An open set ({@link ManagedValue#isOpenSet()}) has no value and is ignored, as
     * is a value declared with no type by a plugin built against an older contract.
     */
    public static <T> void claim(ManagedValue<T> value, Consumer<? super T> sink) {
        if (value == null || value.isOpenSet() || value.type() == null || value.id() == null
                || value.id().isBlank() || sink == null) {
            return;
        }
        SINKS.put(value.id().strip(), held -> {
            T typed = value.cast(held);
            if (typed == null) {
                throw new ClassCastException((held == null ? "null" : held.getClass().getName())
                        + " is not a " + value.type().getName());
            }
            sink.accept(typed);
        });
    }

    /**
     * Invokes every managed method on each class — marked {@code @Managed} or with a plugin's
     * {@link ManagedMarker} annotation — and gives what it returns to whoever claimed its id.
     *
     * <p>Only a {@code public static} method taking no arguments is a value, which is {@link Managed}'s own
     * rule. A marked type is not installed: it holds constants the bot names where it uses them.
     *
     * <p><b>Nothing here throws.</b> An unclaimed id means that plugin is not on this bot's classpath, and a
     * method that throws is the bot author's own code failing; both are one line on {@code System.err} naming
     * the class and the id, because a value that silently never arrives is the failure this exists to end.
     * Methods are taken in name order, since reflection promises none.
     */
    public static void install(Class<?>... valueClasses) {
        if (valueClasses == null) {
            return;
        }
        for (Class<?> type : valueClasses) {
            if (type == null) {
                continue;
            }
            for (Found found : managedMethods(type)) {
                installOne(type, found.method(), found.id());
            }
        }
    }

    /** A managed method and the id it is marked with. */
    private record Found(Method method, String id) {}

    /** Every {@code public static} no-argument managed method of {@code type}, in name order. */
    private static List<Found> managedMethods(Class<?> type) {
        List<Found> found = new ArrayList<>();
        Method[] declared;
        try {
            declared = type.getDeclaredMethods();
        } catch (RuntimeException | LinkageError unreadable) {
            System.err.println("[values] " + type.getName() + " could not be read: " + unreadable);
            return List.of();
        }
        for (Method method : declared) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)
                    || method.getParameterCount() != 0) {
                continue;
            }
            String id = idOn(method);
            if (id != null) {
                found.add(new Found(method, id));
            }
        }
        found.sort(Comparator.comparing(each -> each.method().getName()));
        return found;
    }

    /**
     * The id {@code element} is marked with, or null: a plugin's {@link ManagedMarker} annotation answers
     * {@link ManagedValue#idOf} its constant, and only without one does {@code @Managed} answer its text.
     */
    static String idOn(AnnotatedElement element) {
        String typed = typedIdOn(element);
        if (typed != null) {
            return typed;
        }
        Managed managed = element.getAnnotation(Managed.class);
        return managed == null || managed.value() == null || managed.value().isBlank() ? null
                : managed.value().strip();
    }

    private static String typedIdOn(AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> marker = annotation.annotationType();
            if (!marker.isAnnotationPresent(ManagedMarker.class)) {
                continue;
            }
            try {
                Method value = marker.getDeclaredMethod("value");
                value.trySetAccessible();
                if (value.invoke(annotation) instanceof Enum<?> constant) {
                    return ManagedValue.idOf(constant);
                }
            } catch (ReflectiveOperationException | RuntimeException unreadable) {
                System.err.println("[values] " + marker.getName() + " could not be read: " + unreadable);
            }
        }
        return null;
    }

    private static void installOne(Class<?> type, Method method, String id) {
        Object value;
        try {
            value = method.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            System.err.println("[values] " + type.getSimpleName() + "." + method.getName()
                    + "() threw, so '" + id.strip() + "' is not installed: " + cause(failed));
            return;
        }
        Consumer<Object> sink = SINKS.get(id.strip());
        if (sink == null) {
            System.err.println("[values] nothing claims '" + id.strip() + "' — the plugin that reads it is "
                    + "not on this bot's classpath, so " + type.getSimpleName() + "." + method.getName()
                    + "() is ignored.");
            return;
        }
        try {
            sink.accept(value);
        } catch (RuntimeException | LinkageError refused) {
            System.err.println("[values] the plugin that claims '" + id.strip() + "' refused the value from "
                    + type.getSimpleName() + "." + method.getName() + "(): " + cause(refused));
        }
    }

    private static Throwable cause(Throwable thrown) {
        return thrown.getCause() == null ? thrown : thrown.getCause();
    }
}
