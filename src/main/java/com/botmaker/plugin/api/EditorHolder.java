package com.botmaker.plugin.api;

import com.botmaker.plugin.api.slot.SlotEditor;

import java.util.List;

/**
 * A class giving the editors a type cannot choose for itself, named by {@link PluginDeclaration#editors(Class[])}.
 * See {@link TypeHolder} for how a holder is built and asked.
 */
public interface EditorHolder {

    /**
     * The editors, <b>in the order the host consults them</b>, narrowest first — {@link StudioPlugin#slotEditors()}.
     */
    List<SlotEditor> editors();
}
