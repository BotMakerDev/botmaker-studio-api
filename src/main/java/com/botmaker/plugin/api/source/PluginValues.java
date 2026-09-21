package com.botmaker.plugin.api.source;

import com.botmaker.plugin.api.slot.ValueContext;

import java.util.List;
import java.util.Optional;

/**
 * A plugin's own values, as they are written in the bot's Java.
 *
 * <p>Each one is a {@code @Managed("id")} method in the file this plugin gave the bot
 * ({@link PluginSource}). The host finds it by that id, works out its type from the method's declared return
 * type, and hands back a {@link ValueContext} over the expression the method returns — the same context a
 * slot on the canvas and a row in the Parameters window are edited through. So a plugin's own window reads
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
 * <h2>Two methods, because that is all a plugin needs</h2>
 *
 * <p>What ids are there, and open one. Everything else — where the file is, which buffer is unsaved, how to
 * make the write undoable, whether the expression is one the value catalog can read — is the host's, and a
 * plugin asking about any of it would be asking about the editor rather than about its own data.
 *
 * <p><b>Reading may fail, and that is ordinary.</b> A user may hand-edit a {@code @Managed} body into
 * something that is not a single {@code return <expression>;}, or use a value type no loaded plugin
 * registers. The host shows that read-only with a sentence saying why, and {@link #open} answers empty: the
 * value is intact and the user has not lost anything, so this is a state to report rather than repair.
 */
public interface PluginValues {

    /**
     * Every {@code @Managed} id this plugin has in the open project, in no promised order.
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
     * A host that keeps no source tree: no ids, nothing to open.
     *
     * <p>Total rather than absent, as {@code Sources.NONE} and {@code Runs.NONE} are, so the
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
