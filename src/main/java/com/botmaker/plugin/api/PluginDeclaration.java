package com.botmaker.plugin.api;

import com.botmaker.plugin.api.assist.AssistantTool;
import com.botmaker.plugin.api.assist.TrialEntry;
import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.overlay.OverlayPart;
import com.botmaker.plugin.api.run.Readiness;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Everything a plugin contributes, stated in one expression and handed to {@link DeclaredPlugin}:
 *
 * <pre>{@code
 * public final class SdkPlugin extends DeclaredPlugin {
 *     public SdkPlugin() {
 *         super(StudioPlugin.declare()
 *                 .types(SdkTypes.class, FlowTypes.class, CaptureTypes.class)
 *                 .values(SdkValues.class)
 *                 .editors(() -> SdkEditors.ALL)
 *                 .toolbar(() -> SdkToolbarItems.ALL)
 *                 .runOverlay(() -> List.of(SdkRunOverlay.PART))
 *                 .overlay(() -> SdkOverlay.PART)
 *                 .assistant(() -> SdkAssist.ALL)
 *                 .assistantGuide(SdkAssist.GUIDE)
 *                 .trial(Bot::trial));
 *     }
 * }
 * }</pre>
 *
 * <p>It names no id and no name: the plugin's pom carries both ({@link StudioPlugin}). Each surface is optional
 * and names what it lists, so a reader sees the whole plugin
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

    // Set only on a fresh copy, inside with(…), before any step hands it out.
    private Supplier<PaletteCatalog> catalog;
    private List<Class<?>> typeHolders = List.of();
    private Supplier<? extends List<? extends PluginType<?>>> types;
    private Supplier<? extends List<? extends ComponentType<?>>> parts;
    private Supplier<? extends List<? extends SlotEditor>> editors;
    private List<Class<?>> valueHolders = List.of();
    private Supplier<? extends List<? extends ManagedValue<?>>> values;
    private Supplier<? extends List<? extends ToolbarItem>> toolbar;
    private Supplier<? extends List<? extends RunOverlayPart>> runOverlay;
    private Supplier<OverlayPart> overlay;
    private Supplier<? extends List<? extends AssistantTool<?>>> assistant;
    private String guide = "";
    private Method trial;
    private Readiness.Check readiness;

    private PluginDeclaration() {
    }

    /** What {@link StudioPlugin#declare()} answers: no surface yet. */
    static PluginDeclaration empty() {
        return new PluginDeclaration();
    }

    /** A copy of this declaration with {@code set} applied to it: every step's one way to answer a copy. */
    private PluginDeclaration with(Consumer<PluginDeclaration> set) {
        PluginDeclaration copy = new PluginDeclaration();
        copy.catalog = catalog;
        copy.typeHolders = typeHolders;
        copy.types = types;
        copy.parts = parts;
        copy.editors = editors;
        copy.valueHolders = valueHolders;
        copy.values = values;
        copy.toolbar = toolbar;
        copy.runOverlay = runOverlay;
        copy.overlay = overlay;
        copy.assistant = assistant;
        copy.guide = guide;
        copy.trial = trial;
        copy.readiness = readiness;
        set.accept(copy);
        return copy;
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
        List<Class<?>> named = holders("types", holders);
        return with(copy -> copy.typeHolders = named);
    }

    /**
     * The types this plugin owns as a list it builds — {@link StudioPlugin#types()}. {@link #types(Class[])} is
     * the usual way; this one is for a list computed at run time. Both may be given: the host gets the two.
     */
    public PluginDeclaration types(Supplier<? extends List<? extends PluginType<?>>> types) {
        return with(copy -> copy.types = types);
    }

    /**
     * The calls inside its values that are not types of their own, as a list it builds —
     * {@link StudioPlugin#componentTypes()}. A holder named by {@link #types(Class[])} already gives its parts.
     */
    public PluginDeclaration parts(Supplier<? extends List<? extends ComponentType<?>>> parts) {
        return with(copy -> copy.parts = parts);
    }

    /** The editors a type cannot choose for itself, narrowest first — {@link StudioPlugin#slotEditors()}. */
    public PluginDeclaration editors(Supplier<? extends List<? extends SlotEditor>> editors) {
        return with(copy -> copy.editors = editors);
    }

    /**
     * The classes holding the managed values its windows keep — {@link StudioPlugin#managedValues()}: each
     * {@code public static final} {@link ManagedValue} field of a holder, or a list of them, in the order the
     * fields are declared, holder by holder. A field reached twice counts once.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    public PluginDeclaration values(Class<?>... holders) {
        List<Class<?>> named = holders("values", holders);
        return with(copy -> copy.valueHolders = named);
    }

    /**
     * The managed values its windows keep, as a list it builds — {@link StudioPlugin#managedValues()}.
     * {@link #values(Class[])} is the usual way; both may be given.
     */
    public PluginDeclaration values(Supplier<? extends List<? extends ManagedValue<?>>> values) {
        return with(copy -> copy.values = values);
    }

    /** Its toolbar buttons — {@link StudioPlugin#toolbarItems()}. */
    public PluginDeclaration toolbar(Supplier<? extends List<? extends ToolbarItem>> toolbar) {
        return with(copy -> copy.toolbar = toolbar);
    }

    /** What it shows in the host's run overlay while the bot runs — {@link StudioPlugin#runOverlayParts()}. */
    public PluginDeclaration runOverlay(Supplier<? extends List<? extends RunOverlayPart>> runOverlay) {
        return with(copy -> copy.runOverlay = runOverlay);
    }

    /** Its part of the overlay editor: targets, watched screen, tools, probes — {@link StudioPlugin#overlayPart()}. */
    public PluginDeclaration overlay(Supplier<OverlayPart> overlay) {
        return with(copy -> copy.overlay = overlay);
    }

    /** The tools it offers the AI assistant — {@link StudioPlugin#assistantTools()}. */
    public PluginDeclaration assistant(Supplier<? extends List<? extends AssistantTool<?>>> assistant) {
        return with(copy -> copy.assistant = assistant);
    }

    /**
     * How a bot built on this plugin is laid out, for the AI assistant that builds one —
     * {@link StudioPlugin#assistantGuide()}. Plain sentences: where each kind of work goes, what to name and
     * how, what to read before an image. Point it at a {@code static final String}.
     */
    public PluginDeclaration assistantGuide(String guide) {
        String said = guide == null ? "" : guide.strip();
        return with(copy -> copy.guide = said);
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
        Method resolved = TrialEntry.resolve(Objects.requireNonNull(trial, "trial"));
        return with(copy -> copy.trial = resolved);
    }

    /**
     * Whether the open project's bot can start now, by method reference: {@code .readiness(MyReadiness::now)} —
     * {@link StudioPlugin#readiness()}. The host calls it off the JavaFX thread while a run window is open.
     */
    public PluginDeclaration readiness(Readiness.Check readiness) {
        Readiness.Check check = Objects.requireNonNull(readiness, "readiness");
        return with(copy -> copy.readiness = check);
    }

    /** A palette built by hand, overriding the one the host discovers — rarely wanted; see {@link StudioPlugin#catalog()}. */
    public PluginDeclaration catalog(Supplier<PaletteCatalog> catalog) {
        return with(copy -> copy.catalog = catalog);
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

    Optional<Readiness> readiness() {
        return readiness == null ? Optional.empty() : Optional.ofNullable(readiness.now());
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
