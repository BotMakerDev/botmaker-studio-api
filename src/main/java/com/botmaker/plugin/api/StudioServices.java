package com.botmaker.plugin.api;

import com.botmaker.plugin.api.source.PluginValues;

import java.nio.file.Path;

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
 * <p><b>The rule this replaced, and why.</b> Until 2026-08-27 the test was "a real editor needed it", and it
 * grew an {@code Assets} service (the project's named pictures), a {@code Capture.SourceChoice} vocabulary and
 * a {@code Frame}/{@code Sample} pair — every one of them a concept belonging to the SDK's <em>own</em> API:
 * a named picture is {@code ImageTemplate}'s, a capture source is {@code CaptureSource}'s, a launcher is
 * {@code Game}'s. Putting them here let plugin #1 reach through the contract for its own vocabulary, which no
 * second plugin could do — the back door this module exists to close. They were deleted rather than
 * generalised, because generalising them would only have moved the same privilege behind a wider name.
 */
public interface StudioServices {

    /** The root of the open project — where a plugin reads and writes the files it owns. */
    Path projectDir();

    /**
     * The open project's resources directory, where images and other assets a bot loads by name live.
     *
     * <p>An editor that lets the user pick an image writes the file here and puts its <em>name</em> in the
     * slot, so the bot resolves it at runtime the same way on every machine.
     */
    Path resourcesDir();

    /** Applying the host's current look to a window, dialog or scene a plugin creates. */
    Theme theme();

    // capture() was here until 2026-08-31, serving a Capture with selectRegion, pickPoint, sampleColor,
    // grabFrame and toFxImage. Its defence was that only a host can put a surface over its own windows —
    // true, and not the point. A full-screen overlay over a running game, asking the user to point at
    // something in it, is about what a bot sees from end to end, and the editor's part in it was only ever
    // that the editor was written first. So a plugin draws its own, over pixels it grabs itself through
    // botmaker-shared, and the toolkit's screen-pick widgets take theirs from ScreenPicks. The last member,
    // grabFrame, was capture-target vocabulary written so as not to say the word: see Capture's own history
    // in the deleted file, and the 2026-08-27 Assets/chooseSource reversal recorded above.

    /** Native file and directory choosers, and the window a plugin's own dialog should be owned by. */
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
     * Find and repoint a token sequence across the bot's own Java sources.
     *
     * <p>Host-only on every count — the open buffers, the walk over what the bot owns, the review mark and
     * the history snapshot are all editor machinery — and it names no plugin's concept, which is what
     * separates it from the {@code Assets} service deleted above. A plugin says <em>replace this with
     * that</em>; the host has no idea what either means. See {@link Sources} for why the needle is a token
     * sequence rather than a regex.
     *
     * <p>{@code default} for the reason {@link #runs()} is: a host with no editor behind it — the
     * {@code botmaker} CLI's validator — answers honestly without implementing anything, and a plugin's
     * rename path finds nothing rather than having to ask whether rewriting is supported.
     */
    default Sources sources() {
        return Sources.NONE;
    }

    /**
     * This plugin's own values, as they are written in the bot's Java — see {@link PluginValues}.
     *
     * <p>Host-only on every part of it, and for the same reasons as {@link Sources}: the source tree, the
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
}
