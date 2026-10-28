package com.botmaker.plugin.api;

import com.botmaker.plugin.api.assist.AssistantTool;

import java.util.List;

/**
 * A class giving the tools a plugin offers the AI assistant, named by {@link PluginDeclaration#assistant(Class[])}.
 * See {@link TypeHolder} for how a holder is built and asked.
 */
public interface AssistantHolder {

    /** The tools — {@link StudioPlugin#assistantTools()}. */
    List<AssistantTool<?>> tools();
}
