package com.botmaker.plugin.api.assist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The key the assistant sends a component of an {@link AssistantTool}'s parameter record under, when the
 * component's own name cannot be that key: a Java keyword ({@code class}, {@code package}) or a name a record
 * component may not take ({@code wait}). {@code record Run(@Key("wait") Integer seconds)} is sent as
 * {@code {"wait": 5}}. Without one, the key is the component's name; two components of one record may not share a
 * key.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Key {

    /** The key, as the assistant sends it. */
    String value();
}
