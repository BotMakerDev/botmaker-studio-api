package com.botmaker.plugin.api.assist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What a parameter of an {@link AssistantTool} means, as the assistant reads it — on a component of the tool's
 * parameter record: {@code record Crop(@Describe("the picture's name") String name, …)}.
 *
 * <p>A component without one is described by its name only. {@link #optional()} lets the assistant leave it
 * out: the handler then sees {@code null}, so an optional component must be a reference type
 * ({@code Integer}, not {@code int}) — the declaration refuses an optional primitive.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Describe {

    /** One sentence, as the assistant reads it. */
    String value();

    /** Whether the assistant may leave it out. */
    boolean optional() default false;
}
