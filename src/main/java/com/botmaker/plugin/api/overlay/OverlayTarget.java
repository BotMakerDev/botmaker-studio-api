package com.botmaker.plugin.api.overlay;

import java.util.Objects;

/**
 * A bot method worth editing from the overlay — where its blocks go.
 *
 * <pre>{@code
 * OverlayTarget.method("com.example.bot.Collect", "body").labelled("Collect").in("Activities")
 * }</pre>
 *
 * <p>A game bot's activities are the case: each is the body of a method the flow names, and the overlay shows
 * one chip per target so the user picks the activity, not a file. The bot's classes are not on the plugin's
 * loader, so the method crosses as text: the declaring class's binary name and the method's name, the same
 * text the plugin's own values hold. The host opens that file and method; a target it cannot find is shown
 * dimmed, never guessed at.
 *
 * <p>Immutable: each step answers a copy. Two targets are equal when they name the same method.
 */
public final class OverlayTarget {

    private final String className;
    private final String method;
    private final String label;
    private final String group;

    private OverlayTarget(String className, String method, String label, String group) {
        this.className = className;
        this.method = method;
        this.label = label;
        this.group = group;
    }

    /**
     * The method {@code method} of the bot class whose binary name is {@code className}
     * ({@code "com.example.bot.Collect"}, {@code "body"}), labelled with its method name until
     * {@link #labelled} says otherwise.
     */
    public static OverlayTarget method(String className, String method) {
        if (className == null || className.isBlank()) throw new IllegalArgumentException("A target needs a class");
        if (method == null || method.isBlank()) throw new IllegalArgumentException("A target needs a method");
        return new OverlayTarget(className.trim(), method.trim(), method.trim(), "");
    }

    /** This target, its chip reading {@code label}: {@code "Collect"}. A blank label keeps the current one. */
    public OverlayTarget labelled(String label) {
        return label == null || label.isBlank() ? this : new OverlayTarget(className, method, label, group);
    }

    /** This target, gathered under the heading {@code group}: {@code "Activities"}. */
    public OverlayTarget in(String group) {
        return new OverlayTarget(className, method, label, group == null ? "" : group.trim());
    }

    /** The declaring class's binary name. */
    public String className() {
        return className;
    }

    /** The method's name. */
    public String method() {
        return method;
    }

    /** What the chip reads. */
    public String label() {
        return label;
    }

    /** The heading the chip sits under, {@code ""} for none. */
    public String group() {
        return group;
    }

    /** {@code Collect::body}, the spelling a flow value and {@code SlotContext.enclosingMethodSource} use. */
    public String methodSource() {
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.substring(simple.lastIndexOf('$') + 1) + "::" + method;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OverlayTarget that && className.equals(that.className) && method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, method);
    }

    @Override
    public String toString() {
        return className + "::" + method + " (" + label + ")";
    }
}
