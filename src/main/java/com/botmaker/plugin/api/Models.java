package com.botmaker.plugin.api;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A plugin's own model, kept as compiled Java in the bot's source tree.
 *
 * <p><b>The problem this solves.</b> A bot is a Maven project with a {@code main}, a pom and Java sources,
 * and the stated goal is that a developer can continue one without any BotMaker tools. An activity's body is
 * already a method they wrote; a user parameter is already a {@code @Param} field. What stayed behind in
 * JSON was the <em>model</em> — which activities exist, what the flow is, what a plugin's own state holds —
 * and that is the part a developer cannot read, grep or refactor with the IDE they already have. It also has
 * the worst failure mode: a field renamed in Java is a compile error, and the same rename against a JSON
 * document is a silently empty value three screens into a bot run. The design is
 * {@code docs/refactor/33-plugin-java.md}.
 *
 * <p><b>A plugin hands over a record; the host derives both directions.</b> No text crosses in either
 * direction, and a plugin writes no emitter, no parser and no template. The alternative considered was a
 * write callback, and it fails for a measured reason: the half nobody is forced to write is the half that
 * rots. {@code ValueCodec.wireOfLiteral} was a {@code default} returning empty and nine of seventeen types
 * ever implemented it. A record makes that impossible — the components give the constructor to write and the
 * accessors to read, from one declaration the compiler checks.
 *
 * <h2>What a model may be made of</h2>
 *
 * <p>Every component's type, recursively: a registered value type, a {@code List} or {@code Map} (or any
 * container a plugin registered) of legal types, another legal record, an enum constant, {@code String}, the
 * eight primitives and their boxes. Anything else — an interface, an abstract class, a {@code Class<?>}, a
 * lambda, a mutable collection — is refused, <b>with the component named</b>, by {@link #problem}. Ask it
 * once when the plugin starts rather than discovering it the first time a user saves: the set the host
 * accepts is exactly the set it can write, which is how <i>what Studio writes always compiles</i> is a
 * property rather than a hope.
 *
 * <p>Nesting is unbounded. The two-container cap elsewhere is a <em>picker</em> cap, and nothing here is
 * picked by a user.
 *
 * <h2>Why the host, and only the host</h2>
 *
 * <p>The same reasoning as {@link Sources}, and it is the strong form of it: the host owns the source tree,
 * the open buffers, the history snapshot and the file roles, while the plugin owns what the data means. A
 * plugin knows a flow has three nodes; it has no idea where the bot's sources are, what is currently unsaved
 * in an editor buffer, or how to make the write undoable.
 *
 * <p><b>The file is the host's, and is not the plugin's to edit.</b> It is written whole every time, into a
 * package belonging to that plugin, and locked in the editor — a model is changed where it is modelled, not
 * in the file. The record class itself stays in the plugin's own jar, which the bot already depends on, so a
 * generated file adds a value and never a vocabulary.
 *
 * <p><b>Not everything a plugin holds belongs here.</b> Behaviour goes into Java; editor state — a node's
 * coordinates on a canvas, a collapsed section, a remembered filter — goes into a sidecar file the templates
 * gitignore. Compiling a pixel coordinate into a bot means a diff on the bot's behaviour every time someone
 * drags a box.
 */
public interface Models {

    /**
     * Writes {@code value} as the bot's own {@code <bot package>.plugins.<segment>.<simpleName>} and answers
     * the file, or empty when it could not be written.
     *
     * <p>Empty is an ordinary answer: no project is open, the project is open for reading, or the record is
     * one {@link #problem} refuses. Nothing here throws, so a plugin's save path has one branch.
     *
     * <p>The write is whole-file and snapshotted like every other host write, and identical content is not a
     * write at all — saving a model that did not change leaves the project clean.
     *
     * @param pluginId the plugin's own {@code StudioPlugin.id()}, whose last segment names the package —
     *                 the same normalisation {@code PluginData} applies to a plugin's JSON, so the two
     *                 schemes cannot drift. A plugin names its own id here for the reason it already does
     *                 when addressing its own storage: one host services instance serves every plugin, and
     *                 the alternative is a per-plugin one threaded through every place an editor is built.
     */
    Optional<Path> write(String pluginId, String simpleName, Record value);

    /**
     * Reads a model back, or empty when there is none.
     *
     * <p>Empty covers a project that never had one, which is the ordinary case on first open and the reason
     * this is not an exception. It also covers a file somebody edited by hand into something the host did
     * not write: the reader does not guess, so the answer is <em>no model</em> rather than a model with a
     * component nobody chose.
     */
    <T extends Record> Optional<T> read(String pluginId, String simpleName, Class<T> type);

    /**
     * Why {@code model} cannot be a model, naming the component, or empty when it can.
     *
     * <p>Total and free of side effects, so a plugin may ask at startup, in a test, or with no project open.
     * A plugin author reading <i>illegal model</i> learns nothing; one reading
     * <i>Flow.nodes.action is java.lang.Runnable</i> is finished.
     */
    Optional<String> problem(Class<? extends Record> model);

    /**
     * A host that keeps no models. Every answer is empty and nothing is refused for a reason it could not
     * check, which is what lets a plugin call these without asking whether they are supported.
     *
     * <p>Total, like {@link Sources#NONE} and {@link Runs#NONE}: the {@code botmaker} CLI's validator and a
     * test harness are hosts too, and neither owns a source tree.
     */
    Models NONE = new Models() {

        @Override
        public Optional<Path> write(String pluginId, String simpleName, Record value) {
            return Optional.empty();
        }

        @Override
        public <T extends Record> Optional<T> read(String pluginId, String simpleName, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public Optional<String> problem(Class<? extends Record> model) {
            return Optional.empty();
        }
    };
}
