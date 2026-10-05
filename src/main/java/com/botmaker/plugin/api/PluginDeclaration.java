package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.record.RecordedValue;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.slot.SlotEditor;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.toolbar.ToolbarItem;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.util.List;
import java.util.function.Supplier;

/**
 * Everything a plugin contributes, stated in one expression and handed to {@link DeclaredPlugin}:
 *
 * <pre>{@code
 * public final class SdkPlugin extends DeclaredPlugin {
 *     public SdkPlugin() {
 *         super(StudioPlugin.id("com.botmaker.sdk").named("BotMaker SDK")
 *                 .types(() -> SdkTypes.ALL)
 *                 .parts(() -> SdkTypes.PARTS)
 *                 .editors(() -> SdkEditors.ALL)
 *                 .values(() -> SdkValues.ALL)
 *                 .toolbar(() -> SdkToolbarItems.ALL)
 *                 .recorded(() -> SdkRecorded.ALL)
 *                 .runOverlay(() -> SdkRunOverlay.ALL));
 *     }
 * }
 * }</pre>
 *
 * <p>Each surface after {@code named} is optional and names what it lists, so a reader sees the whole plugin
 * in one place instead of one override per surface. A surface left out contributes nothing, as the
 * contract's {@code default} does. The palette is left out by nearly every plugin: the host finds the
 * {@code @Palette} classes in its jar.
 *
 * <p><b>Each list is behind a supplier, and that is the one rule to keep.</b> A plugin is constructed while a
 * project opens, and by headless hosts that have no JavaFX ({@code botmaker plugin validate}, the registry's
 * CI). A list built in the constructor — a toolbar item referring to a dialog, say — loads classes on that
 * path, and a class that links JavaFX fails it: the plugin does not load at all. The supplier is asked only
 * when the host asks for that surface. Point it at a {@code static final} list; the host keeps what it gets
 * for the bound project, so there is nothing to cache here.
 *
 * <p>Immutable: each step answers a copy.
 */
public final class PluginDeclaration {

    /** After {@link StudioPlugin#id}: the name a user reads. */
    public static final class Naming {

        private final String id;

        Naming(String id) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("A plugin needs an id");
            this.id = id;
        }

        /** The name shown in Manage Plugins and wherever a contribution is attributed. */
        public PluginDeclaration named(String displayName) {
            String shown = displayName == null || displayName.isBlank() ? id : displayName;
            return new PluginDeclaration(id, shown, null, null, null, null, null, null, null, null);
        }
    }

    private final String id;
    private final String displayName;
    private final Supplier<PaletteCatalog> catalog;
    private final Supplier<? extends List<? extends PluginType<?>>> types;
    private final Supplier<? extends List<? extends ComponentType<?>>> parts;
    private final Supplier<? extends List<? extends SlotEditor>> editors;
    private final Supplier<? extends List<? extends ManagedValue<?>>> values;
    private final Supplier<? extends List<? extends ToolbarItem>> toolbar;
    private final Supplier<? extends List<? extends RecordedValue<?>>> recorded;
    private final Supplier<? extends List<? extends RunOverlayPart>> runOverlay;

    private PluginDeclaration(String id, String displayName, Supplier<PaletteCatalog> catalog,
                              Supplier<? extends List<? extends PluginType<?>>> types,
                              Supplier<? extends List<? extends ComponentType<?>>> parts,
                              Supplier<? extends List<? extends SlotEditor>> editors,
                              Supplier<? extends List<? extends ManagedValue<?>>> values,
                              Supplier<? extends List<? extends ToolbarItem>> toolbar,
                              Supplier<? extends List<? extends RecordedValue<?>>> recorded,
                              Supplier<? extends List<? extends RunOverlayPart>> runOverlay) {
        this.id = id;
        this.displayName = displayName;
        this.catalog = catalog;
        this.types = types;
        this.parts = parts;
        this.editors = editors;
        this.values = values;
        this.toolbar = toolbar;
        this.recorded = recorded;
        this.runOverlay = runOverlay;
    }

    /** The types this plugin owns — {@link StudioPlugin#types()}. */
    public PluginDeclaration types(Supplier<? extends List<? extends PluginType<?>>> types) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** The calls inside its values that are not types of their own — {@link StudioPlugin#componentTypes()}. */
    public PluginDeclaration parts(Supplier<? extends List<? extends ComponentType<?>>> parts) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** The editors a type cannot choose for itself — {@link StudioPlugin#slotEditors()}. */
    public PluginDeclaration editors(Supplier<? extends List<? extends SlotEditor>> editors) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** The {@code @Managed} values its windows keep — {@link StudioPlugin#managedValues()}. */
    public PluginDeclaration values(Supplier<? extends List<? extends ManagedValue<?>>> values) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** Its toolbar buttons — {@link StudioPlugin#toolbarItems()}. */
    public PluginDeclaration toolbar(Supplier<? extends List<? extends ToolbarItem>> toolbar) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** What only it can read off the screen for a recording — {@link StudioPlugin#recordedValues()}. */
    public PluginDeclaration recorded(Supplier<? extends List<? extends RecordedValue<?>>> recorded) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** What it shows in the host's run overlay while the bot runs — {@link StudioPlugin#runOverlayParts()}. */
    public PluginDeclaration runOverlay(Supplier<? extends List<? extends RunOverlayPart>> runOverlay) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    /** A palette built by hand, overriding the one the host discovers — rarely wanted; see {@link StudioPlugin#catalog()}. */
    public PluginDeclaration catalog(Supplier<PaletteCatalog> catalog) {
        return new PluginDeclaration(id, displayName, catalog, types, parts, editors, values, toolbar, recorded,
                runOverlay);
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    PaletteCatalog catalog() {
        PaletteCatalog built = catalog == null ? null : catalog.get();
        return built == null ? PaletteCatalog.empty() : built;
    }

    List<PluginType<?>> types() {
        return list(types);
    }

    List<ComponentType<?>> parts() {
        return list(parts);
    }

    List<SlotEditor> editors() {
        return list(editors);
    }

    List<ManagedValue<?>> values() {
        return list(values);
    }

    List<ToolbarItem> toolbar() {
        return list(toolbar);
    }

    List<RecordedValue<?>> recorded() {
        return list(recorded);
    }

    List<RunOverlayPart> runOverlay() {
        return list(runOverlay);
    }

    private static <E> List<E> list(Supplier<? extends List<? extends E>> surface) {
        List<? extends E> built = surface == null ? null : surface.get();
        return built == null ? List.of() : List.copyOf(built);
    }
}
