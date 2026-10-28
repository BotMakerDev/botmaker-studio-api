package com.botmaker.plugin.api;

/**
 * A class holding the tools a plugin offers the AI assistant, named by {@link PluginDeclaration#assistant(Class[])}:
 * each {@code public static final} {@link com.botmaker.plugin.api.assist.AssistantTool} field, or a list of them,
 * in the order the fields are declared.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface AssistantHolder {
}
