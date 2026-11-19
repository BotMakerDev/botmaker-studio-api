// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.Dialogs;
import com.botmaker.plugin.api.StudioServices;
import com.botmaker.plugin.api.Theme;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevReloadTest {

    public static final class Sample {
        public static void main(String[] args) {
        }
    }

    @Test
    void aHostThatDoesNotReloadRefusesWithWords() {
        StudioServices services = new StudioServices() {
            @Override public Path projectDir() { return Path.of(""); }

            @Override public Path resourcesDir() { return Path.of(""); }

            @Override public Theme theme() { return null; }

            @Override public Dialogs dialogs() { return null; }
        };
        assertEquals(new DevReload.Refused("This host does not reload plugins."), services.devReload(RunEntry.of("Keep exploring", Sample::main)));
    }
}
