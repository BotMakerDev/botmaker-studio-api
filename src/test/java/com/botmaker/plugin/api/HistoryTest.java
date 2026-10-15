package com.botmaker.plugin.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class HistoryTest {

    @Test
    void aHostWithNoEditorHasNothingToUndoAndABatchJustRuns() {
        StudioServices services = new StudioServices() {
            @Override public Path projectDir() { return Path.of(""); }

            @Override public Path resourcesDir() { return Path.of(""); }

            @Override public Theme theme() { return null; }

            @Override public Dialogs dialogs() { return null; }
        };
        History history = services.history();
        assertSame(History.NONE, history);
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals("", history.undoLabel());
        assertEquals("", history.redoLabel());
        assertDoesNotThrow(history::undo);
        assertDoesNotThrow(history::redo);

        int[] ran = {0};
        history.batch("Move 3 cards", () -> ran[0]++);
        assertEquals(1, ran[0], "a batch on a host with no editor still writes");
        AutoCloseable handle = history.onChanged(() -> {
            throw new AssertionError("NONE fired");
        });
        assertDoesNotThrow(handle::close);
        assertDoesNotThrow(() -> history.batch("Nothing", null));
    }
}
