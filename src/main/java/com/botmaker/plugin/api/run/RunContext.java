// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

/**
 * The run a {@link RunCompanion} serves, as the host gives it: which entry runs and the services of the project it
 * runs in. The host builds it; a plugin only reads it.
 */
public interface RunContext {

    /** The entry that runs. */
    RunEntry entry();

    /** The services of the project the run is of, as {@code onProjectOpened} gave them. */
    StudioServices services();
}
