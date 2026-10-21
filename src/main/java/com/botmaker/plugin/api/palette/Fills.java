package com.botmaker.plugin.api.palette;

import com.botmaker.plugin.api.value.PluginType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the method's return type as a value <b>the bot fills in</b>: one a bot may hold but nobody edits,
 * whose fresh value is a call to this method — {@code Vision.lastMatch()}, the match the bot found a moment ago.
 *
 * <pre>{@code
 * @Fills(label = "Picture match")
 * public static MatchResult lastMatch() { … }
 * }</pre>
 *
 * <p>The host finds every method carrying it in the plugin's jar, as it finds {@code @Palette} classes, and
 * declares the return type itself: a fresh value is written {@code Owner.method()}, and the value is drawn as a
 * pill saying {@link #label()}, with its Java in a tooltip. That is the whole declaration; nothing about the type
 * is listed elsewhere, and the plugin draws nothing.
 *
 * <p><b>Why a call and not a value.</b> Freezing the last match into a value would change what the declaration
 * means into a fabricated miss, and calling the method to obtain one would run the plugin's runtime inside a
 * headless validator. The host never calls it.
 *
 * <p><b>A public static method with no parameters</b> that returns the class it fills, declared on a class a bot
 * compiles against; the host reports any other shape as a problem with the plugin and declares nothing for it.
 * One method per class: the class is the type's identity, so a second method filling the same class is refused,
 * as is a class the plugin also declares through {@link PluginType#value}. Retention is {@code RUNTIME}: the
 * host reads it off the loaded method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Fills {

    /**
     * What the pill says, naming the <em>type</em> rather than this call — "Picture match", never "Last picture
     * match" — because it draws every value of the type the host could not read, whatever call wrote it. Blank
     * means the class's simple name.
     */
    String label() default "";
}
