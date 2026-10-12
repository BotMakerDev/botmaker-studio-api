package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Written on a bot's static method by a host: <em>how values of one type become values of another</em>, as the
 * bot's author mapped them.
 *
 * <pre>{@code
 * @Migrate(ImageTemplate.class)
 * public static Picture picture(String file, Area region, double threshold) {   // ImageTemplate's parts
 *     return Picture.of(Path.of(file), region, threshold);
 * }
 *
 * @Migrate(OldColor.class)
 * public static Color color(OldColor old) {
 *     return switch (old) { case RED -> Color.CRIMSON; case BLUE -> Color.NAVY; };
 * }
 * }</pre>
 *
 * <p>{@link #value()} is the source type and the return type the target. For a source written as a call the
 * parameters are its parts, in its declaration's order; a parameter of another type than its part's — {@code
 * Area} where the old part was an {@code OldArea} — takes that part through the {@code @Migrate} method from
 * the part's type to the parameter's. The body is one {@code return} of a value the host's grammar reads, in
 * which a parameter may stand for a part. For an enum source the one parameter is the constant and the body a
 * {@code switch} over it. <b>The host never runs it</b>: for each managed value of the source type it
 * substitutes that value's parts into the body and writes the result beside the original, marked
 * {@link Migrated}. The original stays.
 *
 * <p>A plugin never writes one: a plugin deprecates a type and points at its replacement
 * ({@link com.botmaker.plugin.api.value.PluginType#replacedBy()}); the mapping is the author's, kept in the bot
 * so it can be reopened, changed and run again.
 *
 * <p>{@link RetentionPolicy#SOURCE}, like {@link Refactor}: the host finds it by class in the bot's source.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Migrate {

    /** The type whose values this method maps — usually one its plugin marks {@link Deprecated}. */
    Class<?> value();
}
