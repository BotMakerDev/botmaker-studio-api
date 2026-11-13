package com.botmaker.plugin.api;

import com.botmaker.plugin.api.source.PluginValues;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The host facilities a plugin may use, and deliberately no more than that.
 *
 * <p><b>The test for adding one is that the host is the only possible source of it</b> — not that the host
 * happens to have written it first. Which project is open, what theme the user chose, which window owns a
 * dialog, and the overlay that goes over every window on the screen: a plugin cannot answer any of those, so
 * they are here. Everything else it can do for itself, because {@code botmaker-shared} is published and any
 * plugin may depend on it — enumerating monitors, windows and emulator instances, grabbing pixels from them,
 * reading a launcher's installed-game library — and because the files under {@link #resourcesDir()} are
 * ordinary files.
 *
 * <p><b>Not "a real editor needed it".</b> That test lets a plugin's own vocabulary in — a named picture, a
 * capture source, a launcher — so plugin #1 reaches through the contract for what no second plugin could:
 * the back door this module exists to close. Generalising such a member only moves the same privilege behind
 * a wider name. A full-screen pick over a running game is the plugin's too: it grabs its own pixels through
 * {@code botmaker-shared}, and the toolkit's screen-pick widgets take them from {@code ScreenPicks}.
 */
public interface StudioServices {

    /** The root of the open project — where a plugin reads and writes the files it owns. */
    Path projectDir();

    /**
     * This plugin's own folder of the open project's resources, where the images and other assets its values name
     * live: {@code src/main/resources/plugins/<group>/<artifact>/} (2026-10-30), so two plugins never write the same
     * file. The folder is named as the plugin's holders' package: its groupId and its artifactId, each lower case
     * with {@code _} for any mark a Java name cannot hold ({@code com_github_botmakerdev/botmaker_sdk}).
     *
     * <p>An editor that lets the user pick an image writes the file here and puts its path <em>under this
     * folder</em> in the slot, {@code images/ore.png}; the plugin's runtime finds it on the bot's classpath as
     * {@code plugins/<group>/<artifact>/images/ore.png}, the same way on every machine.
     */
    Path resourcesDir();

    /**
     * This plugin's own state folder of the open bot, {@link PluginState#folder}: what this machine learned about
     * the bot, kept for the next run — created on first ask. The host keeps it out of every commit, publish,
     * {@code .bmk} and Versions snapshot, keeps it when a run copy moves to a newer tag, and deletes it with the
     * bot; what is in it is the plugin's.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host with no bot open — the {@code botmaker} CLI's
     * validator — answers empty.
     */
    default Optional<Path> stateDir() {
        return Optional.empty();
    }

    /**
     * Whether a user interface is attached to this host now, so a window or dialog a plugin opens is seen by
     * someone.
     *
     * <p>False in a host with no screen: the core, which hosts a project's plugins beside Studio's copy of them and
     * shows nothing itself, or serves a bot to the terminal or to an assistant. There {@link #dialogs()} answers {@link Dialogs#NONE} (every chooser cancelled, no dialog shown) and
     * {@link #theme()} {@link Theme#PLAIN}, rather than throwing, so a plugin that asks for a chooser without checking
     * gets the cancel case it already handles. A window the plugin builds itself still needs JavaFX running, which
     * such a host may not have. A plugin with something to show checks this first and says it in its
     * {@link #log()} or {@link #status} instead.
     *
     * <p>{@code default} true, added after 0.14.0 so the contract stays compatible: every host before it had a user
     * interface.
     */
    default boolean hasUi() {
        return true;
    }

    /** Applying the host's current look to a window, dialog or scene a plugin creates; {@link Theme#PLAIN} without a UI. */
    Theme theme();

    /**
     * Native file and directory choosers, and the window a plugin's own dialog should be owned by; {@link Dialogs#NONE}
     * without a UI ({@link #hasUi()}).
     */
    Dialogs dialogs();

