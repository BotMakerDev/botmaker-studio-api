package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.record.RecordedValue;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.slot.SlotEditor;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.toolbar.ToolbarItem;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.util.List;
import java.util.Optional;

/**
 * A plugin stated as one {@link PluginDeclaration}. Extend it with a public no-argument constructor — the
 * host finds a plugin through {@code ServiceLoader} — and pass the declaration to {@code super}:
 *
 * <pre>{@code
 * public final class DiscordPlugin extends DeclaredPlugin {
 *     public DiscordPlugin() {
 *         super(StudioPlugin.id("com.example.discord").named("Discord")
 *                 .types(() -> DiscordTypes.ALL)
 *                 .toolbar(() -> DiscordToolbar.ALL));
 *     }
 * }
 * }</pre>
 *
 * <p>Each surface asks its supplier every time the host asks, and keeps nothing: the lists are
 * {@code static final} constants, built once when their class loads, and the host keeps what it gets for the
 * bound project. {@link #projectOpened} and {@link #projectClosing} are the two methods left to override,
 * because they are not lists — a plugin holding something the operating system counts releases it in the
 * second.
 *
 * <p>Each list is a supplier, so nothing is built while the plugin is constructed.
 */
public class DeclaredPlugin implements StudioPlugin {

    private final PluginDeclaration declaration;

    protected DeclaredPlugin(PluginDeclaration declaration) {
        if (declaration == null) throw new IllegalArgumentException("No declaration given");
        this.declaration = declaration;
    }

    @Override
    public final String id() {
        return declaration.id();
    }

    @Override
    public final String displayName() {
        return declaration.displayName();
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
    public final List<RecordedValue<?>> recordedValues() {
        return declaration.recorded();
    }

    @Override
    public final List<RunOverlayPart> runOverlayParts() {
        return declaration.runOverlay();
    }

    @Override
    public final Optional<OverlayPart> overlayPart() {
        return declaration.overlay();
    }
}
