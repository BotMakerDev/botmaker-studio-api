package com.botmaker.plugin.api;

/**
 * A class holding the managed values a plugin's windows keep, named by {@link PluginDeclaration#values(Class[])}:
 * each {@code public static final} {@link com.botmaker.plugin.api.source.ManagedValue} field, or a list of them,
 * in the order the fields are declared.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface ValueHolder {
}
