package com.botmaker.plugin.api;

import com.botmaker.plugin.api.assist.AssistantTool;
import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.run.Readiness;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.slot.SlotEditor;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.toolbar.ToolbarItem;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * A plugin stated as one {@link PluginDeclaration}. Extend it with a {@code public final} class and a public
 * no-argument constructor, pass the declaration to {@code super}, and name the class in the plugin's pom — the
 * host reads it there, and the pom's coordinate and {@code <name>} are the plugin's id and name:
 *
 * <pre>{@code
 * public final class DiscordPlugin extends DeclaredPlugin {
 *     public DiscordPlugin() {
 *         super(StudioPlugin.declare()
 *                 .types(DiscordTypes.class)
 *                 .toolbar(() -> DiscordToolbar.ALL));
 *     }
 * }
 *
 * <properties><botmaker.plugin>com.example.discord.DiscordPlugin</botmaker.plugin></properties>
 * }</pre>
 *
 * <p>Each surface asks its supplier, or reads its holder classes' fields, every time the host asks, and keeps
 * nothing: the values are {@code static final} constants, built once when their class loads, and the host
 * keeps what it gets for the bound project. {@link #projectOpened} and {@link #projectClosing} are the two
 * methods left to override, because they are not lists — a plugin holding something the operating system
 * counts releases it in the second.
 *
 * <p>Each list is a supplier or a holder class, so nothing is built while the plugin is constructed.
 */
public non-sealed class DeclaredPlugin implements StudioPlugin {

    private final PluginDeclaration declaration;

    protected DeclaredPlugin(PluginDeclaration declaration) {
        if (declaration == null) throw new IllegalArgumentException("No declaration given");
        this.declaration = declaration;
    }

    @Override
    public final PaletteCatalog catalog() {
        return declaration.catalog();
    }

    @Override
    public final List<PluginType<?>> types() {
        return declaration.types();
    }

    @Override
    public final List<ComponentType<?>> componentTypes() {
        return declaration.parts();
    }

    @Override
    public final List<SlotEditor> slotEditors() {
        return declaration.editors();
    }

    @Override
    public final List<ManagedValue<?>> managedValues() {
        return declaration.values();
    }

    @Override
    public final List<ToolbarItem> toolbarItems() {
        return declaration.toolbar();
    }

    @Override
    public final List<RunOverlayPart> runOverlayParts() {
        return declaration.runOverlay();
    }

    @Override
    public final Optional<OverlayPart> overlayPart() {
        return declaration.overlay();
    }

    @Override
    public final List<AssistantTool<?>> assistantTools() {
        return declaration.assistant();
    }

    @Override
    public final String assistantGuide() {
        return declaration.guide();
    }

    @Override
    public final Optional<Method> trialEntry() {
        return declaration.trial();
    }

    @Override
    public final Optional<Readiness> readiness() {
        return declaration.readiness();
    }
}
