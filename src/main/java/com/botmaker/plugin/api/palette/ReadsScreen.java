package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What this member gives back was read off the game's screen: text recognised in a picture, an element's label.
 *
 * <p>A host that traces calls (Studio does: {@code docs/refactor/40-run-trace.md}) marks such a result in the run's
 * log, which keeps it as it is, and hides it in anything that leaves the machine — a report sent to the bot's
 * author, an exported log — since a game's screen can show the runner's name, chat or account. Mark every member
 * whose result can hold such text; nothing guesses it from a name. A host that traces nothing never looks.
 *
 * <p>{@code RUNTIME}, because the host reads it off the member the bot calls, by this annotation's name.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReadsScreen {
}
