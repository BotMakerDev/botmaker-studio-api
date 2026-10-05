package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.record.RecordedValue;
import com.botmaker.plugin.api.slot.SlotEditor;
import com.botmaker.plugin.api.source.ManagedValue;
import com.botmaker.plugin.api.toolbar.ToolbarItem;
import com.botmaker.plugin.api.value.ComponentType;
import com.botmaker.plugin.api.value.PluginType;

import java.util.List;

/**
 * What a BotMaker Studio plugin is, from the host's side: an id and a set of contributions.
 *
 * <p><b>Every method but {@link #id()} is {@code default}, and that is the versioning rule of the whole
 * platform.</b> A bot's own source can be rewritten when the SDK changes — Studio holds an AST of it — but a
 * plugin's compiled {@code .class} files cannot be rewritten by anybody. So this contract may only gain
 * members that older plugins can ignore. A method a plugin has not implemented returns "nothing to
 * contribute" rather than throwing {@code AbstractMethodError}, which means a plugin built against an
 * earlier release of this module keeps working across every Studio release until a Studio <em>major</em>
 * bump explicitly refuses it.
 *
 * <h2>The contribution surfaces</h2>
 * <ul>
 *   <li><b>palette</b> — {@link #catalog()}: the types and members worth proposing, their groups and
 *       their order.</li>
 *   <li><b>types</b> — {@link #types()}: the types a project value may hold. One {@link PluginType} per
 *       type says what it is, what a fresh one is and how a person edits one — and, where its Java is a
 *       call, a {@link com.botmaker.plugin.api.value.ComponentType} beside it says what goes in the
 *       brackets.</li>
 *   <li><b>slot editors</b> — {@link #slotEditors()}: the two editors a type cannot choose — one picked by
 *       the call a slot sits in, and an override of an editor for a type another plugin declared.</li>
 *   <li><b>toolbar</b> &mdash; {@link #toolbarItems()}: buttons, contributed as data. The host owns the
 *       grouping, the order, the packing and the overflow menu; a plugin owns what a press does.</li>
 *   <li><b>managed values</b> &mdash; {@link #managedValues()}: the values this plugin maintains through
 *       its own window, which the host shows but does not let its code canvas edit.</li>
 * </ul>
 *
 * <p>{@link #projectOpened(StudioServices)} and {@link #projectClosing()} are not surfaces — they contribute
 * nothing. They are the two things a plugin cannot find out for itself: which project it is now serving, and
 * that the project it opened an operating-system resource for is gone. The first is what lets a plugin read
 * that project's own files at all, since no contribution surface takes a services argument.
 *
 * <p><b>Panels are deliberately not a surface.</b> A plugin contributes to the editor; it does not
 * contribute editors. The Activity Canvas and every other whole view stays the host's.
 *
 * <p><b>Neither are files.</b> There was briefly a sixth surface — {@code scaffold} and {@code seedings},
 * through which a plugin shipped real compiling classes that a host wrote into a user's project and then
 * maintained forever. It is gone, and the rule that replaces it is the one to apply to anything proposed in
 * its place: <b>a project's structure belongs to the user, and a plugin contributes methods a user calls.</b>
 * A file a plugin owns inside somebody's source tree is a file its user cannot freely edit, rename or delete,
 * and the machinery that keeps such a file owned — a key ledger, a reconciler, a rename engine — is all cost
 * paid to work around that one fact. Everything a seed was for has an answer on this side of the line:
 * behaviour is a static method the user calls, and anything that followed from project data was data all
 * along and is read at runtime.
 */
public interface StudioPlugin {

    /**
     * <b>The way to declare a plugin</b>: the first step of a {@link PluginDeclaration}, handed to
     * {@link DeclaredPlugin}'s constructor — {@code StudioPlugin.id(ID).named(NAME).types(() -> …)…}.
     * Implementing this interface by hand still works; the declaration is there so that nobody has to find
     * out which of its methods to override.
     */
    static PluginDeclaration.Naming id(String id) {
        return new PluginDeclaration.Naming(id);
    }

    /**
     * A stable identifier for this plugin — {@code "botmaker-sdk"} — used to attribute a contribution and to
     * order the host's merge. Never shown as-is; see {@link #displayName()}.
     */
    String id();

    /** The name a user reads. Defaults to {@link #id()}. */
    default String displayName() {
        return id();
    }

