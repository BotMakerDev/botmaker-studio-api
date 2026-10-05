package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Leave calls to this member out of a run's trace.
 *
 * <p>A host may trace every call a bot makes into an offered class, with no line written by the plugin (Studio
 * does: {@code docs/refactor/40-run-trace.md}). Two kinds of member read badly that way: one a bot
 * polls in a tight loop, whose lines bury the rest, and one that holds the whole run, such as an entry point
 * whose only line arrives when the bot stops. Mark those. Everything else needs nothing, and a user can still
 * hide any class or method from the trace themselves.
 *
 * <p>{@code RUNTIME}, because the host reads it off the member the bot calls, by this annotation's name. A host
 * that traces nothing never looks.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Untraced {

    /** Why, in the author's words. Never shown. */
    String value() default "";
}
