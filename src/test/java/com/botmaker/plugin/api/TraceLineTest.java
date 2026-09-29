package com.botmaker.plugin.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A trace line is read totally, whatever a runtime sent: an unknown level is {@code UNKNOWN}, a missing part is
 * empty rather than {@code null}, and a count is at least one. A host that runs no bots delivers none.
 */
class TraceLineTest {

    @Test
    void aLevelIsReadByItsIdAndAnUnknownOneIsKept() {
        assertEquals(TraceLine.Level.DEBUG, TraceLine.Level.fromId("debug"));
        assertEquals(TraceLine.Level.WARN, TraceLine.Level.fromId(" WARN "));
        assertEquals(TraceLine.Level.UNKNOWN, TraceLine.Level.fromId("verbose"));
        assertEquals(TraceLine.Level.UNKNOWN, TraceLine.Level.fromId(""));
        assertEquals(TraceLine.Level.UNKNOWN, TraceLine.Level.fromId(null));
        for (TraceLine.Level level : TraceLine.Level.values()) {
            assertEquals(level, TraceLine.Level.fromId(level.id()), level::name);
        }
    }

    @Test
    void aLineWithMissingPartsReadsAsEmptyNeverNull() {
        TraceLine line = new TraceLine(null, null, null, null, 0, null, null, null, null, null);

        assertEquals(Instant.EPOCH, line.at());
        assertEquals(TraceLine.Level.UNKNOWN, line.level());
        assertEquals("", line.source());
        assertEquals("", line.text());
        assertEquals(1, line.count());
        assertEquals("", line.className());
        assertEquals("", line.writerClass());
        assertEquals("", line.writerMethod());
        assertEquals(OptionalInt.empty(), line.line());
        assertEquals(Optional.empty(), line.where());
    }

    @Test
    void aHostThatRunsNoBotsDeliversNoTrace() {
        AutoCloseable handle = Runs.NONE.onTrace(line -> {
            throw new AssertionError("NONE delivered " + line);
        });
        assertDoesNotThrow(handle::close);
        assertEquals("botmaker.debug", Runs.DEBUG_PROPERTY);
    }
}
