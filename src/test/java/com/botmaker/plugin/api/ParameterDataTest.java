package com.botmaker.plugin.api;

import com.botmaker.plugin.api.value.Range;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueChoice;
import com.botmaker.plugin.api.value.ValueShape;
import com.botmaker.plugin.api.value.ValueType;
import com.botmaker.plugin.api.value.Visibility;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        return ParameterRow.named(name, ValueChoice.of(TEXT));
    }

    // ---- a plugin that has never heard of this surface -------------------------------------------------

    /**
     * The versioning rule of the whole platform, in one assertion: a plugin compiled against an earlier
     * contract implements neither method, and the host reads "nothing to contribute" rather than catching an
     * {@code AbstractMethodError}.
     */
    @Test
    void aPluginThatImplementsOnlyItsIdContributesNoParameterData() {
        StudioPlugin older = () -> "com.example.older";

        assertEquals(List.of(), older.parameterRows(ParameterGroup.DEFAULT_ID));
        assertEquals(List.of(), older.parameterRows("com.example.older/settings"));
        assertEquals(Optional.empty(), older.parameterEdited(ParameterEdit.of("", "rest", "3s")));
        // The lifecycle half is a default too, and both ends of it: a host that tells every plugin which
        // project it has must not need to know which of them have heard of the idea.
        older.projectOpened(null);
        older.projectClosing();
    }

    /** A plugin serving rows answers the edit with the row it stored — the shape the window renders back. */
    @Test
    void theAnswerToAnEditIsTheRowAsStored() {
        StudioPlugin plugin = new StudioPlugin() {
            @Override
            public String id() {
                return "com.example.clamping";
            }

            @Override
            public List<ParameterRow> parameterRows(String groupId) {
                return List.of(ParameterRow.named("retries", ValueChoice.of(WHOLE))
                        .value("2").bounds(new Range("1", "5")).build());
            }

            @Override
            public Optional<ParameterRow> parameterEdited(ParameterEdit edit) {
                if (!"retries".equals(edit.name())) return Optional.empty();
                // The clamp is the owning plugin's, and reporting it is what the answer is for.
                return Optional.of(parameterRows(edit.groupId()).getFirst().withValue("5"));
            }
        };

        Optional<ParameterRow> stored = plugin.parameterEdited(ParameterEdit.of("", "retries", "900"));
        assertEquals("5", stored.orElseThrow().singleValue());
        assertEquals(Optional.empty(), plugin.parameterEdited(ParameterEdit.of("", "unknown", "x")));
    }

    // ---- ParameterRow ---------------------------------------------------------------------------------

    @Test
    void aRowNeedsAName() {
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named("  ", ValueChoice.of(TEXT)));
        assertThrows(IllegalArgumentException.class, () -> ParameterRow.named(null, ValueChoice.of(TEXT)));
    }

    /** Every default is the reading that keeps a project open: a value, a visibility and a range all absent. */
    @Test
    void everyDefaultIsTheSafeReading() {
        ParameterRow bare = row("rest").build();

        assertEquals(List.of(), bare.value());
        assertEquals("", bare.singleValue());
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

        assertFalse(untyped.type().type().known());
        assertEquals("kept", untyped.singleValue());
    }

    @Test
    void theValueIsCopiedRatherThanShared() {
        List<String> live = new ArrayList<>(List.of("a"));
        ParameterRow listed = ParameterRow.named("keys", ValueChoice.listOf(TEXT)).value(live).build();

        live.add("b");

        assertEquals(List.of("a"), listed.value());
        assertThrows(UnsupportedOperationException.class, () -> listed.value().add("c"));
    }

    @Test
    void withValueKeepsEverythingElseAndEqualityIsByComponent() {
        ParameterRow declared = row("rest")
                .value("3s").description("How long to wait").category("Timing")
                .visibility(Visibility.EDITOR_ONLY).options(List.of("3s", "5s")).bounds(new Range("1s", "9s"))
                .build();

        ParameterRow edited = declared.withValue("5s");

        assertEquals("5s", edited.singleValue());
        assertNotEquals(declared, edited);
        assertEquals(declared, edited.withValue("3s"));
        assertEquals(declared.hashCode(), edited.withValue("3s").hashCode());
        assertEquals(declared, declared.toBuilder().build());
        assertEquals("How long to wait", edited.displayLabel());
        assertEquals("Timing", edited.categoryOrGeneral());
    }

    /** A row's category is one of the owning group's, compared the way the group compares it. */
    @Test
    void aCategoryIsOneTheOwningGroupDeclares() {
        ParameterGroup group = ParameterGroup.of("", "Parameters", List.of("Timing", "Vision"));
        ParameterRow filed = row("rest").category("  timing ").build();

        assertEquals("timing", filed.category());
        assertTrue(group.declares(filed.category()));
        assertFalse(group.declares(row("rest").build().category()));
    }

    // ---- ParameterEdit --------------------------------------------------------------------------------

    @Test
    void anEditNamesARowAndCarriesText() {
        ParameterEdit edit = new ParameterEdit(null, "  rest ", null);

        assertEquals(ParameterGroup.DEFAULT_ID, edit.groupId());
        assertEquals("rest", edit.name());
        assertEquals(List.of(), edit.value());
        assertEquals("", edit.singleValue());
        assertThrows(IllegalArgumentException.class, () -> ParameterEdit.of("", " ", "x"));
    }

    @Test
    void anEditKnowsWhetherItChangesTheRowItNames() {
        ParameterRow held = row("rest").value("3s").build();

        assertTrue(ParameterEdit.of("", "rest", "5s").changes(held));
        assertFalse(ParameterEdit.of("", "rest", "3s").changes(held));
        assertFalse(ParameterEdit.of("", "other", "5s").changes(held));
        assertFalse(ParameterEdit.of("", "rest", "5s").changes(null));
    }

    /** A list-shaped row's value crosses item by item, and an edit to it does too. */
    @Test
    void aListShapedRowCrossesOneEntryPerItem() {
        ParameterRow keys = ParameterRow.named("hotkeys", new ValueChoice(TEXT, ValueShape.OPEN_LIST))
                .value(List.of("F1", "F2")).build();
        ParameterEdit edit = new ParameterEdit("", "hotkeys", List.of("F1", "F2", "F3"));

        assertTrue(keys.type().isList());
        assertTrue(edit.changes(keys));
        assertEquals(List.of("F1", "F2", "F3"), keys.withValue(edit.value()).value());
    }

    // ---- the declaration half ---------------------------------------------------------------------------

    /** The three shapes a declaration comes in, and what each says without any verb attached to it. */
    @Test
    void aDeclarationIsAnAddARemovalOrARowAsWanted() {
        ParameterRow wanted = row("rest").value("5s").build();

        ParameterDeclaration added = ParameterDeclaration.added("discord", wanted);
        assertTrue(added.isNew());
        assertFalse(added.isRemoval());
        assertEquals("", added.name(), "a row that does not exist yet has no handle");

        ParameterDeclaration removed = ParameterDeclaration.removed("discord", "rest");
        assertTrue(removed.isRemoval());
        assertEquals("rest", removed.name());

        ParameterDeclaration held = ParameterDeclaration.of("discord", wanted);
        assertFalse(held.isNew());
        assertFalse(held.isRemoval());
        assertEquals("rest", held.name());
    }

    /** A rename is the one shape where the two names differ — which is why both are carried. */
    @Test
    void aRenameIsADeclarationWhoseTwoNamesDiffer() {
        ParameterDeclaration rename =
                new ParameterDeclaration("", "rest", row("restBetweenRuns").build());

        assertEquals("rest", rename.name());
        assertEquals("restBetweenRuns", rename.wanted().name());
    }

    @Test
    void aDeclarationThatNamesNoRowAndWantsNoneSaysNothing() {
        assertThrows(IllegalArgumentException.class, () -> new ParameterDeclaration("", " ", null));
        assertThrows(IllegalArgumentException.class, () -> ParameterDeclaration.added("", null));
        // A blank group is the default section, exactly as it is for an edit.
        assertEquals(ParameterGroup.DEFAULT_ID,
                ParameterDeclaration.removed(null, "rest").groupId());
    }
}
