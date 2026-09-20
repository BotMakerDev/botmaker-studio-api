package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueType;

import java.util.List;
import java.util.Optional;

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
 *   <li><b>palette</b> — {@link #catalog(String)}: the types and members worth proposing, their groups and
 *       their order.</li>
 *   <li><b>slot editors</b> — {@link #slotEditors()}: "for a value of type X, show this UI instead of a
 *       text field". They serve both places the host edits a value: a slot in a bot's Java, and a row in
 *       the Parameters window.</li>
 *   <li><b>value types</b> — {@link #valueTypes()}: the types a project variable may hold, and what their
 *       stored text means.</li>
 *   <li><b>parameters</b> — {@link #parameters(String)}: the sections of the Parameters window this plugin
 *       owns, and the generated class each one's values become fields of.</li>
 *   <li><b>parameter data</b> — {@link #parameterRows(String)} and
 *       {@link #parameterEdited(ParameterEdit)}: the rows currently declared in one of those sections, and
 *       where a changed value goes. The window is the host's; the file behind it is not.</li>
 *   <li><b>toolbar</b> &mdash; {@link #toolbarItems()}: buttons, contributed as data. The host owns the
 *       grouping, the order, the packing and the overflow menu; a plugin owns what a press does.</li>
 *   <li><b>source seeds</b> &mdash; {@link #sourceSeeds()}: what a <em>fresh</em> value of one of this
 *       plugin's types looks like, when the host's generic {@code new T()} would not compile. Data, like
 *       the toolbar and for the same reason — see {@link SourceSeed}.</li>
 * </ul>
 *
 * <p>{@link #projectOpened(StudioServices)} and {@link #projectClosing()} are not surfaces — they contribute
 * nothing. They are the two things a plugin cannot find out for itself: which project it is now serving, and
 * that the project it opened an operating-system resource for is gone. The first is what lets a plugin
 * answer {@link #parameterRows(String)} out of that project's own files, since a data surface takes no
 * services argument.
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
     * A stable identifier for this plugin — {@code "botmaker-sdk"} — used to attribute a contribution and to
     * order the host's merge. Never shown as-is; see {@link #displayName()}.
     */
    String id();

    /** The name a user reads. Defaults to {@link #id()}. */
    default String displayName() {
        return id();
    }

    /**
     * What this plugin offers the palette at the version a project actually pins.
     *
     * <p>The argument is the pinned version <em>as it is written in the project's pom</em> — the plugin
     * decides what that string means, since only it knows its own versioning. A plugin that recognises no
     * such version, or that does not curate at all, returns {@link PaletteCatalog#empty()}, which the host
     * reads as "offer everything the jar contains" rather than "offer nothing".
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}, but
     *                      may be a snapshot or a spelling this plugin does not recognise
     */
    default PaletteCatalog catalog(String pinnedVersion) {
        return PaletteCatalog.empty();
    }

    /**
     * The editors this plugin offers for value slots, in the order it wants them consulted.
     *
     * <p>Order matters only within one plugin: the host consults its own editors before any plugin's, so a
     * slot holding a project variable stays a variable no matter what a plugin claims about its type.
     */
    default List<SlotEditor> slotEditors() {
        return List.of();
    }

    /**
     * What a fresh value of one of this plugin's types looks like in source, for the types the host's
     * generic {@code new T()} cannot fill — an interface, a record with required components, or a type whose
     * meaning is a named constant.
     *
     * <p>Asked <b>every time a slot is seeded</b>, never cached, so a seed may read the project's live state.
     * The host consults its own seeds for the JDK types first, then every plugin's in load order, and falls
     * back to {@code new T()}; a seed whose expression will not parse is skipped rather than written.
     *
     * <p>{@code default} for the reason every method here but {@code id()} is: an older plugin contributes
     * none and the host seeds exactly as it did before.
     *
     * @see SourceSeed
     */
    default List<SourceSeed> sourceSeeds() {
        return List.of();
    }

    /**
     * The fields this plugin maintains through its own window, which the host shows but does not let its
     * code canvas edit — see {@link ManagedField}.
     *
     * <p>Read once per project bind, like the toolbar. A plugin that throws here costs only its own entries.
     * {@code default} for the reason every method here but {@code id()} is: an older plugin manages nothing,
     * and every field stays exactly as editable as it was.
     */
    default List<ManagedField> managedFields() {
        return List.of();
    }

    /**
     * The value types this plugin registers, with the codec that says what each one's stored text means.
     *
     * <p>This is the surface that makes the vocabulary <b>open</b>. It was a closed enum in the SDK until
     * 2026-08-27, which is right for one plugin and wrong for two: a plugin wanting a {@code Channel}
     * variable would have needed a constant granted in somebody else's enum. Now it declares one.
     *
     * <p><b>The id is the identity, and it is what the project file holds.</b> The host merges every
     * plugin's catalog by {@link ValueType#id()} and refuses two plugins claiming one id, because a project
     * that opens differently depending on which plugin loaded first is not a project. Prefix an id that is
     * not obviously yours. A type whose plugin is absent is not an error either — the value keeps its raw
     * text, renders read-only and declines to emit — so uninstalling a plugin costs the user nothing but
     * the ability to edit.
     */
    default ValueCatalog valueTypes() {
        return ValueCatalog.empty();
    }

    /**
     * The Java files this plugin gives a bot to hold its values, copied into the project once when the
     * plugin is added and never regenerated — see {@link PluginSource}.
     *
     * <p>This is how a plugin stores anything that used to live in its own JSON. It writes the class itself,
     * once, with working defaults and one {@code @Managed} method per value; the host copies it in, and from
     * then on rewrites nothing but the expression a {@code @Managed} method returns. So the plugin owns the
     * shape and the host owns the file, and neither has to describe the other's half.
     *
     * <p>Read at plugin-add time only. {@code default} for the reason every method here but {@code id()} is:
     * a plugin that keeps no values of its own contributes none, and a project gains no file.
     */
    default List<PluginSource> pluginSources() {
        return List.of();
    }

    /**
     * The sections this plugin owns in the Parameters window, at the version a project pins.
     *
     * <p>A plugin declares the <em>sections</em>; the user declares the values in them. Each group names a
     * heading, a key the project file files a variable under, and the generated class those variables become
     * fields of — see {@link ParameterGroup}. Returning nothing, the default, means this plugin has no
     * parameters of its own, which is the ordinary case for a plugin that only contributes a palette.
     *
     * <p>The argument is read exactly as {@link #catalog(String)}'s is: the pinned version as the project's
     * pom spells it, interpreted by the plugin alone. A plugin whose parameters class was introduced in a
     * later version may answer nothing for an older pin, and the host will then show that project no section
     * for it — which is the truth, since the jar the bot compiles against has no such class.
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}
     */
    default List<ParameterGroup> parameters(String pinnedVersion) {
        return List.of();
    }

    /**
     * The rows currently declared in one of this plugin's {@linkplain #parameters(String) sections}.
     *
     * <p><b>This is the surface that stops the host owning a project's parameter file.</b> The Parameters
     * window is the host's — one window, one section per group, so a user configuring a bot configures one
     * thing — but what the rows <em>are</em> is project data belonging to whoever stores it. Before this
     * existed, the host parsed that file itself, which meant the host knew one plugin's storage format and
     * no second plugin could have had one.
     *
     * <p><b>Asked, never pushed.</b> The host calls this when it draws the section and again after an edit;
     * a plugin does not notify. There is no listener here because a listener is a capability with a
     * lifecycle — a registration, a thread, an unsubscribe — and the one thing it would buy (a plugin's own
     * dialog changing a value behind the window's back) is worth revisiting when a plugin has such a dialog,
     * not before. Everything the window itself changes comes back through
     * {@link #parameterEdited(ParameterEdit)}, whose answer is the new row.
     *
     * <p>An id this plugin does not own answers nothing, which is the ordinary state rather than an error:
     * the host asks each plugin for each section it declared, and a plugin that has since stopped declaring
     * one is simply not asked again.
     *
     * @param groupId the {@link ParameterGroup#id()} whose rows are wanted; never {@code null}, and
     *                {@link ParameterGroup#DEFAULT_ID} for the default section
     * @return the rows, in the order the window should list them within their categories
     */
    default List<ParameterRow> parameterRows(String groupId) {
        return List.of();
    }

    /**
     * A value the user changed — store it, and answer with the row as stored.
     *
     * <p>The answer is what the window then renders, so a plugin that clamps a number to its
     * {@link com.botmaker.plugin.api.value.Range}, prunes a list to the options still on offer or spells a
     * duration back out canonically reports all of it by answering a row that differs from the edit. A
     * plugin that <b>refuses</b> the edit answers the row it still holds, and the control snaps back; a
     * plugin that does not own the row, or the group, answers {@link Optional#empty()} and the host leaves
     * the screen alone.
     *
     * <p><b>Persisting is the plugin's, and so is when.</b> The host has already told the user their edit
     * landed by the time this returns, so writing a file on every keystroke is the plugin's problem to batch
     * — nothing here promises a save point, and {@link #projectClosing()} is the one moment a plugin is told
     * the project is going away.
     *
     * <p>Throwing is contained and reported, and the row on screen is then left as it was: an edit that
     * cannot be stored must not be able to take the window down with it.
     *
     * @param edit the group, the name and the new stored text — see {@link ParameterEdit}
     * @return the row as it now stands, or empty when this plugin does not own it
     */
    default Optional<ParameterRow> parameterEdited(ParameterEdit edit) {
        return Optional.empty();
    }

    // parameterDeclared(ParameterDeclaration) stood here from 2026-09-10 to 2026-09-17: the declaration half
    // of the parameters window, one call carrying the row as wanted rather than a verb. It is gone with the
    // record it took, because a *user* parameter is no longer a row a plugin stores — it is a @Param field in
    // the bot's own Java, and the host declares it by editing the syntax tree. What a plugin still owns is
    // its own rows (an activity's enable flag, a capture target), and those it declares in its own code,
    // where a wire form for "here is the row I want" buys nothing. The host reads them through
    // parameterRows(String) and changes a value through parameterEdited(ParameterEdit), which are unchanged.

    /**
     * The toolbar buttons this plugin contributes.
     *
     * <p>Data, not nodes — see {@link ToolbarItem} for why, and for the rule that the label is a supplier so
     * a button may say what the project currently holds. The host groups, orders, packs, overflows and
     * themes them; this decides only what is offered and what a press does.
     *
     * <p>{@link ToolbarGroup#STUDIO} is <b>refused</b>, with the plugin named. It is the host's own section,
     * for the things that would still make sense with every plugin uninstalled, and an item quietly re-homed
     * out of it would be worse than a refusal: a user reads that part of the bar as the application rather
     * than as their project.
     *
     * <p>Called once when a project's plugins are bound, not on every layout. A plugin whose set of items
     * depends on state should return them all and let a supplier or an {@link EnabledWhen} say which apply.
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
     * <p><b>It is what makes the data surfaces answerable at all.</b> {@link #parameterRows(String)} takes a
     * group id and nothing else, deliberately: rows are asked for every time a window is drawn, and a
     * surface that had to be handed the host on each call would make every future one take it too. So the
     * host says <em>here is the project</em> once per bind, and the plugin reads its own file from
     * {@link StudioServices#projectDir()} or {@link StudioServices#resourcesDir()} whenever it is asked.
     *
     * <p>Called once per bind, <b>after</b> the outgoing project's plugins have been told it is closing, and
     * again whenever a change to the project's libraries rebinds the set. A plugin that keeps the services
     * should replace what it held rather than accumulate, since the instance is reused across projects.
     *
     * <p><b>Do nothing expensive here.</b> This runs while a project is opening, on the path the user is
     * waiting on. Read the file when a surface is asked for, not now — the same rule that keeps
     * {@code buildCatalog}/{@code buildValueTypes} lazy, and for the same reason: a plugin that parses a
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
