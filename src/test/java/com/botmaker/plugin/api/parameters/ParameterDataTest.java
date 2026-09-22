package com.botmaker.plugin.api.parameters;

import com.botmaker.plugin.api.StudioPlugin;
import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueForm;
import com.botmaker.plugin.api.value.ValueType;
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

    private static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID).label("Text").source("String")
            .build();
    private static final ValueType WHOLE = ValueType.of("WHOLE_NUMBER").label("Whole number").source("int")
            .boxed("Integer").primitive().bounded().build();

    private static ParameterRow.Builder row(String name) {
        return ParameterRow.named(name, ValueForm.of(TEXT));
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
        // The lifecycle half is a default too, and both ends of it: a host that tells every plugin which
        // project it has must not need to know which of them have heard of the idea.
        older.projectOpened(null);
        older.projectClosing();
    }

    // ---- ParameterRow ---------------------------------------------------------------------------------

    @Test
    void aRowNeedsAName() {
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named("  ", ValueForm.of(TEXT)));
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named(null, ValueForm.of(TEXT)));
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
        assertEquals(Range.NONE, bare.bounds());
    }

    /** A row with no type at all is an unknown type rather than a {@code null} the host would trip over. */
    @Test
    void aRowWithNoTypeHoldsAnUnknownOne() {
        ParameterRow untyped = ParameterRow.named("legacy", null).value("kept").build();

        assertFalse(untyped.form().known());
        assertEquals("kept", untyped.value());
    }

    /** A row carries its whole type tree, including the ones the deleted choice pair could not say. */
    @Test
    void aRowCarriesItsWholeForm() {
        ParameterRow listed = ParameterRow.named("keys", ValueForm.listOf(ValueForm.of(TEXT))).build();

        assertEquals(ValueForm.listOf(ValueForm.of(TEXT)), listed.form());
        assertEquals(TEXT, listed.form().leaf(), "the leaf a declared set and a range are asked of");

        ParameterRow mapped = ParameterRow.named("retries",
                ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(WHOLE))).build();

        assertEquals("java.util.Map<String, Integer>", mapped.form().sourceName());
        assertTrue(mapped.form().known(), "and it is readable, where a choice had to call it unknown");
    }

    @Test
    void withValueKeepsEverythingElseAndEqualityIsByComponent() {
        ParameterRow declared = row("rest")
                .value("3s").description("How long to wait").category("Timing")
                .visibility(Visibility.EDITOR_ONLY).options(List.of("3s", "5s")).bounds(new Range("1s", "9s"))
                .build();

        ParameterRow edited = declared.withValue("5s");

        assertEquals("5s", edited.value());
        assertNotEquals(declared, edited);
        assertEquals(declared, edited.withValue("3s"));
        assertEquals(declared.hashCode(), edited.withValue("3s").hashCode());
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
        ParameterRow keys = ParameterRow.named("hotkeys", ValueForm.listOf(ValueForm.of(TEXT)))
                .value("java.util.List.of(\"F1\", \"F2\")").build();

        assertEquals("java.util.List<String>", keys.form().sourceName());
        assertEquals("java.util.List.of(\"F1\", \"F2\", \"F3\")",
                keys.withValue("java.util.List.of(\"F1\", \"F2\", \"F3\")").value());
    }

    // The declaration half stood here until 2026-09-17: ParameterDeclaration, and the three shapes it came
    // in. The storage half followed it on 2026-09-22: ParameterGroup, ParameterEdit, parameters(String),
    // parameterRows(String) and parameterEdited(ParameterEdit). Nothing ever declared a group — the SDK's
    // was the only one and it declared no rows — so what the host read back was a pre-2026-09-17 project's
    // JSON and nothing else. A parameter is a @Param field in the bot's own Java, read and written off the
    // syntax tree. ParameterRow stays because it is still the window's row shape.
}
