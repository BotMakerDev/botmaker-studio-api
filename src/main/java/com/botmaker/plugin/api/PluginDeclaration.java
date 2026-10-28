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

/**
 * Everything a plugin contributes, stated in one expression and handed to {@link DeclaredPlugin}:
 *
 * <pre>{@code
 * public final class SdkPlugin extends DeclaredPlugin {
 *     public SdkPlugin() {
 *         super(StudioPlugin.declare()
 *                 .types(SdkTypes.class, FlowTypes.class, CaptureTypes.class)
 *                 .values(SdkValues.class)
 *                 .editors(SdkEditors.class)
 *                 .toolbar(SdkToolbarItems.class)
 *                 .runOverlay(SdkRunOverlay.class)
 *                 .overlay(SdkOverlay.class)
 *                 .assistant(SdkAssist.class)
 *                 .assistantGuide(SdkAssist.GUIDE)
 *                 .trial(Bot::trial)
 *                 .onProjectOpened(SdkSession::open)
 *                 .onProjectClosing(SdkSession::close));
 *     }
 * }
 * }</pre>
 *
 * <p>It names no id and no name: the plugin's pom carries both ({@link StudioPlugin}). Each surface is optional,
 * so a reader sees the whole plugin in one place. A surface left out contributes nothing. The palette is left out
 * by nearly every plugin: the host finds the {@code @Palette} classes in its jar.
 *
 * <p><b>Every list is named by the classes that hold it, and that is the one way to declare one.</b>
 * {@code .toolbar(MyToolbar.class)} contributes every {@code public static final} {@link ToolbarItem} field of
 * {@code MyToolbar}, and every one inside a {@code public static final} list there, in field order — the order a
 * user sees. A new field is declared by being written: there is no list to forget it in. Each step takes only its
 * own kind of holder ({@link TypeHolder}, {@link ToolbarHolder}, …), so a holder handed to the wrong step does not
 * compile. A field reached twice counts once; other fields are ignored, and so are fields a holder inherits:
 * name the superclass as a holder of its own.
 *
 * <p><b>Nothing is built before the host asks, and that is the one rule to keep.</b> A plugin is constructed
 * while a project opens, and by headless hosts that have no JavaFX ({@code botmaker plugin validate}, the
 * registry's CI). Naming a holder class initialises nothing; the host's first ask for that surface does, so a
 * toolbar holder linking JavaFX costs a headless host nothing. The host keeps what it gets for the bound project,
 * so there is nothing to cache here.
 *
 * <p>Immutable: each step answers a copy.
 */
public final class PluginDeclaration {

    // Set only on a fresh copy, inside with(…), before any step hands it out.
    private List<Class<?>> catalogHolders = List.of();
    private List<Class<?>> typeHolders = List.of();
    private List<Class<?>> editorHolders = List.of();
    private List<Class<?>> valueHolders = List.of();
    private List<Class<?>> toolbarHolders = List.of();
    private List<Class<?>> runOverlayHolders = List.of();
    private List<Class<?>> overlayHolders = List.of();
    private List<Class<?>> assistantHolders = List.of();
    private String guide = "";
    private Method trial;
    private Readiness.Check readiness;
    private Consumer<StudioServices> opened;
    private Runnable closing;

    private PluginDeclaration() {
    }

    /** What {@link StudioPlugin#declare()} answers: no surface yet. */
    static PluginDeclaration empty() {
        return new PluginDeclaration();
    }

    /** A copy of this declaration with {@code set} applied to it: every step's one way to answer a copy. */
    private PluginDeclaration with(Consumer<PluginDeclaration> set) {
        PluginDeclaration copy = new PluginDeclaration();
        copy.catalogHolders = catalogHolders;
        copy.typeHolders = typeHolders;
        copy.editorHolders = editorHolders;
        copy.valueHolders = valueHolders;
        copy.toolbarHolders = toolbarHolders;
        copy.runOverlayHolders = runOverlayHolders;
        copy.overlayHolders = overlayHolders;
        copy.assistantHolders = assistantHolders;
        copy.guide = guide;
        copy.trial = trial;
        copy.readiness = readiness;
        copy.opened = opened;
        copy.closing = closing;
        set.accept(copy);
        return copy;
    }

