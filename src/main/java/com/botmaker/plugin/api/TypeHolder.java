package com.botmaker.plugin.api;

/**
 * A class holding a plugin's types, named by {@link PluginDeclaration#types(Class[])}: each {@code public static
 * final} {@link com.botmaker.plugin.api.value.PluginType} and {@link com.botmaker.plugin.api.value.ComponentType}
 * field, or a list of them, in the order the fields are declared.
 *
 * <p>It has no members. Implementing it is what lets the compiler refuse a holder handed to the wrong step:
 * {@code .toolbar(MyTypes.class)} does not compile. Other fields of the class are ignored, so a holder may keep
 * the constants its declarations share.
 */
public interface TypeHolder {
}
