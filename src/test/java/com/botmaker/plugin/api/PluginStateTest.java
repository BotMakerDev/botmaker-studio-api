package com.botmaker.plugin.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A plugin's state folder of a bot, named the same way by the host and by the plugin's runtime. */
class PluginStateTest {

    @Test
    void aPluginsFolderIsNamedByItsCoordinate() {
        assertEquals(Path.of("bot/.botmaker/plugins/com_github_botmakerdev/botmaker_sdk"),
                PluginState.folder(Path.of("bot"), "com.github.BotMakerDev", "botmaker-sdk"));
        assertEquals("_9lives", PluginState.part("9lives"));
    }

    @Test
    void aHostWithNoBotHasNoStateFolder() {
        StudioServices none = new StudioServices() {
            @Override public Path projectDir() { return Path.of(""); }
            @Override public Path resourcesDir() { return Path.of(""); }
            @Override public Theme theme() { return null; }
            @Override public Dialogs dialogs() { return null; }
        };
        assertEquals(Optional.empty(), none.stateDir());
    }
}
