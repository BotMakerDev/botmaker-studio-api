package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Written on a bot's function by a host: <em>a refactor went through here and guessed</em>.
 *
 * <p>A refactor either knows the answer or has to guess one. A rename knows it — every use is found by binding
 * and rewritten, and the result is compiled before it is kept — so it leaves nothing behind. A guess is a value
 * the user never chose, standing where one of theirs used to be: a plugin upgrade that replaces a call to a
 * member the new version no longer offers with a default value, a signature change that fills a new
 * parameter at every call site, a plugin rewriting the uses of something it renamed. Each of those compiles,
 * and each can be wrong. This annotation is the record of it, on the function it happened in.
 *
 * <pre>{@code
 * @Refactor("the call to Mouse.click(x, y) was removed by SDK 2.0.0, and the value it produced is now false")
 * public static Outcome collect(ActivityContext ctx) { … }
 * }</pre>
 *
 * <p><b>A noun, with a state.</b> {@link #done()} false is a refactor still waiting for someone to look at it;
 * the host lists those. Marking it reviewed sets {@code done = true} rather than deleting it, so the record of
 * what happened stays with the code until the user removes the annotation themselves.
 *
 * <p><b>In the contract, not generated into the bot</b>, where it would be one more file the user never asked
 * for, whose shape the host would match by simple name. A bot that compiles the
 * contract's annotations ({@code @Param}, {@code @ManagedMarker}) compiles this one too, and the host finds it by
 * class. A host writes it only where the classpath carries it, and otherwise makes the change unmarked.
 *
 * <p>{@link RetentionPolicy#SOURCE}: it exists for the person and the host's scan, never at runtime, so a
 * marked bot compiles to exactly the bytes an unmarked one does.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Refactor {

    /** One sentence per thing the refactor guessed in this function, in the order it made them. */
    String[] value();

    /** Whether someone has looked at it. A host lists the ones still {@code false}. */
    boolean done() default false;
}
