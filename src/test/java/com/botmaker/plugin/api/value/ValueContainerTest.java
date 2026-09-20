package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Containers are a contribution, not a fixed list, and nothing about one is a string function.
 *
 * <p>The claim under test is the one the open mechanism makes: a plugin's own composite goes through the
 * same interface, the same registration and the same host code as {@code List}. {@link Either} here is that
 * plugin's container, written in a test with no contract change, and everything asked of the built-in three
 * is asked of it too.
 */
class ValueContainerTest {

    private static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID).label("Text").source("String")
            .build();
    private static final ValueType COUNT = ValueType.of("WHOLE_NUMBER").label("Whole number").source("int")
            .boxed("Integer").primitive().build();

    // ---- a plugin's own container, contributed ------------------------------------------------------------

    record Either<L, R>(L left, R right) {}

    static final class EitherContainer implements ValueContainer<Either<?, ?>> {

        @Override
        public Class<?> type() {
            return Either.class;
        }

        @Override
        public int arity() {
            return 2;
        }

        @Override
        public String factory() {
            return "of";
        }

        @Override
        public List<Object> parts(Either<?, ?> value) {
            return List.of(value.left(), value.right());
        }

        @Override
        public Either<?, ?> build(List<Object> parts) {
            return new Either<>(parts.getFirst(), parts.get(1));
        }

        @Override
        public List<ValueForm> partForms(List<ValueForm> arguments, int parts) {
            return List.copyOf(arguments);
        }
    }

    // ---- the law every container owes ---------------------------------------------------------------------

    @Test
    void aListIsTakenApartAndPutBackTogether() {
        List<?> value = List.of("a", "b", "c");
        assertEquals(3, ValueContainer.LIST.parts(value).size());
        assertEquals(value, ValueContainer.LIST.build(ValueContainer.LIST.parts(value)));
    }

    @Test
    void aMapsPartsAreItsEntries() {
        Map<?, ?> value = Map.of("a", 1);
        List<Object> parts = ValueContainer.MAP.parts(value);
        assertEquals(1, parts.size());
        assertTrue(parts.getFirst() instanceof Map.Entry<?, ?>);
        assertEquals(value, ValueContainer.MAP.build(parts));
    }

    @Test
    void aMapKeepsTheOrderItWasGiven() {
        // Map.ofEntries specifies no iteration order, so building through one would rewrite a generated file
        // with its entries shuffled on some later run, for no change the user made.
        Map<String, Integer> value = new java.util.LinkedHashMap<>();
        value.put("z", 1);
        value.put("a", 2);
        value.put("m", 3);
        List<Object> parts = ValueContainer.MAP.parts(value);
        assertEquals(List.of("z", "a", "m"), parts.stream().map(p -> ((Map.Entry<?, ?>) p).getKey()).toList());
        assertEquals(List.of("z", "a", "m"), List.copyOf(ValueContainer.MAP.build(parts).keySet()));
    }

    @Test
    void anEntryIsAContainerOfItsOwn() {
        Map.Entry<?, ?> value = Map.entry("a", 1);
        assertEquals(List.of("a", 1), ValueContainer.ENTRY.parts(value));
        assertEquals(value, ValueContainer.ENTRY.build(List.of("a", 1)));
    }

    @Test
    void aContributedContainerObeysTheSameLaw() {
        EitherContainer either = new EitherContainer();
        Either<String, Integer> value = new Either<>("a", 1);
        assertEquals(List.of("a", 1), either.parts(value));
        assertEquals(value, either.build(List.of("a", 1)));
    }

    // ---- the syntax the host writes, derived once for every container -------------------------------------

    @Test
    void theFactoryCallIsDerivedNotSupplied() {
        assertEquals("java.util.List.of", ValueContainer.LIST.factorySource());
        assertEquals("java.util.Map.ofEntries", ValueContainer.MAP.factorySource());
        // Map.entry is declared on Map, not on Map.Entry — which is why factoryOwner is separable.
        assertEquals("java.util.Map.entry", ValueContainer.ENTRY.factorySource());
        assertEquals("java.util.Map.Entry", ValueContainer.ENTRY.sourceName());
    }

    @Test
    void aContributedContainerNeedsNoHostChangeToBeWritten() {
        EitherContainer either = new EitherContainer();
        assertEquals("com.botmaker.plugin.api.value.ValueContainerTest.Either", either.id());
        assertEquals("com.botmaker.plugin.api.value.ValueContainerTest.Either.of", either.factorySource());
    }

    // ---- part forms are what keep the recursion typed ------------------------------------------------------

    @Test
    void aListsPartsAreAllItsElementType() {
        List<ValueForm> arguments = List.of(ValueForm.of(TEXT));
        assertEquals(List.of(ValueForm.of(TEXT), ValueForm.of(TEXT), ValueForm.of(TEXT)),
                ValueContainer.LIST.partForms(arguments, 3));
    }

    @Test
    void aMapsPartsAreEntriesOverItsOwnArguments() {
        List<ValueForm> arguments = List.of(ValueForm.of(TEXT), ValueForm.of(COUNT));
        List<ValueForm> parts = ValueContainer.MAP.partForms(arguments, 2);
        assertEquals(2, parts.size());
        assertEquals(new ValueForm.Of(ValueContainer.ENTRY, arguments), parts.getFirst());
        // And an entry's own parts are the key and the value, positionally — so a walk of a map's values
        // reaches String then Integer without ever asking a runtime class what it is.
        assertEquals(arguments, ValueContainer.ENTRY.partForms(arguments, 2));
    }

    // ---- registration --------------------------------------------------------------------------------------

    @Test
    void everyCatalogHasTheThreeTheContractSeeds() {
        ValueCatalog catalog = ValueCatalog.empty();
        assertEquals(List.of("java.util.List", "java.util.Map", "java.util.Map.Entry"),
                catalog.containers().stream().map(ValueContainer::id).toList());
        assertTrue(catalog.containerFor(List.class).isPresent());
        assertTrue(catalog.containerFor(Map.class).isPresent());
        assertTrue(catalog.containerFor(Map.Entry.class).isPresent());
    }

    @Test
    void aPluginRegistersItsOwnBesideThem() {
        ValueCatalog catalog = ValueCatalog.builder().add(new EitherContainer()).build();
        assertEquals(4, catalog.containers().size());
        assertTrue(catalog.containerFor(Either.class).isPresent());
    }

    @Test
    void somethingNoContainerRegistersIsAnOrdinaryAbsence() {
        // A Set, an array, a class nobody contributed. Empty rather than a throw: the host then shows the
        // field's source read-only, which is what it already does for an unknown leaf.
        assertFalse(ValueCatalog.empty().containerFor(java.util.Set.class).isPresent());
        assertFalse(ValueCatalog.empty().container("java.util.Set").isPresent());
    }

    @Test
    void oneBuilderRegisteringAContainerTwiceIsAnError() {
        ValueCatalog.Builder builder = ValueCatalog.builder().add(new EitherContainer());
        assertThrows(IllegalArgumentException.class, () -> builder.add(new EitherContainer()));
        // And the contract's own are already in, so a plugin cannot quietly replace List.
        assertThrows(IllegalArgumentException.class, () -> builder.add(ValueContainer.LIST));
    }

    @Test
    void mergingCarriesContainersAndIsLeftBiased() {
        ValueCatalog mine = ValueCatalog.builder().build();
        ValueCatalog theirs = ValueCatalog.builder().add(new EitherContainer()).build();
        ValueCatalog merged = mine.merge(theirs);
        assertTrue(merged.containerFor(Either.class).isPresent());
        // The seeded three collide on every merge and that is harmless, not a clash to report.
        assertEquals(4, merged.containers().size());
    }
}