    /**
     * The open project's bot as a running process — start, stop, its pid, and what it reports.
     *
     * <p>Host-only for the plainest reason on this interface: the host compiled the project, holds its
     * resolved classpath and owns the process. See {@link Runs} for why telemetry crosses as text.
     *
     * <p>{@code default} rather than abstract, and {@link Runs#NONE} rather than {@code null}: a host that
     * does not run bots — the {@code botmaker} CLI's validator, a test harness — answers honestly without
     * implementing anything, and a plugin never has to ask whether running is supported.
     */
    default Runs runs() {
        return Runs.NONE;
    }

    /**
     * This plugin's own values, as they are written in the bot's Java — see {@link PluginValues}.
     *
     * <p>Host-only on every part of it: the source tree, the
     * open buffers, the syntax tree, the history snapshot and the file roles are the editor's, while what
     * the value <em>means</em> is the plugin's. Nothing in these signatures names a plugin's concept — an id
     * is a string the plugin itself chose — so the rule <em>capabilities, never vocabularies</em> holds.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host with no source tree behind it — the
     * {@code botmaker} CLI's validator — answers honestly without implementing anything, and a plugin's save
     * path never asks whether writing is supported.
     */
    default PluginValues pluginValues() {
        return PluginValues.NONE;
    }

    /**
     * This plugin's values of this PC, every bot's — the values whose holder enum is a
     * {@link com.botmaker.plugin.api.managed.HostHolder} — read and written as {@link #pluginValues()} reads and writes
     * a bot's: the same calls, the same {@link com.botmaker.plugin.api.source.ManagedValue} constants, and the same
     * {@code ValueContext} from {@code open}, so your editors and pickers work on them unchanged.
     *
     * <p>Host-only because the folder is: the host keeps one class per host holder in the plugin's folder of this
     * PC, writes it through its own grammar, checks each write compiles against your plugin, and keeps the folder
     * out of every bot. It is served with or without a bot open. A value that is not {@code onHost()} is refused
     * here, as a host value is by {@link #pluginValues()}. No bot run reads these values.
     *
     * <p><b>One version serves them: the newest of your plugin on this PC.</b> A bot that pins an older version
     * still gets them, through that newest version's code, so the folder only ever moves forward, the way your
     * {@code @Migrate} rules and the two-release rule keep safe. A value your older window cannot hold is shown to
     * it read-only.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host that keeps no such folder answers
     * {@link PluginValues#NONE}.
     */
    default PluginValues hostValues() {
        return PluginValues.NONE;
    }

    /**
     * The open project's undo history, the one the host's ↶/↷ walk — see {@link History}.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host with no editor answers {@link History#NONE},
     * whose batch just runs its writes, so a plugin never asks whether undo is supported.
     */
    default History history() {
        return History.NONE;
    }

    /**
     * The undo of your values of this PC, {@link #hostValues()}: the steps your window's own ↶/↷ take back and do
     * again, one per write, a {@link History#batch} one too.
     *
     * <p>Not {@link #history()}: those values are in no bot, so the editor's ↶ never takes one back, and this one
     * never takes back the bot's code. It is served with or without a bot open, and answers for the calling plugin's
     * values alone. Every host keeps it the way it keeps the values; nothing here says how.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host that keeps no such folder answers
     * {@link History#NONE}.
     */
    default History hostHistory() {
        return History.NONE;
    }

    /**
     * Says one line in the host's own status area, where it says what it is doing.
     *
     * <p>For the running commentary a long action owes its user — <em>Starting…</em>, <em>Listening on
     * …</em>, <em>Could not reach the tunnel</em>. It is the host's furniture, so it is the host's to
     * render: a plugin cannot put a line there itself, and one that opened a window of its own to say
     * <em>Starting…</em> would be answering a different question.
     *
     * <p>Not an error channel and not a dialog. Something the user must act on is a
     * {@link Dialogs modal}; this is the line they may or may not read. A host with no status area is
     * entitled to drop it, which is what the default does.
     */
    default void status(String message) {
    }

    /**
     * Where this plugin writes what happened, shown by the host under the plugin's name — see {@link PluginLog}.
     *
     * <p>Host-only because attribution is: a line a plugin prints itself reaches the host's standard error with
     * no name on it. {@code default} for the reason {@link #runs()} is: a host that keeps no log answers
     * {@link PluginLog#STANDARD_ERROR}.
     */
    default PluginLog log() {
        return PluginLog.STANDARD_ERROR;
    }
}
