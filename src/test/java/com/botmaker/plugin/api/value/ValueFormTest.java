package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type tree, and the bridge that lets it arrive one caller at a time.
 *
 * <p>What is checked here is what {@link ValueChoice} could not say and what must survive the trip back to
 * it. The bridge is lossy in exactly one direction and the four-constant table of
 * {@code docs/refactor/32-generic-values.md} is the specification: {@code ONE} and {@code ONE_OF} are the
 * form itself, {@code ANY_OF} and {@code OPEN_LIST} are a list of it, and whether the author wrote the set
 * down is a question the declaration answers rather than the type.
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
    void aMapIsSomethingValueChoiceCouldNotSay() {
        ValueForm form = ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(POINT));
        // Simple names inside the brackets, exactly as ValueChoice.sourceName already writes them: the
        // import is the form's separate answer, not part of its spelling.
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
    void everyShapeRoundTripsThroughTheBridge() {
        for (ValueShape shape : ValueShape.values()) {
            ValueChoice choice = new ValueChoice(TEXT, shape);
            ValueChoice back = choice.form().asChoice(choice.hasOptions());
            assertEquals(choice, back, shape.name());
        }
    }

    @Test
    void theBridgeCarriesTheSourceSpellingUnchanged() {
        for (ValueShape shape : ValueShape.values()) {
            ValueChoice choice = new ValueChoice(TEXT, shape);
            assertEquals(choice.sourceName(), choice.form().sourceName(), shape.name());
        }
    }

    @Test
    void whatAChoiceCannotSayBecomesUnknownRatherThanWrong() {
        // A map has no ValueChoice, so the bridge answers the state that already means "shown, never
        // rewritten" — silently dropping the value half would retype a user's field.
        ValueChoice back = ValueForm.mapOf(ValueForm.of(TEXT), ValueForm.of(COUNT)).asChoice(false);
        assertFalse(back.type().known());
        assertEquals("java.util.Map<String, Integer>", back.type().id());
        assertEquals(ValueShape.ONE, back.shape());
    }

    @Test
    void aNestedListIsNotFlattenedIntoAList() {
        ValueChoice back = ValueForm.listOf(ValueForm.listOf(ValueForm.of(TEXT))).asChoice(false);
        assertFalse(back.type().known());
    }
}
