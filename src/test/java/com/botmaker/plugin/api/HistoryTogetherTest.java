// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A host that sends no writes together runs a together as its batch (2026-11-21). */
class HistoryTogetherTest {

    @Test
    void noneRunsTheWritesOnceAndAnswersNothing() {
        int[] ran = {0};

        assertTrue(History.NONE.together("Tidy the map", () -> ran[0]++).isEmpty());
        assertEquals(1, ran[0]);
    }

    @Test
    void aHostWithBatchOnlyRunsTheWritesAsItsBatch() {
        List<String> batches = new ArrayList<>();
        int[] ran = {0};
        History host = new History() {
            @Override public boolean canUndo() { return false; }

            @Override public boolean canRedo() { return false; }

            @Override public String undoLabel() { return ""; }

            @Override public String redoLabel() { return ""; }

            @Override public void undo() {}

            @Override public void redo() {}

            @Override public AutoCloseable onChanged(Runnable listener) { return () -> {}; }

            @Override
            public void batch(String label, Runnable writes) {
                batches.add(label);
                writes.run();
            }
        };

        assertTrue(host.together("Tidy the map", () -> ran[0]++).isEmpty());
        assertEquals(List.of("Tidy the map"), batches);
        assertEquals(1, ran[0]);
    }
}
