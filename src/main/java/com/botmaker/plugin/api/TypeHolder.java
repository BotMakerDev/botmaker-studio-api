package com.botmaker.plugin.api;

import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.util.List;

/**
 * A class giving a plugin's types, named by {@link PluginDeclaration#types(Class[])}:
 *
 * <pre>{@code
 * final class DiscordTypes implements TypeHolder {
 *     static final PluginType<Webhook> WEBHOOK = PluginType.value(Webhook.class)…;
 *
 *     @Override
 *     public List<PluginType<?>> types() {
 *         return List.of(WEBHOOK);
 *     }
 * }
 * }</pre>
 *
 * <p>Every holder interface works this way: one method per thing it gives, so the compiler asks for what the
 * holder must say, and a holder handed to the wrong step does not compile ({@code .toolbar(DiscordTypes.class)}).
 * The host builds the holder through its constructor with no arguments, only when it first asks for the surface
 * — never while the plugin is constructed — and asks again each time, keeping what it gets for the bound project.
 * Keep the values in {@code static final} fields, so each is built once and the same object every time.
 */
public interface TypeHolder {

    /** The types users pick, in the order they are offered — {@link StudioPlugin#types()}. */
    List<PluginType<?>> types();

    /**
     * The parts of a value that are not types of their own — {@link StudioPlugin#componentTypes()}. None unless
     * a type is written as calls inside calls.
     */
    default List<ComponentType<?>> components() {
        return List.of();
    }
}