    /**
     * The classes holding this plugin's types — {@link StudioPlugin#types()} and
     * {@link StudioPlugin#componentTypes()} at once: each {@link PluginType} and {@link ComponentType} field, the
     * parts inside a value included. Calling it again adds holders.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration types(Class<? extends TypeHolder>... holders) {
        List<Class<?>> named = holders("types", "MyTypes", holders);
        return with(copy -> copy.typeHolders = added(typeHolders, named));
    }

    /**
     * The classes holding the editors a type cannot choose for itself, narrowest first —
     * {@link StudioPlugin#slotEditors()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration editors(Class<? extends EditorHolder>... holders) {
        List<Class<?>> named = holders("editors", "MyEditors", holders);
        return with(copy -> copy.editorHolders = added(editorHolders, named));
    }

    /**
     * The classes holding the managed values its windows keep — {@link StudioPlugin#managedValues()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration values(Class<? extends ValueHolder>... holders) {
        List<Class<?>> named = holders("values", "MyValues", holders);
        return with(copy -> copy.valueHolders = added(valueHolders, named));
    }

    /**
     * The classes holding its toolbar buttons — {@link StudioPlugin#toolbarItems()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration toolbar(Class<? extends ToolbarHolder>... holders) {
        List<Class<?>> named = holders("toolbar", "MyToolbar", holders);
        return with(copy -> copy.toolbarHolders = added(toolbarHolders, named));
    }

    /**
     * The classes holding what it shows in the host's run overlay while the bot runs —
     * {@link StudioPlugin#runOverlayParts()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration runOverlay(Class<? extends RunOverlayHolder>... holders) {
        List<Class<?>> named = holders("runOverlay", "MyRunOverlay", holders);
        return with(copy -> copy.runOverlayHolders = added(runOverlayHolders, named));
    }

    /**
     * The class holding its one part of the overlay editor: targets, watched screen, tools, probes —
     * {@link StudioPlugin#overlayPart()}.
     */
    public PluginDeclaration overlay(Class<? extends OverlayHolder> holder) {
        List<Class<?>> named = holders("overlay", "MyOverlay", holder);
        return with(copy -> copy.overlayHolders = added(overlayHolders, named));
    }

    /**
     * The classes holding the tools it offers the AI assistant — {@link StudioPlugin#assistantTools()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}
     */
    @SafeVarargs
    public final PluginDeclaration assistant(Class<? extends AssistantHolder>... holders) {
        List<Class<?>> named = holders("assistant", "MyTools", holders);
        return with(copy -> copy.assistantHolders = added(assistantHolders, named));
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

    /**
     * What to do when a project is bound to this plugin, by method reference:
     * {@code .onProjectOpened(MySession::open)}, a {@code static void open(StudioServices)} —
     * {@link StudioPlugin#projectOpened}. Do nothing expensive there; the user is waiting on the project.
     */
    public PluginDeclaration onProjectOpened(Consumer<StudioServices> opened) {
        Consumer<StudioServices> told = Objects.requireNonNull(opened, "onProjectOpened");
        return with(copy -> copy.opened = told);
    }

    /**
     * What to release when the bound project closes, by method reference: {@code .onProjectClosing(MySession::close)}
     * — {@link StudioPlugin#projectClosing}. For what the operating system counts: a port, a display, a process.
     */
    public PluginDeclaration onProjectClosing(Runnable closing) {
        Runnable told = Objects.requireNonNull(closing, "onProjectClosing");
        return with(copy -> copy.closing = told);
    }

    /**
     * The class holding a palette built by hand, overriding the one the host discovers — rarely wanted; see
     * {@link StudioPlugin#catalog()}.
     */
    public PluginDeclaration catalog(Class<? extends CatalogHolder> holder) {
        List<Class<?>> named = holders("catalog", "MyCatalog", holder);
        return with(copy -> copy.catalogHolders = added(catalogHolders, named));
    }

    PaletteCatalog catalog() {
        return one(catalogHolders, PaletteCatalog.class).orElseGet(PaletteCatalog::empty);
    }

    List<PluginType<?>> types() {
        return held(typeHolders, PluginType.class);
    }

    List<ComponentType<?>> parts() {
        List<ComponentType<?>> held = new ArrayList<>(held(typeHolders, ComponentType.class));
        held.removeIf(PluginType.class::isInstance);
        return List.copyOf(held);
    }

    List<SlotEditor> editors() {
        return held(editorHolders, SlotEditor.class);
    }

    List<ManagedValue<?>> values() {
        return held(valueHolders, ManagedValue.class);
    }

    List<ToolbarItem> toolbar() {
        return held(toolbarHolders, ToolbarItem.class);
    }

    List<RunOverlayPart> runOverlay() {
        return held(runOverlayHolders, RunOverlayPart.class);
    }

    Optional<OverlayPart> overlay() {
        return one(overlayHolders, OverlayPart.class).filter(part -> !part.isEmpty());
    }

    List<AssistantTool<?>> assistant() {
        return held(assistantHolders, AssistantTool.class);
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

    void opened(StudioServices services) {
        if (opened != null) opened.accept(services);
    }

    void closing() {
        if (closing != null) closing.run();
    }

    private static List<Class<?>> holders(String step, String example, Class<?>... holders) {
        if (holders == null || holders.length == 0) {
            throw new IllegalArgumentException("." + step + "(…) needs the class that holds them: ." + step
                    + "(" + example + ".class)");
        }
        for (Class<?> holder : holders) {
            if (holder == null) throw new IllegalArgumentException("." + step + "(…) was given a null class");
        }
        return List.of(holders);
    }

    private static List<Class<?>> added(List<Class<?>> before, List<Class<?>> named) {
        List<Class<?>> all = new ArrayList<>(before);
        all.addAll(named);
        return List.copyOf(all);
    }

    /** The one {@code kind} value a single-value holder holds; two are refused, naming the holder. */
    private static <E> Optional<E> one(List<Class<?>> holders, Class<E> kind) {
        List<E> found = held(holders, kind);
        if (found.size() > 1) {
            throw new IllegalStateException(holders.getFirst().getName() + " holds " + found.size() + " "
                    + kind.getSimpleName() + " fields; a plugin has one");
        }
        return found.stream().findFirst();
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
        return List.copyOf(found);
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
}
