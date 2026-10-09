package com.botmaker.plugin.api.managed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a plugin's own annotation the marker of its managed values, so a bot writes the plugin's vocabulary
 * and javac checks the id.
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
 * @SdkValue(SdkValue.Id.FLOW)
 * public static Flow flow() { return Flow.of(…); }
 * }</pre>
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
 * <p>A misspelt constant is a compile error where a misspelt string was a value that silently never arrived,
 * and the id is the enum's binary name plus the constant's — two plugins cannot both have one
 * ({@link com.botmaker.plugin.api.source.ManagedValue#idOf}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ManagedMarker {
}
