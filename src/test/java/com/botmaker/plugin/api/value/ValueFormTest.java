package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type tree: how it spells itself, how deep it goes, and which single type its values are of.
 *
 * <p>What is checked here is what the deleted {@code ValueChoice} could not say — two type arguments, two
 * levels, a class the bot declares — and the one question that survived it, which is the leaf a declared set
 * and a declared range are asked of. {@code docs/refactor/32-generic-values.md} is the specification.
 */
class ValueFormTest {

    private static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID).label("Text").source("String")
            .build();
    private static final ValueType COUNT = ValueType.of("WHOLE_NUMBER").label("Whole number").source("int")
            .boxed("Integer").primitive().build();
    private static final ValueType POINT = ValueType.of("POINT").label("Point").source("Point")
            .importing("com.botmaker.sdk.api.geometry.Point").build();

    @Test
    void aLeafSpellsItsType() {
        assertEquals("String", ValueForm.of(TEXT).sourceName());
        assertEquals(0, ValueForm.of(TEXT).depth());
    }

    @Test
    void aTypeArgumentIsBoxedAndABareDeclarationIsNot() {
        // The one thing a type argument needs from its leaf that a field of that type does not.
        assertEquals("int", ValueForm.of(COUNT).sourceName());
        assertEquals("java.util.List<Integer>", ValueForm.listOf(ValueForm.of(COUNT)).sourceName());
    }

    @Test
    void aMapIsSomethingTheDeletedChoicePairCouldNotSay() {
        ValueForm form = ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(POINT));
        // Simple names inside the brackets: the import is the form's separate answer, not part of its
        // spelling.
        assertEquals("java.util.Map<String, Point>", form.sourceName());
        assertEquals(1, form.depth());
    }

    @Test
    void nestingIsUnbounded() {
        ValueForm form = ValueForm.mapOf(ValueForm.of(TEXT),
                ValueForm.listOf(ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(COUNT))));
        assertEquals("java.util.Map<String, java.util.List<java.util.Map<String, Integer>>>",
                form.sourceName());
        assertEquals(3, form.depth());
    }

    @Test
    void aWrongNumberOfArgumentsIsCorrectedRatherThanThrown() {
        // Total like the rest of the vocabulary: a caller's mistake produces a form that displays, not an
        // exception that fails an open.
        ValueForm.Of map = new ValueForm.Of(ValueContainer.MAP, List.of(ValueForm.of(TEXT)));
        assertEquals(2, map.arguments().size());
        assertFalse(map.known());

        ValueForm.Of list = new ValueForm.Of(ValueContainer.LIST,
                List.of(ValueForm.of(TEXT), ValueForm.of(COUNT)));
        assertEquals(1, list.arguments().size());
    }

    @Test
    void oneUnknownArgumentMakesTheWholeFormUnknown() {
        ValueForm form = ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(ValueType.unknown("discord.Channel")));
        assertFalse(form.known());
        assertTrue(ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(POINT)).known());
    }

    @Test
    void aDeclaredClassNeedsNoArguments() {
        ValueForm.Declared box = new ValueForm.Declared("com.mybot.Box", List.of());
        assertEquals("com.mybot.Box", box.sourceName());
        assertTrue(box.known());
        assertEquals("com.mybot.Box<String>",
                new ValueForm.Declared("com.mybot.Box", List.of(ValueForm.of(TEXT))).sourceName());
    }

    @Test
    void theLeafIsTheTypeTheUserTypesValuesOf() {
        assertEquals(TEXT, ValueForm.of(TEXT).leaf());
        assertEquals(TEXT, ValueForm.listOf(ValueForm.of(TEXT)).leaf());
        // A map's values are what its cell types, so the value argument is the leaf and the key is not.
        assertEquals(COUNT, ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(COUNT)).leaf());
    }

    @Test
    void aContainerOfContainersHasNoLeafOfItsOwn() {
        // The two questions a leaf answers — what may this be, between which numbers — are meaningless for a
        // tree with several, so there is no answer rather than an arbitrary one.
        assertNull(ValueForm.listOf(ValueForm.listOf(ValueForm.of(TEXT))).leaf());
        assertNull(new ValueForm.Declared("com.mybot.Box", List.of(ValueForm.of(TEXT))).leaf());
    }
}
