package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The versioning rule of the whole platform, in one test: a plugin compiled against an earlier contract
 * implements nothing but its id, and the host reads "nothing to contribute" rather than catching an
 * {@code AbstractMethodError}.
 *
 * <p>Held {@code ParameterRow}'s tests beside it until 2026-09-28 as {@code ParameterDataTest}; the row moved
 * to Studio, which is the only thing that ever built or read one.
 */
class StudioPluginDefaultsTest {

    @Test
    void aPluginThatImplementsOnlyItsIdContributesNothing() {
        StudioPlugin older = () -> "com.example.older";

        assertEquals("com.example.older", older.displayName());
        assertEquals(PaletteCatalog.empty().facades(), older.catalog().facades());
        assertEquals(List.of(), older.slotEditors());
        assertEquals(List.of(), older.toolbarItems());
        assertEquals(List.of(), older.types());
        assertEquals(List.of(), older.componentTypes());
        assertEquals(List.of(), older.managedValues());
        assertEquals(List.of(), older.recordedValues());
        // The lifecycle half is a default too, and both ends of it: a host that tells every plugin which
        // project it has must not need to know which of them have heard of the idea.
        older.projectOpened(null);
        older.projectClosing();
    }
}