    /**
     * A palette this plugin builds by hand, overriding the one the host discovers.
     *
     * <p><b>Most plugins leave this alone.</b> The host finds every class carrying {@code @Palette} in the
     * plugin's own jar and offers it, reading members off {@code @Hidden}, {@code @PaletteLabel} and
     * {@code @PaletteDefault}, and catalogues every type of the jar those offered members reach. The annotations are the declaration; no plugin lists its classes. The
     * default, {@link PaletteCatalog#empty()}, means "discover it".
     */
    default PaletteCatalog catalog() {
        return PaletteCatalog.empty();
    }

    /**
     * The types this plugin declares — one {@link PluginType} each, in the order a picker should offer
     * them.
     *
     * <p>This is the surface that makes the vocabulary <b>open</b>. A closed enum is right for one plugin and
     * wrong for two: a plugin wanting a {@code Channel} value would need a constant granted in somebody else's
     * enum.
     *
     * <p><b>The type is the identity, and it is the Java type the user's file writes.</b> The host indexes
     * every plugin's declarations by {@link PluginType#type()}'s canonical name and refuses two plugins
     * <em>owning</em> one type, because a project that opens differently depending on which plugin loaded
     * first is not a project. Offering an editor for somebody else's type is not owning it — that is
     * {@link SlotEditor#forType}, and the host asks the user which to use.
     *
     * <p>A type whose plugin is absent is not an error: the value keeps the expression the author wrote,
     * renders read-only and is never rewritten, so uninstalling a plugin costs the user nothing but the
     * ability to edit.
     *
     * <p>One declaration says all of a type — what it is and what a fresh one is — so the compiler asks for
     * all of it at once, and nothing about a type is a string the compiler never looks at.
     */
    default List<PluginType<?>> types() {
        return List.of();
    }

    /**
     * The composites this plugin takes apart and puts back that are <em>not</em> among {@link #types()} —
     * the parts of a value, never picked on their own.
     *
     * <p>The SDK's {@code Flow} is the case: its {@code Activity}, {@code Edge}, {@code Preset} and
     * {@code Limits} are written as calls inside the one call that writes a flow, so the host has to read
     * each of them back, and none of them is a type anybody declares a parameter of. A
     * {@link ComponentType} that is also a {@link PluginType} need not be listed here — the host finds it
     * among {@link #types()} — though listing it twice is harmless. A {@link ComponentType} that is not also a
     * {@link PluginType} has no other way to reach the host: unlisted, a value built of it could never be
     * decoded.
     */
    default List<ComponentType<?>> componentTypes() {
        return List.of();
    }

    /**
     * The editors this plugin offers that a type cannot choose for itself, in the order it wants them
     * consulted.
     *
     * <p>{@link SlotEditor#onParameter} claims an argument by an annotation on the parameter it is passed to,
     * for values the type cannot tell apart — a Steam app id and a window title are both {@code String}.
     * {@link SlotEditor#forType} <em>overrides</em> the editor for a type <b>another</b> plugin declared,
     * which is how the SDK offers a colour picker that samples the capture target for the
     * {@code java.awt.Color} plugin-basics declares. {@link SlotEditor#when} is for a claim neither states.
     *
     * <p>An editor for this plugin's <em>own</em> type does not belong here: it is
     * {@link com.botmaker.plugin.api.value.EditableType#editor}, declared beside the type, so the type is named
     * once.
     *
     * <p>Order matters only within one plugin: the host consults its own editors before any plugin's, so a
     * slot holding a project variable stays a variable no matter what a plugin claims about its type.
     */
    default List<SlotEditor> slotEditors() {
        return List.of();
    }

    /**
     * The values this plugin maintains through its own window, which the host shows but does not let its
     * code canvas edit — see {@link ManagedValue}.
     *
     * <p>Each one names an id a {@code @Managed} method or type in the bot's Java carries, and the sentence
     * to show when the canvas refuses an edit to it. These are also the ids {@link com.botmaker.plugin.api.source.PluginValues#open} will
     * answer for: what the plugin declares here is what it may read and write.
     *
     * <p>Read once per project bind, like the toolbar. A plugin that throws here costs only its own entries.
     * {@code default} for the reason every method here but {@code id()} is: an older plugin manages nothing,
     * and every value stays exactly as editable as it was.
     */
    default List<ManagedValue<?>> managedValues() {
        return List.of();
    }

