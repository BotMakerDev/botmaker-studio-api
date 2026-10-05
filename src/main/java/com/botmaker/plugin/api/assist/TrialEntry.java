package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.value.Ref;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * The static method Studio calls to try one statement as a bot run would run it — named by method reference,
 * {@code PluginDeclaration.trial(Bot::trial)}.
 *
 * <p>Its shape is the run entry's: a body and the classes whose {@code @Managed} values the run installs,
 * {@code Bot.trial(() -> { …the statement… }, Sdk.class)}. It does the setup a run does — capture source,
 * settings, input — then runs only the body, and returns when the body does. Studio writes the throwaway
 * caller (never into the project's sources) from the method this names, so the plugin writes no Java.
 *
 * <p>A body that throws is the trial's failure, reported like a run's; the entry need not catch it.
 */
@FunctionalInterface
public interface TrialEntry extends Ref {

    void run(Runnable body, Class<?>... values);

    /**
     * The method {@code entry} names, checked: public, static, {@code (Runnable, Class<?>...)}.
     *
     * @throws IllegalArgumentException when it is a lambda, or a method of another shape
     */
    static Method resolve(TrialEntry entry) {
        Executable named = Ref.resolve(entry);
        if (named instanceof Method method && Modifier.isStatic(method.getModifiers())
                && Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 2
                && method.getParameterTypes()[0] == Runnable.class
                && method.getParameterTypes()[1] == Class[].class) {
            return method;
        }
        throw new IllegalArgumentException(named + " is not a public static (Runnable, Class<?>...) trial entry");
    }
}
