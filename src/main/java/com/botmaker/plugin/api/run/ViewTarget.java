// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.util.Objects;

/**
 * Which part of this computer a {@link RunView} shows: a closed set the host can capture and send input to, never a
 * bare string. Built with its factories.
 */
public sealed interface ViewTarget {

    /** A window, by the id the host's window list gives it, with the title it had when named (for the phone). */
    record Window(String id, String title) implements ViewTarget {
        public Window {
            Objects.requireNonNull(id, "id");
            title = title == null ? "" : title;
        }
    }

    /** An Android emulator or phone, by its adb serial. */
    record Emulator(String serial) implements ViewTarget {
        public Emulator {
            Objects.requireNonNull(serial, "serial");
        }
    }

    /** The whole desktop the host runs on. */
    record Desktop() implements ViewTarget {
    }

    /** One monitor, counted from 0 as the host lists them. */
    record Monitor(int index) implements ViewTarget {
        public Monitor {
            if (index < 0) throw new IllegalArgumentException("a monitor's index is not negative: " + index);
        }
    }

    static ViewTarget window(String id, String title) {
        return new Window(id, title);
    }

    static ViewTarget emulator(String serial) {
        return new Emulator(serial);
    }

    static ViewTarget desktop() {
        return new Desktop();
    }

    static ViewTarget monitor(int index) {
        return new Monitor(index);
    }
}
