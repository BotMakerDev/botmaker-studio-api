package com.botmaker.plugin.api.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunEntryTest {

    public static final class Sample {
        public static void main(String[] args) {
        }

        public static void other(String[] args) {
        }
    }

    @Test
    void ofResolvesAMainToItsClass() {
        RunEntry entry = RunEntry.of(" Explore map ", Sample::main);
        assertEquals("Explore map", entry.title());
        assertEquals(Sample.class.getName(), entry.mainClass());
        assertEquals(entry, RunEntry.of("Explore map", Sample::main));
    }

    @Test
    void offBarKeepsIdentity() {
        RunEntry entry = RunEntry.of("Explore map", Sample::main);
        assertTrue(entry.onBar());
        RunEntry off = entry.offBar();
        assertFalse(off.onBar());
        assertEquals(entry, off);
        assertEquals(entry.hashCode(), off.hashCode());
        assertEquals("Explore map", off.title());
        assertEquals(Sample.class.getName(), off.mainClass());
    }

    static final class Hidden {
        public static void main(String[] args) {
        }
    }

    @Test
    void aLambdaIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RunEntry.of("Explore map", args -> { }));
    }

    @Test
    void aMethodTheJvmCannotStartIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RunEntry.of("Explore map", Sample::other));
        assertThrows(IllegalArgumentException.class, () -> RunEntry.of("Explore map", Hidden::main));
    }

    @Test
    void aBlankTitleIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RunEntry.of(" ", Sample::main));
        assertThrows(IllegalArgumentException.class, () -> RunEntry.of(null, Sample::main));
    }
}
