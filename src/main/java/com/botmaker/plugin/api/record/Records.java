package com.botmaker.plugin.api.record;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This public static method is how a bot writes down {@link #value() a gesture} the host recorded.
 *
 * <p>The host records input itself; which call a gesture becomes is the plugin's to say, and this annotation is
 * the only place it says it. The host finds it on the plugin's {@code @Palette} classes — nothing is listed by
 * hand, and nothing is a string.
 *
 * <h2>How the host fills the parameters</h2>
 *
 * <p>Left to right, each parameter takes the first answer that fits:
 * <ol>
 *   <li>a type some loaded plugin answers with a {@link RecordedValue} — resolved at the gesture's spot;</li>
 *   <li>{@code int}, {@code long} or {@code double} — the gesture's next value;</li>
 *   <li>a type whose components are all numbers — as many of the gesture's next values as it has components,
 *       built through its declaring plugin's {@code ComponentType} (a point from {@code x, y}; a
 *       {@code Duration} from a pause's milliseconds);</li>
 *   <li>{@code String} — the typed text;</li>
 *   <li>an enum, or an enum varargs — the key's neutral name ({@code ENTER}, {@code CTRL}, {@code S}), looked
 *       up with {@code valueOf};</li>
 *   <li>a type the plugin gives a fresh value — that fresh value, written as the plugin spells it (for a
 *       capture source, the bot's current one).</li>
 * </ol>
 * <p>A method with a parameter none of these fill is not used for that gesture. Values left over are ignored,
 * so a picture click need not take the coordinates it was made at.
 *
 * <p>Among the methods that can be filled, the highest {@link #rank()} wins.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Records {

    /** The gesture this method writes down. */
    Gesture value();

    /**
     * Preference among methods for one gesture: higher wins when both can be filled. A method that needs a
     * {@link RecordedValue} ranks above the plain one it refines — a click on a picture above a click at a spot.
     */
    int rank() default 0;
}
