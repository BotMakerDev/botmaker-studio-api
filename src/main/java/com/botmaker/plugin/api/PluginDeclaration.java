package com.botmaker.plugin.api;

import com.botmaker.plugin.api.assist.AssistantTool;
import com.botmaker.plugin.api.assist.TrialEntry;
import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.record.RecordedValue;
import com.botmaker.plugin.api.run.RunOverlayPart;
import com.botmaker.plugin.api.slot.SlotEditor;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.toolbar.ToolbarItem;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Everything a plugin contributes, stated in one expression and handed to {@link DeclaredPlugin}:
 *
 * <pre>{@code
 * public final class SdkPlugin extends DeclaredPlugin {
 *     public SdkPlugin() {
 *         super(StudioPlugin.id("com.botmaker.sdk").named("BotMaker SDK")
 *                 .types(SdkTypes.class, FlowTypes.class, CaptureTypes.class)
 *                 .values(SdkValues.class)
 *                 .editors(() -> SdkEditors.ALL)
 *                 .toolbar(() -> SdkToolbarItems.ALL)
 *                 .recorded(() -> SdkRecorded.ALL)
 *                 .runOverlay(() -> List.of(SdkRunOverlay.PART))
 *                 .overlay(() -> SdkOverlay.PART)
 *                 .assistant(() -> SdkAssist.ALL)
 *                 .assistantGuide(SdkAssist.GUIDE)
 *                 .trial(Bot::trial));
 *     }
 * }
 * }</pre>
 *
 * <p>Each surface after {@code named} is optional and names what it lists, so a reader sees the whole plugin
 * in one place instead of one override per surface. A surface left out contributes nothing, as the
 * contract's {@code default} does. The palette is left out by nearly every plugin: the host finds the
 * {@code @Palette} classes in its jar.
 *
 * <p><b>Types and managed values are named by the classes that hold them.</b> {@code .types(MyTypes.class)}
 * contributes every {@code public static final} {@link PluginType} and {@link ComponentType} field of
 * {@code MyTypes}, and every one inside a {@code public static final} list there; {@code .values(...)} the same
 * for {@link ManagedValue}. A new field is declared by being written: there is no list to forget it in. The
 * other surfaces stay lists, because their order is what a user sees.
 *
 * <p><b>Nothing is built before the host asks, and that is the one rule to keep.</b> A plugin is constructed
 * while a project opens, and by headless hosts that have no JavaFX ({@code botmaker plugin validate}, the
 * registry's CI). A list built in the constructor — a toolbar item referring to a dialog, say — loads classes
 * on that path, and a class that links JavaFX fails it: the plugin does not load at all. A holder class is
 * initialised, and a supplier asked, only when the host asks for that surface. Point a supplier at a
 * {@code static final} list; the host keeps what it gets for the bound project, so there is nothing to cache
 * here.
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
            return new PluginDeclaration(id, shown, null, List.of(), null, null, null, List.of(), null, null, null,
                    null, null, null, "", null);
        }
    }

    private final String id;
    private final String displayName;
    private final Supplier<PaletteCatalog> catalog;
    private final List<Class<?>> typeHolders;
    private final Supplier<? extends List<? extends PluginType<?>>> types;
    private final Supplier<? extends List<? extends ComponentType<?>>> parts;
    private final Supplier<? extends List<? extends SlotEditor>> editors;
    private final List<Class<?>> valueHolders;
    private final Supplier<? extends List<? extends ManagedValue<?>>> values;
    private final Supplier<? extends List<? extends ToolbarItem>> toolbar;
    private final Supplier<? extends List<? extends RecordedValue<?>>> recorded;
    private final Supplier<? extends List<? extends RunOverlayPart>> runOverlay;
    private final Supplier<OverlayPart> overlay;
    private final Supplier<? extends List<? extends AssistantTool<?>>> assistant;
    private final String guide;
    private final Method trial;

    private PluginDeclaration(String id, String displayName, Supplier<PaletteCatalog> catalog,
                              List<Class<?>> typeHolders,
                              Supplier<? extends List<? extends PluginType<?>>> types,
                              Supplier<? extends List<? extends ComponentType<?>>> parts,
                              Supplier<? extends List<? extends SlotEditor>> editors,
                              List<Class<?>> valueHolders,
                              Supplier<? extends List<? extends ManagedValue<?>>> values,
                              Supplier<? extends List<? extends ToolbarItem>> toolbar,
                              Supplier<? extends List<? extends RecordedValue<?>>> recorded,
                              Supplier<? extends List<? extends RunOverlayPart>> runOverlay,
                              Supplier<OverlayPart> overlay,
                              Supplier<? extends List<? extends AssistantTool<?>>> assistant,
                              String guide, Method trial) {
        this.id = id;
        this.displayName = displayName;
        this.catalog = catalog;
        this.typeHolders = typeHolders;
        this.types = types;
        this.parts = parts;
        this.editors = editors;
        this.valueHolders = valueHolders;
        this.values = values;
        this.toolbar = toolbar;
        this.recorded = recorded;
        this.runOverlay = runOverlay;
        this.overlay = overlay;
        this.assistant = assistant;
        this.guide = guide;
        this.trial = trial;
    }

    /**
     * The classes holding this plugin's types — {@link StudioPlugin#types()} and
     * {@link StudioPlugin#componentTypes()} at once: each {@code public static final} field of a holder that is a
     * {@link PluginType} or a {@link ComponentType}, or a list of them, in the order the fields are declared,
     * holder by holder. A field reached twice counts once. Naming a class initialises nothing; the host's first
     * ask does.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    public PluginDeclaration types(Class<?>... holders) {
        return new PluginDeclaration(id, displayName, catalog, holders("types", holders), types, parts, editors,
                valueHolders, values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /**
     * The types this plugin owns as a list it builds — {@link StudioPlugin#types()}. {@link #types(Class[])} is
     * the usual way; this one is for a list computed at run time. Both may be given: the host gets the two.
     */
    public PluginDeclaration types(Supplier<? extends List<? extends PluginType<?>>> types) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /**
     * The calls inside its values that are not types of their own, as a list it builds —
     * {@link StudioPlugin#componentTypes()}. A holder named by {@link #types(Class[])} already gives its parts.
     */
    public PluginDeclaration parts(Supplier<? extends List<? extends ComponentType<?>>> parts) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** The editors a type cannot choose for itself, narrowest first — {@link StudioPlugin#slotEditors()}. */
    public PluginDeclaration editors(Supplier<? extends List<? extends SlotEditor>> editors) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /**
     * The classes holding the managed values its windows keep — {@link StudioPlugin#managedValues()}: each
     * {@code public static final} {@link ManagedValue} field of a holder, or a list of them, in the order the
     * fields are declared, holder by holder. A field reached twice counts once.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    public PluginDeclaration values(Class<?>... holders) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors,
                holders("values", holders), values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /**
     * The managed values its windows keep, as a list it builds — {@link StudioPlugin#managedValues()}.
     * {@link #values(Class[])} is the usual way; both may be given.
     */
    public PluginDeclaration values(Supplier<? extends List<? extends ManagedValue<?>>> values) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** Its toolbar buttons — {@link StudioPlugin#toolbarItems()}. */
    public PluginDeclaration toolbar(Supplier<? extends List<? extends ToolbarItem>> toolbar) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** What only it can read off the screen for a recording — {@link StudioPlugin#recordedValues()}. */
    public PluginDeclaration recorded(Supplier<? extends List<? extends RecordedValue<?>>> recorded) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** What it shows in the host's run overlay while the bot runs — {@link StudioPlugin#runOverlayParts()}. */
    public PluginDeclaration runOverlay(Supplier<? extends List<? extends RunOverlayPart>> runOverlay) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** Its part of the overlay editor: targets, watched screen, tools, probes — {@link StudioPlugin#overlayPart()}. */
    public PluginDeclaration overlay(Supplier<OverlayPart> overlay) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /** The tools it offers the AI assistant — {@link StudioPlugin#assistantTools()}. */
    public PluginDeclaration assistant(Supplier<? extends List<? extends AssistantTool<?>>> assistant) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
    }

    /**
     * How a bot built on this plugin is laid out, for the AI assistant that builds one —
     * {@link StudioPlugin#assistantGuide()}. Plain sentences: where each kind of work goes, what to name and
     * how, what to read before an image. Point it at a {@code static final String}.
     */
    public PluginDeclaration assistantGuide(String guide) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide == null ? "" : guide.strip(), trial);
    }

    /**
     * The static method ▶ Try runs one statement through, by method reference: {@code .trial(Bot::trial)} —
     * {@link StudioPlugin#trialEntry()}. Resolved and checked here, so a lambda or a method of the wrong
     * shape fails the plugin's own construction and tests, never the user's ▶ Try. Resolving loads the
     * entry's class without initialising it.
     *
     * @throws IllegalArgumentException when {@code trial} is a lambda, or not a public static
     *                                  {@code (Runnable, Class<?>...)}
     */
    public PluginDeclaration trial(TrialEntry trial) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide,
                TrialEntry.resolve(Objects.requireNonNull(trial, "trial")));
    }

    /** A palette built by hand, overriding the one the host discovers — rarely wanted; see {@link StudioPlugin#catalog()}. */
    public PluginDeclaration catalog(Supplier<PaletteCatalog> catalog) {
        return new PluginDeclaration(id, displayName, catalog, typeHolders, types, parts, editors, valueHolders,
                values, toolbar, recorded, runOverlay, overlay, assistant, guide, trial);
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
        return joined(list(types), held(typeHolders, PluginType.class));
    }

    List<ComponentType<?>> parts() {
        List<ComponentType<?>> held = new ArrayList<>(held(typeHolders, ComponentType.class));
        held.removeIf(PluginType.class::isInstance);
        return joined(list(parts), held);
    }

    List<SlotEditor> editors() {
        return list(editors);
    }

    List<ManagedValue<?>> values() {
        return joined(list(values), held(valueHolders, ManagedValue.class));
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

    Optional<OverlayPart> overlay() {
        OverlayPart built = overlay == null ? null : overlay.get();
        return built == null || built.isEmpty() ? Optional.empty() : Optional.of(built);
    }

    List<AssistantTool<?>> assistant() {
        return list(assistant);
    }

    String guide() {
        return guide;
    }

    Optional<Method> trial() {
        return Optional.ofNullable(trial);
    }

    private static <E> List<E> list(Supplier<? extends List<? extends E>> surface) {
        List<? extends E> built = surface == null ? null : surface.get();
        return built == null ? List.of() : List.copyOf(built);
    }

    private static List<Class<?>> holders(String step, Class<?>... holders) {
        if (holders == null || holders.length == 0) {
            throw new IllegalArgumentException("." + step + "(…) needs the class that holds them: ." + step
                    + "(MyTypes.class)");
        }
        for (Class<?> holder : holders) {
            if (holder == null) throw new IllegalArgumentException("." + step + "(…) was given a null class");
        }
        return List.of(holders);
    }

    /**
     * The {@code kind} values a holder's {@code public static final} fields hold, directly or in a collection, in
     * field order. {@link Class#getDeclaredFields} promises no order and gives the declared one on every JVM a
     * host runs on; {@code DeclaredPluginTest} pins it.
     */
    @SuppressWarnings("unchecked")
    private static <E> List<E> held(List<Class<?>> holders, Class<? super E> kind) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<E> found = new ArrayList<>();
        for (Class<?> holder : holders) {
            for (Field field : holder.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)
                        || field.isSynthetic()) {
                    continue;
                }
                Object value = valueOf(field);
                Collection<?> each = value instanceof Collection<?> many ? many : Collections.singletonList(value);
                for (Object one : each) {
                    if (kind.isInstance(one) && seen.add(one)) found.add((E) one);
                }
            }
        }
        return found;
    }

    private static Object valueOf(Field field) {
        try {
            field.trySetAccessible();
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(field.getDeclaringClass().getName() + "." + field.getName()
                    + " cannot be read; make its class public", e);
        }
    }

    private static <E> List<E> joined(List<E> listed, List<E> held) {
        if (listed.isEmpty()) return List.copyOf(held);
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.addAll(listed);
        List<E> all = new ArrayList<>(listed);
        for (E one : held) if (seen.add(one)) all.add(one);
        return List.copyOf(all);
    }
}
