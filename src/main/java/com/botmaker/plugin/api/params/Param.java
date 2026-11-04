package com.botmaker.plugin.api.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What the person running this bot may change — <b>the declaration itself, in the bot's own Java</b>.
 *
 * <h2>On an interface: a schema (2026-11-04)</h2>
 *
 * <p>An interface marked {@code @Param} is a <b>machine-scope schema</b>: each {@code default} method is one
 * parameter, its body the author's value. The interface is committed; this machine's values are a class
 * implementing it in the bot's {@code values} package, {@code values/<Schema>Values.java}, which is never committed,
 * versioned or published, and which overrides only the methods this machine has a value for.
 *
 * <pre>{@code
 * @Param
 * public interface RunChoices {
 *     @Param.Shown(description = "How long to rest between attempts", visibility = Param.PUBLIC)
 *     default Duration restBetween() { return Duration.ofMinutes(1); }
 * }
 * }</pre>
 *
 * <p>The members on the interface are each method's own when its {@link Shown} names none. In the Parameters window
 * an {@link #EDITOR} parameter is the author's, so a change rewrites the {@code default} body; a {@link #PUBLIC} one
 * is the runner's, so a change writes the values class.
 *
 * <h2>On a field: the form before schemas</h2>
 *
 * <p><b>Deprecated for removal</b> (2026-11-04): a field's value is Java in the bot, so the runner's choices reach the
 * bot's remote with its code. Studio's upgrade turns a bot's {@code @Param} fields into a schema; the field form is
 * removed in the next release. The rest of this page describes it.
 *
 * <pre>{@code
 * public final class Parameters {
 *     @Param(description = "How long to rest between attempts")
 *     public static Duration restBetween = Duration.ofSeconds(3);
 *
 *     @Param(category = "Limits")
 *     public static int maxAttempts = 10;
 * }
 * }</pre>
 *
 * <p>The bot reads {@code Parameters.maxAttempts} — a field, so a misspelling is a compile error and the
 * type is the type. Studio reads the same fields off the syntax tree and draws the Parameters window from
 * them; editing a value there rewrites the initializer, and adding a row writes a new field.
 *
 * <h2>Why it is a field, not a name</h2>
 *
 * <p>A parameter read back by name ({@code load("maxAttempts", int.class)}) is a string on both sides: a typo
 * compiles and answers the type's fallback, the declaration lives where the author cannot see it, and the
 * editor has to own a vocabulary of parameter kinds. A field is checked by javac and sits in the author's own
 * file.
 *
 * <h2>The rules Studio applies</h2>
 *
 * <p>All of them are about what a value cell can safely edit, and a field that breaks one is <b>shown
 * read-only</b> rather than refused: the bot still compiles, and the author is told why the cell is grey.
 *
 * <ul>
 *   <li><b>{@code public static}</b>, and not {@code final} if the runner is ever to override it. A
 *       {@code final} field is read-only in the window; a non-{@code public} one is not a parameter at all.
 *   <li><b>A type some plugin declares</b> — one the loaded plugins offer a
 *       {@link com.botmaker.plugin.api.value.PluginType} for, a {@code List<T>} or {@code Map<K, V>} of
 *       those, an {@code enum} whose constants become the choices, or a {@code record} the bot itself
 *       declares. An undeclared type reads as itself and is shown read-only.
 *   <li><b>An initializer the host's grammar understands</b>: a literal, an enum constant, or the
 *       constructor or factory call the type's own
 *       {@link com.botmaker.plugin.api.value.ComponentType} describes. A computed initializer (a method
 *       call, an expression over another field) is kept, shown, and never rewritten.
 * </ul>
 *
 * <h2>Why it is in the contract</h2>
 *
 * <p>This annotation sits on a <b>bot's</b> own fields, and a bot has the contract jar because the SDK
 * declares this module at {@code compile}. Nothing about rule 2 changes:
 * {@code PluginLoader} is parent-first for {@code com.botmaker.plugin.api.**}, so a plugin still links
 * against the <em>host's</em> copy however many are on a bot's classpath.
 *
 * <h2>It describes the field, never the value</h2>
 *
 * <p>No range and no list of choices (removed 2026-10-29, the user's call): a rule about what a value may be
 * belongs to the value's type — an {@code enum} for a closed choice, or a type of its own whose editor knows its
 * limits. Each member here is about the row, so the contract between Studio and a plugin carries no value rules.
 *
 * @see com.botmaker.plugin.api.managed.ManagedMarker the same idea for a value a <em>plugin's</em> window keeps
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Param {

    /**
     * How one method of a {@code @Param} schema is shown: its rail heading, its line, who sees it. Each member left
     * at its default takes the schema's own ({@code @Param(category = "Limits")} on the interface).
     *
     * <p>A second annotation rather than {@code @Param} on the method: {@code @Param} on a type says what the type
     * <em>is</em>, and a method of it is a parameter by being there, marked or not.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Shown {

        /** As {@link Param#category()}; {@code ""} takes the schema's. */
        String category() default "";

        /** As {@link Param#description()}; {@code ""} says nothing. */
        String description() default "";

        /** As {@link Param#visibility()}; {@code ""} takes the schema's. */
        String visibility() default "";
    }

    /**
     * The Parameters window only. The default, and what anything unrecognised is read as.
     *
     * <p>Constants rather than two string literals in Studio and two more in whatever reads a bot: an
     * annotation may declare them, and the side that defines the vocabulary is the side that should spell
     * it. A {@code String} rather than an enum: an annotation element's value is written into a bot's class
     * file, and pinning that to an enum this module owns would make renaming a constant a break in every
     * compiled bot.
     */
    String EDITOR = "editor";

    /** The Runner too: the person starting a run changes it without opening the project. */
    String PUBLIC = "public";

    /**
     * Who sees a parameter, typed: what a plugin's own parameter is declared with
     * ({@code ManagedValue.param(…).shown(…, Param.Visibility.PUBLIC)}). The annotation element stays a
     * {@code String} for the reason {@link #EDITOR} gives; {@link #id()} is that string.
     */
    enum Visibility {

        /** {@link Param#EDITOR}: the Parameters window only. */
        EDITOR(Param.EDITOR, "Editor only"),

        /** {@link Param#PUBLIC}: the Runner too. */
        PUBLIC(Param.PUBLIC, "Editor and Runner");

        private final String id;
        private final String displayName;

        Visibility(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** The annotation's spelling: {@code "editor"} or {@code "public"}. */
        public String id() {
            return id;
        }

        /** How a window names it. */
        public String displayName() {
            return displayName;
        }

        /** The visibility {@code id} spells; anything else is {@link #EDITOR}, which shows a parameter to fewer people. */
        public static Visibility fromId(String id) {
            return PUBLIC.id.equals(id) ? PUBLIC : EDITOR;
        }
    }

    /**
     * The rail heading this parameter appears under, free text, {@code ""} for the section's default group.
     *
     * <p>Free text and not an enumeration: the categories were the SDK plugin's declared list until this
     * annotation existed ({@code Timing}, {@code Targets}, {@code Vision}, {@code Input}, {@code Limits},
     * {@code Debug}), which is a vocabulary — and the contract grows capabilities, never vocabularies. The
     * window lists the distinct strings it finds.
     */
    String category() default "";

    /** One line under the field name in the window. The field's own javadoc is not read; say it here. */
    String description() default "";

    /**
     * Who may see and change it: {@link #EDITOR} (the default — the Parameters window only) or
     * {@link #PUBLIC} (also the Runner, where the person running the bot changes it before a run).
     *
     * <p>Anything else is read as {@link #EDITOR}, which is the answer that shows a parameter to fewer
     * people rather than more.
     */
    String visibility() default EDITOR;
}
