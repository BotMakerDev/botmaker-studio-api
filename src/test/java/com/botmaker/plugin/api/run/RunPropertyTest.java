package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.Runs;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunPropertyTest {

    private static final RunProperty<Integer> COUNT = RunProperty.of("com.example.count", Integer::valueOf,
            String::valueOf);

    @Test
    void aPropertyReadsAndWritesItsValueAsText() {
        assertEquals(Optional.of(3), COUNT.read("3"));
        assertTrue(COUNT.read("three").isEmpty(), "text the parser throws on reads as nothing");
        assertTrue(COUNT.read(" ").isEmpty());
        assertEquals("3", COUNT.write(3));
        assertNull(COUNT.write(null), "no value clears the property");
        assertEquals(Optional.of("x"), RunProperty.text("com.example.mode").read("x"));
    }

    @Test
    void aPropertyNeedsAName() {
        assertThrows(IllegalArgumentException.class, () -> RunProperty.text(" "));
    }

    @Test
    void runsKeepATypedPropertyThroughTheHostsText() {
        Map<String, String> kept = new HashMap<>();
        Runs host = new Runs() {
            @Override public void start() { }
            @Override public void stop() { }
            @Override public boolean isRunning() { return false; }
            @Override public OptionalLong pid() { return OptionalLong.empty(); }
            @Override public AutoCloseable onStateChanged(Consumer<Boolean> listener) { return () -> { }; }
            @Override public AutoCloseable onTelemetry(Consumer<byte[]> listener) { return () -> { }; }
            @Override public <T> Optional<T> get(RunProperty<T> property) {
                return property.read(kept.get(property.name()));
            }
            @Override public <T> void set(RunProperty<T> property, T value) {
                kept.put(property.name(), property.write(value));
            }
        };

        host.set(COUNT, 7);
        assertEquals(Map.of("com.example.count", "7"), kept);
        assertEquals(Optional.of(7), host.get(COUNT));
        assertTrue(Runs.NONE.get(COUNT).isEmpty(), "a host that runs no bots keeps nothing");
    }
}