    /**
     * The parameter types of this plugin's {@link com.botmaker.plugin.api.record.Records} methods that only it can
     * fill from a recording — see {@link RecordedValue}. Read once per project bind. A plugin with none records
     * with what the host fills alone.
     */
    default List<RecordedValue<?>> recordedValues() {
        return List.of();
    }

    /**
     * The toolbar buttons this plugin contributes.
     *
     * <p>Data, not nodes — see {@link ToolbarItem} for why, and for the rule that the label is a supplier so
     * a button may say what the project currently holds. The host groups, orders, packs, overflows and
     * themes them; this decides only what is offered and what a press does.
     *
     * <p>{@link com.botmaker.plugin.api.toolbar.ToolbarGroup#STUDIO} is <b>refused</b>, with the plugin named. It is the host's own section,
     * for the things that would still make sense with every plugin uninstalled, and an item quietly re-homed
     * out of it would be worse than a refusal: a user reads that part of the bar as the application rather
     * than as their project.
     *
     * <p>Called once when a project's plugins are bound, not on every layout. A plugin whose set of items
     * depends on state should return them all and let a supplier or an {@link com.botmaker.plugin.api.toolbar.EnabledWhen} say which apply.
     */
    default List<ToolbarItem> toolbarItems() {
        return List.of();
    }

    /**
     * A project has been bound to this plugin — this is which one.
     *
     * <p><b>The mirror of {@link #projectClosing()}, and a capability for the same reason.</b> Which project
     * is open is the one fact a plugin cannot establish for itself: a plugin is constructed once by
     * {@code ServiceLoader} and then serves whatever the host binds to it, so without being told it is
     * holding a project it cannot name. Nothing else can supply it — a plugin polling for the answer would
     * be guessing at a moment the host knows precisely, which is the argument {@code projectClosing()}
     * already makes from the other end.
     *
     * <p>No contribution surface takes a services argument, deliberately: a surface that had to be handed the
     * host on each call would make every future one take it too. So the host says <em>here is the project</em>
     * once per bind, and the plugin reads its own values ({@link StudioServices#pluginValues()}) and files
     * ({@link StudioServices#resourcesDir()}) whenever it is asked.
     *
     * <p>Called once per bind, <b>after</b> the outgoing project's plugins have been told it is closing, and
     * again whenever a change to the project's libraries rebinds the set. A plugin that keeps the services
     * should replace what it held rather than accumulate, since the instance is reused across projects.
     *
     * <p><b>Do nothing expensive here.</b> This runs while a project is opening, on the path the user is
     * waiting on. Read the file when a surface is asked for, not now — the same rule that keeps
     * {@link DeclaredPlugin}'s surfaces behind suppliers, and for the same reason: a plugin that parses a
     * project on bind is a plugin every project open pays for whether or not anything reads the result.
     *
     * <p>Throwing is contained and reported, and the project still opens: a plugin must not be able to
     * prevent one from being bound.
     *
     * @param services the open project's paths, theme, dialogs and run channel; never {@code null}
     */
    default void projectOpened(StudioServices services) {
    }

    /**
     * The open project is closing — release anything held on its behalf.
     *
     * <p>Called once per bind, on the plugins that were serving the project being left, and <b>before</b>
     * their classloader is closed, so a plugin may still run its own code here. It is called on the way to
     * another project, on the way to no project at all, and whenever a change to the project's libraries
     * rebinds the set.
     *
     * <p><b>This is a capability, not a courtesy.</b> A contribution surface returns data and needs no
     * lifecycle; but a plugin that opens something the operating system counts — a bound port, a nested
     * display, a child process, a watch on a directory — has no way to learn that the project it opened them
     * for is gone. Nothing else can tell it: which project is open is exactly the question
     * {@link StudioServices} exists for, and a plugin polling for the answer would be guessing at a moment
     * the host already knows precisely. Everything that can be released by garbage collection should be, and
     * needs no implementation here.
     *
     * <p><b>The instance is reused.</b> A plugin is constructed once by {@code ServiceLoader} and serves
     * every project bound to it afterwards, so this says *this project is over*, never *you are being
     * discarded*. Leave the object usable: whatever is released here has to be reacquired on demand.
     *
     * <p><b>Throwing is contained but not free.</b> The host catches and reports, then carries on binding the
     * next project — a plugin must not be able to prevent one from opening. What it cannot do is finish
     * releasing on the plugin's behalf, so an exception thrown halfway through leaks whatever was left.
     */
    default void projectClosing() {
    }

}
