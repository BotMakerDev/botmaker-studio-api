package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.slot.ValueContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A plugin's own values, as they are written in the bot's Java.
 *
 * <p>Each one is a method marked with the plugin's own annotation ({@code @SdkValue(SdkValue.Id.FLOW)},
 * meta-annotated {@link com.botmaker.plugin.api.managed.ManagedMarker}) in the bot's own source — the file its
 * template shipped, or the one {@link #create} wrote. Its id is
 * {@link com.botmaker.plugin.api.source.ManagedValue#idOf} that constant: the enum's binary name and the
 * constant's ({@code com.example.SdkValue$Id.FLOW}). The host finds the method by that id, works out its type
 * from the method's declared return type, and hands back a {@link ValueContext} over the expression the method
 * returns — the same context a slot on the canvas and a row in the Parameters window are edited through. So a plugin's own window reads
 * and writes its value with the interface it already knows, and no second way to edit a value exists.
 *
 * <h2>This is where a plugin's JSON went</h2>
 *
 * <p>Until this interface, a plugin kept its data in {@code plugins/<id>/<name>.json} and a bot read it back
 * by name at runtime. That is the arrangement {@code docs/refactor/33-plugin-java.md} exists to remove: a
 * name renamed in Java is a compile error, while the same rename against JSON is a silently empty value
 * three screens into a run. A value that lives in the bot's own source is one a developer with no BotMaker
 * installed can read, grep, refactor and hand to a compiler.
 *
 * <h2>What ids are there, open one — and for an open set, its constants</h2>
 *
 * <p>For a value held by one method: what ids are there, and open one. For an open set — a whole class of
 * constants, {@code Pictures} — the same two questions one level down ({@link #members}, {@link #open(String,
 * String)}), plus the four changes a set undergoes: {@link #add}, {@link #rename}, {@link #repoint},
 * {@link #remove}. Everything else — where the file is, which buffer is unsaved, how to make the write
 * undoable, what refers to a constant, whether the bot still compiles — is the host's, and a plugin asking
 * about any of it would be asking about the editor rather than about its own data.
 *
 * <p><b>By binding, never by spelling.</b> A find-and-replace over how a plugin guesses its names are spelled
 * misses a qualified use, a static import or a class the user renamed, and renames the uses but not the
 * declaration. A constant's uses are what javac resolves to it, and a rename or repoint that would stop the bot compiling
 * is refused. A path literal a user wrote in their own code is theirs and nothing matches it.
 *
 * <p><b>Reading may fail, and that is ordinary.</b> A user may hand-edit a marked method's body into
 * something that is not a single {@code return <expression>;}, or use a value type no loaded plugin
 * registers. The host shows that read-only with a sentence saying why, and {@link #open} answers empty: the
 * value is intact and the user has not lost anything, so this is a state to report rather than repair.
 */
public interface PluginValues {

    /**
     * Every managed id this plugin has in the open project, in no promised order.
     *
     * <p>Empty when the plugin's file is not in the project — it was never added, or the user deleted it.
     * That is not an error: a plugin whose file is gone has no values, and offering to put it back is a
     * better answer than writing one uninvited.
     */
    List<String> ids();

    /**
     * The value behind {@code id}, or empty when there is none to edit.
     *
     * <p>Empty means <em>no method with that id</em>, <em>its body is not a single return</em>, or <em>its
     * type is one nothing registers</em> — three different sentences to a user, one answer to a plugin,
     * because a plugin's response to all three is the same: do not offer to edit it.
     *
     * <p>The context writes through the host's ordinary path, so a write lands in the open buffer as well as
     * the file and takes one history entry. Calling {@link ValueContext#set} repeatedly is fine; each call
     * replaces what the last one wrote.
     */
    Optional<ValueContext> open(String id);

    /**
     * Writes the class that holds {@code id} when the project has none, and answers why not when it did not.
     *
     * <p>The host creates {@code src/main/java/<bot package>/plugins/<last id segment>/<holder>.java} once —
     * every method-shaped value of the declaring plugin with the same {@link ManagedValue#holder}, each
     * returning its type's fresh value — and never overwrites a file that exists. From then on it is the
     * user's, as a file a template shipped is. It does not edit {@code main}: the status line says what to add.
     *
     * <p>Only the host can: it alone knows the bot's package and the grammar a fresh value is written in.
     * Empty when {@code id} is now there to {@link #open}, including when it already was.
     *
     * @return empty on success, else the sentence to show — no plugin declares {@code id} with a holder, no
     *         project is open, or the file could not be written
     */
    default Optional<String> create(String id) {
        return Optional.of(NO_SOURCE_TREE);
    }

    // ── an open set ─────────────────────────────────────────────────────────────────────────────────────
    //
    // A ManagedValue.openSet is a whole class — @SdkValue(SdkValue.Id.PICTURES) on Pictures — whose public
    // static final constants, or an enum's constants for a set declared ofEnum, the plugin adds, renames and
    // removes. Every operation below is addressed by the set's id and
    // one constant's name, and every one is done by binding: the host resolves the constant and changes what
    // javac says refers to it, never a spelling. Each is total and refuses with the sentence to show.

    /**
     * One place the bot's source refers to a constant, for the list a refusal shows. Built by the host.
     *
     * @param file the source file
     * @param line 1-based
     * @param text the line, trimmed
     */
    record Use(Path file, int line, String text) {}

    /**
     * The names of the constants the open set {@code id} holds, in the order they are written. Empty when the
     * project has no class carrying {@code id}'s mark.
     */
    default List<String> members(String id) {
        return List.of();
    }

    /**
     * One constant's initialiser as a value — {@code new ImageTemplate("…/ore.png")} for {@code Pictures.ORE}
     * — read and written through the same context a slot is. Empty when there is no such constant, or its
     * initialiser is not one the grammar reads, and always for an enum set's constant
     * ({@link ManagedValue#isEnum}), whose name is all it has.
     */
    default Optional<ValueContext> open(String id, String member) {
        return Optional.empty();
    }

    /**
     * Declares {@code public static final <T> member = <value>;} in the open set's class, {@code T} being the
     * value's class and the initialiser written by the grammar. Refused when the name is not a Java name, is
     * already taken, the value is not of the set's {@link ManagedValue#type() element type}, or it is one no
     * loaded plugin declares. In an enum set ({@link ManagedValue#isEnum}) it is the bare constant
     * {@code member}, and {@code value} — what {@link ManagedValue#byName} makes of it — is only checked.
     */
    default Optional<String> add(String id, String member, Object value) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Every use of the constant outside its own declaration. Empty when it is unused or cannot be resolved. */
    default List<Use> uses(String id, String member) {
        return List.of();
    }

    /**
     * Renames the constant and every use of it — the host's one rename, by binding, refused when the bot would
     * stop compiling. Exact, so nothing is marked for review.
     */
    default Optional<String> rename(String id, String member, String newName) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /**
     * Points every use of {@code member} at {@code replacement}, another constant of the same set. That is a
     * guess on the user's behalf, so each function it touched is marked {@code @Refactor(note)} where the bot
     * can compile the mark; a blank note marks nothing. Refused when either constant is missing or the result
     * would add a compile error. The declaration of {@code member} stays: {@link #remove} it after.
     */
    default Optional<String> repoint(String id, String member, String replacement, String note) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Deletes the constant's declaration. Refused while anything uses it, the uses listed in the sentence. */
    default Optional<String> remove(String id, String member) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** What every write answers on a host with no project behind it. */
    String NO_SOURCE_TREE = "This host keeps no source tree.";

    /**
     * A host that keeps no source tree: no ids, nothing to open.
     *
     * <p>Total rather than absent, as {@code Runs.NONE} is, so the
     * {@code botmaker} CLI's validator can construct a plugin and let it ask without a null check reaching
     * plugin code.
     */
    PluginValues NONE = new PluginValues() {

        @Override
        public List<String> ids() {
            return List.of();
        }

        @Override
        public Optional<ValueContext> open(String id) {
            return Optional.empty();
        }
    };
}
