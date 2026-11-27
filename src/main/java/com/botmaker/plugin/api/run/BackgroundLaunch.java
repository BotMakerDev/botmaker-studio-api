// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.util.Objects;

/**
 * What a plugin's bot wants started in a private display when the user asks the host's Remote Pilot for Background
 * mode (2026-11-27, pilot in core): the host brings the display up, launches the game in it and routes the phone
 * through it; the plugin only says what to launch, for it alone knows the bot's launch target and settings. Given by
 * {@link RunView#background()}.
 *
 * @param spec    the launch target, as text the host's launch layer reads ({@code kind:token}); never blank
 * @param backend the display backend to use ({@code "xephyr"}, {@code "gamescope"}, …), or empty for the host's own
 *                choice for that target
 */
public record BackgroundLaunch(String spec, String backend) {

    public BackgroundLaunch {
        Objects.requireNonNull(spec, "spec");
        if (spec.isBlank()) throw new IllegalArgumentException("a background launch names what to launch");
        backend = backend == null ? "" : backend;
    }
}
