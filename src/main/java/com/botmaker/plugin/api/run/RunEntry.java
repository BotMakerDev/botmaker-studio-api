package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.value.Ref;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A run a plugin offers beside the bot's own {@code main}: a title and a {@code main} named by method reference,
 * {@code RunEntry.of("Explore map", ExploreMap::main)}. Named by a {@link com.botmaker.plugin.api.RunEntryHolder}.
 *
 * <p>The host runs it as it runs the bot — same JVM options, the project's classpath, the run's log and its
 * end-of-run dialog — with this entry's class as the JVM's main class instead of the bot's. Its {@code args} are
 * the binary names of the bot's value-holder classes, the same list ▶ Try hands the trial entry, so the entry can
 * install the bot's values without naming them.
 */
public final class RunEntry {

    /** The entry's {@code main}, named by method reference. */
    @FunctionalInterface
    public interface Main extends Ref {
        void main(String[] args);
    }

    private final String title;
    private final String mainClass;

    private RunEntry(String title, String mainClass) {
        this.title = title;
        this.mainClass = mainClass;
    }

    /**
     * An entry titled {@code title} (stripped) whose run calls {@code main}. Resolving loads the method's class
     * without initialising it.
     *
     * @throws IllegalArgumentException when the title is blank, {@code main} is a lambda, or the method it names is
     *                                  not a public static {@code void main(String[])}
     */
    public static RunEntry of(String title, Main main) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A run entry needs a title");
        Executable named = Ref.resolve(Objects.requireNonNull(main, "main"));
        if (!(named instanceof Method method) || !Modifier.isStatic(method.getModifiers())
                || !Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())
                || !method.getName().equals("main") || method.getReturnType() != void.class
                || method.getParameterCount() != 1 || method.getParameterTypes()[0] != String[].class) {
            throw new IllegalArgumentException(named + " is not a public static main(String[])");
        }
        return new RunEntry(title.strip(), method.getDeclaringClass().getName());
    }

    /** What the host shows on the entry's button and what the assistant names it by. */
    public String title() {
        return title;
    }

    /** The binary name of the class whose {@code main} the run calls. */
    public String mainClass() {
        return mainClass;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RunEntry other && title.equals(other.title) && mainClass.equals(other.mainClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, mainClass);
    }

    @Override
    public String toString() {
        return "RunEntry[" + title + " → " + mainClass + "]";
    }
}
