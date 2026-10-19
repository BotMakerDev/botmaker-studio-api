package com.botmaker.plugin.api.source;

import java.util.Optional;

/**
 * What a conditional parameter's condition reads ({@link ManagedParam.ConditionStep#when}): the bot's other values of the
 * same plugin, as they stand in its Java. Built by the host, never by a plugin.
 *
 * <p>Only the plugin's own values: a condition is about the plugin's own surface — "the VM parameter only while the
 * settings run the game in a VM". Anything about the computer (its system, what is installed) the plugin's own
 * predicate reads itself; the host has nothing to add there.
 */
public interface ParamScope {

    /**
     * What the bot holds for {@code value} — a method's value or another parameter's — or empty when it holds none
     * the grammar reads. A host with no project answers empty.
     */
    default <V> Optional<V> value(ManagedValue<V> value) {
        return Optional.empty();
    }

    /** A scope with nothing in it: every value empty. */
    ParamScope NONE = new ParamScope() {};
}
