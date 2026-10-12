package com.botmaker.plugin.api.run;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * One run property a plugin keeps on this machine, declared once as a constant with how its value is spelled:
 * {@code static final RunProperty<String> MODE = RunProperty.text("com.example.mode")}.
 *
 * <p>The host keeps a run property as text beside the project and passes it to every run as
 * {@code -D<name>=<text>} ({@link com.botmaker.plugin.api.Runs#get}). This names the property and the one way its
 * text is read and written, so a plugin reads a {@code T} and never parses a property by hand in two places.
 *
 * <p>The name is a system property's, and a plugin names only its own: {@code botmaker.launch.target} belongs to
 * the plugin whose runtime reads it.
 *
 * @param <T> what the property holds
 */
public final class RunProperty<T> {

    private final String name;
    private final Function<String, ? extends T> parse;
    private final Function<? super T, String> format;

    private RunProperty(String name, Function<String, ? extends T> parse, Function<? super T, String> format) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("a run property needs a name");
        this.name = name;
        this.parse = Objects.requireNonNull(parse, "parse");
        this.format = Objects.requireNonNull(format, "format");
    }

    /**
     * A property holding a {@code T}: {@code parse} reads the stored text, answering {@code null} for text it
     * does not read, and {@code format} writes a value as text.
     */
    public static <T> RunProperty<T> of(String name, Function<String, ? extends T> parse,
                                        Function<? super T, String> format) {
        return new RunProperty<>(name, parse, format);
    }

    /** A property holding its text as it is. */
    public static RunProperty<String> text(String name) {
        return new RunProperty<>(name, Function.identity(), Function.identity());
    }

    /** The system property's name: {@code botmaker.launch.target}. */
    public String name() {
        return name;
    }

    /** What {@code text} reads as; empty for none, blank, or text {@code parse} refused or threw on. */
    public Optional<T> read(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        try {
            return Optional.ofNullable(parse.apply(text));
        } catch (RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    /**
     * {@code value} as the stored text; {@code null} for no value, which clears the property. What {@code format}
     * throws is thrown: a value the plugin's own formatter refuses is the plugin's bug, never a property to clear.
     */
    public String write(T value) {
        return value == null ? null : format.apply(value);
    }

    @Override
    public String toString() {
        return "RunProperty[" + name + "]";
    }
}
