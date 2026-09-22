package com.botmaker.plugin.api.parameters;

import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.api.value.Visibility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parameter-data surface, checked for what holds before any host or plugin has implemented it.
 *
 * <p>What is worth asserting here is not that a getter returns what was set. It is the three properties the
 * surface exists for: that a plugin which has never heard of it still loads and contributes nothing, that
 * every default is the safe reading rather than a crash, and that a row survives being copied — the host
 * compares what a plugin answered against what it sent, so value equality is load-bearing rather than
 * cosmetic.
 */
class ParameterDataTest {

    private static ParameterRow.Builder row(String name) {
        return ParameterRow.named(name, "String");
    }

    // ---- a plugin that has never heard of this surface -------------------------------------------------

    /**
     * The versioning rule of the whole platform, in one assertion: a plugin compiled against an earlier
     * contract implements nothing but its id, and the host reads "nothing to contribute" rather than
     * catching an {@code AbstractMethodError}.
     */
    @Test
    void aPluginThatImplementsOnlyItsIdContributesNothing() {
        StudioPlugin older = () -> "com.example.older";

        assertEquals(List.of(), older.slotEditors());
        assertEquals(List.of(), older.toolbarItems());
        assertEquals(List.of(), older.types());
        // The lifecycle half is a default too, and both ends of it: a host that tells every plugin which
        // project it has must not need to know which of them have heard of the idea.
        older.projectOpened(null);
        older.projectClosing();
    }

    // ---- ParameterRow ---------------------------------------------------------------------------------

    @Test
    void aRowNeedsAName() {
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named("  ", "String"));
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named(null, "String"));
    }

    /** Every default is the reading that keeps a project open: a value, a visibility and a range all absent. */
    @Test
    void everyDefaultIsTheSafeReading() {
        ParameterRow bare = row("rest").build();

        assertEquals("", bare.value());
        assertEquals("", bare.description());
        assertEquals("rest", bare.displayLabel());
        assertEquals("", bare.category());
        assertEquals(ParameterRow.GENERAL, bare.categoryOrGeneral());
        assertEquals(Visibility.PUBLIC, bare.visibility());
        assertTrue(bare.isPublic());
        assertEquals(List.of(), bare.options());
        assertFalse(bare.isBounded());
        assertEquals(Double.NEGATIVE_INFINITY, bare.min());
        assertEquals(Double.POSITIVE_INFINITY, bare.max());
    }

    /** A row with no type at all is blank rather than a {@code null} the host would trip over. */
    @Test
    void aRowWithNoTypeHoldsABlankOne() {
        ParameterRow untyped = ParameterRow.named("legacy", null).value("kept").build();

        assertEquals("", untyped.typeName());
        assertEquals("kept", untyped.value());
    }

    /**
     * A row carries the type exactly as the field writes it, including the ones the deleted choice pair
     * could not say at all.
     */
    @Test
    void aRowCarriesItsTypeAsWritten() {
        assertEquals("java.util.List<String>", ParameterRow.named("keys", "java.util.List<String>")
                .build().typeName());
        assertEquals("Map<String, List<Point>>", ParameterRow.named("retries", "Map<String, List<Point>>")
                .build().typeName());
    }

    /** One end of a range is a sentence a person says, and both ends are independent. */
    @Test
    void oneEndOfARangeIsEnough() {
        ParameterRow atMost = row("attempts").bounds(Double.NEGATIVE_INFINITY, 10).build();

        assertTrue(atMost.isBounded());
        assertEquals(10, atMost.max());
        assertEquals(Double.NEGATIVE_INFINITY, atMost.min());
    }

    @Test
    void withValueKeepsEverythingElseAndEqualityIsByComponent() {
        ParameterRow declared = ParameterRow.named("rest", "java.time.Duration")
                .value("java.time.Duration.ofSeconds(3)").description("How long to wait").category("Timing")
                .visibility(Visibility.EDITOR_ONLY)
                .options(List.of("java.time.Duration.ofSeconds(3)", "java.time.Duration.ofSeconds(5)"))
                .bounds(1000, 9000)
                .build();

        ParameterRow edited = declared.withValue("java.time.Duration.ofSeconds(5)");

        assertEquals("java.time.Duration.ofSeconds(5)", edited.value());
        assertNotEquals(declared, edited);
        assertEquals(declared, edited.withValue("java.time.Duration.ofSeconds(3)"));
        assertEquals(declared.hashCode(), edited.withValue("java.time.Duration.ofSeconds(3)").hashCode());
        assertEquals(declared, declared.toBuilder().build());
        assertEquals("How long to wait", edited.displayLabel());
        assertEquals("Timing", edited.categoryOrGeneral());
    }

    /** A category is free text the window files a row under — trimmed, and never a vocabulary. */
    @Test
    void aCategoryIsFreeTextTheWindowFilesARowUnder() {
        ParameterRow filed = row("rest").category("  Timing ").build();

        assertEquals("Timing", filed.category());
        assertEquals(ParameterRow.GENERAL, row("rest").build().categoryOrGeneral());
    }

    /**
     * A composite row's value crosses as the one initialiser that produces it, however deep it goes — which
     * is the whole reason the list of wires went.
     */
    @Test
    void aCompositeValueCrossesAsOneInitializer() {
        ParameterRow keys = ParameterRow.named("hotkeys", "java.util.List<String>")
                .value("java.util.List.of(\"F1\", \"F2\")").build();

        assertEquals("java.util.List.of(\"F1\", \"F2\", \"F3\")",
                keys.withValue("java.util.List.of(\"F1\", \"F2\", \"F3\")").value());
    }

    // The declaration half stood here until 2026-09-17: ParameterDeclaration, and the three shapes it came
    // in. The storage half followed it on 2026-09-22: ParameterGroup, ParameterEdit, parameters(String),
    // parameterRows(String) and parameterEdited(ParameterEdit). Nothing ever declared a group — the SDK's
    // was the only one and it declared no rows — so what the host read back was a pre-2026-09-17 project's
    // JSON and nothing else. A parameter is a @Param field in the bot's own Java, read and written off the
    // syntax tree. ParameterRow stays because it is still the window's row shape.
    //
    // ValueForm and Range went the same day, out of the row and out of the contract. A form is how the host
    // walks Map<String, List<Point>> while writing and reading it, and nothing outside the host ever walked
    // one; a row's consumers use the spelling, which is typeName(). A Range was two strings so a codec could
    // parse "30s", and no plugin parses anything now.
}
