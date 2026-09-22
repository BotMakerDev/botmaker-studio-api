package com.botmaker.plugin.api.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field the person running this bot may change — <b>the declaration itself, in the bot's own Java</b>.
 *
 * <pre>{@code
 * public final class Parameters {
 *     @Param(description = "How long to rest between attempts")
 *     public static Duration restBetween = Duration.ofSeconds(3);
 *
 *     @Param(category = "Limits", min = 1, max = 50)
 *     public static int maxAttempts = 10;
 * }
 * }</pre>
 *
 * <p>The bot reads {@code Parameters.maxAttempts} — a field, so a misspelling is a compile error and the
 * type is the type. Studio reads the same fields off the syntax tree and draws the Parameters window from
 * them; editing a value there rewrites the initializer, and adding a row writes a new field.
 *
 * <h2>What this replaced, and why it is not a name any more</h2>
 *
 * <p>Until 2026-09-17 a user parameter was a row in a plugin's JSON file, read back by name
 * ({@code Settings.load("maxAttempts", int.class)}). The name was a string on both sides, so a typo
 * compiled and answered the type's fallback, the declaration lived where the author could not see it, and
 * the editor had to own a vocabulary of parameter kinds. {@code Settings.load} stays — it is never-delete,
 * and a <em>plugin's</em> own rows are still read that way — but a <b>user</b> parameter is a field now.
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
 * <h2>Why it is here, and why it took until 2026-09-22</h2>
 *
 * <p>It lived in {@code botmaker-plugin-basics} because this module was {@code provided} on the SDK, which
 * is not transitive, so a bot had no contract jar — and this annotation sits on a <b>bot's</b> own fields.
 * The SDK declares this module at {@code compile} now, so a bot has one. Nothing about rule 2 changes:
 * {@code PluginLoader} is parent-first for {@code com.botmaker.plugin.api.**}, so a plugin still links
 * against the <em>host's</em> copy however many are on a bot's classpath.
 *
 * <p>Being here is what let {@link #min} and {@link #max} become numbers. They were strings so a duration
 * bound could be written the way a duration is ({@code "30s"}) and a plugin's codec would parse it — and no
 * plugin parses anything now.
 *
 * @see com.botmaker.plugin.api.managed.Managed the same idea for a value a <em>plugin's</em> window keeps
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Param {

    /**
     * The Parameters window only. The default, and what anything unrecognised is read as.
     *
     * <p>Constants rather than two string literals in Studio and two more in whatever reads a bot: an
     * annotation may declare them, and the side that defines the vocabulary is the side that should spell
     * it. Still a {@code String} rather than {@link com.botmaker.plugin.api.value.Visibility}: an
     * annotation element's value is written into a bot's class file, and pinning that to an enum this
     * module owns would make renaming a constant a break in every compiled bot.
     */
    String EDITOR = "editor";

    /** The Runner too: the person starting a run changes it without opening the project. */
    String PUBLIC = "public";

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

    /**
     * Inclusive lower bound. The default is no bound, and the two ends are independent — "at most 10" is a
     * sentence a person says.
     *
     * <p>Advice to the widget and a clamp when a value is normalised, never a validation that can fail: a
     * value outside the range is pulled to the nearest bound, because the alternative is a project that
     * refuses to save because of a limit somebody tightened after the fact.
     *
     * <p>A {@code double} covers every bounded type, including the ones measured by a number rather than
     * being one: a {@code Duration}'s bound is its millisecond count.
     */
    double min() default Double.NEGATIVE_INFINITY;

    /** Inclusive upper bound. See {@link #min}. */
    double max() default Double.POSITIVE_INFINITY;

    /**
     * The only values offered, each as the Java expression that writes one — a closed choice for a type
     * that is not an enum.
     *
     * <p>Empty means the type's own editor decides. For an {@code enum} field this is unnecessary: the
     * constants are the choices, and anything named here is merged with them.
     */
    String[] options() default {};
}
