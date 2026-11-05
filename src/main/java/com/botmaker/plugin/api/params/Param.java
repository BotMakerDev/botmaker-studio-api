package com.botmaker.plugin.api.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What the person running this bot may change: <b>the user's data, whole and private</b>, in the bot's own Java.
 *
 * <h2>On a class: a parameter group (2026-11-04)</h2>
 *
 * <p>A class marked {@code @Param} in the bot's {@code params} package is one <b>group of parameters</b>. Each
 * instance method with no parameter is one parameter, whole: its name, its return type and, as its body, this
 * computer's value. The bot reads a parameter by calling its method.
 *
 * <pre>{@code
 * @Param
 * public class RunChoices {
 *     @Param.Shown(description = "How long to rest between attempts")
 *     public Duration restBetween() { return Duration.ofMinutes(1); }
 *
 *     @Param.Shown(changes = Param.Changes.DURING_RUN)
 *     public Instant lastClaim() { return Instant.EPOCH; }
 * }
 * }</pre>
 *
 * <p>Every parameter is private: the {@code params} package is never committed, versioned or published. The author's
 * starting values are the same files under the bot's {@code starting-params/} folder, which is committed; a host fits
 * {@code params} to it (a parameter the starting copy lacks is dropped, one it retyped is reset, one it adds is
 * added), so a fresh copy of a bot runs at once and a stale value never stops a build.
 *
 * <p><b>The group is the category</b> (2026-11-05): the Parameters window lists a parameter under the class that
 * holds it, {@code RunChoices} above, and there is no other heading. A free-text {@code category} member was removed;
 * Studio's upgrade strips it from a bot.
 *
 * <p>Each parameter changes either outside a run (the Parameters window edits it, the bot only reads it) or during
 * a run (the bot writes it, the window shows it read-only): {@link Shown#changes()}.
 *
 * <p>A value only the author sets is not a parameter: it is the bot's data, a plain constant of the bot or a value
 * a plugin marks as its own ({@link com.botmaker.plugin.api.managed.ManagedMarker}).
 *
 * <h2>On an interface: the schema form</h2>
 *
 * <p><b>Replaced</b> (2026-11-04): a {@code @Param} interface whose {@code default} methods were the author's values,
 * beside a private values class overriding them. A parameter is one thing now, so the interface and its values class
 * become one group class; Studio's upgrade does it.
 *
 * <h2>On a field: the form before groups</h2>
 *
 * <p><b>Deprecated for removal</b> (2026-11-04): a field's value is Java in the bot, so the runner's choices reach the
 * bot's remote with its code. Studio's upgrade turns a bot's {@code @Param} fields into a group; the field form is
 * removed in the next release.
 *
 * <pre>{@code
 * public final class Parameters {
 *     @Param(description = "How long to rest between attempts")
 *     public static Duration restBetween = Duration.ofSeconds(3);
 * }
 * }</pre>
 *
 * <h2>The rules Studio applies</h2>
 *
 * <p>All of them are about what a value cell can safely edit, and a parameter that breaks one is <b>shown
 * read-only</b> rather than refused: the bot still compiles, and the author is told why the cell is grey.
 *
 * <ul>
 *   <li><b>A type some plugin declares</b> — one the loaded plugins offer a
 *       {@link com.botmaker.plugin.api.value.PluginType} for, a {@code List<T>} or {@code Map<K, V>} of
 *       those, an {@code enum} whose constants become the choices, or a {@code record} the bot itself
 *       declares. An undeclared type reads as itself and is shown read-only.
 *   <li><b>A value the host's grammar understands</b>: a literal, an enum constant, or the
 *       constructor or factory call the type's own
 *       {@link com.botmaker.plugin.api.value.ComponentType} describes. A computed value (a method
 *       call, an expression over another parameter) is kept, shown, and never rewritten.
 * </ul>
 *
 * <h2>Why it is in the contract</h2>
 *
 * <p>This annotation sits on a <b>bot's</b> own classes, and a bot has the contract jar because the SDK
 * declares this module at {@code compile}. Nothing about rule 2 changes:
 * {@code PluginLoader} is parent-first for {@code com.botmaker.plugin.api.**}, so a plugin still links
 * against the <em>host's</em> copy however many are on a bot's classpath.
 *
 * <h2>It describes the parameter, never the value</h2>
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
     * How one method of a {@code @Param} group is shown, and when it changes. Each text member left at its default
     * takes the group's own ({@code @Param(description = "…")} on the class).
     *
     * <p>A second annotation rather than {@code @Param} on the method: {@code @Param} on a type says what the type
     * <em>is</em>, and a method of it is a parameter by being there, marked or not.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Shown {

        /** As {@link Param#description()}; {@code ""} says nothing. */
        String description() default "";

        /**
         * When the parameter changes: {@link Changes#OUTSIDE_RUN} (the default: the Parameters window edits it) or
         * {@link Changes#DURING_RUN} (the bot writes it; the window shows it read-only).
         */
        Changes changes() default Changes.OUTSIDE_RUN;

        /**
         * As {@link Param#visibility()}; {@code ""} takes the group's.
         *
         * @deprecated every parameter is the user's: a value only the author sets is the bot's data, not a
         *     parameter. Use {@link #changes()}.
         */
        @Deprecated(forRemoval = true)
        String visibility() default "";
    }

    /**
     * When a run last wrote this during-a-run parameter's value (2026-11-04): only a run writes it, on the method in
     * the bot's {@code params} package, never by hand and never in the starting copy. It belongs to the value, so a
     * host's fit keeps it with the value and setting the value back to its starting one takes it away. A plugin reads
     * it to tell how old a value is.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Kept {

        /** The time, as {@link java.time.Instant#toString()} spells it. */
        String at();
    }

    /**
     * When a parameter changes (2026-11-04): it is never "set by the user or by the code", but edited outside a run
     * or written during one.
     *
     * <p>An enum, unlike {@link Visibility}'s string: every host reads it off the bot's source, and a bot is
     * recompiled from its source on every run. The constant is still written into a compiled bot's class file, so a
     * reader of an old class file would fail on a renamed constant: rename one only with a {@code @Migrate} rule,
     * never in place.
     */
    enum Changes {

        /** Edited outside a run, in the Parameters window; the bot only reads it. */
        OUTSIDE_RUN("outside-run", "Outside a run"),

        /** Written by the bot during a run (a game fact); read-only in the window. */
        DURING_RUN("during-run", "During a run");

        private final String id;
        private final String displayName;

        Changes(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /** A stable spelling for a tool or a file: {@code "outside-run"} or {@code "during-run"}. */
        public String id() {
            return id;
        }

        /** How a window names it. */
        public String displayName() {
            return displayName;
        }

        /**
         * The constant {@code id} spells, by {@link #id()} or by constant name; anything else is {@link #OUTSIDE_RUN},
         * the default a method without {@link Shown} has.
         */
        public static Changes fromId(String id) {
            for (Changes each : values()) {
                if (each.id.equals(id) || each.name().equals(id)) return each;
            }
            return OUTSIDE_RUN;
        }
    }

    /**
     * The Parameters window only. The default, and what anything unrecognised is read as.
     *
     * @deprecated a value only the author sets is the bot's data, not a parameter (2026-11-04).
     */
    @Deprecated(forRemoval = true)
    String EDITOR = "editor";

    /**
     * The Runner too: the person starting a run changes it without opening the project.
     *
     * @deprecated every parameter is the user's now (2026-11-04).
     */
    @Deprecated(forRemoval = true)
    String PUBLIC = "public";

    /**
     * Who sees a parameter, typed: what a plugin's own parameter is declared with
     * ({@code ManagedValue.param(…).shown(…, Param.Visibility.PUBLIC)}).
     *
     * @deprecated a value only the author sets is the bot's data, not a parameter, so there is no one to hide a
     *     parameter from (2026-11-04). When a parameter changes is {@link Changes}.
     */
    @Deprecated(forRemoval = true)
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

    /** One line under the parameter's name in the window. Its javadoc is not read; say it here. */
    String description() default "";

    /**
     * Who may see and change it: {@link #EDITOR} or {@link #PUBLIC}.
     *
     * @deprecated every parameter is the user's (2026-11-04); a value only the author sets is the bot's data.
     */
    @Deprecated(forRemoval = true)
    String visibility() default EDITOR;
}
