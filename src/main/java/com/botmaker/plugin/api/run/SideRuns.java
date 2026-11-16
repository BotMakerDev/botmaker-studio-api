// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Running a bot that is not the open project's: one a plugin keeps a copy of (a run copy of a catalog bot, a local
 * checkout), started as its own process beside whatever bot is open, or with none open.
 *
 * <p>Host-only for the reason {@link com.botmaker.plugin.api.Runs} is: the host compiles bots, resolves their
 * classpath and keeps every run's log and report the same way. A plugin that started a JVM itself would be a second
 * launcher none of that sees.
 *
 * <p>Each start compiles the bot when its sources changed since the last one, then runs its {@code main}. A run property
 * is one {@code -D} (what {@link RunProperty} names); an environment variable reaches the run's process only, never its
 * command line, which other processes on the computer may read: anything a run must not show goes there.
 */
public interface SideRuns {

    /**
     * Starts the bot in {@code bot}, a folder holding its {@code pom.xml} or an unpacked {@code .bmk}.
     *
     * @param properties  each run property's name and its text, as {@link RunProperty#write} gives it
     * @param environment variables added to the run's own environment
     * @return the run, already started
     * @throws IOException when the folder holds no bot, its classpath does not resolve or it does not compile; the
     *                     message says which, in the user's words
     */
    SideRun start(Path bot, Map<String, String> properties, Map<String, String> environment) throws IOException;

    /**
     * The run copy of the catalog bot {@code repo} ({@code owner/repo}), a folder to {@link #start}: its newest approved
     * release, the {@code .bmk} checked against the catalog's sha256 and its jars as {@code Get} checks them, kept once;
     * offline, the copy kept the last time. For a plugin that runs a bot of its own, such as the SDK's Google sign-in,
     * without the user getting it from the bot list. Only a bot the catalog ranks official is got this way: no trust
     * sheet is shown.
     *
     * @throws IOException when there is none to run, the message saying why in the user's words: not listed, not
     *                     official, not released yet, or offline with no copy kept
     */
    default Path catalogCopy(String repo) throws IOException {
        throw new IOException("This BotMaker gets no bot from the catalog.");
    }

    /** A host that runs no bot but the open one: every start refuses. */
    SideRuns NONE = (bot, properties, environment) -> {
        throw new IOException("This BotMaker runs no bot beside the open one.");
    };
}
