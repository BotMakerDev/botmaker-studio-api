package com.botmaker.plugin.api.managed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A value a plugin keeps up to date through its own window — <b>the declaration itself, in the bot's own
 * Java</b>.
 *
 * <pre>{@code
 * public final class Sdk {
 *
 *     @Managed("flow")
 *     public static Flow flow() {
 *         return Flow.of(List.of(...), List.of(...), "Collect", Flow.limits(1000, 1000));
 *     }
 * }
 * }</pre>
 *
 * <p>The file this sits in was <b>copied from the plugin when it was added</b>, and is yours from that
 * moment: nothing regenerates it. BotMaker rewrites the expression a {@code @Managed} method returns and
 * nothing else, so comments, helper methods and anything else you write around them survive untouched.
 *
 * <h2>Why it is Java, not a data file</h2>
 *
 * <p>A value read back by name from a file beside the bot turns a rename into a silently empty value three
 * screens into a run, where the same rename here is a compile error — and a value in the bot's source is one
 * a developer with no BotMaker installed can read, grep, refactor and hand to a compiler.
 *
 * <h2>The rules BotMaker applies to a method</h2>
 *
 * <p>All of them are about what the window can safely rewrite, and a method that breaks one is <b>shown
 * read-only with the reason</b> rather than refused: the bot still compiles, and the author is told why.
 *
 * <ul>
 *   <li><b>{@code public static}</b>, and taking no parameters. A value is not computed from arguments.
 *   <li><b>A return type some plugin registers</b> — one of the value types, a container over them, or a
 *       record the bot itself declares. An unregistered type reads as itself and is shown read-only.
 *   <li><b>A body that is exactly one {@code return <expression>;}</b>. Anything else — a local variable, a
 *       branch, a loop — is code the author wrote deliberately, and it is kept and never rewritten.
 * </ul>
 *
 * <p>So the one method a host ever writes, rewrites or a bot's runtime installs is
 * {@code public static T id() { return <expression>; }}. A plugin's side is held to it before it is published:
 * {@code botmaker plugin validate}'s {@code managed} check writes each declared value's first
 * expression the way a host would and reads it back, so a value no host could write never reaches a bot.
 *
 * <h2>On a type instead: an open set</h2>
 *
 * <p>{@code @Managed} on a class says the plugin's window owns <em>every member of it</em>, so the code
 * canvas may not add to it, rename in it or delete from it. That is the shape for a set the user grows —
 * one constant per captured picture — where a method is the shape for a fixed value the plugin shipped a
 * declaration for. The difference is whether the rest of the bot names the parts: {@code Pictures.COLLECT}
 * is written at its use sites, and a flow's activities are not.
 *
 * <h2>A typed id instead</h2>
 *
 * <p>A plugin may give its values typed ids — its own annotation, marked {@link ManagedMarker}, whose
 * {@code value()} is an enum constant — so a bot writes {@code @SdkValue(SdkValue.Id.FLOW)} and javac checks
 * it. That is where managed ids are going; this annotation is read beside it until the plugins move.
 *
 * <h2>Why the id is a string</h2>
 *
 * <p>It is a <b>persisted identity</b>, paired with the same string on the plugin's side
 * ({@link com.botmaker.plugin.api.StudioPlugin#managedValues}), and a {@code Class} cannot be one: the
 * plugin that claims an id and the bot that carries it are on different classloaders, where comparing
 * {@code Class} objects silently answers false. An id no loaded plugin claims is an ordinary method,
 * editable on the canvas like any other.
 *
 * <p>A bot has it because the SDK declares this module at {@code compile} — see
 * {@link com.botmaker.plugin.api.params.Param} for what that does and does not change.
 *
 * @see com.botmaker.plugin.api.params.Param the same idea for a value the bot's <em>user</em> changes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Managed {

    /**
     * Which of the plugin's values this is — {@code "flow"}, {@code "capture"}, {@code "pictures"}.
     *
     * <p>Plugin-local: the plugin that shipped the file chose it and declares the same string. An id no
     * loaded plugin claims is an ordinary method, editable on the canvas like any other.
     */
    String value();
}
