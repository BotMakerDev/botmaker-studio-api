package com.botmaker.plugin.api;

import com.botmaker.plugin.api.model.ModelCall;
import com.botmaker.plugin.api.model.ModelStatement;

import java.nio.file.Path;
import java.util.List;
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
 * <h2>A model is a sequence of calls</h2>
 *
 * <p>A plugin hands over {@link ModelStatement}s — <i>call {@code Activities.declare} with these six
 * things</i> — and the host writes the Java and reads it back. <b>No text crosses in either direction</b>,
 * and a plugin writes no emitter, no parser and no template.
 *
 * <p>The shape is the one the contract already had one level down. {@code ValueCatalog.initializer} writes
 * {@code java.util.List.of(a, b, c)} and {@code valueOf} reads it back; a statement is that with a
 * semicolon. So the writer and the reader are machinery that exists and is tested, and what a plugin adds is
 * a description of its own calls ({@link ModelCall}) rather than any code that produces text.
 *
 * <p>It is also the shape the bot already uses: {@code Activities.define("Collect", ctx -> {…})} is a name
 * and a body written as a statement in the user's own Java. The generated file reads like the file beside
 * it.
 *
 * <h2>Why the host, and only the host</h2>
 *
 * <p>The same reasoning as {@link Sources}, in its strong form: the host owns the source tree, the open
 * buffers, the history snapshot and the file roles, while the plugin owns what the data means. A plugin
 * knows a flow has three nodes; it has no idea where the bot's sources are, what is currently unsaved in an
 * editor buffer, or how to make the write undoable.
 *
 * <p><b>The file is the host's, and is not the plugin's to edit.</b> It is written whole every time, into a
 * package belonging to that plugin, and locked in the editor — a model is changed where it is modelled, not
 * in the file.
 *
 * <p><b>Not everything a plugin holds belongs here.</b> Behaviour goes into Java; editor state — a node's
 * coordinates on a canvas, a collapsed section, a remembered filter — goes into a sidecar file the templates
 * gitignore. Compiling a pixel coordinate into a bot means a diff on the bot's behaviour every time someone
 * drags a box.
 */
public interface Models {

    /**
     * Writes {@code statements} as the bot's own {@code <bot package>.plugins.<segment>.<simpleName>} and
     * answers the file, or empty when it could not be written.
     *
     * <p>Empty is an ordinary answer: no project is open, the project is open for reading, a statement names
     * a call this plugin never declared, or an argument is one no codec can write. Nothing here throws, so a
     * plugin's save path has one branch.
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
    Optional<Path> write(String pluginId, String simpleName, List<ModelStatement> statements);

    /**
     * Reads a model back, in the order it was written, or an empty list when there is none.
     *
     * <p>Empty covers a project that never had one, which is the ordinary case on first open and the reason
     * this is not an exception. It also covers a file somebody edited by hand into something the host did
     * not write: the reader does not guess, so the answer is <em>no model</em> rather than a model with a
     * statement nobody chose.
     */
    List<ModelStatement> read(String pluginId, String simpleName);

    /**
     * Why {@code call} cannot be written, naming the argument, or empty when it can.
     *
     * <p>One question per argument — <i>is this a form the catalog can write and read back</i> — asked once
     * when the plugin starts rather than the first time a user saves. A plugin author reading <i>illegal
     * model</i> learns nothing; one reading <i>Activities.declare#outcomes is a Set, which no installed
     * plugin registers as a container</i> is finished.
     *
     * <p>The catalog it is answered against is the <b>merged</b> one, which is why this is on the host: no
     * plugin can assemble it alone, and whether {@code Duration} is a registered type depends on what else
     * is loaded.
     */
    Optional<String> problem(ModelCall call);

    /**
     * A host that keeps no models. Every answer is empty and nothing is refused for a reason it could not
     * check, which is what lets a plugin call these without asking whether they are supported.
     *
     * <p>Total, like {@link Sources#NONE} and {@link Runs#NONE}: the {@code botmaker} CLI's validator and a
     * test harness are hosts too, and neither owns a source tree.
     */
    Models NONE = new Models() {

        @Override
        public Optional<Path> write(String pluginId, String simpleName, List<ModelStatement> statements) {
            return Optional.empty();
        }

        @Override
        public List<ModelStatement> read(String pluginId, String simpleName) {
            return List.of();
        }

        @Override
        public Optional<String> problem(ModelCall call) {
            return Optional.empty();
        }
    };
}
