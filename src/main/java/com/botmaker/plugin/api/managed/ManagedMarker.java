package com.botmaker.plugin.api.managed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a plugin's own annotation the marker of its managed values: a value a plugin keeps up to date through
 * its own window, declared in the bot's own Java, where the bot writes the plugin's vocabulary and javac
 * checks the id.
 *
 * <pre>{@code
 * // the plugin
 * @ManagedMarker
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.TYPE, ElementType.METHOD})
 * public @interface SdkValue {
 *     Id value();
 *
 *     enum Id { FLOW, CAPTURE, ACTIVITIES }
 * }
 *
 * public static final ManagedValue<Flow> FLOW = ManagedValue.method(SdkValue.Id.FLOW)…;
 *
 * // the bot
 * public final class Sdk {
 *
 *     @SdkValue(SdkValue.Id.FLOW)
 *     public static Flow flow() {
 *         return Flow.of(…);
 *     }
 * }
 * }</pre>
 *
 * <p>The bot's file was <b>copied from the plugin when it was added</b>, and is the bot's from that moment:
 * nothing regenerates it. BotMaker rewrites the expression a marked method returns and nothing else, so
 * comments, helper methods and anything else written around them survive untouched.
 *
 * <h2>The shape a marked annotation has</h2>
 *
 * <ul>
 *   <li><b>Runtime retention</b>, on types and methods: the bot's runtime reads it ({@link ManagedValues}).</li>
 *   <li><b>One element, {@code value()}, an enum nested in the annotation</b>: each constant is one managed
 *       value. {@link com.botmaker.plugin.api.source.ManagedValue#method(Enum)} refuses a constant of any
 *       other enum.</li>
 * </ul>
 *
 * <h2>Why typed ids</h2>
 *
 * <p>A misspelt constant is a compile error, and the id is the enum's binary name plus the constant's — two
 * plugins cannot both have one ({@link com.botmaker.plugin.api.source.ManagedValue#idOf}). A {@code Class}
 * could not be the identity: the plugin that claims an id and the bot that carries it are on different
 * classloaders, where comparing {@code Class} objects silently answers false, so the id is compared by name.
 *
 * <h2>Why it is Java, not a data file</h2>
 *
 * <p>A value read back by name from a file beside the bot turns a rename into a silently empty value three
 * screens into a run, where the same rename here is a compile error — and a value in the bot's source is one
 * a developer with no BotMaker installed can read, grep, refactor and hand to a compiler.
 *
 * <h2>The rules BotMaker applies to a marked method</h2>
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
 * <p>The marker on a class says the plugin's window owns <em>every member of it</em>, so the code canvas may
 * not add to it, rename in it or delete from it. That is the shape for a set the user grows — one constant per
 * captured picture — where a method is the shape for a fixed value the plugin shipped a declaration for. The
 * difference is whether the rest of the bot names the parts: {@code Pictures.COLLECT} is written at its use
 * sites, and a flow's activities are not.
 *
 * @see com.botmaker.plugin.api.params.Param the same idea for a value the bot's <em>user</em> changes
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ManagedMarker {
}
