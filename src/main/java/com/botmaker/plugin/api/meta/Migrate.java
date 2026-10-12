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
 * @Migrate
 * public static Picture picture(String file, double threshold) {   // ImageTemplate's components, in order
 *     return Picture.of(Path.of(file), Region.FULL, threshold);
 * }
 * }</pre>
 *
 * <p>The parameters are the source type's components, in its declaration's order; the return type is the
 * target. The body is one {@code return} of a value the host's grammar reads, in which a parameter may stand
 * for a part, and a call to another {@code @Migrate} method maps a part whose type changed too. <b>The host
 * never runs it</b>: for each managed value of the source type it substitutes that value's written parts into
 * the body and writes the result beside the original, marked {@link Migrated}. The original stays.
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
public @interface Migrate {}
