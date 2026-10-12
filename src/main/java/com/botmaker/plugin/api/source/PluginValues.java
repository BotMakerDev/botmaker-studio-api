package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.slot.ValueContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
 * <p><b>Addressed by the declaration, never by its id's spelling.</b> Every operation takes the plugin's own
 * {@link ManagedValue} constant, and a constant of an open set is a {@link Member}: a plugin never spells an id
 * or a {@code "Group.NAME"}. (The 0.8 operations taking a {@code String} id went in 0.9.0.)
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
 * constants, {@code Pictures} — the same two questions one level down ({@link #members(ManagedValue)},
 * {@link #open(ManagedValue, Member)}), plus the five changes a set undergoes: {@link #add(ManagedValue, Member,
 * Object) add}, {@link #rename(ManagedValue, Member, String) rename}, {@link #repoint(ManagedValue, Member, Member,
 * String) repoint}, {@link #remove(ManagedValue, Member) remove}, {@link #moveBefore(ManagedValue, Member, Member)
 * moveBefore}. Everything else — where the file is, which buffer is unsaved, how to make
 * the write undoable, what refers to a constant, whether the bot still compiles — is the host's, and a plugin asking
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
 *
 * <p>Every operation but {@link #ids} and {@link #open(ManagedValue)} is a {@code default} answering what a host
 * with no source tree answers: empty, or {@link #NO_SOURCE_TREE}.
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
     * The value {@code value} declares, or empty when there is none to edit.
     *
     * <p>Empty means <em>no method with that id</em>, <em>its body is not a single return</em>, or <em>its
     * type is one nothing registers</em> — three different sentences to a user, one answer to a plugin,
     * because a plugin's response to all three is the same: do not offer to edit it.
     *
     * <p>The context writes through the host's ordinary path, so a write lands in the open buffer as well as
     * the file and takes one history entry. Calling {@link ValueContext#set} repeatedly is fine; each call
     * replaces what the last one wrote.
     */
    Optional<ValueContext> open(ManagedValue<?> value);

    /**
     * Writes the class that holds {@code value} when the project has none, and answers why not when it did not.
     *
     * <p>The host creates {@code src/main/java/<bot package>/plugins/<last id segment>/<holder>.java} once —
     * every method-shaped value of the declaring plugin with the same {@link ManagedValue#holder}, each
     * returning its type's fresh value — and never overwrites a file that exists. From then on it is the
     * user's, as a file a template shipped is. It does not edit {@code main}: the status line says what to add.
     *
     * <p>Only the host can: it alone knows the bot's package and the grammar a fresh value is written in.
     * Empty when {@code value} is now there to {@link #open}, including when it already was.
     *
     * @return empty on success, else the sentence to show — no plugin declares {@code value} with a holder, no
     *         project is open, or the file could not be written
     */
    default Optional<String> create(ManagedValue<?> value) {
        return Optional.of(NO_SOURCE_TREE);
    }

    // ── an open set ─────────────────────────────────────────────────────────────────────────────────────
    //
    // A ManagedValue.openSet is a whole class — @SdkValue(SdkValue.Id.PICTURES) on Pictures — whose public
    // static final constants, or an enum's constants for a set declared ofEnum, the plugin adds, renames and
    // removes. Every operation below is addressed by the set's declaration and one constant's Member, and every
    // one is done by binding: the host resolves the constant and changes what javac says refers to it, never a
    // spelling. Each is total and refuses with the sentence to show.

    /**
     * One constant of an open set: its name, and for a set of nested enums ({@link ManagedValue#isGrouped}) the
     * simple name of the enum it is in — {@code Collect.WON} is {@code Member.of("Collect", "WON")}.
     *
     * <p>A value, compared by its parts. It owns the one spelling a member crosses as, {@code "Group.NAME"} or
     * {@code "NAME"} ({@link #toString}), so no plugin joins or splits one by hand.
     */
    final class Member {

        private final String group;
        private final String name;

        private Member(String group, String name) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("a member needs a name");
            if (group != null && group.isBlank()) throw new IllegalArgumentException(name + ": a blank group");
            this.group = group == null ? null : group.trim();
            this.name = name.trim();
        }

        /** A constant of a set of one class: {@code Pictures.ORE} is {@code Member.of("ORE")}. */
        public static Member of(String name) {
            return new Member(null, name);
        }

        /** A constant of the nested enum {@code group}: {@code Activities.Collect.WON}. */
        public static Member of(String group, String name) {
            return new Member(Objects.requireNonNull(group, "group"), name);
        }

        /**
         * The member {@code spelled} names, {@code "Collect.WON"} or {@code "ORE"} — the spelling a host reads off
         * the source. Throws for {@code null}, blank, or more than one dot.
         */
        public static Member parse(String spelled) {
            if (spelled == null) throw new IllegalArgumentException("no member spelled");
            int dot = spelled.indexOf('.');
            if (dot < 0) return of(spelled);
            if (spelled.indexOf('.', dot + 1) >= 0) throw new IllegalArgumentException(spelled + " is not a member");
            return of(spelled.substring(0, dot), spelled.substring(dot + 1));
        }

        /** {@link #parse}, total: empty for a spelling that names no member. */
        public static Optional<Member> read(String spelled) {
            try {
                return Optional.of(parse(spelled));
            } catch (IllegalArgumentException notAMember) {
                return Optional.empty();
            }
        }

        /** The nested enum's simple name, or {@code null} in a set of one class. */
        public String group() {
            return group;
        }

        /** The constant's own name: {@code WON}. */
        public String name() {
            return name;
        }

        /** Whether this is a constant of a nested enum. */
        public boolean grouped() {
            return group != null;
        }

        /** This member under {@code newName}, in the same enum. */
        public Member renamed(String newName) {
            return new Member(group, newName);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Member that && Objects.equals(group, that.group) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(group, name);
        }

        /** {@code "Collect.WON"}, or {@code "ORE"} in a set of one class. */
        @Override
        public String toString() {
            return group == null ? name : group + "." + name;
        }
    }

    /**
     * One place the bot's source refers to a constant, for the list a refusal shows. Built by the host.
     *
     * @param file the source file
     * @param line 1-based
     * @param text the line, trimmed
     */
    record Use(Path file, int line, String text) {}

    /**
     * The constants the open set {@code set} holds, in the order they are written. Empty when the project has no
     * class carrying its mark.
     */
    default List<Member> members(ManagedValue<?> set) {
        return List.of();
    }

    /**
     * One constant's initialiser as a value — {@code new ImageTemplate("…/ore.png")} for {@code Pictures.ORE}
     * — read and written through the same context a slot is. Empty when there is no such constant, or its
     * initialiser is not one the grammar reads, and always for an enum set's constant
     * ({@link ManagedValue#isEnum}), whose name is all it has.
     */
    default Optional<ValueContext> open(ManagedValue<?> set, Member member) {
        return Optional.empty();
    }

    /**
     * Declares {@code public static final <T> member = <value>;} in the open set's class, {@code T} being the
     * value's class and the initialiser written by the grammar. Refused when the name is not a Java name, is
     * already taken, or the value is one no loaded plugin declares. In an enum set ({@link ManagedValue#isEnum})
     * it is the bare constant {@code member}, and {@code value} — what {@link ManagedValue#byName} makes of it — is
     * only checked.
     */
    default <E> Optional<String> add(ManagedValue<E> set, Member member, E value) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Every use of the constant outside its own declaration. Empty when it is unused or cannot be resolved. */
    default List<Use> uses(ManagedValue<?> set, Member member) {
        return List.of();
    }

    /**
     * Renames the constant to {@code newName} — its own name, the enum it is in unchanged — and every use of it:
     * the host's one rename, by binding, refused when the bot would stop compiling. Exact, so nothing is marked
     * for review.
     */
    default Optional<String> rename(ManagedValue<?> set, Member member, String newName) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /**
     * Points every use of {@code member} at {@code replacement}, another constant of the same set. That is a
     * guess on the user's behalf, so each function it touched is marked {@code @Refactor(note)} where the bot
     * can compile the mark; a blank note marks nothing. Refused when either constant is missing or the result
     * would add a compile error. The declaration of {@code member} stays: {@link #remove(ManagedValue, Member)}
     * it after.
     */
    default Optional<String> repoint(ManagedValue<?> set, Member member, Member replacement, String note) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Deletes the constant's declaration. Refused while anything uses it, the uses listed in the sentence. */
    default Optional<String> remove(ManagedValue<?> set, Member member) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /**
     * Declares {@code member} just above {@code before}, another constant of the same set, when it is written
     * below it; nothing is written when it already comes first. What a plugin asks when one constant's initialiser
     * is to read another: javac refuses a static field read above its declaration, so the one read moves up.
     *
     * <p>The declaration moves whole, its javadoc and annotations with it; nothing that uses it changes. Refused
     * when either constant is missing, they are one constant, {@code member} shares its declaration with another
     * field, or the bot would not compile after the move — {@code member}'s own initialiser reads a constant
     * declared between the two. Always refused in an enum set ({@link ManagedValue#isEnum}, a set of nested
     * enums): an enum constant reads no other, and moving one changes its {@code ordinal()} and
     * {@code values()}.
     */
    default Optional<String> moveBefore(ManagedValue<?> set, Member member, Member before) {
        return Optional.of(NO_SOURCE_TREE);
    }

    // ── a set of nested enums ───────────────────────────────────────────────────────────────────────────
    //
    // A ManagedValue.openSet(…).ofEnums(…) holder is a class of enums nested in it, one per group:
    // Activities { enum Collect implements Outcome { NEXT, WON } }. Every operation above takes a member of such
    // a set as Member.of("Collect", "WON"), and members(set) lists them so. The operations below are on whole
    // enums, named by their simple name, and are done by binding as the ones above are.

    /** The simple names of the enums nested in the grouped set {@code set}, in the order they are written. */
    default List<String> groups(ManagedValue<?> set) {
        return List.of();
    }

    /**
     * Declares {@code public enum group implements <element> { constants… }} in the grouped set's class, after
     * its last one. Refused when the name is not a Java name or is taken, or {@code constants} is empty: an
     * enum a method returns needs a constant to return.
     */
    default Optional<String> addGroup(ManagedValue<?> set, String group, List<String> constants) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Renames the nested enum and every use of it — its class literal, its constants' qualifiers — by binding. */
    default Optional<String> renameGroup(ManagedValue<?> set, String group, String newName) {
        return Optional.of(NO_SOURCE_TREE);
    }

    /** Deletes the nested enum. Refused while anything uses it or one of its constants, the uses listed. */
    default Optional<String> removeGroup(ManagedValue<?> set, String group) {
        return Optional.of(NO_SOURCE_TREE);
    }

    // ── the bot's methods ───────────────────────────────────────────────────────────────────────────────

    /**
     * Every {@code public static} method with no parameter in the bot's own sources returning the nested enum
     * {@code group} of the grouped set {@code set}, each as the method reference a bot writes,
     * {@code Collect::body}, in source order.
     *
     * <p>What a plugin offers where a value names "the method that does this": the SDK's flow lists the methods
     * that can be one activity's body. The reference is the text the value crosses as
     * ({@code docs/architecture/two-strings.md}), compared and never parsed.
     */
    default List<String> methods(ManagedValue<?> set, String group) {
        return List.of();
    }

    /**
     * Every {@code public static void} method with no parameter in the bot's own sources, as
     * {@link #methods(ManagedValue, String)} lists them: what can be a flow's way home or its setup.
     */
    default List<String> voidMethods() {
        return List.of();
    }

    /**
     * The simple names of the bot's own top-level classes a method can be written into, in source order —
     * {@code Gamebot}, {@code Collect} — the plugins' value holders left out.
     *
     * <p>What a plugin offers beside {@link #createMethod(ManagedValue, String, String, String)}: the bot's
     * author picks the class the new method goes in, or types a new name, which that call then creates.
     */
    default List<String> classes() {
        return List.of();
    }

    /**
     * Writes {@code public static <group> method() { return <group>.<its first constant>; }} into the bot's class
     * {@code className}, creating the class beside the bot's main one when there is none.
     *
     * <p>Refused when either name is not a Java name, the method is already there, or the bot would not compile.
     *
     * @return the method reference written, {@code Collect::body}, or the sentence saying why nothing was
     */
    default MethodWrite createMethod(ManagedValue<?> set, String group, String className, String method) {
        return new MethodWrite(null, NO_SOURCE_TREE);
    }

    /** {@link #createMethod(ManagedValue, String, String, String)} for {@code public static void method() {}}. */
    default MethodWrite createVoidMethod(String className, String method) {
        return new MethodWrite(null, NO_SOURCE_TREE);
    }

    /**
     * What {@link #createMethod(ManagedValue, String, String, String)} did: the reference written, or why nothing
     * was.
     *
     * @param reference {@code Collect::body}, or null when refused
     * @param refusal   the sentence to show, or null when written
     */
    record MethodWrite(String reference, String refusal) {

        /** Whether the method was written. */
        public boolean written() {
            return reference != null;
        }
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
        public Optional<ValueContext> open(ManagedValue<?> value) {
            return Optional.empty();
        }
    };
}
