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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import java.util.function.Function;

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
 * <p><b>Every surface is named by the classes that give it, and that is the one way to declare one.</b> Each step
 * takes its own kind of holder ({@link TypeHolder}, {@link ToolbarHolder}, …), whose one method says what the
 * holder must give: {@code .toolbar(MyToolbar.class)} contributes {@code MyToolbar}'s {@code toolbar()}, and a
 * holder handed to the wrong step does not compile. A holder is a concrete class with a constructor taking no
 * arguments; it may be package-private. Several holders of a list give their values in the order they are named;
 * a value given twice counts once.
 *
 * <p><b>Nothing is built before the host asks, and that is the one rule to keep.</b> A plugin is constructed
 * while a project opens, and by headless hosts that have no JavaFX ({@code botmaker plugin validate}, the
 * registry's CI). Naming a holder class initialises nothing; the host's first ask for that surface builds the
 * holder, so a toolbar holder linking JavaFX costs a headless host nothing. The host keeps what it gets for the
 * bound project, so there is nothing to cache here.
 *
 * <p>Immutable: each step answers a copy.
 */
public final class PluginDeclaration {

    // Set only on a fresh copy, inside with(…), before any step hands it out.
    private List<Class<? extends TypeHolder>> typeHolders = List.of();
    private List<Class<? extends EditorHolder>> editorHolders = List.of();
    private List<Class<? extends ValueHolder>> valueHolders = List.of();
    private List<Class<? extends ToolbarHolder>> toolbarHolders = List.of();
    private List<Class<? extends RunOverlayHolder>> runOverlayHolders = List.of();
    private List<Class<? extends AssistantHolder>> assistantHolders = List.of();
    private Class<? extends OverlayHolder> overlayHolder;
    private Class<? extends CatalogHolder> catalogHolder;
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
        copy.typeHolders = typeHolders;
        copy.editorHolders = editorHolders;
        copy.valueHolders = valueHolders;
        copy.toolbarHolders = toolbarHolders;
        copy.runOverlayHolders = runOverlayHolders;
        copy.assistantHolders = assistantHolders;
        copy.overlayHolder = overlayHolder;
        copy.catalogHolder = catalogHolder;
        copy.guide = guide;
        copy.trial = trial;
        copy.readiness = readiness;
        copy.opened = opened;
        copy.closing = closing;
        set.accept(copy);
        return copy;
    }

    /**
     * The classes giving this plugin's types — {@link StudioPlugin#types()} and {@link StudioPlugin#componentTypes()}
     * at once. Calling it again adds holders.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration types(Class<? extends TypeHolder>... holders) {
        List<Class<? extends TypeHolder>> named = holders("types", "MyTypes", holders);
        return with(copy -> copy.typeHolders = added(typeHolders, named));
    }

    /**
     * The classes giving the editors a type cannot choose for itself, narrowest first —
     * {@link StudioPlugin#slotEditors()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration editors(Class<? extends EditorHolder>... holders) {
        List<Class<? extends EditorHolder>> named = holders("editors", "MyEditors", holders);
        return with(copy -> copy.editorHolders = added(editorHolders, named));
    }

    /**
     * The classes giving the managed values its windows keep — {@link StudioPlugin#managedValues()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration values(Class<? extends ValueHolder>... holders) {
        List<Class<? extends ValueHolder>> named = holders("values", "MyValues", holders);
        return with(copy -> copy.valueHolders = added(valueHolders, named));
    }

    /**
     * The classes giving its toolbar buttons — {@link StudioPlugin#toolbarItems()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration toolbar(Class<? extends ToolbarHolder>... holders) {
        List<Class<? extends ToolbarHolder>> named = holders("toolbar", "MyToolbar", holders);
        return with(copy -> copy.toolbarHolders = added(toolbarHolders, named));
    }

    /**
     * The classes giving what it shows in the host's run overlay while the bot runs —
     * {@link StudioPlugin#runOverlayParts()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration runOverlay(Class<? extends RunOverlayHolder>... holders) {
        List<Class<? extends RunOverlayHolder>> named = holders("runOverlay", "MyRunOverlay", holders);
        return with(copy -> copy.runOverlayHolders = added(runOverlayHolders, named));
    }

    /**
     * The classes giving the tools it offers the AI assistant — {@link StudioPlugin#assistantTools()}.
     *
     * @throws IllegalArgumentException when no holder is given, or one is {@code null}, abstract or an inner class
     */
    @SafeVarargs
    public final PluginDeclaration assistant(Class<? extends AssistantHolder>... holders) {
        List<Class<? extends AssistantHolder>> named = holders("assistant", "MyTools", holders);
        return with(copy -> copy.assistantHolders = added(assistantHolders, named));
    }

    /**
     * The class giving its one part of the overlay editor: targets, watched screen, tools, probes —
     * {@link StudioPlugin#overlayPart()}.
     *
     * @throws IllegalArgumentException when {@code holder} is {@code null}, abstract or an inner class
     * @throws IllegalStateException    when an overlay holder was already declared: a plugin has one part
     */
    public PluginDeclaration overlay(Class<? extends OverlayHolder> holder) {
        Class<? extends OverlayHolder> named = holders("overlay", "MyOverlay", holder).getFirst();
        once("overlay", overlayHolder);
        return with(copy -> copy.overlayHolder = named);
    }

    /**
     * The class giving a palette built by hand, overriding the one the host discovers — rarely wanted; see
     * {@link StudioPlugin#catalog()}.
     *
     * @throws IllegalArgumentException when {@code holder} is {@code null}, abstract or an inner class
     * @throws IllegalStateException    when a catalog holder was already declared: a plugin has one palette
     */
    public PluginDeclaration catalog(Class<? extends CatalogHolder> holder) {
        Class<? extends CatalogHolder> named = holders("catalog", "MyCatalog", holder).getFirst();
        once("catalog", catalogHolder);
        return with(copy -> copy.catalogHolder = named);
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

    PaletteCatalog catalog() {
        PaletteCatalog given = catalogHolder == null ? null : built(catalogHolder).catalog();
        return given == null ? PaletteCatalog.empty() : given;
    }

    List<PluginType<?>> types() {
        return given(typeHolders, TypeHolder::types);
    }

    List<ComponentType<?>> parts() {
        return given(typeHolders, TypeHolder::components);
    }

    List<SlotEditor> editors() {
        return given(editorHolders, EditorHolder::editors);
    }

    List<ManagedValue<?>> values() {
        return given(valueHolders, ValueHolder::values);
    }

    List<ToolbarItem> toolbar() {
        return given(toolbarHolders, ToolbarHolder::toolbar);
    }

    List<RunOverlayPart> runOverlay() {
        return given(runOverlayHolders, RunOverlayHolder::runOverlay);
    }

    Optional<OverlayPart> overlay() {
        if (overlayHolder == null) return Optional.empty();
        return Optional.ofNullable(built(overlayHolder).overlay()).filter(part -> !part.isEmpty());
    }

    List<AssistantTool<?>> assistant() {
        return given(assistantHolders, AssistantHolder::tools);
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

    /**
     * The named holders, refused here when the host could never build one. Reads only the class's modifiers, which
     * loads nothing more and initialises nothing.
     */
    @SafeVarargs
    private static <H> List<Class<? extends H>> holders(String step, String example, Class<? extends H>... holders) {
        if (holders == null || holders.length == 0) {
            throw new IllegalArgumentException("." + step + "(…) needs the class that gives them: ." + step
                    + "(" + example + ".class)");
        }
        for (Class<?> holder : holders) {
            if (holder == null) throw new IllegalArgumentException("." + step + "(…) was given a null class");
            int modifiers = holder.getModifiers();
            if (holder.isInterface() || Modifier.isAbstract(modifiers)) {
                throw new IllegalArgumentException(holder.getName() + " is abstract; the host builds the holder, so "
                        + "." + step + "(…) needs a concrete class");
            }
            if (holder.isEnum()) {
                throw new IllegalArgumentException(holder.getName() + " is an enum; the host builds the holder, so "
                        + "." + step + "(…) needs a class");
            }
            if (holder.isMemberClass() && !Modifier.isStatic(modifiers)) {
                throw new IllegalArgumentException(holder.getName() + " is an inner class; make it static so the "
                        + "host can build it");
            }
            if (holder.isLocalClass() || holder.isAnonymousClass()) {
                throw new IllegalArgumentException(holder.getName() + " is declared inside a method; make it a "
                        + "class of its own, or a static nested one, so the host can build it");
            }
        }
        return List.of(holders);
    }

    private static void once(String step, Class<?> already) {
        if (already != null) {
            throw new IllegalStateException("." + step + "(…) was already given " + already.getName()
                    + "; a plugin has one");
        }
    }

    private static <H> List<Class<? extends H>> added(List<Class<? extends H>> before,
                                                       List<Class<? extends H>> named) {
        List<Class<? extends H>> all = new ArrayList<>(before);
        all.addAll(named);
        return List.copyOf(all);
    }

    /** What each holder gives, in the order the holders were named; a value given twice counts once. */
    private static <H, E> List<E> given(List<Class<? extends H>> holders,
                                        Function<H, ? extends Collection<? extends E>> gives) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<E> found = new ArrayList<>();
        for (Class<? extends H> holder : holders) {
            Collection<? extends E> each = gives.apply(built(holder));
            if (each == null) continue;
            for (E one : each) {
                if (one != null && seen.add(one)) found.add(one);
            }
        }
        return List.copyOf(found);
    }

    /**
     * A fresh holder, built through its constructor with no arguments, whatever its access. Every way that fails —
     * its class failing to initialise or to link included — is an {@link IllegalStateException} naming the holder.
     */
    private static <H> H built(Class<? extends H> holder) {
        try {
            Constructor<? extends H> constructor = holder.getDeclaredConstructor();
            constructor.trySetAccessible();
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(holder.getName() + " needs a constructor with no arguments", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(holder.getName() + " failed while it was built", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(holder.getName() + " cannot be built from the host; make the class and "
                    + "its constructor public, or open its package", e);
        } catch (LinkageError e) {
            throw new IllegalStateException(holder.getName() + " could not be loaded", e);
        }
    }
}
