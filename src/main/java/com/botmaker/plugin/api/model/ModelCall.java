package com.botmaker.plugin.api.model;

import com.botmaker.plugin.api.value.ValueForm;

import java.util.List;
import java.util.Objects;

/**
 * One call a plugin's model may be written as — the owner, the method, and what its arguments are.
 *
 * <p><b>This is {@link com.botmaker.plugin.api.value.ValueContainer} one level up</b>, and deliberately so.
 * A container says <i>write {@code java.util.List.of(a, b, c)}</i>; a call says <i>write
 * {@code Activities.declare(a, b, c);}</i>. The host already writes the first
 * ({@code ValueCatalog.initializer}) and reads it back by matching the prefix and splitting at depth zero
 * ({@code SourceSplit}), so a statement is that with a semicolon and the machinery is the machinery that
 * exists.
 *
 * <h2>Why a plugin's model is a sequence of calls</h2>
 *
 * <p>A bot already stores the hard half this way: {@code Activities.define("Collect", ctx -> {…})} is a name
 * and a body, written as a statement in the user's own Java. Storing the rest the same way means the
 * generated file reads like the file beside it, and it removes every question a data format has to answer
 * and a statement does not — what order things go in, which constructor to call, what a derived value is
 * doing in the file. A statement is ordered by being a statement, and its arguments are arguments.
 *
 * <h2>The class is read, never loaded</h2>
 *
 * <p>Only {@link Class#getCanonicalName()} is taken off {@link #owner()}, exactly as
 * {@code ValueCatalog.forJava} does. Two classloaders make {@code ==} silently false
 * ({@code docs/refactor/25-compatibility.md} §4), so nothing here compares a class by identity and nothing
 * calls {@code Class.forName}.
 *
 * <h2>Reached through {@link #of}, never through a constructor</h2>
 *
 * <p>The same rule as {@code ParameterRow} and for the same reason: a plugin that constructed one directly
 * would take a {@code NoSuchMethodError} the day this grows a component, and a plugin's compiled classes
 * cannot be rewritten by anybody.
 */
public final class ModelCall {

    /**
     * One argument of a call: what it is called, and what may be written there.
     *
     * <p>Two kinds, because there are two things a model statement ever says. Most arguments are
     * <b>values</b> — a name, a flag, a list of outcomes — and a value has a {@link ValueForm} and a codec
     * that writes and reads it. One is a <b>body</b>: a reference to a method the user wrote, which has no
     * type of its own and no codec that could honestly claim its spelling. Keeping them apart means the
     * writer special-cases exactly one thing and the reader does too.
     */
    public sealed interface Argument {

        /** What this argument is called, for a message that has to name it. */
        String name();

        /** An argument that is a value of {@code form}. */
        record Value(String name, ValueForm form) implements Argument {

            public Value {
                name = name == null ? "" : name.strip();
                if (form == null) form = ValueForm.of(com.botmaker.plugin.api.value.ValueType.unknown(""));
            }
        }

        /** An argument that is a {@link MethodRef} — a method in the bot's own Java. */
        record Body(String name) implements Argument {

            public Body {
                name = name == null ? "" : name.strip();
            }
        }

        /** A value argument of {@code form}. */
        static Argument value(String name, ValueForm form) {
            return new Value(name, form);
        }

        /** A reference to one of the bot's own methods. */
        static Argument body(String name) {
            return new Body(name);
        }
    }

    private final Class<?> owner;
    private final String method;
    private final List<Argument> arguments;

    private ModelCall(Class<?> owner, String method, List<Argument> arguments) {
        this.owner = owner;
        this.method = method;
        this.arguments = arguments;
    }

    /**
     * A call of {@code method} on {@code owner}, taking {@code arguments} in order.
     *
     * @throws IllegalArgumentException when the owner is missing or the method is not a Java identifier —
     *                                  a call that cannot be written down is a programming error in the
     *                                  plugin, and there is a compiler-adjacent moment to notice it in
     */
    public static ModelCall of(Class<?> owner, String method, Argument... arguments) {
        Objects.requireNonNull(owner, "owner");
        String name = method == null ? "" : method.strip();
        if (name.isEmpty() || !isIdentifier(name)) {
            throw new IllegalArgumentException("\"" + method + "\" is not a method name");
        }
        return new ModelCall(owner, name, List.of(arguments == null ? new Argument[0] : arguments));
    }

    /** The class the method is called on. Read for its name, never loaded. */
    public Class<?> owner() {
        return owner;
    }

    /** The method's name. */
    public String method() {
        return method;
    }

    /** What this call takes, in the order it takes it. */
    public List<Argument> arguments() {
        return arguments;
    }

    /**
     * The stable identity of this call, which is how it is written before the bracket —
     * {@code com.botmaker.sdk.api.bot.Activities.declare}.
     *
     * <p>Derived rather than declared, on the same rule as {@code ValueContainer.factorySource()}: a call
     * already has a name in Java, and inventing a second one would be a second thing to keep in step.
     */
    public String id() {
        return ownerSource() + "." + method;
    }

    /** How a generated file names the owner when it is fully qualified. */
    public String ownerSource() {
        String canonical = owner.getCanonicalName();
        return canonical != null ? canonical : owner.getName();
    }

    /** The owner's own name, as a file importing it would write it. */
    public String simpleOwner() {
        String source = ownerSource();
        int lastDot = source.lastIndexOf('.');
        return lastDot < 0 ? source : source.substring(lastDot + 1);
    }

    @Override
    public String toString() {
        return id();
    }

    private static boolean isIdentifier(String name) {
        if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
        return name.chars().allMatch(Character::isJavaIdentifierPart);
    }
}
