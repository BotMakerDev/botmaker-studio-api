package com.botmaker.plugin.api;

import com.botmaker.plugin.api.source.ManagedValue;

import java.util.List;

/**
 * A class giving the managed values a plugin's windows keep, named by {@link PluginDeclaration#values(Class[])}.
 * See {@link TypeHolder} for how a holder is built and asked.
 */
public interface ValueHolder {

    /** The managed values, each a {@code static final} field — {@link StudioPlugin#managedValues()}. */
    List<ManagedValue<?>> values();
}
